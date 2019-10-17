package com.resonance.cashdisplay;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import android.widget.Toast;


import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.resonance.cashdisplay.load.DownloadMedia;
import com.resonance.cashdisplay.uart.UartWorker;
import com.resonance.cashdisplay.utils.ImageUtils;


import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Arrays;

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

    private final static byte CMD_START_OF_TEXT = (byte)1; //старт
    private final static byte CMD_END_OF_TEXT = (byte)2; //стоп


    //экран "Список покупок"
    private final  String CMD_ADDL = "ADDL";//Добавить в перечень товара//ADDL0;1247;0;1;700;700;sik sadochok;5047
    private final  String CMD_CLRL = "CLRL";//Очистить перечень товара
    private final  String CMD_SETi = "SETi";//Редактировать позицию с индексом в перечне товара
    private final  String CMD_DELi = "DELi";//Удалить позицию с индексом в перечне товара
    private final  String CMD_TOTL = "TOTL";//Итоговая сумма по экрану "Список покупок"
    private final  String CMD_NWRK = "NWRK";//Касса не работает
    private final  String CMD_THNK = "THNK";//Спасибо за покупку  THNK95E5
    private final  String CMD_UPDATE_SCREEN = "UPDT";//Обновить экран


    private final  String[] ArrComands2 = new String[]{CMD_ADDL, CMD_CLRL, CMD_SETi, CMD_DELi, CMD_TOTL, CMD_NWRK, CMD_THNK};
    private final  String[] ArrFunc2 =new String[]{"Add_TovarList", "Clear_TovarList", "SetPosition_TovarList", "DeletePosition_TovarList", "TotalSumm_TovarList",
                                                    "SetScreenCacheNotWork", "SetScreenThanks"};


    private final  int LEN_EXT_BUFFER = 1024*4;

    private ProductInfo mProductInfo;
    private static Boolean ExtHeadFound = false;
    private byte[] ExtBuf;
    private int IndexExtBuf = 0;
    private Handler mHandler;

    private String Uri_IMG_SOU;

    private Context mContext;

    /**
     * Парсер данных с UART
     * @param productInfo  привязка к экранным объектам
     * @param handler      хандлер для передачи нанных в главное активити
     * @param context
     * @param Uri_img      путь к хранилищу изображений товаров
     */
    public CommandParser( ProductInfo productInfo,Handler handler, Context context, String Uri_img) {

        this.Uri_IMG_SOU = Uri_img;
        this.mHandler = handler;
        this.mContext = context;
        mProductInfo = productInfo;
        ExtBuf = new byte[LEN_EXT_BUFFER];

    }

    public void ParseInputStr(byte[] arr, int Cnt)
    {
        for (int i = 0; i < Cnt; i++)
        {
            if (IndexExtBuf>=LEN_EXT_BUFFER)//переполнение буфера
            {
                ExtHeadFound = false;
                IndexExtBuf = 0;
            }

            if (arr[i] == CMD_START_OF_TEXT) {//найден символ начала пакета данных
                ExtHeadFound = true;
                IndexExtBuf = 0;
                continue;
            } else if (arr[i] == CMD_END_OF_TEXT) {//найден символ окончания пакета данных
                if (IndexExtBuf>1)
                    DoBufferData(ExtBuf, IndexExtBuf);
                ExtHeadFound = false;
                IndexExtBuf = 0;
                continue;
            }
            if (ExtHeadFound) {//найден заголовок расширенной команды или QR команда для 2 строчного дисплея
                ExtBuf[IndexExtBuf] = (byte) arr[i];//добавляем в буффер

                IndexExtBuf++;
                continue;
            }
        }

    }



    private void DoBufferData(byte[] Buf, int lenBuf) {

        FormatComand formatComand = new FormatComand();
        if (Buf.length>=4) {
            formatComand.Comand = new String(Buf, 0, 4,ENCODING_CHARSET);//команда

        }
        //перешлем на отображение на экран для отладки
        SendToMain(1234, "["+new String(Buf, 0, lenBuf ,ENCODING_CHARSET)+"]", 0, 0);

        //Идентификация  протокола Ver 2
        for (int i = 0; i < ArrComands2.length; i++) {

            if (formatComand.Comand.contains(ArrComands2[i]))
            {

                byte tmpBuf[] = Arrays.copyOfRange(Buf, 0, lenBuf - 4);
                int CRC16_calculated = CountCRC16(tmpBuf, tmpBuf.length);

                try {
                    String crc_str = new String(Buf,lenBuf-4,4,ENCODING_CHARSET);
                    int crc = Integer.parseInt(crc_str,16);
                    if (crc != CRC16_calculated) {
                        Log.e(TAG, "Ошибка CRC, расчетный:  " + CRC16_calculated + ", а указанный: " + crc + " >>" + new String(Buf, 0, lenBuf));
                        SendToMain(1234, "*** Ошибка CRC, расчетная: " + CRC16_calculated + ", а указанный: " + crc , 0, 0);
                        Toast myToast = Toast.makeText(mContext.getApplicationContext(),"Ошибка CRC, расчетный:  " + CRC16_calculated + ", а указанный: " + crc, Toast.LENGTH_LONG);
                        myToast.show();
                            return;
                    }


                 formatComand.Params = new String(Buf, 4, lenBuf - 4,ENCODING_CHARSET);


                 Method defFunc = CommandParser.class.getMethod(ArrFunc2[i],String.class);
                 defFunc.invoke(this ,formatComand.Params);
                }catch(Exception e)
                {
                    Log.e(TAG, "Ошибка парсера(v2) :  " + e+ " >>" + new String(Buf, 0, lenBuf));
                };
                break;
            }
        }


    }






    /*обработчики  для экрана "Список покупок"*/

    /**
     * Добавить товар в список
     * @param param
     */
    public void Add_TovarList(String param){
        Log.d(TAG, "Add_TovarList :"+param);
        SendToMain(MainActivity.MSG_ADD_TOVAR_SHOPPING_LIST, param, MainActivity.CONTEXT_SHOPPING_LIST, 0);
    }

    /**
     * Очистить список товаров
     * @param param
     */
    public void Clear_TovarList(String param){
        SendToMain(MainActivity.MSG_CLEAR_SHOPPING_LIST, param, MainActivity.CONTEXT_SHOPPING_LIST, 0);
        Log.d(TAG, "Clear_TovarList :"+param);

    }

    /**
     * Установить товар в указанной позиции списка
     * @param param
     */
    public void SetPosition_TovarList(String param){
        SendToMain(MainActivity.MSG_SET_TOVAR_SHOPPING_LIST, param, MainActivity.CONTEXT_SHOPPING_LIST, 0);
        Log.d(TAG, "SetPosition_TovarList :"+param);

    }

    /**
     * Удалить товар из указанной позиции
     * @param param
     */
    public void DeletePosition_TovarList(String param){
        SendToMain(MainActivity.MSG_DEL_TOVAR_SHOPPING_LIST, param, MainActivity.CONTEXT_SHOPPING_LIST, 0);
        Log.d(TAG, "DeletePosition_TovarList :"+param);
    }

    /**
     * Указать общую сумму по товарам
     * @param param
     */
    public void TotalSumm_TovarList(String param)
    {
        Log.d(TAG, "TotalSumm_TovarList :"+param);
        SendToMain(MainActivity.MSG_TOTAL_SUMM_SHOPPING_LIST, param, MainActivity.CONTEXT_SHOPPING_LIST, 0);
    }

    /**
     * Отобразить экран "Касса не работает"
     * @param param
     */
    public void SetScreenCacheNotWork(String param)
    {
        Log.d(TAG, "SetScreenCacheNotWork :"+param);
        SendToMain(MainActivity.MSG_SET_SCREEN_NOT_WORK, param, MainActivity.CONTEXT_CONNECT, 0);
    }

    /**
     * Отобразить экран "Спасибо за покупку"
     * @param param
     */
    public void SetScreenThanks(String param)
    {
        Log.d(TAG, "SetScreenThanks :"+param);
        SendToMain(MainActivity.MSG_SET_SCREEN_THANKS, param, MainActivity.CONTEXT_THANKS, 0);
    }




    public static int CountCRC16(byte[] buf, int len)
    {
        int crc = 0xFFFF;

        for (int pos = 0; pos < len; pos++) {
            crc ^= (int)buf[pos] & 0xFF;

            for (int i = 8; i != 0; i--) {
                if ((crc & 0x0001) != 0) {
                    crc >>= 1;
                    crc ^= 0xA001;
                }
                else
                    crc >>= 1;
            }
        }
        return (crc);
    }

    /**
     * передача параметров на главное активити
     * @param TypeParam
     * @param obj
     * @param arg1
     * @param arg2
     */
       private void SendToMain(int TypeParam, Object obj, int arg1,int arg2) {
           Message msg = new Message();
           msg.what = TypeParam;
           msg.obj = obj;
           msg.arg1 = arg1;
           msg.arg2 = arg2;
           mHandler.sendMessage(msg);
       }



   /****************************************************************************************************/
    public class FormatComand {

        public String Comand;
        public String Params;
        public int Lenth;
        public int CRC16;
    }


}
