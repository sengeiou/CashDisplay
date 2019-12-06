package com.resonance.cashdisplay;

import android.content.SharedPreferences;

//import android.util.Log;

/**
 * Created by Святослав on 11.05.2016.
 */
public class PreferenceParams {

    private static final String SERIALIZER_KEY = "ce.serilizer";    // not change already, users has settings with this name
    public static int SMB1 = 0;
    public static int SMB2 = 1;
    public static int FTP = 2;

    public static int VIDEO = 0;
    public static int SLIDE = 1;

    public static final int MIN_VIDEO_TIMEOUT = 10;
    public static final int MIN_SLIDE_TIME_SHOW = 1;
    public static final String[] DEF_PROTOCOL = new String[]{"SMB1", "SMB2", "FTP"};
    public static final String[] DEF_UARTS = new String[]{"EKKR", "PC"};

    // product list look will be choose using specified code
    public static final int LOOK_BASKET = 0;            // shop in Kharkov
    public static final int LOOK_DMART = 1;             //  shop in Dnepr
    public static final int[] PRODUCT_LIST_LOOK = {LOOK_BASKET, LOOK_DMART};
    private static int productListLookCode = PRODUCT_LIST_LOOK[0];  // default and initial value
    private static final String PRODUCT_LIST_BACK_IMAGE_PREFIX = "custom_product_list_";

    public PreferenceParams() {
        // sharedPreferences = MainActivity.mContext.getSharedPreferences(SERIALIZER_KEY, MainActivity.mContext.MODE_PRIVATE);
    }

    public synchronized static PreferencesValues getParameters() {
        SharedPreferences sharedPreferences = MainActivity.context.getSharedPreferences(SERIALIZER_KEY, MainActivity.context.MODE_PRIVATE);
        PreferencesValues params = new PreferencesValues();

        params.uartName = sharedPreferences.getString("sUartName", DEF_UARTS[0]);
        params.smbHost = sharedPreferences.getString("sSmbHost", "server");
        params.smbImg = sharedPreferences.getString("sSmbImg", "/indi10/Img/");
        params.smbVideo = sharedPreferences.getString("sSmbVideo", "/indi10/Video/");
        params.smbSlide = sharedPreferences.getString("sSmbSlide", "/indi10/Slide/");
        params.user = sharedPreferences.getString("sUser", "indi10");
        params.passw = sharedPreferences.getString("sPassw", "20671");
        params.checkEnableVideo = sharedPreferences.getBoolean("sCheckEnableVideo", false);
        params.videoTimeout = sharedPreferences.getLong("svideoTimeout", 20);
        params.admin = sharedPreferences.getString("sAdmin", "admin");
        params.adminPassw = sharedPreferences.getString("sAdminPassw", "admin");
        params.downloadAtStart = sharedPreferences.getBoolean("sDownloadAtStart", false);
        params.percentVolume = sharedPreferences.getInt("sPercentVolume", 50);

        params.ip = sharedPreferences.getString("sIP", "192.168.1.200");
        params.mask = sharedPreferences.getString("sMask", "255.255.255.0");
        params.gateWay = sharedPreferences.getString("sGW", "192.168.1.1");
        params.dns = sharedPreferences.getString("sDNS", "8.8.8.8");
        params.dhcp = sharedPreferences.getBoolean("sDHCP", true);
        params.transferProtocol = sharedPreferences.getString("sProtocol", DEF_PROTOCOL[SMB1]);
        params.defaultBackgroundImage = sharedPreferences.getString("sDefaultBackGroundImage", "default_background_picture.png");
        params.showNavigationBar = sharedPreferences.getBoolean("sShowNavigationBar", false);
        params.timeSlideImage = sharedPreferences.getInt("sTimeSlideImage", 10);
        params.videoOrSlide = sharedPreferences.getInt("sVideoOrSlide", VIDEO);

        params.backgroundShoppingList = sharedPreferences.getString("background_shopping_list", "default_background_picture.png");
        params.backgroundCashNotWork = sharedPreferences.getString("background_cash_not_work", "default_background_picture.png");
        params.backgroundThanks = sharedPreferences.getString("background_thanks", "default_background_picture.png");

        params.pathToScreenImg = sharedPreferences.getString("sPathToScreenImg", "/indi10/ScreenImg/");

        params.productListLookCode = sharedPreferences.getInt("productListLookCode", PRODUCT_LIST_LOOK[0]);

        return params;
    }

