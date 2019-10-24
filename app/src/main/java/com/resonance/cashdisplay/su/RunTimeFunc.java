package com.resonance.cashdisplay.su;

import android.os.Build;
import android.util.Log;

import java.io.IOException;

//import android.util.Log;


public class RunTimeFunc {
    public static final String TAG = RunTimeFunc.class.getSimpleName();

    // Подсобная функция, которая просто выполняет shell-команду
    public static boolean runCommandWait(String cmd, boolean needsu) {
        try {
            String su = "sh";
            if (needsu) {
                su = "su";
            }
            Log.d(TAG, "runCommandWait: " + cmd);

            Process process = Runtime.getRuntime().exec(new String[]{su, "-c", cmd});
            int result = process.waitFor();

            return (result == 0);

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // Функция делает указанное приложение системным и отправляет смартфон в мягкую перезагрузку
    public static void makeAppSystem(String appName) {
        String systemPrivAppDir = "/system/priv-app/";
        String systemAppDir = "/system/app/";

        String appPath = "/data/app/" + appName;

        // Подключаем /system в режиме чтения-записи
        if (!runCommandWait("mount -o remount,rw /system", true)) {
            Log.e(TAG, "makeAppSystem: Can't mount /system");
            return;
        }

        int api = Build.VERSION.SDK_INT;
        String appDir = systemPrivAppDir;

        // Копируем приложение в /system/priv-app или /system/app в зависимости от версии Android
        if (api >= 21) {
            runCommandWait("cp -R " + appPath + "* " + appDir, true);
            runCommandWait("chown -R 0:0 " + appDir + appName + "*", true);
            runCommandWait("rm -Rf " + appPath + "*", true);
        } else {
            if (api < 20) {
                appDir = systemAppDir;
            }
            runCommandWait("cp " + appPath + "* " + appDir, true);
            runCommandWait("chown 0:0 " + appDir + appName + "*", true);
            runCommandWait("rm -f " + appPath + "*", true);
        }
        // Отправляем смартфон в мягкую перезагрузку
        runCommandWait("am restart", true);
    }

    // Функция перенастраивает ADB на работу с TCP IP
    static public void set_ADB_to_TCPIP(long port) {
        Modify_SU_Preferences.executeCmd("setprop service.adb.tcp.port " + port, 3000);
        Modify_SU_Preferences.executeCmd("stop adbd", 3000);
        Modify_SU_Preferences.executeCmd("start adbd", 3000);//3000
    }
}
