package com.resonance.cashdisplay;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.NetworkOnMainThreadException;

import com.resonance.cashdisplay.su.Modify_SU_Preferences;

import org.ini4j.Ini;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class UpdateFirmware {

    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36";
    public final static String BROADCAST_ACTION = "com.resonance.cashdisplay.p0961servicebackbroadcast";
    public final static String PARAM_RESULT = "result";

    private static final String TAG = "UpdateFirmware";
    private static Context mContext;

    public static final String FILE_SETTING = "settings.ini";
    public static final String UPDATE_URI = "http://dev.ekka.com.ua/indicator/";
    public static final String LOCAL_DIR = Environment.getExternalStorageDirectory() + "/Documents/";

    private static int typeFileLoad = 0;
    public static String pathToApkFile = "";

    public UpdateFirmware(Context context) {
        this.mContext = context;
    }

    private void registerReceiver() {
        mContext.registerReceiver(br, new IntentFilter(BROADCAST_ACTION));
    }

    private void unregisterReceiver() {
        mContext.unregisterReceiver(br);
    }

    BroadcastReceiver br = new BroadcastReceiver() {
        // действия при получении сообщений
        @Override
        public void onReceive(Context context, Intent intent) {
            int status = intent.getIntExtra(PARAM_RESULT, -1);
            Log.d(TAG, "onReceive: " + status);
            if (status == 0) {
                if (typeFileLoad == 1) {
                    treatmentSettingFile();
                } else if (typeFileLoad == 2) {
                    unregisterReceiver();
                    resultForFirmWareFile();
                }
            } else {
                unregisterReceiver();
                typeFileLoad = 0;
            }
        }
    };

    /**
     * Инициация обновления ПО
     */
    public void update() {
        if (typeFileLoad > 0)
            return;

        //Проверка директории
        File dir = new File(LOCAL_DIR);
        if (!dir.exists()) {
            Log.d(TAG, "CREATE DIR:" + LOCAL_DIR);
            dir.mkdirs();
        }

        if (!dir.exists() || !dir.isDirectory()) {
            Log.e(TAG, "Ошибка создания :" + LOCAL_DIR);
            MainActivity.httpServer.sendQueWebStatus("операція не виконана, сталася помилка системи # 101", true);
            return;
        }
        registerReceiver();
        typeFileLoad = 1;
        new DownloadSettingsTask().execute(FILE_SETTING);
    }

    /**
     * Загрузчик обновления ПО
     */
    private void treatmentSettingFile() {
        try {
            Ini ini = new Ini(new File(LOCAL_DIR + FILE_SETTING));
            String build = ini.get("default", "build");
            build = build.replaceAll("\"", "");
            String file_name = ini.get("default", "file_name");
            file_name = file_name.replaceAll("\"", "");

            Log.w(TAG, "build: " + build + " file_name:" + file_name);

            boolean enableUpdate = false;

            if (Integer.parseInt(build) > BuildConfig.VERSION_CODE)
                enableUpdate = true;
            if (enableUpdate) {
                MainActivity.httpServer.sendQueWebStatus("доступна версія ПЗ: " + build, true);
                typeFileLoad = 2;
                pathToApkFile = LOCAL_DIR + file_name + ".apk";
                new DownloadSettingsTask().execute(file_name + ".apk");
            } else {
                MainActivity.httpServer.sendQueWebStatus("Вже встановлена остання версія ПЗ", true);
                typeFileLoad = 0;
                return;
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException: " + LOCAL_DIR + FILE_SETTING + " " + e);
        } catch (IOException e) {
            Log.e(TAG, "IOException: " + LOCAL_DIR + FILE_SETTING + " " + e);
            MainActivity.httpServer.sendQueWebStatus("ПОМИЛКА " + e.getMessage(), true);
        }
    }

    public void resultForFirmWareFile() {
        updateAPK(pathToApkFile);
        typeFileLoad = 0;
    }

    /**
     * Установка APK
     *
     * @param fileName
     */
    private void updateAPK(final String fileName) {
        Log.d(TAG, "updateAPK" + fileName);
        File apkFile = new File(fileName);
        if (!apkFile.exists()) {
            Log.d(TAG, "APK file " + fileName + " not exist");
            MainActivity.httpServer.sendQueWebStatus("ПОМИЛКА. Файл оновлення відсутнiй", true);
            return;
        }

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                MainActivity.httpServer.sendQueWebStatus("систему буде перезавантажено...", true);
                doRestart();//старт с отложенным запуском, даем время на завершение установки приложения
                String command = "pm install -r -f " + fileName;
                Log.d(TAG, "updateAPK " + command);
                Modify_SU_Preferences.executeCmd(command, 4000);
                System.exit(0);
            }
        });
        thread.start();
    }

    /**
     * Рестарт системы
     */
    private void doRestart() {
        Intent mStartActivity = new Intent(mContext, MainActivity.class);
        mStartActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        int mPendingIntentId = 123456;
        PendingIntent mPendingIntent = PendingIntent.getActivity(mContext, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 8000, mPendingIntent);
    }


    private static int crc32(String str) {
        byte bytes[] = str.getBytes();
        Checksum checksum = new CRC32();
        checksum.update(bytes, 0, bytes.length);
        return (int) checksum.getValue();
    }

    final class DownloadSettingsTask extends AsyncTask<Object, Void, Integer> {

        protected void onPreExecute() {
            super.onPreExecute();
            MainActivity.httpServer.sendQueWebStatus("підключення до сервера оновлень...", true);
        }

        @Override
        protected Integer doInBackground(Object... params) {

            Log.d(TAG, "DownloadSettingsTask... ");

            File fileSetting = new File(LOCAL_DIR + params[0]);

            HttpURLConnection connection = null;
            // OutputStreamWriter outputStreamWriter = null;
            FileOutputStream fos = null;
            try {
                URL url = new URL(UPDATE_URI + params[0]);
                Log.d(TAG, "url... " + UPDATE_URI + params[0]);
                connection = (HttpURLConnection) url.openConnection();
                //connection.setConnectTimeout(5000);
                //connection.setRequestProperty("User-Agent",USER_AGENT);
                connection.setRequestMethod("GET");
                connection.getDoOutput();
                connection.connect();

                Log.d(TAG, " UpdateFirmware... " + connection.getResponseMessage() + " code: " + connection.getResponseCode());

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream in = new BufferedInputStream(connection.getInputStream());
                    // outputStreamWriter = new OutputStreamWriter(new FileOutputStream(fileSetting));

                    Log.d(TAG, " UpdateFirmware... connection.getContentLength: " + connection.getContentLength());

                    fos = new FileOutputStream(fileSetting);

                    // BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                    // StringBuilder sb = new StringBuilder();

                    byte[] buffer = new byte[8192];
                    int len;
                    long totalbytes = 0;
                    int prevValuePercent = 0;
                    MainActivity.httpServer.sendQueWebStatus("завантаження даних...", false);
                    while ((len = in.read(buffer)) != -1) {
                        //Log.d(TAG, ">>" + new String(buffer, 0, len));
                        fos.write(buffer, 0, len);

                        totalbytes += len;
                        int percent = (int) (totalbytes * 100 / connection.getContentLength());

                        //if (percent%10)
                        if (prevValuePercent != percent) {
                            MainActivity.httpServer.sendQueWebStatus("завантаження даних..." + percent + " %", true);
                            // Log.d(TAG, ">>" + percent);
                            prevValuePercent = percent;
                        }
                    }

                } else if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                    Log.e(TAG, "Путь:  " + url.getFile() + " не найден");
                    MainActivity.httpServer.sendQueWebStatus("не знайдено сервер оновлень  " + HttpURLConnection.HTTP_NOT_FOUND, true);
                    return 2;
                } else {
                    Log.e(TAG, "*** ERROR:  " + connection.getResponseCode());
                    MainActivity.httpServer.sendQueWebStatus("Помилки при завантаженнi. Перевiрте налаштунки мережi. ", true);
                    return 3;
                }
            } catch (NetworkOnMainThreadException e) {
                Log.e(TAG, "UpdateFirmware NetworkOnMainThreadException:  " + e.getLocalizedMessage());
                MainActivity.httpServer.sendQueWebStatus("Помилки при завантаженнi оновлень. Перевiрте налаштунки мережi. ", true);
                return 4;
            } catch (IOException e) {
                Log.e(TAG, "UpdateFirmware IOException:  " + e);
                //if (e.getMessage().contains("Unable to resolve host"))
                MainActivity.httpServer.sendQueWebStatus("не знайдено сервер оновлень. Перевiрте налаштунки мережi. " + e.getLocalizedMessage(), true);
                return 5;
            } finally {
                try {
                    if (fos != null)
                        fos.close();
                } catch (IOException e) {

                }
                if (connection != null)
                    connection.disconnect();
            }
            return 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            Log.d(TAG, "DownloadTask result : " + result);
            Intent intent = new Intent(BROADCAST_ACTION);
            intent.putExtra(PARAM_RESULT, result);
            mContext.sendBroadcast(intent);
         /*  if (TypeFileLoad==1)
              ResultForSettingFile(result);
           else
              ResultForFirmWareFile(result);
*/
        }
    }

    public int[] convertVersionToInt(String versionName) {
        int[] arr = new int[]{0, 0, 0};
        if (versionName.matches("^(\\d{1,4})\\.(\\d{1,4})\\.(\\d{1,4})$")) {
            String[] groups = versionName.split("\\.");
            for (int i = 0; i < 3; i++) {
                String segment = groups[i];
                if (segment == null || segment.length() <= 0) {
                    return (new int[]{0, 0, 0});
                }
                int value = 0;
                try {
                    arr[i] = Integer.parseInt(segment);
                } catch (NumberFormatException e) {
                    return (new int[]{0, 0, 0});
                }
            }
            return arr;
        }
        return (new int[]{0, 0, 0});
    }
}

