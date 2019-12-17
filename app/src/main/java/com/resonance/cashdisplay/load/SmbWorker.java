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
        //Log.d(TAG, "SMB2 doDownload, Host:"+Host+",User:"+User+", Passw:"+Passw+",shareName:"+shareName+", shareFolder:"+shareFolder+",DestinationFolder:"+DestinationFolder);
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

            int error = 0;
            String ConnectionStr = "";
            HashMap<String, Object> auHashMap = params[0];
            HashMap<String, Object> imgHashMap = params[1];
            HashMap<String, Object> videoHashMap = params[2];
            HashMap<String, Object> slideHashMap = params[3];
            HashMap<String, Object> screenImgHashMap = params[4];

            String user = (String) auHashMap.get("User");
            String password = (String) auHashMap.get("Passw");
            String host = (String) auHashMap.get("Host");

            String shareImg = (String) imgHashMap.get("shareImg");
            String folderImg = (String) imgHashMap.get("folderImg");
            String destinationImg = (String) imgHashMap.get("DestinationImg");
            String[] extensionImg = (String[]) imgHashMap.get("extensionArrayImg");

            String shareVideo = (String) videoHashMap.get("shareVideo");
            String folderVideo = (String) videoHashMap.get("folderVideo");
            String destinationVideo = (String) videoHashMap.get("DestinationVideo");
            String[] extensionVideo = (String[]) videoHashMap.get("extensionArrayVideo");

            String shareSlide = (String) slideHashMap.get("shareSlide");
            String folderSlide = (String) slideHashMap.get("folderSlide");
            String destinationSlide = (String) slideHashMap.get("DestinationSlide");
            String[] extensionSlide = (String[]) slideHashMap.get("extensionArraySlide");

            String shareScreenImg = (String) screenImgHashMap.get("shareScreenImg");
            String folderScreenImg = (String) screenImgHashMap.get("folderScreenImg");
            String destinationScreenImg = (String) screenImgHashMap.get("DestinationScreenImg");
            String[] extensionScreenImg = (String[]) screenImgHashMap.get("extensionArrayScreenImg");

            Log.d(TAG, "SmbTask... ");

            try {
                //download IMG
                resultImg = handleFiles(user, password, host, shareImg, folderImg, destinationImg, extensionImg);
                //download Video
                resultVideo = handleFiles(user, password, host, shareVideo, folderVideo, destinationVideo, extensionVideo);
                //download Slides
                resultSlide = handleFiles(user, password, host, shareSlide, folderSlide, destinationSlide, extensionSlide);
                //download Screen images
                resultScreenImg = handleFiles(user, password, host, shareScreenImg, folderScreenImg, destinationScreenImg, extensionScreenImg);

              /*  ConnectionStr = Host + (shareImg.startsWith("/") ? "" : "/") + shareImg + (folderImg.startsWith("/") ? "" : "/") + folderImg;
                ChangeStatus(mContext.getString(R.string.connect_to_server) + ": " + ConnectionStr, true);
                SmbFile smb = ConnectToSource(ConnectionStr, User, Passw);
                if (smb == null) {

                    Log.d(TAG, "Соединение c " + ConnectionStr + " - не удалось");
                    ChangeStatus("підключення до сервера не вдалося:" + ConnectionStr, true);
                    error = DOWNLOAD_RESULT_CONNECTION_ERROR;
                    resultImg.HasError =  DOWNLOAD_RESULT_CONNECTION_ERROR;
                } else {
                    Log.d(TAG, "Соединение c " + ConnectionStr);
                    resultImg = DownloadFromShareFolder(smb, DestinationImg, extensionImg);
                }

                    //download Video
                    ConnectionStr = Host + (shareVideo.startsWith("/") ? "" : "/") + shareVideo + (folderVideo.startsWith("/") ? "" : "/") + folderVideo;
                    ChangeStatus(mContext.getString(R.string.connect_to_server) + ": " + ConnectionStr, true);
                    smb = ConnectToSource(ConnectionStr, User, Passw);
                    Log.d(TAG, "Соединение c " + ConnectionStr);
                    if (smb == null)
                    {
                        Log.d(TAG, "Соединение c " + ConnectionStr + " - не удалось");
                        ChangeStatus("Не вдалося підключення до сервера: " + ConnectionStr, false);
                        error = DOWNLOAD_RESULT_SHARE_CONNECTION_ERROR;
                        resultVideo.HasError= DOWNLOAD_RESULT_SHARE_CONNECTION_ERROR;
                    }else {
                        resultVideo = DownloadFromShareFolder(smb, DestinationVideo, extensionVideo);
                    }

                //download Slides
                ConnectionStr = Host + (shareSlide.startsWith("/") ? "" : "/") + shareSlide + (folderSlide.startsWith("/") ? "" : "/") + folderSlide;
                ChangeStatus(mContext.getString(R.string.connect_to_server) + ": " + ConnectionStr, true);
                smb = ConnectToSource(ConnectionStr, User, Passw);
                Log.d(TAG, "Соединение c " + ConnectionStr);
                if (smb == null) {
                    Log.d(TAG, "Соединение c " + ConnectionStr + " - не удалось");
                    ChangeStatus("Не вдалося підключення до сервера: " + ConnectionStr, false);
                    error = DOWNLOAD_RESULT_SHARE_CONNECTION_ERROR;
                    resultSlide.HasError= DOWNLOAD_RESULT_SHARE_CONNECTION_ERROR;
                }else {

                    resultSlide = DownloadFromShareFolder(smb, DestinationSlide, extensionSlide);
                }
                */
            } catch (Exception e) {
                Log.e(TAG, "Smb Exception: " + e);

                error = UPLOAD_RESULT_ERROR_SMB_SERVER;
                if (e.getMessage().contains("Could not connect") || e.getMessage().contains("failed to connect")) {
                    error = UPLOAD_RESULT_CONNECTION_ERROR;

                }
                if (e.getMessage().contains("IllegalArgumentException")) {
                    error = UPLOAD_RESULT_BAD_ARGUMENTS;
                }
            } finally {

                try {
                    //выгрузим на сервер лог загрузки
                    String connStr = host + (shareScreenImg.startsWith("/") ? "" : "/") + shareScreenImg + "/LOG/";//(folderScreenImg.startsWith("/") ? "" : "/") + folderScreenImg;
                    uplopadToSmb(user, password, connStr, UploadMedia.logFile);
                } catch (Exception e) {
                    Log.e(TAG, "Smb Exception: " + e);
                }
                //удалим лог, предназначеный для передачи на сервер
                UploadMedia.deleteUploadLog();

            }
            return error;
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
                            "<font color=\"blue\"><B>Фоновi (допомiжнi) зображення:</B><br></font>[" + (resultScreenImg.hasError > 0 ? extendedErrorScreenImg : "завантажено : <B>" + resultScreenImg.countFiles + "</B>, iснуючих : <B>" + resultScreenImg.countSkipped + "</B>, видалено : <B>" + resultScreenImg.countDeleted) + "</B>];  <br>" +
                                    "<font color=\"blue\"><B>Вiдео:</B><br></font>[" + (resultVideo.hasError > 0 ? extendedErrorVideo : "завантажено : <B>" + resultVideo.countFiles + "</B>, iснуючих : <B>" + resultVideo.countSkipped + "</B>, видалено : <B>" + resultVideo.countDeleted) + "</B>];  <br>" +
                                    "<font color=\"blue\"><B>Зображення товарiв:</B><br></font>[" + (resultImg.hasError > 0 ? extendedErrorImage : "завантажено : <B>" + resultImg.countFiles + "</B>, iснуючих : <B>" + resultImg.countSkipped + "</B>, видалено : <B>" + resultImg.countDeleted) + "</B>];  <br>" +
                                    "<font color=\"blue\"><B>Слайди:</B><br></font>[" + (resultSlide.hasError > 0 ? extendedErrorSlide : "завантажено : <B>" + resultSlide.countFiles + "</B>, iснуючих : <B>" + resultSlide.countSkipped + "</B>, видалено : <B>" + resultSlide.countDeleted) + "</B>];";
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

    private UploadResult handleFiles(String User, String Passw, String Host, String share, String source_folder, String destFolder, String[] extFiles) {
        UploadResult result = new UploadResult();
        result.hasError = UPLOAD_RESULT_SUCCESSFULL;

        String connectionStr = Host + (share.startsWith("/") ? "" : "/") + share + (source_folder.startsWith("/") ? "" : "/") + source_folder;
        UploadMedia.appendToUploadLog("(SMB1) " + connectionStr);
        changeStatus(mContext.getString(R.string.connect_to_server) + ": " + connectionStr, true);
        SmbFile smb = connectToSource(connectionStr, User, Passw, false);

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

    private SmbFile connectToSource(String url, String Usr, String Passw, boolean create) {
        String URL = url;
        NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication("", Usr, Passw);

        System.setProperty("jcifs.smb.client.responseTimeout", "2000"); // default: 30000 millisec.
        System.setProperty("jcifs.smb.client.soTimeout", "2000"); // default: 35000 millisec.

        boolean anonymous = ((Usr.length() == 0 && Passw.length() == 0) ? true : false);

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

    private UploadResult downloadFromShareFolder(SmbFile smb, String DestinationFolder, String[] extensionFile) {
        String[] filesAlreadyExists = null;//список файлов уже существующих
        UploadResult dr = new UploadResult();
        dr.hasError = UPLOAD_RESULT_SUCCESSFULL;
        dr.countFiles = 0;
        dr.countSkipped = 0;
        dr.countDeleted = 0;

        //получим список файлов уже существующих
        File dirSou = new File(DestinationFolder);
        if (dirSou.isDirectory()) {
            filesAlreadyExists = dirSou.list();
        }
        changeStatus("отримання списку файлів...", true);

        try {
            SmbFile[] arrSmbFiles = smb.listFiles();
            changeStatus("файлiв..." + arrSmbFiles.length, false);
            UploadMedia.appendToUploadLog("*** Файлов на обработку :" + arrSmbFiles.length + " ***");

            for (int i = 0; i < arrSmbFiles.length; i++) {
                UploadMedia.resetMediaPlay();//остановка демонстрации видео/слайдов
                //если файл существует, копировать не будем
                if (UploadMedia.ifAlreadyExistFile(DestinationFolder, arrSmbFiles[i].getName(), arrSmbFiles[i].length())) {
                    Log.d(TAG, "Smb file: " + arrSmbFiles[i].getPath() + "  - SKIPED");
                    UploadMedia.appendToUploadLog("Файл :" + arrSmbFiles[i].getPath() + " - пропущен");
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
                FileOutputStream out = new FileOutputStream(DestinationFolder + arrSmbFiles[i].getName());

                long t0 = System.currentTimeMillis();

                byte[] b = new byte[1024 * 500];
                long totalRead = 0;
                int readBytes = 0;
                long t1 = t0;
                int update_progress = 0;
                while ((readBytes = in.read(b)) > 0) {

                    // Log.d(TAG, "read bytes:" + totalRead);
                    out.write(b, 0, readBytes);
                    totalRead += readBytes;
                    if (update_progress++ > 50) {
                        update_progress = 0;
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

                UploadMedia.appendToUploadLog("Загружен : " + arrSmbFiles[i].getName() + ", размер : " + arrSmbFiles[i].length());

                File destination = new File(DestinationFolder, arrSmbFiles[i].getName());
                if (!destination.exists())
                    Log.e(TAG, "File [" + destination + "] NOT CREATED");


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
                            UploadMedia.appendToUploadLog("Удален : " + filesAlreadyExists[i]);
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

    private boolean uplopadToSmb(String User, String Passw, String remote_destination_path, File localFile) {

        InputStream in = null;
        OutputStream out = null;
        boolean result = false;
        try {
            //проверим наличие и создадим директорию
            SmbFile remoteDir = connectToSource(remote_destination_path, User, Passw, true);//

            //проверим наличие и создадим файл
            SmbFile remoteFile = connectToSource(remote_destination_path + localFile.getName(), User, Passw, true);

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
            Log.e(TAG, "UplopadToSmb: " + e);
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
