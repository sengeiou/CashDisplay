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
    public interface SMB_StatusCallback {
        void onSmbStatus(String status, boolean delRemaininng);
    }

    private static SMB_StatusCallback callback_onSmbStatus;

    public void onChangeSmbStatusCallBack(SMB_StatusCallback cback) {
        callback_onSmbStatus = cback;
    }

    private void ChangeStatus(String status, boolean delRemaininng) {
        callback_onSmbStatus.onSmbStatus(status, delRemaininng);
    }

    /**** колбэк окончания загрузки *********************/
    public interface SMB_EndDownloadCallback {
        void onSmbEndDownload(int status);
    }

    private static SMB_EndDownloadCallback callback_onEndSmbDownload;

    public void onEndDownloadCallBack(SMB_EndDownloadCallback cback) {
        callback_onEndSmbDownload = cback;
    }

    private void setVentEndDownload(int status) {
        callback_onEndSmbDownload.onSmbEndDownload(status);
    }

    /*****************************************************************************************************/
    public void doDownload(HashMap<String, Object> au_hashMap,
                           HashMap<String, Object> img_hashMap,
                           HashMap<String, Object> video_hashMap,
                           HashMap<String, Object> slide_hashMap,
                           HashMap<String, Object> screen_img_hashMap) {
        //Log.d(TAG, "SMB2 doDownload, Host:"+Host+",User:"+User+", Passw:"+Passw+",shareName:"+shareName+", shareFolder:"+shareFolder+",DestinationFolder:"+DestinationFolder);
        new SmbTask().execute(au_hashMap, img_hashMap, video_hashMap, slide_hashMap, screen_img_hashMap);
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
            HashMap<String, Object> au_hashMap = params[0];
            HashMap<String, Object> img_hashMap = params[1];
            HashMap<String, Object> video_hashMap = params[2];
            HashMap<String, Object> slide_hashMap = params[3];
            HashMap<String, Object> screenimg_hashMap = params[4];

            String User = (String) au_hashMap.get("User");
            String Passw = (String) au_hashMap.get("Passw");
            String Host = (String) au_hashMap.get("Host");

            String shareImg = (String) img_hashMap.get("shareImg");
            String folderImg = (String) img_hashMap.get("folderImg");
            String DestinationImg = (String) img_hashMap.get("DestinationImg");
            String[] extensionImg = (String[]) img_hashMap.get("extensionArrayImg");

            String shareVideo = (String) video_hashMap.get("shareVideo");
            String folderVideo = (String) video_hashMap.get("folderVideo");
            String DestinationVideo = (String) video_hashMap.get("DestinationVideo");
            String[] extensionVideo = (String[]) video_hashMap.get("extensionArrayVideo");

            String shareSlide = (String) slide_hashMap.get("shareSlide");
            String folderSlide = (String) slide_hashMap.get("folderSlide");
            String DestinationSlide = (String) slide_hashMap.get("DestinationSlide");
            String[] extensionSlide = (String[]) slide_hashMap.get("extensionArraySlide");

            String shareScreenImg = (String) screenimg_hashMap.get("shareScreenImg");
            String folderScreenImg = (String) screenimg_hashMap.get("folderScreenImg");
            String DestinationScreenImg = (String) screenimg_hashMap.get("DestinationScreenImg");
            String[] extensionScreenImg = (String[]) screenimg_hashMap.get("extensionArrayScreenImg");

            Log.d(TAG, "SmbTask... ");

            try {
                //download Screen images
                resultScreenImg = HandlerFiles(User, Passw, Host, shareScreenImg, folderScreenImg, DestinationScreenImg, extensionScreenImg);
                //download IMG
                resultImg = HandlerFiles(User, Passw, Host, shareImg, folderImg, DestinationImg, extensionImg);
                //download Video
                resultVideo = HandlerFiles(User, Passw, Host, shareVideo, folderVideo, DestinationVideo, extensionVideo);
                //download Slides
                resultSlide = HandlerFiles(User, Passw, Host, shareSlide, folderSlide, DestinationSlide, extensionSlide);

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
                    String ConnStr = Host + (shareScreenImg.startsWith("/") ? "" : "/") + shareScreenImg + "/LOG/";//(folderScreenImg.startsWith("/") ? "" : "/") + folderScreenImg;
                    uplopadToSmb(User, Passw, ConnStr, UploadMedia.logFile);
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

            String ExtendedError_image = "підключення до сервера не вдалося";
            String ExtendedError_slide = "підключення до сервера не вдалося";
            String ExtendedError_video = "підключення до сервера не вдалося";
            String ExtendedError_screenImg = "підключення до сервера не вдалося";

            if (resultImg.hasError == UPLOAD_RESULT_NOT_FREE_MEMORY)
                ExtendedError_image = "недостатньо пам'ятi";

            if (resultVideo.hasError == UPLOAD_RESULT_NOT_FREE_MEMORY)
                ExtendedError_video = "недостатньо пам'ятi";

            if (resultSlide.hasError == UPLOAD_RESULT_NOT_FREE_MEMORY)
                ExtendedError_slide = "недостатньо пам'ятi";

            switch (result) {
                case UPLOAD_RESULT_SUCCESSFULL:
                case UPLOAD_RESULT_SHARE_CONNECTION_ERROR:

                    String status =
                            "<font color=\"blue\"><B>Фоновi зображення:</B><br></font>[" + (resultScreenImg.hasError > 0 ? ExtendedError_screenImg : "завантажено : <B>" + resultScreenImg.countFiles + "</B>, iснуючих : <B>" + resultScreenImg.countSkipped + "</B>, видалено : <B>" + resultScreenImg.countDeleted) + "</B>];  <br>" +
                                    "<font color=\"blue\"><B>Вiдео:</B><br></font>[" + (resultVideo.hasError > 0 ? ExtendedError_video : "завантажено : <B>" + resultVideo.countFiles + "</B>, iснуючих : <B>" + resultVideo.countSkipped + "</B>, видалено : <B>" + resultVideo.countDeleted) + "</B>];  <br>" +
                                    "<font color=\"blue\"><B>Зображення товарiв:</B><br></font>[" + (resultImg.hasError > 0 ? ExtendedError_image : "завантажено : <B>" + resultImg.countFiles + "</B>, iснуючих : <B>" + resultImg.countSkipped + "</B>, видалено : <B>" + resultImg.countDeleted) + "</B>];  <br>" +
                                    "<font color=\"blue\"><B>Слайди:</B><br></font>[" + (resultSlide.hasError > 0 ? ExtendedError_slide : "завантажено : <B>" + resultSlide.countFiles + "</B>, iснуючих : <B>" + resultSlide.countSkipped + "</B>, видалено : <B>" + resultSlide.countDeleted) + "</B>];";
                    ChangeStatus(status, true);
                    break;
                case UPLOAD_RESULT_NOT_SUPPORT_PROTOCOL:
                    ChangeStatus("Сервер не підтримує протокол ", true);
                    break;
                case UPLOAD_RESULT_CONNECTION_ERROR:
                    ChangeStatus("Неможливо пiдключитися до файлового сервера", true);
                    break;
                case UPLOAD_RESULT_BAD_ARGUMENTS:
                    ChangeStatus("Невiрнi параметри пiдключення до файлового сервера", true);
                    break;
                case UPLOAD_RESULT_NOT_FREE_MEMORY:
                    ChangeStatus("недостатньо пам'яті на носії" + ExtSDSource.getAvailableMemory_SD(), true);
                    break;
                case UPLOAD_RESULT_ERROR_SMB_SERVER:
                    ChangeStatus("Помилка сервера SMB", true);
                    break;

            }
            setVentEndDownload(result);

        }
    }

    private UploadResult HandlerFiles(String User, String Passw, String Host, String share, String source_folder, String destination_folder, String[] extension_files) {
        UploadResult result = new UploadResult();
        result.hasError = UPLOAD_RESULT_SUCCESSFULL;

        String ConnectionStr = Host + (share.startsWith("/") ? "" : "/") + share + (source_folder.startsWith("/") ? "" : "/") + source_folder;
        UploadMedia.appendToUploadLog("(SMB1) " + ConnectionStr);
        ChangeStatus(mContext.getString(R.string.connect_to_server) + ": " + ConnectionStr, true);
        SmbFile smb = ConnectToSource(ConnectionStr, User, Passw, false);

        if (smb == null) {

            Log.d(TAG, "Соединение c " + ConnectionStr + " - не удалось");
            ChangeStatus("підключення до сервера не вдалося:" + ConnectionStr, true);
            result.hasError = UPLOAD_RESULT_CONNECTION_ERROR;
        } else {
            Log.d(TAG, "Соединение c " + ConnectionStr);
            result = DownloadFromShareFolder(smb, destination_folder, extension_files);
        }
        return result;
    }

    private SmbFile ConnectToSource(String url, String Usr, String Passw, boolean create) {
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
                ChangeStatus("невiрнi параметри аутентифiкацii", true);
            else if (e.getMessage().toString().contains("Access is denied"))
                ChangeStatus("У доступі відмовлено :" + URL, true);
            else if (e.getMessage().toString().contains("UnknownHostException"))
                ChangeStatus("не знайдено ресурс :" + URL, true);
            else {

                ChangeStatus("Не вдалося підключення до сервера: " + URL, false);
            }
            return null;
        } catch (MalformedURLException e) {
            Log.e(TAG, "MalformedURLException:" + e.getMessage());
            ChangeStatus("Не вдалося підключення до сервера", false);
            return null;
        } catch (IOException e) {
            Log.e(TAG, "IOException:" + e.getMessage());
        }
        return f;
    }

    private UploadResult DownloadFromShareFolder(SmbFile smb, String DestinationFolder, String[] extensionFile) {
        String[] FilesAlreadyExists = null;//список файлов уже существующих
        UploadResult dr = new UploadResult();
        dr.hasError = UPLOAD_RESULT_SUCCESSFULL;
        dr.countFiles = 0;
        dr.countSkipped = 0;
        dr.countDeleted = 0;

        //получим список файлов уже существующих
        File dirSou = new File(DestinationFolder);
        if (dirSou.isDirectory()) {
            FilesAlreadyExists = dirSou.list();
        }
        ChangeStatus("отримання списку файлів...", true);

        try {
            SmbFile[] arr_smb_files = smb.listFiles();
            ChangeStatus("файлiв..." + arr_smb_files.length, false);
            UploadMedia.appendToUploadLog("*** Файлов на обработку :" + arr_smb_files.length + " ***");

            for (int i = 0; i < arr_smb_files.length; i++) {
                UploadMedia.resetMediaPlay();//остановка демонстрации видео/слайдов
                //если файл существует, копировать не будем
                if (UploadMedia.ifAlreadyExistFile(DestinationFolder, arr_smb_files[i].getName(), arr_smb_files[i].length())) {
                    Log.d(TAG, "Smb file: " + arr_smb_files[i].getPath() + "  - SKIPED");
                    UploadMedia.appendToUploadLog("Файл :" + arr_smb_files[i].getPath() + " - пропущен");
                    dr.countSkipped++;
                    if (dr.countSkipped % 10 == 0)
                        ChangeStatus("пропущено..." + arr_smb_files[i].getPath(), true);
                    continue;
                }

                if (ExtSDSource.getAvailableMemory(MainActivity.context, ExtSDSource.DEFAULT_SD) < arr_smb_files[i].length()) {
                    Log.e(TAG, "Not anougth memory");
                    UploadMedia.appendToUploadLog("Недостаточно памяти на SD карте: " + ExtSDSource.getAvailableMemory(MainActivity.context, ExtSDSource.DEFAULT_SD));
                    dr.hasError = UPLOAD_RESULT_NOT_FREE_MEMORY;
                    break;
                }

                //фильтр по расширению файла
                boolean isValidExtension = false;
                for (int a = 0; a < extensionFile.length; a++) {
                    if (arr_smb_files[i].toString().endsWith(extensionFile[a].substring(2))) {
                        isValidExtension = true;
                        break;
                    }
                }

                if (!isValidExtension) continue;

                Log.d(TAG, "Download File [" + i + "] " + arr_smb_files[i].getPath());
                SmbFileInputStream in = new SmbFileInputStream(arr_smb_files[i]);
                FileOutputStream out = new FileOutputStream(DestinationFolder + arr_smb_files[i].getName());

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
                        ChangeStatus("Завантаження  " + arr_smb_files[i].getName() + " - " + (int) ((100 * totalRead) / arr_smb_files[i].length()) + " %,  загалом " + (int) ((100 * i) / arr_smb_files.length) + " %", true);
                    }
                }
                long t = System.currentTimeMillis() - t0;

                Log.d(TAG, totalRead + " bytes transfered in " + (t / 1000) + " seconds at " + ((totalRead / 1000) / Math.max(1, (t / 1000))) + "Kbytes/sec");
                ChangeStatus("Завантаження  " + (int) ((100 * i) / arr_smb_files.length) + " %", true);

                in.close();
                out.close();


                dr.countFiles++;

                UploadMedia.appendToUploadLog("Загружен : " + arr_smb_files[i].getName() + ", размер : " + arr_smb_files[i].length());

                File destination = new File(DestinationFolder, arr_smb_files[i].getName());
                if (!destination.exists())
                    Log.e(TAG, "File [" + destination + "] NOT CREATED");


            }

            //удалим неиспользуемые файлы
            //if (dr.CountFiles>0)
            {
                if (FilesAlreadyExists != null) {

                    for (int i = 0; i < FilesAlreadyExists.length; i++) {
                        boolean forDelete = true;
                        for (int y = 0; y < arr_smb_files.length; y++) {
                            if (arr_smb_files[y].getName().equals(FilesAlreadyExists[i])) {
                                forDelete = false;
                                break;
                            }
                        }
                        if (forDelete) {
                            Log.w(TAG, "To delete: " + FilesAlreadyExists[i]);

                            FileOperation.deleteFile(dirSou.getCanonicalPath() + "/" + FilesAlreadyExists[i]);
                            UploadMedia.appendToUploadLog("Удален : " + FilesAlreadyExists[i]);
                            dr.countDeleted++;
                            if (dr.countDeleted % 10 == 0)
                                ChangeStatus("Видалення..." + FilesAlreadyExists[i], true);
                            //new File(DirSou, FilesAlreadyExists[i]).delete();

                        }
                    }
                }
            }
        } catch (SmbException e) {
            Log.e(TAG, "SmbException:" + e.getMessage() + " " + e);
            // showToast("ОШИБКА ЗАГРУЗКИ :\n"+e.getMessage());

            ChangeStatus("ПОМИЛКА ЗАВАНТАЖЕННЯ :" + e.getMessage(), false);
            dr.hasError = UPLOAD_RESULT_ERROR_SMB_SERVER;
        } catch (IOException e) {
            Log.e(TAG, "IOException:" + e.getMessage());

            ChangeStatus("ПОМИЛКА ЗАВАНТАЖЕННЯ :" + e.getMessage(), false);
            dr.hasError = UPLOAD_RESULT_IO_ERROR;
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e.getMessage());

            ChangeStatus("ПОМИЛКА ЗАВАНТАЖЕННЯ :" + e.getMessage(), false);
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
            SmbFile remoteDir = ConnectToSource(remote_destination_path, User, Passw, true);//

            //проверим наличие и создадим файл
            SmbFile remoteFile = ConnectToSource(remote_destination_path + localFile.getName(), User, Passw, true);

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
