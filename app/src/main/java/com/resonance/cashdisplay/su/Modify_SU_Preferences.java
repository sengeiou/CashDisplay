package com.resonance.cashdisplay.su;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.resonance.cashdisplay.MainActivity;

import org.ini4j.Ini;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import android.util.Log;
//import android.util.Log;

public class Modify_SU_Preferences {


    private static final String TAG = "Modify_SU_Preferences";
    // private final static String TMP_SU_CFG_XML = "/data/data/eu.chainfire.supersu/shared_prefs/eu.chainfire.supersu_preferences._xml";
    private static String TMP_PATH_SU_CFG_XML = "/data/data/eu.chainfire.supersu/shared_prefs/";

    private static final String file_user_info = "/data/data/eu.chainfire.supersu/shared_prefs/user_info.xml";

    private static final String CMD_HIDE_NAVIGATION_BAR = "pm disable com.android.systemui";//скрыть строку навигации
    private static final String CMD_SHOW_NAVIGATION_BAR = "pm enable com.android.systemui";//показать строку навигации
    private static final String CMD_GET_PROPERTIES = "/system/bin/getprop";//запрос параметров


    private final String URI_SU_CFG = "/data/data/eu.chainfire.supersu/files/supersu.cfg";
    private final String URI_SU_CFG_XML = "/data/data/eu.chainfire.supersu/shared_prefs/eu.chainfire.supersu_preferences.xml";
    private static final String CMD_ROOT = "busybox whoami";
    private static boolean isRooted = false;

    private static Context mContext;

    public Modify_SU_Preferences(Context context) {
        this.mContext = context;
    }

    public static boolean checkSystemBootCompleted() {

        final String FOUND_PARAM = "[sys.boot_completed]: [";

        String Str = executeCmd(CMD_GET_PROPERTIES, 1000);

        int index = Str.indexOf(FOUND_PARAM);
        if (index < 0) return false;

        Log.d(TAG, "CheckSystemBootCompleted:" + index);

        index += FOUND_PARAM.length();
        int res = Integer.valueOf(Str.substring(index, index + 1));

        return (res == 1);
    }


