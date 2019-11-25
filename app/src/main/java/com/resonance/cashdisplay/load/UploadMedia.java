package com.resonance.cashdisplay.load;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.resonance.cashdisplay.ExtSDSource;
import com.resonance.cashdisplay.FileOperation;
import com.resonance.cashdisplay.Log;
import com.resonance.cashdisplay.MainActivity;
import com.resonance.cashdisplay.PreferenceParams;
import com.resonance.cashdisplay.PreferencesValues;
import com.resonance.cashdisplay.eth.EthernetSettings;
import com.resonance.cashdisplay.slide_show.VideoSlideService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import static com.resonance.cashdisplay.PreferenceParams.DEF_PROTOCOL;
import static com.resonance.cashdisplay.PreferenceParams.FTP;
import static com.resonance.cashdisplay.PreferenceParams.SMB1;
import static com.resonance.cashdisplay.PreferenceParams.SMB2;

/**
 * Класс управления загрузкой изображений товаров, видео, слайдов
 */
public class UploadMedia {
    private static final FileOperation FileOperation = new FileOperation();
    public final String TAG = "UploadMedia";

    private static final String NEW_LINE = System.getProperty("line.separator");
    private static final String DATE_FORMAT = "yyyy-MM-dd HH-mm-ss";
    private static final DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

    public static File mLogFile;

    public static final int UPLOAD_RESULT_SUCCESSFULL = 0;
    public static final int UPLOAD_RESULT_NOT_FREE_MEMORY = 1;
    public static final int UPLOAD_RESULT_SHARE_CONNECTION_ERROR = 2;
    public static final int UPLOAD_RESULT_CONNECTION_ERROR = 3;
    public static final int UPLOAD_RESULT_NOT_SUPPORT_PROTOCOL = 4;
    public static final int UPLOAD_RESULT_BAD_ARGUMENTS = 5;
    public static final int UPLOAD_RESULT_IO_ERROR = 6;
    public static final int UPLOAD_RESULT_FILE_NOT_FOUND = 7;
    public static final int UPLOAD_RESULT_ERROR_SMB_SERVER = 8;

    public static final String IMG_URI = "/Documents/IMG/";
    public static final String VIDEO_URI = "/Documents/VIDEO/";
    public static final String SLIDE_URI = "/Documents/SLIDE/";
    public static final String IMG_SCREEN = "/Documents/SCREEN/";   // изображения экранов

    private static boolean uploadThreadStarted = false;             // флаг активации загрузки файлов

    public static String[] destinationDirs = null;
    public static final int IMAGE = 0;
    public static final int VIDEO = 1;
    public static final int SLIDE = 2;
    public static final int SCREEN = 3;

    private Context context;
    private static SmbjWorker smbjWorker = null;
    private static SmbWorker smbWorker = null;
    private static FtpWorker ftpWorker = null;

    private static HashMap<String, Object> authParam;
    private static HashMap<String, Object> imgParam;
    private static HashMap<String, Object> videoParam;
    private static HashMap<String, Object> slideParam;
    private static HashMap<String, Object> screenImgParam;

    enum eResult {
        OK,
        DIR_ERROR,
        SD_CARD_ERROR
    }

    public UploadMedia(Context context) {

        this.context = context;

        authParam = new HashMap<>();
        imgParam = new HashMap<>();
        videoParam = new HashMap<>();
        slideParam = new HashMap<>();
        screenImgParam = new HashMap<>();

        //Инициализация всех возможных вариантов загрузки
        smbjWorker = new SmbjWorker(context);
        smbjWorker.onChangeStatusCallBack(smbjStatusCallback);
        smbjWorker.onEndDownloadCallBack(smbjEndDownloadCallback);

        smbWorker = new SmbWorker(context);
        smbWorker.onChangeSmbStatusCallBack(smbStatusCallback);
        smbWorker.onEndDownloadCallBack(smbEndDownloadCallback);

        ftpWorker = new FtpWorker(context);
        ftpWorker.onChangeStatusCallBack(ftpStatusCallback);
        ftpWorker.onEndDownloadCallBack(ftpEndDownloadCallback);
    }

