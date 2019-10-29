package com.resonance.cashdisplay;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import java.lang.reflect.Method;
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

    public static final Charset ENCODING_CHARSET = Charset.forName("cp866");//для работы с кассой
    private final static String TAG = "CommandParser";

    private final static byte CMD_START_OF_TEXT = (byte) 1; //старт
    private final static byte CMD_END_OF_TEXT = (byte) 2; //стоп


    //экран "Список покупок"
    private final String CMD_ADDL = "ADDL";//Добавить в перечень товара//ADDL0;1247;0;1;700;700;sik sadochok;5047
    private final String CMD_CLRL = "CLRL";//Очистить перечень товара
    private final String CMD_SETi = "SETi";//Редактировать позицию с индексом в перечне товара
    private final String CMD_DELi = "DELi";//Удалить позицию с индексом в перечне товара
    private final String CMD_TOTL = "TOTL";//Итоговая сумма по экрану "Список покупок"
    private final String CMD_NWRK = "NWRK";//Касса не работает
    private final String CMD_THNK = "THNK";//Спасибо за покупку  THNK95E5
    private final String CMD_UPDATE_SCREEN = "UPDT";//Обновить экран


    private final String[] ArrComands2 = new String[]{CMD_ADDL, CMD_CLRL, CMD_SETi, CMD_DELi, CMD_TOTL, CMD_NWRK, CMD_THNK};
    private final String[] ArrFunc2 = new String[]{"addTovarList", "clearTovarList", "setPositionTovarList",
            "deletePositionTovarList", "totalSumTovarList", "setScreenCacheNotWork", "setScreenThanks"};


    private final int LEN_EXT_BUFFER = 1024 * 4;

    private ProductInfo mProductInfo;
    private static Boolean extHeadFound = false;
    private byte[] extBuf;
    private int indexExtBuf = 0;
    private Handler mHandler;

    private String Uri_IMG_SOU;

    private Context mContext;

    /**
     * Парсер данных с UART
     *
     * @param productInfo привязка к экранным объектам
     * @param handler     хандлер для передачи нанных в главное активити
     * @param context
     * @param Uri_img     путь к хранилищу изображений товаров
     */
    public CommandParser(ProductInfo productInfo, Handler handler, Context context, String Uri_img) {

        this.Uri_IMG_SOU = Uri_img;
        this.mHandler = handler;
        this.mContext = context;
        mProductInfo = productInfo;
        extBuf = new byte[LEN_EXT_BUFFER];

    }

    public void parseInputStr(byte[] arr, int cnt) {
        for (int i = 0; i < cnt; i++) {
            if (indexExtBuf >= LEN_EXT_BUFFER) { //переполнение буфера
                extHeadFound = false;
                indexExtBuf = 0;
            }
            if (arr[i] == CMD_START_OF_TEXT) { //найден символ начала пакета данных
                extHeadFound = true;
                indexExtBuf = 0;
                continue;
            } else if (arr[i] == CMD_END_OF_TEXT) { //найден символ окончания пакета данных
                if (indexExtBuf > 1)
                    doBufferData(extBuf, indexExtBuf);
                extHeadFound = false;
                indexExtBuf = 0;
                continue;
            }
            if (extHeadFound) {//найден заголовок расширенной команды или QR команда для 2 строчного дисплея
                extBuf[indexExtBuf] = (byte) arr[i];//добавляем в буффер
                indexExtBuf++;
                continue;
            }
        }
    }

    private void doBufferData(byte[] buf, int lenBuf) {

        FormatComand formatComand = new FormatComand();
        if (buf.length >= 4) {
            formatComand.command = new String(buf, 0, 4, ENCODING_CHARSET);//команда

        }
        //перешлем на отображение на экран для отладки
        sendToMain(MSG_ADD_PRODUCT_DEBUG, "[" + new String(buf, 0, lenBuf, ENCODING_CHARSET) + "]", 0, 0);

        //Идентификация  протокола Ver 2
        for (int i = 0; i < ArrComands2.length; i++) {

            if (formatComand.command.contains(ArrComands2[i])) {

                byte tmpBuf[] = Arrays.copyOfRange(buf, 0, lenBuf - 4);
                int CRC16_calculated = CountCRC16(tmpBuf, tmpBuf.length);

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

                    formatComand.params = new String(buf, 4, lenBuf - 4, ENCODING_CHARSET);

                    Method defFunc = CommandParser.class.getMethod(ArrFunc2[i], String.class);
                    defFunc.invoke(this, formatComand.params);
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка парсера(v2) :  " + e + " >>" + new String(buf, 0, lenBuf));
                    e.printStackTrace();
                }
                break;
            }
        }
    }



    /*обработчики  для экрана "Список покупок"*/

    /**
     * Добавить товар в список
     *
     * @param param
     */
    public void addTovarList(String param) {
        Log.d(TAG, "addTovarList :" + param);
        sendToMain(MainActivity.MSG_ADD_TOVAR_SHOPPING_LIST, param, MainActivity.CONTEXT_SHOPPING_LIST, 0);
    }

    /**
     * Очистить список товаров
     *
     * @param param
     */
    public void clearTovarList(String param) {
        sendToMain(MainActivity.MSG_CLEAR_SHOPPING_LIST, param, MainActivity.CONTEXT_SHOPPING_LIST, 0);
        Log.d(TAG, "clearTovarList :" + param);

    }

    /**
     * Установить товар в указанной позиции списка
     *
     * @param param
     */
    public void setPositionTovarList(String param) {
        sendToMain(MainActivity.MSG_SET_TOVAR_SHOPPING_LIST, param, MainActivity.CONTEXT_SHOPPING_LIST, 0);
        Log.d(TAG, "setPositionTovarList :" + param);
    }

    /**
     * Удалить товар из указанной позиции
     *
     * @param param
     */
    public void deletePositionTovarList(String param) {
        sendToMain(MainActivity.MSG_DEL_TOVAR_SHOPPING_LIST, param, MainActivity.CONTEXT_SHOPPING_LIST, 0);
        Log.d(TAG, "deletePositionTovarList :" + param);
    }

    /**
     * Указать общую сумму по товарам
     *
     * @param param
     */
    public void totalSumTovarList(String param) {
        Log.d(TAG, "totalSumTovarList :" + param);
        sendToMain(MainActivity.MSG_TOTAL_SUMM_SHOPPING_LIST, param, MainActivity.CONTEXT_SHOPPING_LIST, 0);
    }

    /**
     * Отобразить экран "Касса не работает"
     *
     * @param param
     */
    public void setScreenCacheNotWork(String param) {
        Log.d(TAG, "setScreenCacheNotWork :" + param);
        sendToMain(MainActivity.MSG_SET_SCREEN_NOT_WORK, param, MainActivity.CONTEXT_CONNECT, 0);
    }

    /**
     * Отобразить экран "Спасибо за покупку"
     *
     * @param param
     */
    public void setScreenThanks(String param) {
        Log.d(TAG, "setScreenThanks :" + param);
        sendToMain(MainActivity.MSG_SET_SCREEN_THANKS, param, MainActivity.CONTEXT_THANKS, 0);
    }


    public static int CountCRC16(byte[] buf, int len) {
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
    private void sendToMain(int typeParam, Object obj, int arg1, int arg2) {
        Message msg = new Message();
        msg.what = typeParam;
        msg.obj = obj;
        msg.arg1 = arg1;
        msg.arg2 = arg2;
        mHandler.sendMessage(msg);
    }

    /****************************************************************************************************/
    public class FormatComand {

        public String command;
        public String params;
        public int Lenth;
        public int CRC16;
    }
}
