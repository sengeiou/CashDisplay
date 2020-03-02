package com.resonance.cashdisplay;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.Arrays;

import static com.resonance.cashdisplay.MainActivity.MSG_ADD_PRODUCT_DEBUG;

/**
 * Created by Святослав on 19.04.2016.
 */

/**
 * Класс обрабатывает данные приходящие с UART
 * Обеспечена командная совместимость с двустрочным индикатором
 */
public class CommandParser {

    public static final Charset ENCODING_CHARSET = Charset.forName("cp866"); //для работы с кассой
    private final static String TAG = "CommandParser";

    private static final byte CMD_START_OF_TEXT = (byte) 1; //старт
    private static final byte CMD_END_OF_TEXT = (byte) 2;   //стоп
    public static final byte SYMBOL_SEPARATOR = (byte) 0x03;

    //экран "Список покупок"
    public static final String CMD_ADDL = "ADDL"; // Добавить товар в список  // ADDL0;1247;0;1;700;700;sik sadochok;5047
    public static final String CMD_SETi = "SETi"; // Установить товар в указанной позиции списка
    public static final String CMD_DELi = "DELi"; // Удалить товар из указанной позиции
    public static final String CMD_CLRL = "CLRL"; // Очистить список товаров
    public static final String CMD_PRLS = "PRLS"; // Отобразить экран "Список покупок"
    public static final String CMD_NWRK = "NWRK"; // Отобразить экран "Касса не работает"
    public static final String CMD_THNK = "THNK"; // Отобразить экран "Спасибо за покупку"  THNK95E5

    private final String[] arrComands2 = new String[]{CMD_ADDL, CMD_SETi, CMD_DELi, CMD_CLRL, CMD_PRLS, CMD_NWRK, CMD_THNK};

    private final int LEN_EXT_BUFFER = 1024 * 4;

    private ViewModel viewModel;
    private static Boolean extHeadFound = false;
    private byte[] extBuf;
    private int indexExtBuf = 0;
    private static Handler mHandler;

    private Display2x20Emulator display2x20Emulator;

    private Context mContext;

    /**
     * Парсер данных с UART
     *
     * @param viewModel привязка к экранным объектам
     * @param handler   хандлер для передачи нанных в главное активити
     * @param context
     */
    public CommandParser(ViewModel viewModel, Handler handler, Context context) {
        mHandler = handler;
        this.mContext = context;
        this.viewModel = viewModel;
        this.display2x20Emulator = new Display2x20Emulator();
        extBuf = new byte[LEN_EXT_BUFFER];
    }

    public void parseInputStr(byte[] arr, int cnt) {
        for (int i = 0; i < cnt; i++) {
            if (indexExtBuf >= LEN_EXT_BUFFER) {       // переполнение буфера
                extHeadFound = false;
                indexExtBuf = 0;
            }
            if (arr[i] == CMD_START_OF_TEXT) {         // найден символ начала пакета данных
                extHeadFound = true;
                indexExtBuf = 0;
                continue;
            } else if (arr[i] == CMD_END_OF_TEXT) {    // найден символ окончания пакета данных
                if (indexExtBuf > 1)
                    doBufferData(extBuf, indexExtBuf);
                extHeadFound = false;
                indexExtBuf = 0;
                continue;
            }
            if (extHeadFound) {   //найден заголовок расширенной команды или QR команда для 2 строчного дисплея
                extBuf[indexExtBuf] = (byte) arr[i];   // добавляем в буффер
                indexExtBuf++;
                continue;
            }
        }

        if (MainActivity.testMode) {
            for (int i = 0; i < cnt; i++) {        // parse data for 2x20 display (aimed for data from EKKR autonomic mode and ResPOS indicator test)
                switch (arr[i]) {
                    case 0x0B:                    // EKKR aimed (byte not appears from ResPOS)
                        if (display2x20Emulator.lineDetectCounter == 3) {    // 1-st line
                            display2x20Emulator.startNewLine(1);
                            display2x20Emulator.isEkkrData = true;
                        }
                        display2x20Emulator.lineDetectCounter++;
                        break;
                    case 0x0A:                    // EKKR aimed (byte not appears from ResPOS)
                        if (display2x20Emulator.lineDetectCounter == 3) {    // 2-nd line
                            display2x20Emulator.startNewLine(2);
                            display2x20Emulator.isEkkrData = true;
                        }
                        break;
                    case 0x40:                    // ResPOS Terminal aimed
                        if (!display2x20Emulator.isEkkrData && (arr[i + 1] != 0x1B)) {
                            display2x20Emulator.addToLineBuffer(arr[i]);
                            break;
                        }
                    case 0x1B:                    // ResPOS Terminal aimed
                    case 0x52:                    // ResPOS Terminal aimed
                        display2x20Emulator.resposDetectCounter++;
                        break;
                    case 0x0C:                    // ResPOS Terminal aimed (ALSO appears from EKKR)
                        if (display2x20Emulator.resposDetectCounter == 4) {
                            display2x20Emulator.startNewLine(1);
                            display2x20Emulator.isEkkrData = false;
                            display2x20Emulator.resposDetectCounter = 0;
                            break;
                        }
                    default:
                        display2x20Emulator.addToLineBuffer(arr[i]);

                        if (!display2x20Emulator.isEkkrData)
                            if (display2x20Emulator.bufferCursor == 0)
                                if (display2x20Emulator.lineNumber == 1)
                                    display2x20Emulator.startNewLine(2);
                                else display2x20Emulator.startNewLine(1);
                        break;
                }
            }
        }
    }

