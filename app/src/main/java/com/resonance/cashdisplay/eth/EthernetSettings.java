package com.resonance.cashdisplay.eth;

import android.content.ContentResolver;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.resonance.cashdisplay.PreferenceParams;
import com.resonance.cashdisplay.PreferencesValues;
import com.resonance.cashdisplay.su.Modify_SU_Preferences;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

//import android.util.Log;


public class EthernetSettings {

    public static final String TAG = EthernetSettings.class.getSimpleName();

    public static final String ETH_ON = "eth_on";
    public static final String ETH_MODE = "eth_mode";
    public static final String ETH_IP = "eth_ip";
    public static final String ETH_MASK = "eth_mask";
    public static final String ETH_DNS = "eth_dns";
    public static final String ETH_ROUTE = "eth_route";
    public static final String ETH_CONF = "eth_conf";
    public static final String ETH_IFNAME = "eth_ifname";
    public static final String ETH_KEEP = "eth_always_on";

    private IP_Settings[] tempStatAddresses = {
            (new IP_Settings("192.168.1.200", "255.255.255.0", "192.168.1.1", "8.8.8.8")),
            (new IP_Settings("192.168.0.200", "255.255.255.0", "192.168.0.1", "8.8.8.8")),
            (new IP_Settings("192.168.2.200", "255.255.255.0", "192.168.2.1", "8.8.8.8")),
            (new IP_Settings("192.168.88.200", "255.255.255.0", "192.168.88.1", "8.8.8.8")),
            (new IP_Settings("10.1.1.200", "255.0.0.0", "10.0.0.1", "8.8.8.8")),
            (new IP_Settings("172.16.1.200", "255.240.0.0", "172.16.0.1", "8.8.8.8")),
            (new IP_Settings("192.168.1.200", "255.255.0.0", "192.168.0.1", "8.8.8.8")),
            (new IP_Settings("169.254.1.200", "255.255.0.0", "169.254.0.1", "8.8.8.8"))};
    public static boolean tempStatic = false;  // mode specifies that static address must be set temporary (if DHCP couldn't be received)
    public static final int TIME_CHECK_DHCP_ENABLE = 20000;

    //private static final String CMD_ROOT = "busybox whoami";
    private static final String CMD_GET_IP_MASK = "ifconfig eth0";
    private static final String CMD_GET_GW = "ip route show";
    private static final String CMD_DELETE_ALL_GW = "ip route flush 0/0";
    private static final String CMD_ETH_UP = "ifconfig eth0 up";
    private static final String CMD_ETH_DOWN = "ifconfig eth0 down";
    private static final String CMD_DHCP_START = "ifconfig eth0 dhcp start";
    private static final String CMD_REBOOT = "reboot";

    private static String currentStatus = "";
    private int isEthAlwaysOn;
    private static Context mContext;

    public EthernetSettings(Context context) {
        mContext = context;
        Log.d(TAG, "Start EthernetSettings ... ");
    }

    /**
     * устанавливает настройки Ethernet
     */
    public synchronized void applyEthernetSettings() {

        //Получим настройки программы
        PreferencesValues preferencesValues = PreferenceParams.getParameters();

        Log.d(TAG, "get connected mode DHCP: " + preferencesValues.dhcp);
        if (preferencesValues.dhcp) {
            startDHCP();
        } else {
            IP_Settings ipSettings = get_IP_MASK_GW();

            Log.d(TAG, "STATIC ip: " + ipSettings.getIp() + " nm: " + ipSettings.getNetmask() + " gw:" + ipSettings.getGateway());

            if (!ipSettings.getIp().contains(preferencesValues.ip)
                    || !ipSettings.getNetmask().contains(preferencesValues.mask)
                    || !ipSettings.getGateway().contains(preferencesValues.gateway)) {
                set_IP_MASK_GW(preferencesValues.ip, preferencesValues.mask, preferencesValues.gateway);
            }
        }
        currentStatus = "";
        setupLanCallback.onSetupLAN(1);
    }