    /**
     * Коллбэк обновления статуса загрузки SMB2  на WEB консоли
     */
    private SmbjWorker.SMBJ_StatusCallback smbjStatusCallback = new SmbjWorker.SMBJ_StatusCallback() {
        @Override
        public void onSmbjStatus(final String msg, final boolean delRemaininng) {
            MainActivity.httpServer.sendQueWebStatus(msg, delRemaininng);
        }
    };

    /**
     * Коллбэк окончания загрузки SMB2
     */
    private SmbjWorker.SMBJ_EndDownloadCallback smbjEndDownloadCallback = new SmbjWorker.SMBJ_EndDownloadCallback() {
        @Override
        public void onSmbjEndDownload(final int msg) {
            uploadThreadStarted = false;//флаг активации загрузки файлов
        }
    };
    /**
     * Коллбэк обновления статуса загрузки SMB1  на WEB консоли
     */
    private SmbWorker.SMB_StatusCallback smbStatusCallback = new SmbWorker.SMB_StatusCallback() {
        @Override
        public void onSmbStatus(final String msg, final boolean delRemaininng) {
            MainActivity.httpServer.sendQueWebStatus(msg, delRemaininng);
        }
    };
    /**
     * Коллбэк окончания загрузки SMB1
     */
    private SmbWorker.SMB_EndDownloadCallback smbEndDownloadCallback = new SmbWorker.SMB_EndDownloadCallback() {
        @Override
        public void onSmbEndDownload(final int msg) {
            uploadThreadStarted = false;//флаг активации загрузки файлов
        }
    };
/*******************************************************************************/
    /**
     * Коллбэк обновления статуса загрузки FTP на WEB консоли
     */

    private FtpWorker.FTP_StatusCallback ftpStatusCallback = new FtpWorker.FTP_StatusCallback() {
        @Override
        public void onFtpStatus(final String msg, final boolean delRemaininng) {
            MainActivity.httpServer.sendQueWebStatus(msg, delRemaininng);
        }
    };

    /**
     * Коллбэк окончания загрузки FTP
     */
    private FtpWorker.FTP_EndDownloadCallback ftpEndDownloadCallback = new FtpWorker.FTP_EndDownloadCallback() {
        @Override
        public void onFtpEndDownload(final int msg) {
            uploadThreadStarted = false;//флаг активации загрузки файлов
        }
    };