    public synchronized static void setParameters(PreferencesValues parameters) {
        SharedPreferences sharedPreferences = MainActivity.context.getSharedPreferences(SERIALIZER_KEY, MainActivity.context.MODE_PRIVATE);

        sharedPreferences.edit().putString("sUartName", parameters.uartName).apply();
        sharedPreferences.edit().putString("sSmbHost", parameters.smbHost).apply();
        sharedPreferences.edit().putString("sSmbImg", parameters.smbImg).apply();
        sharedPreferences.edit().putString("sSmbVideo", parameters.smbVideo).apply();
        sharedPreferences.edit().putString("sSmbSlide", parameters.smbSlide).apply();

        sharedPreferences.edit().putString("sUser", parameters.user).apply();
        sharedPreferences.edit().putString("sPassw", parameters.passw).apply();
        sharedPreferences.edit().putBoolean("sCheckEnableVideo", parameters.checkEnableVideo).apply();

        if (parameters.videoTimeout < MIN_VIDEO_TIMEOUT)
            parameters.videoTimeout = MIN_VIDEO_TIMEOUT;
        sharedPreferences.edit().putLong("svideoTimeout", parameters.videoTimeout).apply();

        sharedPreferences.edit().putString("sAdmin", parameters.admin).apply();

        sharedPreferences.edit().putString("sAdminPassw", parameters.adminPassw).apply();
        sharedPreferences.edit().putBoolean("sDownloadAtStart", parameters.downloadAtStart).apply();
        sharedPreferences.edit().putInt("sPercentVolume", parameters.percentVolume).apply();

        sharedPreferences.edit().putString("sIP", parameters.ip).apply();

        sharedPreferences.edit().putString("sMask", parameters.mask).apply();

        sharedPreferences.edit().putString("sGW", parameters.gateWay).apply();
        sharedPreferences.edit().putString("sDNS", parameters.dns).apply();
        sharedPreferences.edit().putBoolean("sDHCP", parameters.dhcp).apply();
        assert (parameters.transferProtocol != DEF_PROTOCOL[SMB1] && parameters.transferProtocol != DEF_PROTOCOL[SMB2] && parameters.transferProtocol != DEF_PROTOCOL[FTP]);
        sharedPreferences.edit().putString("sProtocol", parameters.transferProtocol).apply();
        sharedPreferences.edit().putString("sDefaultBackGroundImage", parameters.defaultBackgroundImage).apply();
        sharedPreferences.edit().putBoolean("sShowNavigationBar", parameters.showNavigationBar).apply();

        if (parameters.timeSlideImage < MIN_SLIDE_TIME_SHOW)
            parameters.timeSlideImage = MIN_SLIDE_TIME_SHOW;

        sharedPreferences.edit().putInt("sTimeSlideImage", parameters.timeSlideImage).apply();

        sharedPreferences.edit().putInt("sVideoOrSlide", parameters.videoOrSlide).apply();

        sharedPreferences.edit().putString("background_shopping_list", parameters.backgroundShoppingList).apply();
        sharedPreferences.edit().putString("background_cash_not_work", parameters.backgroundCashNotWork).apply();
        sharedPreferences.edit().putString("background_thanks", parameters.backgroundThanks).apply();

        sharedPreferences.edit().putString("sPathToScreenImg", parameters.pathToScreenImg).apply();

        // determine from background image for product list if we have to set custom look or default
        productListLookCode = PRODUCT_LIST_LOOK[0];
        if (parameters.backgroundShoppingList.matches(PRODUCT_LIST_BACK_IMAGE_PREFIX + "\\d+.*")) {
            String strLookCode = parameters.backgroundShoppingList.replaceAll("\\D+", "");
            int lookCode = Integer.valueOf(strLookCode);
            for (int authLookCode : PRODUCT_LIST_LOOK) {
                if (lookCode == authLookCode) {
                    productListLookCode = lookCode;
                    break;
                }
            }
        }
        sharedPreferences.edit().putInt("productListLookCode", productListLookCode).apply();
    }
}
