package com.resonance.cashdisplay;

import android.content.SharedPreferences;
//import android.util.Log;

import java.util.concurrent.BlockingQueue;

import static com.resonance.cashdisplay.uart.UartWorker.SERIAL_PORT_ARR;

/**
 * Created by Святослав on 11.05.2016.
 */
public class PreferenceParams {


    private static final String SERIALIZER_KEY = "ce.serilizer";
    public static int _SMB1 = 0;
    public static int _SMB2 = 1;
    public static int _FTP = 2;

    public static int _VIDEO = 0;
    public static int _SLIDE = 1;


    public static final int MIN_VIDEO_TIMEOUT = 10;
    public static final int MIN_SLIDE_TIME_SHOW = 1;
    public static final String[] DEF_PROTOCOL = new String[]{"SMB1","SMB2","FTP"};
    public static final String[] DEF_UARTS = new String[]{"EKKR","PC"};


    public PreferenceParams() {
       // sharedPreferences = MainActivity.mContext.getSharedPreferences(SERIALIZER_KEY, MainActivity.mContext.MODE_PRIVATE);
    }

    public  synchronized static PreferencesValues getParameters() {
        SharedPreferences sharedPreferences = MainActivity.mContext.getSharedPreferences(SERIALIZER_KEY, MainActivity.mContext.MODE_PRIVATE);
        PreferencesValues params = new PreferencesValues();


        params.sUartName = sharedPreferences.getString("sUartName", DEF_UARTS[0]);
        params.sSmbHost = sharedPreferences.getString("sSmbHost", "server");
        params.sSmbImg = sharedPreferences.getString("sSmbImg", "/indi10/Img/");
        params.sSmbVideo = sharedPreferences.getString("sSmbVideo", "/indi10/Video/");
        params.sSmbSlide = sharedPreferences.getString("sSmbSlide", "/indi10/Slide/");
        params.sUser = sharedPreferences.getString("sUser", "indi10");
        params.sPassw = sharedPreferences.getString("sPassw", "20671");
        params.sCheckEnableVideo = sharedPreferences.getBoolean("sCheckEnableVideo", false);
        params.videoTimeout = sharedPreferences.getLong("svideoTimeout", 20);
        params.sAdmin = sharedPreferences.getString("sAdmin", "admin");
        params.sAdminPassw = sharedPreferences.getString("sAdminPassw", "admin");
        params.sDownloadAtStart = sharedPreferences.getBoolean("sDownloadAtStart", false);
        params.sPercentVolume = sharedPreferences.getInt("sPercentVolume", 50);

        params.sIP = sharedPreferences.getString("sIP", "192.168.1.200");
        params.sMask = sharedPreferences.getString("sMask", "255.255.255.0");
        params.sGW = sharedPreferences.getString("sGW", "192.168.1.1");
        params.sDNS = sharedPreferences.getString("sDNS", "8.8.8.8");
        params.sDHCP = sharedPreferences.getBoolean("sDHCP", true);
        params.sProtocol = sharedPreferences.getString("sProtocol", DEF_PROTOCOL[_SMB1]);
        params.sDefaultBackGroundImage = sharedPreferences.getString("sDefaultBackGroundImage", "default background picture.png");
        params.sShowNavigationBar = sharedPreferences.getBoolean("sShowNavigationBar", false);
        params.sTimeSlideImage = sharedPreferences.getInt("sTimeSlideImage", 10);
        params.sVideoOrSlide = sharedPreferences.getInt("sVideoOrSlide", _VIDEO);

        params.Background_shoppingList = sharedPreferences.getString("Background_shoppingList", "default background picture.png");
        params.Background_CashNotWork = sharedPreferences.getString("Background_CashNotWork", "default background picture.png");
        params.Background_Thanks = sharedPreferences.getString("Background_Thanks", "default background picture.png");

        params.sPathToScreenImg = sharedPreferences.getString("sPathToScreenImg", "/indi10/ScreenImg/");

        return params;
    }

    public synchronized static void setParameters(PreferencesValues parameters) {
        SharedPreferences sharedPreferences = MainActivity.mContext.getSharedPreferences(SERIALIZER_KEY, MainActivity.mContext.MODE_PRIVATE);

        sharedPreferences.edit().putString("sUartName", parameters.sUartName).apply();
        sharedPreferences.edit().putString("sSmbHost", parameters.sSmbHost).apply();
        sharedPreferences.edit().putString("sSmbImg", parameters.sSmbImg).apply();
        sharedPreferences.edit().putString("sSmbVideo", parameters.sSmbVideo).apply();
        sharedPreferences.edit().putString("sSmbSlide", parameters.sSmbSlide).apply();

        sharedPreferences.edit().putString("sUser", parameters.sUser).apply();
        sharedPreferences.edit().putString("sPassw", parameters.sPassw).apply();
        sharedPreferences.edit().putBoolean("sCheckEnableVideo", parameters.sCheckEnableVideo).apply();

        if (parameters.videoTimeout<MIN_VIDEO_TIMEOUT)
            parameters.videoTimeout = MIN_VIDEO_TIMEOUT;
        sharedPreferences.edit().putLong("svideoTimeout", parameters.videoTimeout).apply();;
        sharedPreferences.edit().putString("sAdmin", parameters.sAdmin).apply();;
        sharedPreferences.edit().putString("sAdminPassw", parameters.sAdminPassw).apply();;
        sharedPreferences.edit().putBoolean("sDownloadAtStart", parameters.sDownloadAtStart).apply();
        sharedPreferences.edit().putInt("sPercentVolume", parameters.sPercentVolume).apply();


        sharedPreferences.edit().putString("sIP", parameters.sIP).apply();;
        sharedPreferences.edit().putString("sMask", parameters.sMask).apply();;
        sharedPreferences.edit().putString("sGW", parameters.sGW).apply();
        sharedPreferences.edit().putString("sDNS", parameters.sDNS).apply();
        sharedPreferences.edit().putBoolean("sDHCP", parameters.sDHCP).apply();
        assert(parameters.sProtocol!=DEF_PROTOCOL[_SMB1]&&parameters.sProtocol!=DEF_PROTOCOL[_SMB2]&&parameters.sProtocol!=DEF_PROTOCOL[_FTP]);
        sharedPreferences.edit().putString("sProtocol", parameters.sProtocol).apply();
        sharedPreferences.edit().putString("sDefaultBackGroundImage", parameters.sDefaultBackGroundImage).apply();
        sharedPreferences.edit().putBoolean("sShowNavigationBar", parameters.sShowNavigationBar).apply();

        if (parameters.sTimeSlideImage<MIN_SLIDE_TIME_SHOW)
            parameters.sTimeSlideImage=MIN_SLIDE_TIME_SHOW;

        sharedPreferences.edit().putInt("sTimeSlideImage", parameters.sTimeSlideImage).apply();

        sharedPreferences.edit().putInt("sVideoOrSlide", parameters.sVideoOrSlide).apply();

        sharedPreferences.edit().putString("Background_shoppingList", parameters.Background_shoppingList).apply();
        sharedPreferences.edit().putString("Background_CashNotWork", parameters.Background_CashNotWork).apply();
        sharedPreferences.edit().putString("Background_Thanks", parameters.Background_Thanks).apply();
        sharedPreferences.edit().putString("sPathToScreenImg", parameters.sPathToScreenImg).apply();




    }
}