    /**
     * If DHCP don't used in connected LAN, we set static IP parameters for indicator temporary.
     * Temporary static parameters are actual till the reboot and don't saved to preferences.
     */
    public synchronized void setTempStatic() {
        tempStatic = true;
        PreferencesValues prefValues = PreferenceParams.getParameters();
        new Thread(() -> {
            for (int i = 0; i < tempStatAddresses.length; i++) {
                if (tempStatic) {
                    prefValues.ip = tempStatAddresses[i].getIp();
                    prefValues.mask = tempStatAddresses[i].getNetmask();
                    prefValues.gateway = tempStatAddresses[i].getGateway();

                    set_IP_MASK_GW(prefValues.ip, prefValues.mask, prefValues.gateway);
                    PreferenceParams.setParameters(prefValues);
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    /**
     * Получение настроек Ethernet
     *
     * @return
     */
    public static EthernetDevInfo getSystemEthernetSettings() {

        EthernetDevInfo ethDevInfo = null;
        Class<?> ethManagerClass = null;
        try {
            ethManagerClass = Class.forName("android.net.ethernet.EthernetManager");

        } catch (ClassNotFoundException e) {
            Log.d(TAG, "CheckClass: " + e);
            e.printStackTrace();
        }

        Object ethernetManagerObject = mContext.getSystemService("ethernet");
        Method methodgetSavedEthConfig = null;
        try {
            methodgetSavedEthConfig = ethManagerClass.getMethod("getSavedEthConfig");

        } catch (NoSuchMethodException e) {
            Log.e(TAG, "NoSuchMethodException " + e);
            e.printStackTrace();
        }
        methodgetSavedEthConfig.setAccessible(true);
        try {

            ethDevInfo = (EthernetDevInfo) methodgetSavedEthConfig.invoke(ethernetManagerObject);
            if (ethDevInfo != null) {
                //manual/dhcp
                Log.w(TAG, "ethDevInfo IP: " + ethDevInfo.getIpAddress());
                Log.w(TAG, "ethDevInfo MASK: " + ethDevInfo.getNetMask());
                Log.w(TAG, "ethDevInfo ROUTE: " + ethDevInfo.getRouteAddr());
                Log.w(TAG, "ethDevInfo DNS: " + ethDevInfo.getDnsAddr());
                Log.w(TAG, "ethDevInfo mode: " + ethDevInfo.getConnectMode());
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "IllegalArgumentException " + e);
            ethDevInfo = null;
            // e.printStackTrace();
        } catch (IllegalAccessException e) {
            Log.e(TAG, "IllegalAccessException " + e);
            ethDevInfo = null;
            // e.printStackTrace();
        } catch (InvocationTargetException e) {
            Log.e(TAG, "InvocationTargetException " + e.getCause());
            e.printStackTrace();
            ethDevInfo = null;
        }
        return ethDevInfo;
    }

    private boolean isEthConfigured() {
        final ContentResolver cr = mContext.getContentResolver();
        int x = Settings.System.getInt(cr, ETH_CONF, 0);
        if (x == 1)
            return true;
        return false;
    }

    /*
     * Получение настроек Ethernet
     */
    public synchronized EthernetDevInfo getSavedEthConfig() {
        if (isEthConfigured()) {
            final ContentResolver cr = mContext.getContentResolver();
            EthernetDevInfo info = new EthernetDevInfo();
            info.setConnectMode(Settings.System.getString(cr, ETH_MODE));
            info.setIfName(Settings.System.getString(cr, ETH_IFNAME));
            info.setIpAddress(Settings.System.getString(cr, ETH_IP));
            info.setDnsAddr(Settings.System.getString(cr, ETH_DNS));
            info.setNetMask(Settings.System.getString(cr, ETH_MASK));
            info.setRouteAddr(Settings.System.getString(cr, ETH_ROUTE));
            isEthAlwaysOn = getPersistedState(ETH_KEEP, 0);
            info.setAlwaysOn(isEthAlwaysOn);

            return info;
        }
        return null;
    }

    private int getPersistedState(String name, int defaultVal) {
        final ContentResolver cr = mContext.getContentResolver();
        try {
            //return Settings.Global.getInt(cr, Settings.Global.ETH_ON);
            return Settings.System.getInt(cr, name);
        } catch (Settings.SettingNotFoundException e) {
            //return EthernetManager.ETH_STATE_DISABLED;
            return defaultVal;
        }
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    /**
     * Получение IP адреса
     */
    public static String getNetworkInterfaceIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface networkInterface = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = networkInterface.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        String host = inetAddress.getHostAddress();
                        if (!TextUtils.isEmpty(host)) {
                            return host;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "getLocalIpAddress:" + ex);
        }
        return null;
    }

    public static boolean isValidIP4Address(String ipAddress) {
        if (ipAddress.matches("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$")) {
            String[] groups = ipAddress.split("\\.");

            for (int i = 0; i <= 3; i++) {
                String segment = groups[i];
                if (segment == null || segment.length() <= 0) {
                    return false;
                }

                int value = 0;
                try {
                    value = Integer.parseInt(segment);
                } catch (NumberFormatException e) {
                    return false;
                }
                if (value > 255) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Проверка наличия подключения LAN
     *
     * @return
     */
    public static boolean isConnected() {
        boolean connected = false;
        try {
            ConnectivityManager cm = (ConnectivityManager) mContext.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo nInfo = cm.getActiveNetworkInfo();
            connected = nInfo != null && nInfo.isAvailable() && nInfo.isConnected();
            return connected;
        } catch (Exception e) {
            Log.e("Connectivity Exception", e.getMessage());
        }
        return connected;
    }

/***************************************************************************/

    /**
     * Коллбэк окончания настройки сети
     */

    public interface SetupLanCallback {
        void onSetupLAN(int result);
    }

    private static EthernetSettings.SetupLanCallback setupLanCallback;

    public void setSetupLanCallback(EthernetSettings.SetupLanCallback cback) {
        setupLanCallback = cback;
    }

    /*********************************************/
    /**
     * Установка настроек сети
     *
     * @param ip
     * @param mask
     * @param gw
     */
    public synchronized void set_IP_MASK_GW(final String ip, final String mask, final String gw) {
        final String CMD_SET_IP_MASK = "ifconfig eth0 " + ip + " netmask " + mask + " up";
        final String CMD_SET_GW = "route add default gw " + gw + " dev eth0";

        new Thread(() -> {
            try {
                currentStatus = "встановлення статичної адреси...";
                Modify_SU_Preferences.executeCmd(CMD_ETH_DOWN, 500);
                Modify_SU_Preferences.executeCmd(CMD_SET_IP_MASK, 500);
                if (gw.length() > 0) {
                    Modify_SU_Preferences.executeCmd(CMD_DELETE_ALL_GW, 500);
                    Modify_SU_Preferences.executeCmd(CMD_SET_GW, 500);
                }
                currentStatus = "";
                if (tempStatic)
                    currentStatus = "(тимчасово) IP: " + ip;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Поток установки DHCP
     */
    public synchronized void startDHCP() {
        new Thread(() -> {
            try {
                currentStatus = "DHCP отримання адреси ...";
                Modify_SU_Preferences.executeCmd(CMD_ETH_DOWN, 0);   //500
                Modify_SU_Preferences.executeCmd(CMD_DHCP_START, 0); //3000
                Modify_SU_Preferences.executeCmd(CMD_ETH_UP, 0);     //5000
                currentStatus = "";
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Получение настроек сети
     *
     * @return IP_Settings
     */
    public static synchronized IP_Settings get_IP_MASK_GW() {
        IP_Settings settings = new IP_Settings();
        String tmpStr = Modify_SU_Preferences.executeCmd(CMD_GET_IP_MASK, 3000);

        if (tmpStr.contains("eth0: ip")) {
            int indexStart = tmpStr.indexOf("eth0: ip", 0) + "eth0: ip".length();
            int indexStop = tmpStr.indexOf("mask", indexStart);
            settings.setIp(tmpStr.substring(indexStart, indexStop));
            indexStart = tmpStr.indexOf("mask", 0) + "mask".length();
            indexStop = tmpStr.indexOf("flags", indexStart);
            settings.setNetmask(tmpStr.substring(indexStart, indexStop));
        }
        settings.setGateway(get_GW());
        return settings;
    }

    /**
     * Получение шлюза
     *
     * @return
     */
    private static synchronized String get_GW() {
        String tmpStr = Modify_SU_Preferences.executeCmd(CMD_GET_GW, 1000);
        String result = "";
        if (tmpStr.contains("default via")) {
            int indexStart = tmpStr.indexOf("default via", 0) + "default via".length();
            int indexStop = tmpStr.indexOf("dev eth0", indexStart);
            result = tmpStr.substring(indexStart, indexStop);

        }
        return result;
    }
}


