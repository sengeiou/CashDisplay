package com.resonance.cashdisplay;

import android.content.SharedPreferences;

/**
 * Created by Святослав on 11.05.2016.
 */
public class PrefWorker {

    private static final String SERIALIZER_KEY = "ce.serilizer";    // not change already, users has settings with this name
    public static int SMB1 = 0;
    public static int SMB2 = 1;
    public static int FTP = 2;

    public static int VIDEO = 0;
    public static int SLIDE = 1;

    public static final int MIN_VIDEO_TIMEOUT = 10;
    public static final int MIN_SLIDE_TIME_SHOW = 1;
    public static final String[] DEF_PROTOCOL = {"SMB1", "SMB2", "FTP"};
    public static final String[] DEF_UARTS = {"EKKR", "PC"};

    // product list look will be choose using specified code
    public static final int LOOK_BASKET = 0;            // shop in Kharkov
    public static final int LOOK_DMART = 1;             // shop in Dnepr
    public static final int LOOK_SUBWAY = 2;            // Kiev subway
    public static final int[] PRODUCT_LIST_LOOK = {LOOK_BASKET, LOOK_DMART, LOOK_SUBWAY};
    // in early versions look code was determined from image name prefix
    @Deprecated
    private static final String PRODUCT_LIST_BACK_IMAGE_PREFIX = "custom_product_list_";

    public PrefWorker() {
        // sharedPreferences = MainActivity.mContext.getSharedPreferences(SERIALIZER_KEY, MainActivity.mContext.MODE_PRIVATE);
    }

    public synchronized static PrefValues getValues() {
        SharedPreferences sharedPreferences = MainActivity.context.getSharedPreferences(SERIALIZER_KEY, MainActivity.context.MODE_PRIVATE);
        PrefValues prefValues = new PrefValues();

        prefValues.smbHost = sharedPreferences.getString("sSmbHost", "server");
        prefValues.user = sharedPreferences.getString("sUser", "indi10");
        prefValues.passw = sharedPreferences.getString("sPassw", "20671");
        prefValues.transferProtocol = sharedPreferences.getString("sProtocol", DEF_PROTOCOL[SMB1]);

        prefValues.pathToScreenImg = sharedPreferences.getString("sPathToScreenImg", "/indi10/ScreenImg/");
        prefValues.smbImg = sharedPreferences.getString("sSmbImg", "/indi10/Img/");
        prefValues.smbVideo = sharedPreferences.getString("sSmbVideo", "/indi10/Video/");
        prefValues.smbSlide = sharedPreferences.getString("sSmbSlide", "/indi10/Slide/");

        prefValues.downloadAtStart = sharedPreferences.getBoolean("sDownloadAtStart", false);

        prefValues.uartName = sharedPreferences.getString("sUartName", DEF_UARTS[0]);

        prefValues.checkEnableVideo = sharedPreferences.getBoolean("sCheckEnableVideo", false);
        prefValues.videoOrSlide = sharedPreferences.getInt("sVideoOrSlide", VIDEO);
        prefValues.percentVolume = sharedPreferences.getInt("sPercentVolume", 50);
        prefValues.videoTimeout = sharedPreferences.getLong("svideoTimeout", 20);
        prefValues.timeSlideImage = sharedPreferences.getInt("sTimeSlideImage", 10);

        prefValues.defaultBackgroundImage = sharedPreferences.getString("sDefaultBackGroundImage", "default_background_picture.png");
        prefValues.backgroundShoppingList = sharedPreferences.getString("background_shopping_list", "default_background_picture.png");
        prefValues.backgroundCashNotWork = sharedPreferences.getString("background_cash_not_work", "default_background_picture.png");
        prefValues.backgroundThanks = sharedPreferences.getString("background_thanks", "default_background_picture.png");
        prefValues.productListLookCode = sharedPreferences.getInt("productListLookCode", PRODUCT_LIST_LOOK[0]);

        prefValues.dhcp = sharedPreferences.getBoolean("sDHCP", true);
        prefValues.ip = sharedPreferences.getString("sIP", "192.168.1.200");
        prefValues.mask = sharedPreferences.getString("sMask", "255.255.255.0");
        prefValues.gateway = sharedPreferences.getString("sGW", "192.168.1.1");
        prefValues.dns = sharedPreferences.getString("sDNS", "8.8.8.8");

        prefValues.admin = sharedPreferences.getString("sAdmin", "admin");
        prefValues.adminPassw = sharedPreferences.getString("sAdminPassw", "admin");

        prefValues.showNavigationBar = sharedPreferences.getBoolean("sShowNavigationBar", false);

        return prefValues;
    }

