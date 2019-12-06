package com.resonance.cashdisplay.uart;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;

import com.crashlytics.android.Crashlytics;
import com.resonance.cashdisplay.Log;
import com.resonance.cashdisplay.MainActivity;
import com.resonance.cashdisplay.PreferenceParams;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.resonance.cashdisplay.PreferenceParams.DEF_UARTS;

/**
 * Класс обработчик данных от USART
 */
public class UartWorker {

    public static final int ACTION_UART_OPEN = 0;
    public static final int ACTION_UART_CLOSED = 1;
    public static final int ACTION_UART_READ = 2;
    public static final int ACTION_UART_ERROR = 3;

    public static String UART_CHANGE_SETTINGS = "uart_change_settings";

    private static final String TAG = "UART";
    public static String[] SERIAL_PORT_ARR = new String[]{"/dev/ttySAC1", "/dev/ttySAC2"};

    public static String SERIAL_PORT = SERIAL_PORT_ARR[0];
    private static int DATA_BAUDRATE = 9600;

    private Handler mHandler;
    private SerialPort serialPort;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private ReadThread mReadThread;

    public UartWorker(Handler handler) {
        mHandler = handler;
        IntentFilter intentFilter = new IntentFilter(UART_CHANGE_SETTINGS);
        MainActivity.context.registerReceiver(uartChangeSettings, intentFilter);
    }

    /**
     * Открытие порта
     *
     * @param dev      "/dev/ttySAC1","/dev/ttySAC2"
     * @param baudrate скорость
     * @param flags
     * @return
     */
    public int openSerialPort(String dev, int baudrate, int flags) {
        SERIAL_PORT = (!dev.isEmpty() ? dev : SERIAL_PORT);
        DATA_BAUDRATE = (baudrate != 0 ? baudrate : DATA_BAUDRATE);

        // инит порта
        try {
            serialPort = SerialPort
                    .newBuilder(new File(SERIAL_PORT), DATA_BAUDRATE) // Serial address, baud rate
                    .parity(0) // Parity bit; 0: No parity bit (NONE, default); 1: Odd parity bit (ODD); 2: Even parity bit (EVEN)
                    .dataBits(8) // Data bits, default 8; optional values are 5 ~ 8
                    .stopBits(1) // Stop bit, default 1; 1: 1 stop bit; 2: 2 stop bit
                    .build();

        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException: " + e.getMessage());
            return 2;
        } catch (IOException e) {
            Log.e(TAG, "Init uart IOException: " + e.getMessage() + "\n");
            return 3;
        }

        if (serialPort != null) {
            mOutputStream = serialPort.getOutputStream();
            mInputStream = serialPort.getInputStream();

            if (mReadThread != null)
                mReadThread.interrupt();

            mReadThread = new ReadThread();
            mReadThread.start();
            Log.d(TAG, "Uart: " + SERIAL_PORT + ", BAUDRATE:" + DATA_BAUDRATE + ",  init successfully");
            sendMsgToParent(ACTION_UART_OPEN, "", 0);
            return 0;
        }
        Log.d(TAG, "Init uart: " + dev + " - ERROR");
        return 4;
    }

    /**
     * Закрытие порта
     */
    public void closeSerialPort() {
        if (mReadThread != null) mReadThread.interrupt();
        if (serialPort != null) {
            serialPort.close();
            serialPort = null;
            Log.w(TAG, "SerialPort CLOSE");
            sendMsgToParent(ACTION_UART_CLOSED, "", 0);
        }
    }

    /**
     * Запись данных в порт
     *
     * @param data
     */
    public void sendData(String data) {
        if (serialPort != null) {
            if (mOutputStream != null) {
                try {
                    mOutputStream.write(data.getBytes(), 0, data.length());
                } catch (IOException e) {
                    sendMsgToParent(ACTION_UART_ERROR, "", 0);
                    Log.e(TAG, "SendData, IOException :" + e.getMessage());
                }
            }
        }
    }

    /**
     * Передача сообщения на главное окно
     *
     * @param msgAction
     * @param data
     * @param size
     */
    private void sendMsgToParent(int msgAction, Object data, int size) {
        if (mHandler != null)
            mHandler.obtainMessage(msgAction, 1, size, data).sendToTarget();
    }

    /*********************************************************************************************/

    /**
     * Поток читает данные из UART и транслирует на главное активити
     */
    private class ReadThread extends Thread {

        @Override
        public void run() {

            int size;
            while (!isInterrupted()) {
                try {
                    if (mInputStream == null || serialPort == null) {
                        Log.e(TAG, "SerialPort or it's mInputStream became null unexpectedly");
                        sendMsgToParent(ACTION_UART_ERROR, null, 0);
                        return;
                    }
                    byte[] buffer = new byte[64];
                    size = mInputStream.read(buffer);
                    if (size > 0) {
                        if (mHandler != null)
                            mHandler.obtainMessage(ACTION_UART_READ, 1, size, buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    sendMsgToParent(ACTION_UART_ERROR, null, 0);
                    Crashlytics.logException(e);
                    return;
                }
            }
            Log.w(TAG, "ReadThread  CLOSED");
        }
    }

    /**
     * Приемник сообщений об изменении параметров настройки порта
     */
    public BroadcastReceiver uartChangeSettings = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "BroadcastReceiver [uartChangeSettings]");

            if (intent.getAction().equals(UART_CHANGE_SETTINGS)) {
                String newConnection = getCoreNameUart(PreferenceParams.getParameters().uartName);
                if (!newConnection.equals(SERIAL_PORT)) {
                    closeSerialPort();
                    SERIAL_PORT = newConnection;
                    openSerialPort(SERIAL_PORT, 0, 0);
                }
            }
        }
    };

    /**
     * Проверка соответствия возможно доступных портов
     *
     * @param appNameUart
     * @return
     */
    public static String getCoreNameUart(String appNameUart) {

        String result = SERIAL_PORT_ARR[0];
        int iPort = -1;
        for (int i = 0; i < SERIAL_PORT_ARR.length; i++) {
            if (appNameUart.equals(DEF_UARTS[i])) {
                iPort = i;
                break;
            }
        }
        if (iPort >= 0) {
            result = SERIAL_PORT_ARR[iPort];
        }
        return result;
    }
}