    /**
     * Инициация загрузки
     */
    public void upload() {
        if (uploadThreadStarted)
            return;
        MainActivity.httpServer.sendQueWebStatus("Завантаження...", true);

        eResult res = verifyDestinationDirs();
        if (res == eResult.DIR_ERROR) {
            MainActivity.httpServer.sendQueWebStatus("Помилка носiя SDCARD", true);
            return;
        } else if (res == eResult.SD_CARD_ERROR) {
            MainActivity.httpServer.sendQueWebStatus("Помилка носiя SDCARD", true);
            return;
        }

        //очистка хлама
        File lostDir = new File(ExtSDSource.getExternalSdCardPath() + "/LOST.DIR");
        if (lostDir.exists()) {
            if (lostDir.isDirectory()) {
                FileOperation.deleteRecursive(lostDir);
            }
        } else
            Log.w(TAG, "Dir:" + ExtSDSource.getExternalSdCardPath() + "/LOST.DIR" + " - not exist");

        PreferencesValues prefValues = PreferenceParams.getParameters();

        initLogFile();//инициализация лог файла для отправки на удаленный сервер

        ShareParam ParseImg = ParseSmbjFolders(prefValues.sSmbImg);
        ShareParam ParseVideo = ParseSmbjFolders(prefValues.sSmbVideo);
        ShareParam ParseSlide = ParseSmbjFolders(prefValues.sSmbSlide);
        ShareParam ParseScreen = ParseSmbjFolders(prefValues.sPathToScreenImg);

        if (!ParseImg.result) {
            Log.d(TAG, "Ошибка разбора параметров пути:" + prefValues.sSmbImg);
            MainActivity.httpServer.sendQueWebStatus("Неправильно вказанi параметри до ресурсу зображень : [" + prefValues.sSmbImg + "]", true);
            return;
        }
        if (!ParseVideo.result) {
            Log.d(TAG, "Ошибка разбора параметров пути :" + prefValues.sSmbVideo);
            MainActivity.httpServer.sendQueWebStatus("Неправильно вказанi параметри до ресурсу вiдео : [" + prefValues.sSmbVideo + "]", true);
            return;
        }
        if (!ParseSlide.result) {
            Log.d(TAG, "Ошибка разбора параметров пути :" + prefValues.sSmbSlide);
            MainActivity.httpServer.sendQueWebStatus("Неправильно вказанi параметри до ресурсу слайдiв : [" + prefValues.sSmbSlide + "]", true);
            return;
        }
        if (!ParseScreen.result) {
            Log.d(TAG, "Ошибка разбора параметров пути :" + prefValues.sPathToScreenImg);
            MainActivity.httpServer.sendQueWebStatus("Неправильно вказанi параметри до ресурсу фонових зображень : [" + prefValues.sPathToScreenImg + "]", true);
            return;
        }

        //подготовка параметров загрузки соответствующему загрузчику

        //параметры аутентификации
        authParam.clear();
        authParam.put("User", prefValues.sUser);
        authParam.put("Passw", prefValues.sPassw);
        authParam.put("Host", prefValues.sSmbHost);

        //изображения для товаров
        imgParam.clear();
        imgParam.put("shareImg", ParseImg.share);
        imgParam.put("folderImg", ParseImg.folder);
        imgParam.put("DestinationImg", destinationDirs[IMAGE]);
        imgParam.put("extensionArrayImg", new String[]{"*.png", "*.jpg"});

        //видео
        videoParam.clear();
        videoParam.put("shareVideo", ParseVideo.share);
        videoParam.put("folderVideo", ParseVideo.folder);
        videoParam.put("DestinationVideo", destinationDirs[VIDEO]);
        videoParam.put("extensionArrayVideo", new String[]{"*.avi", "*.mp4"});

        //изображения для слайдов
        slideParam.clear();
        slideParam.put("shareSlide", ParseSlide.share);
        slideParam.put("folderSlide", ParseSlide.folder);
        slideParam.put("DestinationSlide", destinationDirs[SLIDE]);
        slideParam.put("extensionArraySlide", new String[]{"*.png", "*.jpg"});

        //фоновые изображения экранов
        screenImgParam.clear();
        screenImgParam.put("shareScreenImg", ParseScreen.share);
        screenImgParam.put("folderScreenImg", ParseScreen.folder);
        screenImgParam.put("DestinationScreenImg", destinationDirs[SCREEN]);
        screenImgParam.put("extensionArrayScreenImg", new String[]{"*.png", "*.jpg"});

        if (prefValues.sProtocol.equals(DEF_PROTOCOL[SMB2])) {
            uploadThreadStarted = true;
            smbjWorker.doDownload(authParam, imgParam, videoParam, slideParam, screenImgParam);
        } else if (prefValues.sProtocol.equals(DEF_PROTOCOL[SMB1])) {
            uploadThreadStarted = true;
            smbWorker.doDownload(authParam, imgParam, videoParam, slideParam, screenImgParam);

        } else if (prefValues.sProtocol.equals(DEF_PROTOCOL[FTP])) {
            uploadThreadStarted = true;
            ftpWorker.doDownload(authParam, imgParam, videoParam, slideParam);
        }
    }

    /**
     * Парсер пути положения файлов на удаленном сервере
     *
     * @param params
     * @return ShareParam
     */
    public ShareParam ParseSmbjFolders(String params) {
        ShareParam shareParam = new ShareParam();
        shareParam.result = false;
        if (params.startsWith("/")) {
            int SlashEndShare = params.indexOf("/", 1);
            if (SlashEndShare >= 0) {
                shareParam.share = params.substring(1, SlashEndShare);
                shareParam.folder = params.substring(SlashEndShare + 1);
                shareParam.result = true;

            }
        }
        return shareParam;
    }