    public synchronized static void setValues(PrefValues prefValues) {
        SharedPreferences sharedPreferences = MainActivity.context.getSharedPreferences(SERIALIZER_KEY, MainActivity.context.MODE_PRIVATE);

        sharedPreferences.edit().putString("sSmbHost", prefValues.smbHost).apply();
        sharedPreferences.edit().putString("sUser", prefValues.user).apply();
        sharedPreferences.edit().putString("sPassw", prefValues.passw).apply();
        sharedPreferences.edit().putString("sProtocol", prefValues.transferProtocol).apply();

        sharedPreferences.edit().putString("sPathToScreenImg", prefValues.pathToScreenImg).apply();
        sharedPreferences.edit().putString("sSmbImg", prefValues.smbImg).apply();
        sharedPreferences.edit().putString("sSmbVideo", prefValues.smbVideo).apply();
        sharedPreferences.edit().putString("sSmbSlide", prefValues.smbSlide).apply();

        sharedPreferences.edit().putBoolean("sDownloadAtStart", prefValues.downloadAtStart).apply();

        sharedPreferences.edit().putString("sUartName", prefValues.uartName).apply();

        sharedPreferences.edit().putBoolean("sCheckEnableVideo", prefValues.checkEnableVideo).apply();
        sharedPreferences.edit().putInt("sVideoOrSlide", prefValues.videoOrSlide).apply();
        sharedPreferences.edit().putInt("sPercentVolume", prefValues.percentVolume).apply();
        if (prefValues.videoTimeout < MIN_VIDEO_TIMEOUT) {
            prefValues.videoTimeout = MIN_VIDEO_TIMEOUT;
        }
        sharedPreferences.edit().putLong("svideoTimeout", prefValues.videoTimeout).apply();
        if (prefValues.timeSlideImage < MIN_SLIDE_TIME_SHOW) {
            prefValues.timeSlideImage = MIN_SLIDE_TIME_SHOW;
        }
        sharedPreferences.edit().putInt("sTimeSlideImage", prefValues.timeSlideImage).apply();

        sharedPreferences.edit().putString("sDefaultBackGroundImage", prefValues.defaultBackgroundImage).apply();
        sharedPreferences.edit().putString("background_shopping_list", prefValues.backgroundShoppingList).apply();
        sharedPreferences.edit().putString("background_cash_not_work", prefValues.backgroundCashNotWork).apply();
        sharedPreferences.edit().putString("background_thanks", prefValues.backgroundThanks).apply();
        sharedPreferences.edit().putInt("productListLookCode", prefValues.productListLookCode).apply();

        sharedPreferences.edit().putBoolean("sDHCP", prefValues.dhcp).apply();
        sharedPreferences.edit().putString("sIP", prefValues.ip).apply();
        sharedPreferences.edit().putString("sMask", prefValues.mask).apply();
        sharedPreferences.edit().putString("sGW", prefValues.gateway).apply();
        sharedPreferences.edit().putString("sDNS", prefValues.dns).apply();

        sharedPreferences.edit().putString("sAdmin", prefValues.admin).apply();
        sharedPreferences.edit().putString("sAdminPassw", prefValues.adminPassw).apply();

        sharedPreferences.edit().putBoolean("sShowNavigationBar", prefValues.showNavigationBar).apply();

        // dummy only for look 1 (Dmart) to keep backward compability
        String strLookCode = prefValues.backgroundShoppingList.replaceAll("\\D+", "");
        if (prefValues.backgroundShoppingList.matches(PRODUCT_LIST_BACK_IMAGE_PREFIX + "\\d+.*")
                && (Integer.valueOf(strLookCode) == LOOK_DMART)) {
            sharedPreferences.edit().putInt("productListLookCode", PRODUCT_LIST_LOOK[LOOK_DMART]).apply();
        }
    }
}