    //необходима проверка наличия файла
    public void checkfileUserInfo() {
        File fileCfgSU = new File(file_user_info);
        if (fileCfgSU.exists()) {
            Log.w(TAG, "File:" + file_user_info + " is exist");
            return;
        }

        try {
            InputStream is = MainActivity.context.getAssets().open(fileCfgSU.getName());
            if (is != null) {
                byte[] bufRead = new byte[500];
                int totalRead = is.read(bufRead);
                Log.d(TAG, "File:" + fileCfgSU.getName() + " read:" + new String(bufRead, 0, totalRead));
                is.close();

                if (totalRead <= 0) return;

                try {
                    saveToFile(new String(bufRead, 0, totalRead), file_user_info);
                    Log.d(TAG, "user_info save complete ");
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "user_info FileNotFoundException, File:" + file_user_info + "  " + e);
                } catch (IOException e) {
                    Log.e(TAG, "create :" + file_user_info + " IOException:" + e);
                }
            }

        } catch (IOException e) {
            Log.e(TAG, "IOException Checkfile_user_info:" + file_user_info);
        }
    }

    private static boolean isPatched(String xmlFile) {

        final String REGEX_REAU = "<boolean name=.reauthenticate.\\svalue=.false.\\s/>";
        final String REGEX_NOTIFY = "<boolean name=.config_default_notify.\\svalue=.false.\\s/>";
        final String REGEX_SU = "<boolean name=.superuser.\\svalue=.true.\\s/>";

        File FileCfgSU = new File(xmlFile);
        if (!FileCfgSU.exists()) {
            Log.e(TAG, "File:" + xmlFile + " is NOT exist");
            return false;
        }

        String tmpStr = executeCmd("cat " + xmlFile, 2000);

        boolean bReauAlreadyModify = false;
        boolean bNotifyAlreadyModify = false;
        boolean bsuperuserAlreadyModify = true;

        Pattern p = Pattern.compile(REGEX_REAU);
        Matcher m = p.matcher(tmpStr);
        if (m.find()) {//уже модифицирован
            Log.d(TAG, "reauthenticate already modified");
            bReauAlreadyModify = true;
        }

        p = Pattern.compile(REGEX_NOTIFY);
        m = p.matcher(tmpStr);
        if (m.find()) {//уже модифицирован
            Log.d(TAG, "default_notify already modified");
            bNotifyAlreadyModify = true;
        }

        p = Pattern.compile(REGEX_SU);
        m = p.matcher(tmpStr);
        if (m.find()) {//уже модифицирован
            Log.d(TAG, "superuser already modified");
            bsuperuserAlreadyModify = true;
        }

        Log.d(TAG, "bReauAlreadyModify:" + bReauAlreadyModify + "   bNotifyAlreadyModify:" + bNotifyAlreadyModify + "  bsuperuserAlreadyModify:" + bsuperuserAlreadyModify);

        return (bReauAlreadyModify & bNotifyAlreadyModify & bsuperuserAlreadyModify);
    }

    public static synchronized String executeCmd(String cmd, long timeout) {

        long stopTime = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeout);
        long startTime = System.nanoTime();
        String result = "";

        int cnt = 0;
        char[] buf = new char[1024];

        DataOutputStream outputStream = null;
        BufferedReader reader = null;
        try {
            Process su;
            Log.d(TAG, "Exec Cmd: " + cmd);

            if (timeout > 0) {
                su = Runtime.getRuntime().exec("su");
                outputStream = new DataOutputStream(su.getOutputStream());
                reader = new BufferedReader(new InputStreamReader(su.getInputStream()));

                outputStream.writeBytes(cmd + "\n");//" && echo \"DONE\"
                outputStream.flush();

                while (!reader.ready()) {
                    if (stopTime <= System.nanoTime()) {
                        result = "TIMEOUT";
                        break;
                    }
                    Thread.currentThread().sleep(100);
                }

                while (reader.ready()) {
                    cnt = reader.read(buf, 0, buf.length);
                    result += new String(buf, 0, cnt);
                }
                outputStream.writeBytes("exit\n");
                outputStream.flush();
            } else {
                su = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
                su.waitFor();
                result = (su.exitValue() == 0 ? "OK" : "ERROR");
            }

            Log.w(TAG, "Time for execute: " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime) + "ms,  result: " + result);

        } catch (IOException e) {
            Log.e(TAG, "executeCmd IOException Error: " + e.getMessage());
            result = "ERROR";

        } catch (InterruptedException e) {
            Log.e(TAG, "executeCmd InterruptedException Error: " + e.getMessage());
            result = "ERROR";

        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    result = "ERROR";

                }
            }
            if (outputStream != null) {
                try {
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    result = "ERROR";
                }
            }
        }
        return result;
    }

    private static void saveToFile(String strData, String to) throws IOException {

        File newFile = new File(to);
        executeCmd("touch " + newFile, 3000);//разрешения на запись
        executeCmd("chmod -R 0666 " + newFile, 3000);//разрешения на запись
        newFile.createNewFile();

        FileOutputStream output = new FileOutputStream(newFile);

        byte[] buf = strData.getBytes();

        output.write(buf, 0, buf.length);
        output.flush();
        output.close();
    }

    private static void fileCopyUsingFileStreams(String from, String to) throws IOException {
        File fileToCopy = new File(from);
        FileInputStream input = new FileInputStream(fileToCopy);

        File newFile = new File(to);
        Modify_SU_Preferences.executeCmd("chmod -R 0666 " + newFile, 0);//3000разрешения на запись
        Log.d(TAG, "reauthenticate Rename to: " + to);
        FileOutputStream output = new FileOutputStream(newFile);

        byte[] buf = new byte[1024];
        int bytesRead;

        while ((bytesRead = input.read(buf)) > 0) {
            output.write(buf, 0, bytesRead);
        }

        input.close();
        output.flush();
        output.close();
    }

    /*************************************************************************************************/

    public static void tryChangingSuperSuDefaultAccess(Context context) throws Exception {

        Log.d(TAG, "tryChangingSuperSuDefaultAccess....");

        String packageName = context.getPackageName();
        PackageManager pm = context.getPackageManager();

        // Get the preferences for SuperSu
        Context packageContext = context.createPackageContext("eu.chainfire.supersu", 0);
        SharedPreferences superSuPrefs = PreferenceManager.getDefaultSharedPreferences(packageContext);
        File superSuPrefsFile = getSharedPreferencesFile(superSuPrefs);

        if (isPatched(superSuPrefsFile.getPath())) {
            Log.d(TAG, "Already patched SU Preferences file : " + superSuPrefsFile.getName());
            return;
        }

        // Copy SuperSu preferences to our app's shared_prefs directory
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        File directory = getSharedPreferencesFile(preferences).getParentFile();
        File destination = new File(directory, "eu.chainfire.supersu.xml");
        int uid = pm.getApplicationInfo(context.getPackageName(), 0).uid;
        destination.getParentFile().mkdirs();
        executeCmd("cp \"" + superSuPrefsFile + "\" \"" + destination + "\"", 00);//1000
        executeCmd("chmod 0660 \"" + destination + "\"", 0);//1000
        executeCmd("chown " + uid + " " + uid + " \"" + destination + "\"", 0);//1000

        // Now we can edit the shared preferences
        superSuPrefs = context.getSharedPreferences("eu.chainfire.supersu", Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = superSuPrefs.edit();
        editor.putBoolean("reauthenticate", false); // disable SuperSu notifications
        editor.putBoolean("config_default_notify", false); // Set access to grant for this app
        editor.putBoolean("superuser", true); // Set access to grant for this app
        editor.putString(String.format("config_%s_notify", packageName), "no");
        editor.putString(String.format("config_%s_access", packageName), "grant");

        // noinspection all
        editor.commit();

        executeCmd("cp \"" + destination + "\" \"" + superSuPrefsFile + "\"", 1000);
        // Copy the edited shared preferences back
        Log.d(TAG, "tryChangingSuperSuDefaultAccess ... complete");
        executeCmd("cat " + superSuPrefsFile, 3000);
        executeCmd("reboot", 3000);

    }

    private static File getSharedPreferencesFile(SharedPreferences preferences)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = preferences.getClass().getDeclaredField("mFile");
        if (!field.isAccessible()) field.setAccessible(true);
        return (File) field.get(preferences);
    }


    public static void setSystemUIEnabled(boolean enabled) {
        executeCmd(enabled ? CMD_SHOW_NAVIGATION_BAR : CMD_HIDE_NAVIGATION_BAR, 500);//3000
    }

    private static SetupRootCallback setupRootCallback;

    public interface SetupRootCallback {
        void onSetupRoot(final int result);
    }

    public void setSetupRootCallback(SetupRootCallback cback) {
        setupRootCallback = cback;
    }

    private void rootIsSetEvent(final int result) {
        setupRootCallback.onSetupRoot(result);
    }

    private boolean checkRoot() {
        boolean result = false;
        String tmpStr = Modify_SU_Preferences.executeCmd(CMD_ROOT, 10000);//10000
        if (tmpStr.contains("root")) {
            Modify_SU_Preferences.executeCmd("sync ", 0);//2000
            result = true;
        } else {
            Log.e(TAG, "access root is NOT granted");
            result = false;
        }
        return result;
    }

    private void modify_CFG_SU_File() {

        File cfgSU = new File(URI_SU_CFG);
        if (!cfgSU.exists()) {
            Log.e(TAG, "File:" + URI_SU_CFG + " is NOT exist");
            return;
        }
        Modify_SU_Preferences.executeCmd("chmod -R 0666 " + URI_SU_CFG, 0);//3000//разрешения на запись

        //...определить готовность ситемы
        Modify_SU_Preferences.executeCmd("ls -l " + URI_SU_CFG, 10000);

        if ((!cfgSU.canRead()) && (!cfgSU.canWrite())) {
            Log.e(TAG, "File: " + URI_SU_CFG + " is can't read");
            return;
        }

        try {
            Ini ini = new Ini(new File(URI_SU_CFG));
            String notify_param = ini.get("default", "notify");
            if (notify_param.length() > 0) {
                int state = Integer.parseInt(notify_param);
                Log.w(TAG, "Notify messages: " + (state == 0 ? "OFF" : "ON"));

                if (state == 1) {
                    ini.put("default", "notify", 0);
                    ini.store();
                }
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException: " + URI_SU_CFG + " " + e);
        } catch (IOException e) {
            Log.e(TAG, "IOException: " + URI_SU_CFG + " " + e);
        }
        ;
    }

    public void verifyRootRights() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                isRooted = false;
                do {
                    try {
                        //Log.d(TAG, "VerifyRootRights... ");
                        isRooted = checkRoot();
                        if (isRooted) {
                            Log.d(TAG, "VerifyRootRights... allowed");
                            Modify_SU_Preferences.executeCmd("busybox pkill -KILL eu.chainfire.supersu", 0);//3000//разрешения на запись
                            //ps -Z посмотреть процесс
                            modify_CFG_SU_File();
                            checkfileUserInfo();
                            Modify_SU_Preferences.tryChangingSuperSuDefaultAccess(mContext);
                            rootIsSetEvent(1);
                        } else {
                            Log.d(TAG, "VerifyRootRights... not allowed");
                            rootIsSetEvent(0);
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } while (!isRooted);
            }
        });
        thread.start();
    }
}