    /**
     * проверка наличия директорий для хранения данных, создание при необходимости
     *
     * @return
     */
    private eResult verifyDestinationDirs() {

        Log.d("SSS", "SD CARD isMounted:" + ExtSDSource.isMounted(context) + ", isReadOnly :" + ExtSDSource.isReadOnly());

        destinationDirs = new String[]{ExtSDSource.getExternalSdCardPath() + IMG_URI, ExtSDSource.getExternalSdCardPath() + VIDEO_URI, ExtSDSource.getExternalSdCardPath() + SLIDE_URI, ExtSDSource.getExternalSdCardPath() + IMG_SCREEN};

        if (!ExtSDSource.isMounted(context)) {
            showToast("ОТСУТСТВУЕТ SD карта");
            MainActivity.httpServer.sendQueWebStatus("Вiдсутнiй SD носiй", true);
            return eResult.SD_CARD_ERROR;
        }

        for (int i = 0; i < destinationDirs.length; i++) {
            //Проверка директории
            File dir = new File(destinationDirs[i]);
            if (!dir.exists()) {
                Log.d("SSS", "CREATE IMAGE DIR:" + destinationDirs);
                dir.mkdirs();
            }
            if (!dir.exists() || !dir.isDirectory()) {
                showToast("Ошибка создания хранилиша для данных");
                MainActivity.httpServer.sendQueWebStatus("Помилка створення директорії для зберігання даних", true);
                return eResult.DIR_ERROR;
            }
        }
        return eResult.OK;
    }

    private void showToast(String message) {
        new Handler(Looper.getMainLooper()).post(
                () -> Toast.makeText(context, message, Toast.LENGTH_LONG).show());
    }

    /**
     * Проверка наличия файла
     *
     * @param urlDest  путь к папке с файлами
     * @param fileName имя файла
     * @param sizeFile размер файла
     * @return
     */
    public static boolean ifAlreadyExistFile(String urlDest, String fileName, long sizeFile) {
        boolean result = false;
        File f = new File(urlDest + fileName);

        if (f.exists()) {
            if (f.length() == sizeFile) {
                result = true;
            }
        }
        return result;
    }

    /**
     * Сигнал для остановки демонстрации медиа
     */
    public static void resetMediaPlay() {
        Intent i = new Intent(VideoSlideService.VIDEO_SLIDE_RESET_TIME);
        MainActivity.context.sendBroadcast(i);
    }

    /**
     * Лог файл загрузки, предназначен для отправки на удаленный ресурс
     */
    private static void initLogFile() {
        Date currentDate = new Date();
        // convert date to calendar
        Calendar c = Calendar.getInstance();
        c.setTime(currentDate);
        c = Calendar.getInstance();
        c.setTime(currentDate);

        mLogFile = new File(Environment.getExternalStorageDirectory(), "Upload " + EthernetSettings.getNetworkInterfaceIpAddress() + " " + dateFormat.format(c.getTime()) + ".log");

        //удалим существующий файл
        if (mLogFile.exists()) {
            mLogFile.delete();

        }
        //создадим новый файл
        if (!mLogFile.exists()) {
            try {
                mLogFile.createNewFile();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * добавляет строку в лог-файл
     *
     * @param text
     */
    public static synchronized void appendToUploadLog(String text) {
        if (!mLogFile.exists())
            initLogFile();

        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        try {
            final FileWriter fileOut = new FileWriter(mLogFile, true);
            fileOut.append(sdf.format(new Date()) + " : " + text + NEW_LINE);
            fileOut.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Удаляет лог-файл
     */
    public static synchronized void deleteUploadLog() {
        //удалим существующий файл
        if (mLogFile.exists()) {
            mLogFile.delete();
        }
    }

    class ShareParam {
        public String share = "";
        public String folder = "";
        public boolean result = false;
    }
}
