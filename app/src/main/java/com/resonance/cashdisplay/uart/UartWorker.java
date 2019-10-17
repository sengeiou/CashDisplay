package com.resonance.cashdisplay.uart;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
//import android.util.Log;

import com.resonance.cashdisplay.Log;
import com.resonance.cashdisplay.MainActivity;
import com.resonance.cashdisplay.PreferenceParams;
//import com.resonance.cashdisplay.SerialPortFinder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    public static String[] SERIAL_PORT_ARR = new String[]{"/dev/ttySAC1","/dev/ttySAC2"};

    public static String SERIAL_PORT = SERIAL_PORT_ARR[0];
    private static int DATA_BAUDRATE = 9600;


    Handler mHandler;
    private SerialPort serialPort;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private ReadThread mReadThread;


    public UartWorker(Handler handler)
    {
        mHandler = handler;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UART_CHANGE_SETTINGS);
        MainActivity.mContext.registerReceiver(uartChangeSettings,intentFilter);
    }

    /**
     * Открытие порта
     * @param dev           "/dev/ttySAC1","/dev/ttySAC2"
     * @param baudrate      скорость
     * @param flags
     * @return
     */
    public int OpenSerialPort(String dev,int baudrate, int flags)
    {
        SERIAL_PORT = (!dev.isEmpty()?dev:SERIAL_PORT);
        DATA_BAUDRATE = (baudrate!=0?baudrate:DATA_BAUDRATE);

            //инит порта
        try {
            serialPort = new SerialPort(new File(SERIAL_PORT), DATA_BAUDRATE, flags);

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

            if (mReadThread!=null)
                mReadThread.interrupt();

            mReadThread = new ReadThread();
            mReadThread.start();
            Log.d(TAG, "Uart: " + SERIAL_PORT + ", BAUDRATE:"+DATA_BAUDRATE+",  init successfully");
            SendMsgToParent(ACTION_UART_OPEN, "",  0);
            return 0;
        }
        Log.d(TAG, "Init uart: " + dev + " - ERROR");
       return 4;

    }

    /**
     * Закрытие порта
     */
    public void CloseSerialPort() {
        if (serialPort != null) {
            mReadThread.interrupt();
            serialPort.close();
            serialPort = null;
            Log.w(TAG, "SerialPort CLOSE");
            SendMsgToParent(ACTION_UART_CLOSED, "",  0);
        }
    }

    /**
     * Запись данных в порт
     * @param data
     */
    public void SendData(String data)
    {
        if (serialPort != null) {
            if (mOutputStream != null){
                try {
                    mOutputStream.write(data.getBytes(), 0, data.length());
                }catch (IOException e){
                    SendMsgToParent(ACTION_UART_ERROR, "",  0);
                    Log.e(TAG,"SendData, IOException :"+e.getMessage());
                };
            }
        }
    }

    /**
     * Передача сообщения на главное окно
     * @param msgAction
     * @param data
     * @param size
     */
    private void SendMsgToParent(int msgAction, Object data, int size)
    {
        if (mHandler != null)
            mHandler.obtainMessage(msgAction,1,size, data).sendToTarget();
    }

    /*********************************************************************************************/

    /**
     * Поток читает данные из UART и транслирует на главное активити
     */
    private class ReadThread extends Thread {

        @Override
        public void run() {
            super.run();


            while (!isInterrupted()) {
                int size=0;
                byte[] buffer;
                if (serialPort != null) {
                    try {
                        if (mInputStream == null) {

                            SendMsgToParent(ACTION_UART_ERROR, null,  0);
                            this.interrupt();
                            return;
                        }
                        if (mInputStream.available() > 0) {
                            buffer = new byte[mInputStream.available()];
                            for (int i=0;i<buffer.length;i++)
                                buffer[i]=0;
                            size = mInputStream.read(buffer);
                            if (size > 0)
                            {
                                if (mHandler != null)
                                 mHandler.obtainMessage(ACTION_UART_READ,1,size,(byte[])buffer).sendToTarget();
                            }
                        }
                    } catch(IOException e){
                        e.printStackTrace();
                        SendMsgToParent(ACTION_UART_ERROR, null,  0);
                        return;
                    }
                }
            }
            Log.w("SSS", "ReadThread  CLOSED");
        }
    }

    /**
     * Приемник сообщений об изменении параметров настройки порта
     */
    BroadcastReceiver uartChangeSettings = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "BroadcastReceiver [uartChangeSettings]");

            if (intent.getAction().equals(UART_CHANGE_SETTINGS))
            {
                String newConnection = getCoreNameUart(PreferenceParams.getParameters().sUartName);
                if (!newConnection.equals(SERIAL_PORT))
                {
                    CloseSerialPort();
                    SERIAL_PORT = newConnection;
                    OpenSerialPort(SERIAL_PORT, 0, 0);
                }
            }
        };
    };

    /**
     * Проверка соответствия возможно доступных портов
     * @param AppNameUart
     * @return
     */
    public static String getCoreNameUart(String AppNameUart){

        String result = SERIAL_PORT_ARR[0];
        int iPort = -1;
        for (int i=0;i<SERIAL_PORT_ARR.length;i++)
        {
            if (AppNameUart.equals(DEF_UARTS[i])){
                iPort = i;
                break;
            }
        }
        if (iPort>=0){
            result =SERIAL_PORT_ARR[iPort];
        }
        return result;
    }



}