    private void doBufferData(byte[] buf, int lenBuf) {

        FormatCommand formatCommand = new FormatCommand();
        if (buf.length >= 4) {
            formatCommand.command = new String(buf, 0, 4, ENCODING_CHARSET);//команда
        }
        // перешлем на отображение на экран для отладки
        sendToMain(MSG_ADD_PRODUCT_DEBUG, "[" + new String(buf, 0, lenBuf, ENCODING_CHARSET) + "]", 0, 0);

        // Идентификация протокола Ver 2
        for (int i = 0; i < arrComands2.length; i++) {
            if (formatCommand.command.contains(arrComands2[i])) {
                byte tmpBuf[] = Arrays.copyOfRange(buf, 0, lenBuf - 4);
                int CRC16_calculated = countCRC16(tmpBuf, tmpBuf.length);
                try {
                    String crc_str = new String(buf, lenBuf - 4, 4, ENCODING_CHARSET);
                    int crc = Integer.parseInt(crc_str, 16);
                    if (crc != CRC16_calculated) {
                        Log.e(TAG, "Ошибка CRC, расчетный:  " + CRC16_calculated + ", а указанный: " + crc + " >>" + new String(buf, 0, lenBuf));
                        sendToMain(MSG_ADD_PRODUCT_DEBUG, "*** Ошибка CRC, расчетная: " + CRC16_calculated + ", а указанный: " + crc, 0, 0);
                        Toast myToast = Toast.makeText(mContext.getApplicationContext(), "Ошибка CRC, расчетный:  " + CRC16_calculated + ", а указанный: " + crc, Toast.LENGTH_LONG);
                        myToast.show();
                        return;
                    }
                    formatCommand.params = new String(buf, 4, lenBuf - 4 - 4, ENCODING_CHARSET);
                    handleCommand(formatCommand.command, formatCommand.params);
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка парсера(v2) :  " + e + " >>" + new String(buf, 0, lenBuf));
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    /**
     * Prepares data to send message to handler of screen "Список покупок"
     */
    public static void handleCommand(String command, String arg) {
        switch (command) {
            case CMD_ADDL:
                sendToMain(MainActivity.MSG_ADD_TOVAR_PRODUCT_LIST, arg, 0, 0);
                Log.d("CMD", "ADDL: " + arg);
                break;
            case CMD_SETi:
                sendToMain(MainActivity.MSG_SET_TOVAR_PRODUCT_LIST, arg, 0, 0);
                Log.d("CMD", "SETi: " + arg);
                break;
            case CMD_DELi:
                sendToMain(MainActivity.MSG_DEL_TOVAR_PRODUCT_LIST, arg, 0, 0);
                Log.d("CMD", "DELi: " + arg);
                break;
            case CMD_CLRL:
                sendToMain(MainActivity.MSG_CLEAR_PRODUCT_LIST, arg, 0, 0);
                Log.d("CMD", "CLRL: " + arg);
                break;
            case CMD_PRLS:
                sendToMain(MainActivity.MSG_SET_SCREEN_PRODUCT_LIST, arg, 0, 0);
                Log.d("CMD", "PRLS: " + arg);
                break;
            case CMD_NWRK:
                sendToMain(MainActivity.MSG_SET_SCREEN_NOT_WORK, arg, 0, 0);
                Log.d("CMD", "NWRK: " + arg);
                break;
            case CMD_THNK:
                sendToMain(MainActivity.MSG_SET_SCREEN_THANKS, arg, 0, 0);
                Log.d("CMD", "THNK: " + arg);
                break;
            default:
                break;
        }
    }

    private int countCRC16(byte[] buf, int len) {
        int crc = 0xFFFF;

        for (int pos = 0; pos < len; pos++) {
            crc ^= (int) buf[pos] & 0xFF;

            for (int i = 8; i != 0; i--) {
                if ((crc & 0x0001) != 0) {
                    crc >>= 1;
                    crc ^= 0xA001;
                } else
                    crc >>= 1;
            }
        }
        return (crc);
    }

    /**
     * передача параметров на главное активити
     */
    private static void sendToMain(int typeParam, Object obj, int arg1, int arg2) {
        Message msg = new Message();
        msg.what = typeParam;
        msg.obj = obj;
        msg.arg1 = arg1;
        msg.arg2 = arg2;
        mHandler.sendMessage(msg);
    }

    /****************************************************************************************************/
    private class FormatCommand {
        private String command;
        private String params;
    }

    /**
     * Class for display 2x20 emulation (primarily, for testing of UART, getting data from EKKR or from ResPOS)
     */
    private class Display2x20Emulator {
        private int charInLineAmount = 20;
        private int lineNumber;
        private int lineDetectCounter = 0;
        private boolean isEkkrData = true;
        private int resposDetectCounter = 0;
        private int bufferCursor = 0;
        private byte[] buffer = new byte[20];

        public Display2x20Emulator() {
            viewModel.setLine1("");
            viewModel.setLine2("");
        }

        private void startNewLine(int number) {
            lineNumber = number;
            bufferCursor = 0;
        }

        private void addToLineBuffer(byte b) {
            buffer[bufferCursor] = b;
            bufferCursor++;
            if (bufferCursor == charInLineAmount) {
                sendToDisplay();
                bufferCursor = 0;
                lineDetectCounter = 0;
            }
        }

        private void sendToDisplay() {
            String data = new String(buffer, 0, bufferCursor, ENCODING_CHARSET);
            if (lineNumber == 1)
                viewModel.setLine1(data);
            if (lineNumber == 2)
                viewModel.setLine2(data);
        }
    }
}
