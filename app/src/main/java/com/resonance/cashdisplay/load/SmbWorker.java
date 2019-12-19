package com.resonance.cashdisplay.load;

import android.content.Context;
import android.os.AsyncTask;

import com.resonance.cashdisplay.ExtSDSource;
import com.resonance.cashdisplay.FileOperation;
import com.resonance.cashdisplay.Log;
import com.resonance.cashdisplay.MainActivity;
import com.resonance.cashdisplay.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.HashMap;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;

import static com.resonance.cashdisplay.load.UploadMedia.UPLOAD_RESULT_BAD_ARGUMENTS;
import static com.resonance.cashdisplay.load.UploadMedia.UPLOAD_RESULT_CONNECTION_ERROR;
import static com.resonance.cashdisplay.load.UploadMedia.UPLOAD_RESULT_ERROR_SMB_SERVER;
import static com.resonance.cashdisplay.load.UploadMedia.UPLOAD_RESULT_IO_ERROR;
import static com.resonance.cashdisplay.load.UploadMedia.UPLOAD_RESULT_NOT_FREE_MEMORY;
import static com.resonance.cashdisplay.load.UploadMedia.UPLOAD_RESULT_NOT_SUPPORT_PROTOCOL;
import static com.resonance.cashdisplay.load.UploadMedia.UPLOAD_RESULT_SHARE_CONNECTION_ERROR;
import static com.resonance.cashdisplay.load.UploadMedia.UPLOAD_RESULT_SUCCESSFULL;

public class SmbWorker {

    public final String TAG = "SmbWorker";
    private static final String NEW_LINE = System.getProperty("line.separator");

    private Context mContext;

    public SmbWorker(Context context) {
        mContext = context;
        // jcifs.Config.registerSmbURLHandler();
    }

    /**** колбэк статуса выполнения загрузки *********************/
    public interface SmbStatusCallback {
        void onSmbStatusChanged(String status, boolean delRemaining);
    }

    private static SmbStatusCallback smbStatusCallback;

    public void setSmbStatusCallBack(SmbStatusCallback cback) {
        smbStatusCallback = cback;
    }

    private void changeStatus(String status, boolean delRemaining) {
        smbStatusCallback.onSmbStatusChanged(status, delRemaining);
    }

    /**** колбэк окончания загрузки *********************/
    public interface SmbEndDownloadCallback {
        void onSmbEndDownload(int status);
    }

    private static SmbEndDownloadCallback smbEndDownloadCallback;

    public void setEndDownloadCallback(SmbEndDownloadCallback cback) {
        smbEndDownloadCallback = cback;
    }

    private void setEventEndDownload(int status) {
        smbEndDownloadCallback.onSmbEndDownload(status);
    }

    /*****************************************************************************************************/
    public void doDownload(HashMap<String, Object> auHashMap,
                           HashMap<String, Object> imgHashMap,
                           HashMap<String, Object> videoHashMap,
                           HashMap<String, Object> slideHashMap,
                           HashMap<String, Object> screenImgHashMap) {
        new SmbTask().execute(auHashMap, imgHashMap, videoHashMap, slideHashMap, screenImgHashMap);
    }

    /***********************************************************************************************/

    private class SmbTask extends AsyncTask<HashMap<String, Object>, Void, Integer> {

        UploadResult resultImg = new UploadResult();
        UploadResult resultVideo = new UploadResult();
        UploadResult resultSlide = new UploadResult();
        UploadResult resultScreenImg = new UploadResult();

        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Integer doInBackground(HashMap<String, Object>... params) {

            HashMap<String, Object> auHashMap = params[0];
            HashMap<String, Object> imgHashMap = params[1];
            HashMap<String, Object> videoHashMap = params[2];
            HashMap<String, Object> slideHashMap = params[3];
            HashMap<String, Object> screenImgHashMap = params[4];

            String user = (String) auHashMap.get("user");
            String password = (String) auHashMap.get("passw");
            String host = (String) auHashMap.get("host");

            String shareImg = (String) imgHashMap.get("shareImg");
            String folderImg = (String) imgHashMap.get("folderImg");
            String destImg = (String) imgHashMap.get("destImg");
            String[] extImg = (String[]) imgHashMap.get("extArrayImg");

            String shareVideo = (String) videoHashMap.get("shareVideo");
            String folderVideo = (String) videoHashMap.get("folderVideo");
            String destVideo = (String) videoHashMap.get("destVideo");
            String[] extVideo = (String[]) videoHashMap.get("extArrayVideo");

            String shareSlide = (String) slideHashMap.get("shareSlide");
            String folderSlide = (String) slideHashMap.get("folderSlide");
            String destSlide = (String) slideHashMap.get("destSlide");
            String[] extSlide = (String[]) slideHashMap.get("extArraySlide");

            String shareScreenImg = (String) screenImgHashMap.get("shareScreenImg");
            String folderScreenImg = (String) screenImgHashMap.get("folderScreenImg");
            String destScreenImg = (String) screenImgHashMap.get("destScreenImg");
            String[] extScreenImg = (String[]) screenImgHashMap.get("extArrayScreenImg");

            int result = 0;

            Log.d(TAG, "SmbTask... ");

            try {
                // download IMG
                resultImg = handleFiles(user, password, host, shareImg, folderImg, destImg, extImg);
                // download Video
                resultVideo = handleFiles(user, password, host, shareVideo, folderVideo, destVideo, extVideo);
                // download Slides
                resultSlide = handleFiles(user, password, host, shareSlide, folderSlide, destSlide, extSlide);
                // download Screen images
                resultScreenImg = handleFiles(user, password, host, shareScreenImg, folderScreenImg, destScreenImg, extScreenImg);

            } catch (Exception e) {
                Log.e(TAG, "Smb Exception: " + e);

                result = UPLOAD_RESULT_ERROR_SMB_SERVER;
                if (e.getMessage().contains("Could not connect") || e.getMessage().contains("failed to connect")) {
                    result = UPLOAD_RESULT_CONNECTION_ERROR;
                }
                if (e.getMessage().contains("IllegalArgumentException")) {
                    result = UPLOAD_RESULT_BAD_ARGUMENTS;
                }
            } finally {
                try {
                    //выгрузим на сервер лог загрузки
                    String connStr = host + (shareScreenImg.startsWith("/") ? "" : "/") + shareScreenImg + "/LOG/";//(folderScreenImg.startsWith("/") ? "" : "/") + folderScreenImg;
                    uploadToSmb(user, password, connStr, UploadMedia.logFile);
                    UploadMedia.deleteUploadLog();
                } catch (Exception e) {
                    Log.e(TAG, "Smb Exception: " + e);
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            Log.d(TAG, "SmbTask result : " + result);

            String extendedErrorScreenImg = "підключення до сервера не вдалося";
            String extendedErrorImage = "підключення до сервера не вдалося";
            String extendedErrorSlide = "підключення до сервера не вдалося";
            String extendedErrorVideo = "підключення до сервера не вдалося";

            if (resultScreenImg.hasError == UPLOAD_RESULT_NOT_FREE_MEMORY)
                extendedErrorScreenImg = "недостатньо пам'ятi";

            if (resultImg.hasError == UPLOAD_RESULT_NOT_FREE_MEMORY)
                extendedErrorImage = "недостатньо пам'ятi";

            if (resultVideo.hasError == UPLOAD_RESULT_NOT_FREE_MEMORY)
                extendedErrorVideo = "недостатньо пам'ятi";

            if (resultSlide.hasError == UPLOAD_RESULT_NOT_FREE_MEMORY)
                extendedErrorSlide = "недостатньо пам'ятi";

            switch (result) {
                case UPLOAD_RESULT_SUCCESSFULL:
                case UPLOAD_RESULT_SHARE_CONNECTION_ERROR:
                    String status =
                            "<font color=\"blue\"><B>Фоновi та допомiжнi зображення:</B><br></font>[" + (resultScreenImg.hasError > 0 ? extendedErrorScreenImg : ("завантажено : <B>" + resultScreenImg.countFiles + "</B>, iснуючих : <B>" + resultScreenImg.countSkipped + "</B>, видалено : <B>" + resultScreenImg.countDeleted) + "</B>];  <br>") +
                                    "<font color=\"blue\"><B>Зображення товарiв:</B><br></font>[" + (resultImg.hasError > 0 ? extendedErrorImage : ("завантажено : <B>" + resultImg.countFiles + "</B>, iснуючих : <B>" + resultImg.countSkipped + "</B>, видалено : <B>" + resultImg.countDeleted) + "</B>];  <br>") +
                                    "<font color=\"blue\"><B>Вiдео:</B><br></font>[" + (resultVideo.hasError > 0 ? extendedErrorVideo : ("завантажено : <B>" + resultVideo.countFiles + "</B>, iснуючих : <B>" + resultVideo.countSkipped + "</B>, видалено : <B>" + resultVideo.countDeleted) + "</B>];  <br>") +
                                    "<font color=\"blue\"><B>Слайди:</B><br></font>[" + (resultSlide.hasError > 0 ? extendedErrorSlide : ("завантажено : <B>" + resultSlide.countFiles + "</B>, iснуючих : <B>" + resultSlide.countSkipped + "</B>, видалено : <B>" + resultSlide.countDeleted) + "</B>];");
                    changeStatus(status, true);
                    break;
                case UPLOAD_RESULT_NOT_SUPPORT_PROTOCOL:
                    changeStatus("Сервер не підтримує протокол ", true);
                    break;
                case UPLOAD_RESULT_CONNECTION_ERROR:
                    changeStatus("Неможливо пiдключитися до файлового сервера", true);
                    break;
                case UPLOAD_RESULT_BAD_ARGUMENTS:
                    changeStatus("Невiрнi параметри пiдключення до файлового сервера", true);
                    break;
                case UPLOAD_RESULT_NOT_FREE_MEMORY:
                    changeStatus("недостатньо пам'яті на носії" + ExtSDSource.getAvailableMemory_SD(), true);
                    break;
                case UPLOAD_RESULT_ERROR_SMB_SERVER:
                    changeStatus("Помилка сервера SMB", true);
                    break;
            }
            setEventEndDownload(result);
        }
    }

    private UploadResult handleFiles(String user, String passw, String host, String share, String sourceFolder, String destFolder, String[] extFiles) {
        UploadResult result = new UploadResult();

        String connectionStr = host + (share.startsWith("/") ? "" : "/") + share + (sourceFolder.startsWith("/") ? "" : "/") + sourceFolder;
        UploadMedia.appendToUploadLog("\n(SMB1) " + connectionStr);
        changeStatus(mContext.getString(R.string.connect_to_server) + ": " + connectionStr, true);

        SmbFile smb = connectToSource(connectionStr, user, passw, false);

        if (smb == null) {
            Log.d(TAG, "Соединение c " + connectionStr + " - не удалось");
            changeStatus("підключення до сервера не вдалося:" + connectionStr, true);
            result.hasError = UPLOAD_RESULT_CONNECTION_ERROR;
        } else {
            Log.d(TAG, "Соединение c " + connectionStr);
            result = downloadFromShareFolder(smb, destFolder, extFiles);
        }
        return result;
    }

    private SmbFile connectToSource(String url, String usr, String passw, boolean create) {
        String URL = url;
        NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication("", usr, passw);

        System.setProperty("jcifs.smb.client.responseTimeout", "2000"); // default: 30000 millisec.
        System.setProperty("jcifs.smb.client.soTimeout", "2000"); // default: 35000 millisec.

        boolean anonymous = ((usr.length() == 0 && passw.length() == 0) ? true : false);

        SmbFile f = null;

        if (url.startsWith("//"))
            URL = url.substring(2);

        try {
            f = new SmbFile("smb://" + URL, (anonymous ? NtlmPasswordAuthentication.ANONYMOUS : auth));
            if (f == null) return null;
            if (create && (!f.exists())) {
                if (url.endsWith("/"))
                    f.mkdir();
                else
                    f.createNewFile();
            }
            if (!f.exists()) return null;
            f.connect();

        } catch (SmbException e) {
            Log.e(TAG, "SmbException:" + e.getMessage() + " " + e);
            if (e.getMessage().toString().contains("Logon failure"))
                changeStatus("невiрнi параметри аутентифiкацii", true);
            else if (e.getMessage().toString().contains("Access is denied"))
                changeStatus("У доступі відмовлено :" + URL, true);
            else if (e.getMessage().toString().contains("UnknownHostException"))
                changeStatus("не знайдено ресурс :" + URL, true);
            else {
                changeStatus("Не вдалося підключення до сервера: " + URL, false);
            }
            return null;
        } catch (MalformedURLException e) {
            Log.e(TAG, "MalformedURLException:" + e.getMessage());
            changeStatus("Не вдалося підключення до сервера", false);
            return null;
        } catch (IOException e) {
            Log.e(TAG, "IOException:" + e.getMessage());
        }
        return f;
    }

    private UploadResult downloadFromShareFolder(SmbFile smb, String destFolder, String[] extensionFile) {
        String[] filesAlreadyExists = null;//список файлов уже существующих
        UploadResult dr = new UploadResult();
        dr.hasError = UPLOAD_RESULT_SUCCESSFULL;
        dr.countFiles = 0;
        dr.countSkipped = 0;
        dr.countDeleted = 0;

        //получим список файлов уже существующих
        File dirSou = new File(destFolder);
        if (dirSou.isDirectory()) {
            filesAlreadyExists = dirSou.list();
        }
        changeStatus("отримання списку файлів...", true);

        try {
            SmbFile[] arrSmbFiles = smb.listFiles();
            changeStatus("файлiв..." + arrSmbFiles.length, false);
            UploadMedia.appendToUploadLog("*** Файлов для загрузки :" + arrSmbFiles.length + " ***");

            for (int i = 0; i < arrSmbFiles.length; i++) {
                UploadMedia.resetMediaPlay();//остановка демонстрации видео/слайдов
                //если файл существует, копировать не будем
                if (UploadMedia.ifAlreadyExistFile(destFolder, arrSmbFiles[i].getName(), arrSmbFiles[i].length())) {
                    Log.d(TAG, "Smb file: " + arrSmbFiles[i].getPath() + "  - SKIPED");
                    UploadMedia.appendToUploadLog("Перезаписан: " + arrSmbFiles[i].getPath());
                    dr.countSkipped++;
                    if (dr.countSkipped % 10 == 0)
                        changeStatus("пропущено..." + arrSmbFiles[i].getPath(), true);
                    continue;
                }

                if (ExtSDSource.getAvailableMemory(MainActivity.context, ExtSDSource.DEFAULT_SD) < arrSmbFiles[i].length()) {
                    Log.e(TAG, "Not anougth memory");
                    UploadMedia.appendToUploadLog("Недостаточно памяти на SD карте: " + ExtSDSource.getAvailableMemory(MainActivity.context, ExtSDSource.DEFAULT_SD));
                    dr.hasError = UPLOAD_RESULT_NOT_FREE_MEMORY;
                    break;
                }

                //фильтр по расширению файла
                boolean isValidExtension = false;
                for (int a = 0; a < extensionFile.length; a++) {
                    if (arrSmbFiles[i].toString().endsWith(extensionFile[a].substring(2))) {
                        isValidExtension = true;
                        break;
                    }
                }

                if (!isValidExtension) continue;

                Log.d(TAG, "Download File [" + i + "] " + arrSmbFiles[i].getPath());
                SmbFileInputStream in = new SmbFileInputStream(arrSmbFiles[i]);
                FileOutputStream out = new FileOutputStream(destFolder + arrSmbFiles[i].getName());

                long t0 = System.currentTimeMillis();

                byte[] b = new byte[1024 * 500];
                long totalRead = 0;
                int readBytes = 0;
                long t1 = t0;
                int updateProgress = 0;
                while ((readBytes = in.read(b)) > 0) {

                    // Log.d(TAG, "read bytes:" + totalRead);
                    out.write(b, 0, readBytes);
                    totalRead += readBytes;
                    if (updateProgress++ > 50) {
                        updateProgress = 0;
                        UploadMedia.resetMediaPlay();//остановка демонстрации видео/слайдов
                        changeStatus("Завантаження  " + arrSmbFiles[i].getName() + " - " + (int) ((100 * totalRead) / arrSmbFiles[i].length()) + " %,  загалом " + (int) ((100 * i) / arrSmbFiles.length) + " %", true);
                    }
                }
                long t = System.currentTimeMillis() - t0;

                Log.d(TAG, totalRead + " bytes transfered in " + (t / 1000) + " seconds at " + ((totalRead / 1000) / Math.max(1, (t / 1000))) + "Kbytes/sec");
                changeStatus("Завантаження  " + (int) ((100 * i) / arrSmbFiles.length) + " %", true);

                in.close();
                out.close();

                dr.countFiles++;

                File destination = new File(destFolder, arrSmbFiles[i].getName());
                if (!destination.exists())
                    Log.e(TAG, "File [" + destination + "] NOT CREATED");
                else
                    UploadMedia.appendToUploadLog("Загружен:    " + arrSmbFiles[i].getName() + ", размер : " + arrSmbFiles[i].length());
            }

            //удалим неиспользуемые файлы
            //if (dr.CountFiles>0)
            {
                if (filesAlreadyExists != null) {

                    for (int i = 0; i < filesAlreadyExists.length; i++) {
                        boolean forDelete = true;
                        for (int y = 0; y < arrSmbFiles.length; y++) {
                            if (arrSmbFiles[y].getName().equals(filesAlreadyExists[i])) {
                                forDelete = false;
                                break;
                            }
                        }
                        if (forDelete) {
                            Log.w(TAG, "To delete: " + filesAlreadyExists[i]);

                            FileOperation.deleteFile(dirSou.getCanonicalPath() + "/" + filesAlreadyExists[i]);
                            UploadMedia.appendToUploadLog("Удалён:      " + filesAlreadyExists[i]);
                            dr.countDeleted++;
                            if (dr.countDeleted % 10 == 0)
                                changeStatus("Видалення..." + filesAlreadyExists[i], true);
                            //new File(DirSou, FilesAlreadyExists[i]).delete();
                        }
                    }
                }
            }
        } catch (SmbException e) {
            Log.e(TAG, "SmbException:" + e.getMessage() + " " + e);
            // showToast("ОШИБКА ЗАГРУЗКИ :\n"+e.getMessage());

            changeStatus("ПОМИЛКА ЗАВАНТАЖЕННЯ :" + e.getMessage(), false);
            dr.hasError = UPLOAD_RESULT_ERROR_SMB_SERVER;
        } catch (IOException e) {
            Log.e(TAG, "IOException:" + e.getMessage());

            changeStatus("ПОМИЛКА ЗАВАНТАЖЕННЯ :" + e.getMessage(), false);
            dr.hasError = UPLOAD_RESULT_IO_ERROR;
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e.getMessage());

            changeStatus("ПОМИЛКА ЗАВАНТАЖЕННЯ :" + e.getMessage(), false);
            dr.hasError = UPLOAD_RESULT_IO_ERROR;
        }
        return dr;
    }

    private boolean uploadToSmb(String user, String passw, String remotedestPath, File localFile) {

        InputStream in = null;
        OutputStream out = null;
        boolean result = false;
        try {
            // проверим наличие и создадим директорию
            SmbFile remoteDir = connectToSource(remotedestPath, user, passw, true);

            // проверим наличие и создадим файл
            SmbFile remoteFile = connectToSource(remotedestPath + localFile.getName(), user, passw, true);

            in = new BufferedInputStream(new FileInputStream(localFile));
            out = new BufferedOutputStream(new SmbFileOutputStream(remoteFile));

            byte[] buffer = new byte[4096];
            int len = 0; //Read length
            while ((len = in.read(buffer, 0, buffer.length)) != -1) {
                out.write(buffer, 0, len);
            }
            out.flush(); //The refresh buffer output stream
            result = true;
        } catch (Exception e) {
            Log.e(TAG, "UploadToSmb: " + e);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (Exception e) {
            }
        }
        return result;
    }
}
