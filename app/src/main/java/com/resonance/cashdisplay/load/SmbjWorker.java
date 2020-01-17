package com.resonance.cashdisplay.load;

import android.content.Context;
import android.os.AsyncTask;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.security.jce.JceSecurityProvider;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.SmbConfig;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.auth.NtlmAuthenticator;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.utils.SmbFiles;
import com.resonance.cashdisplay.ExtSDSource;
import com.resonance.cashdisplay.Log;
import com.resonance.cashdisplay.MainActivity;
import com.resonance.cashdisplay.R;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

import static com.resonance.cashdisplay.load.UploadMedia.UPLOAD_RESULT_BAD_ARGUMENTS;
import static com.resonance.cashdisplay.load.UploadMedia.UPLOAD_RESULT_CONNECTION_ERROR;
import static com.resonance.cashdisplay.load.UploadMedia.UPLOAD_RESULT_ERROR_SMB_SERVER;
import static com.resonance.cashdisplay.load.UploadMedia.UPLOAD_RESULT_FILE_NOT_FOUND;
import static com.resonance.cashdisplay.load.UploadMedia.UPLOAD_RESULT_IO_ERROR;
import static com.resonance.cashdisplay.load.UploadMedia.UPLOAD_RESULT_NOT_FREE_MEMORY;
import static com.resonance.cashdisplay.load.UploadMedia.UPLOAD_RESULT_NOT_SUPPORT_PROTOCOL;
import static com.resonance.cashdisplay.load.UploadMedia.UPLOAD_RESULT_SHARE_CONNECTION_ERROR;
import static com.resonance.cashdisplay.load.UploadMedia.UPLOAD_RESULT_SUCCESSFULL;

public class SmbjWorker {


    private static List<String> listFilesAlreadyExists;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public final String TAG = "SmbjWorker";
    private static final String NEW_LINE = System.getProperty("line.separator");

    private static Context mContext;

    public SmbjWorker(Context context) {
        this.mContext = context;
    }

    public SmbjWorker() {
    }

    /**** колбэк статуса выполнения загрузки *********************/
    public interface SmbjStatusCallback {
        void onSmbjStatusChanged(String status, boolean delRemaininng);
    }

    private static SmbjStatusCallback smbjStatusCallback;

    public void setSmbjStatusCallBack(SmbjStatusCallback cback) {
        smbjStatusCallback = cback;
    }

    private void changeStatus(String status, boolean delRemaininng) {
        smbjStatusCallback.onSmbjStatusChanged(status, delRemaininng);
    }

    /**** колбэк окончания загрузки *********************/
    public interface SmbjEndDownloadCallback {
        void onSmbjEndDownload(int status);
    }

    private static SmbjEndDownloadCallback smbjEndDownloadCallback;

    public void setEndDownloadCallBack(SmbjEndDownloadCallback cback) {
        smbjEndDownloadCallback = cback;
    }

    private void setEventEndDownload(int status) {
        smbjEndDownloadCallback.onSmbjEndDownload(status);
    }

    public void doDownload(HashMap<String, Object> auHashMap,
                           HashMap<String, Object> imgHashMap,
                           HashMap<String, Object> videoHashMap,
                           HashMap<String, Object> slideHashMap,
                           HashMap<String, Object> screenImgHashMap) {
        new SmbjTask().execute(auHashMap, imgHashMap, videoHashMap, slideHashMap, screenImgHashMap);
    }

    private class SmbjTask extends AsyncTask<HashMap<String, Object>, Void, Integer> {

        UploadResult resultScreenImg = new UploadResult();
        UploadResult resultImg = new UploadResult();
        UploadResult resultVideo = new UploadResult();
        UploadResult resultSlide = new UploadResult();

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
            String passw = (String) auHashMap.get("passw");
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

            Log.d(TAG, "SmbjTask... ");

            SmbConfig config = SmbConfig.builder()
                    .withMultiProtocolNegotiate(true)
                    .withSecurityProvider(new JceSecurityProvider(new BouncyCastleProvider()))
                    .withSigningRequired(true)
                    //.withDfsEnabled(true)
                    .withAuthenticators(new NtlmAuthenticator.Factory())
                    .build();

            int result = UPLOAD_RESULT_SUCCESSFULL;
            Connection connection = null;
            Session session = null;
            // DiskShare share = null;

            try {
                changeStatus(mContext.getString(R.string.connect_to_server) + ": " + host, true);
                SMBClient client = new SMBClient(config);

                Log.d(TAG, "Smbj, try connection : " + host + " User:" + user + ", Passw:" + passw);
                connection = client.connect(host);
                AuthenticationContext authenticationContext = new AuthenticationContext(user, passw.toCharArray(), "");
                session = connection.authenticate(authenticationContext);
                Log.d(TAG, "Smbj connect successfull");

                changeStatus(mContext.getString(R.string.get_data_screenFiles), false);
                resultScreenImg = handleFiles(session, shareScreenImg, folderScreenImg, destScreenImg, extScreenImg);

                changeStatus(mContext.getString(R.string.get_data_img), false);
                resultImg = handleFiles(session, shareImg, folderImg, destImg, extImg);

                changeStatus(mContext.getString(R.string.get_data_video), false);
                resultVideo = handleFiles(session, shareVideo, folderVideo, destVideo, extVideo);

                changeStatus(mContext.getString(R.string.get_data_slide), false);
                resultSlide = handleFiles(session, shareSlide, folderSlide, destSlide, extSlide);

            } catch (Exception e) {
                Log.e(TAG, "Smbj Exception: " + e);
                if (e.getMessage().contains(" is not supported")) {
                    result = UPLOAD_RESULT_NOT_SUPPORT_PROTOCOL;
                } else if (e.getMessage().contains("Could not connect") || e.getMessage().contains("failed to connect")) {
                    result = UPLOAD_RESULT_CONNECTION_ERROR;
                } else if ((e.getMessage().contains("IllegalArgumentException")) || (e.getMessage().contains("Cannot require message signing when authenticating"))) {
                    result = UPLOAD_RESULT_BAD_ARGUMENTS;
                }
            } finally {
                try {
                    //выгрузим на сервер лог загрузки
                    DiskShare share = (DiskShare) session.connectShare(shareScreenImg);
                    if (share.isConnected()) {
                        try {
                            String logDir = "LOG";
                            if (!share.folderExists(logDir)) {
                                share.mkdir(logDir);
                            }
                            String remoteLogFile = logDir + "/" + UploadMedia.logFile.getName();
                            if (!share.fileExists(remoteLogFile)) {
                                com.hierynomus.smbj.share.File file = share.openFile(remoteLogFile, EnumSet.of(AccessMask.GENERIC_ALL), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_CREATE, null);
                                file.close();
                            }

                            SmbFiles.copy(UploadMedia.logFile, share, remoteLogFile, true);
                            UploadMedia.deleteUploadLog();
                        } catch (IOException e) {
                            Log.e(TAG, "Smb IOException: " + e);
                        }
                    } else
                        Log.w(TAG, "Smb share is not connect ");
                } catch (Exception e) {
                    Log.e(TAG, "Smb Exception(2): " + e);
                }

                if (session != null) {
                    session = null;
                }
                try {
                    if ((connection != null) && (connection.isConnected())) {
                        connection.close(true);
                    }
                } catch (IOException e1) {
                    Log.e(TAG, "Smbj IOException: " + e1);
                }
                connection = null;
            }
            return result;
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            Log.d(TAG, "SmbjTask result : " + result);

            String extendedErrorImage = "помилки";
            String extendedErrorSlide = "помилки";
            String extendedErrorVideo = "помилки";
            String extendedErrorScreenImg = "помилки";

            if (resultImg.hasError == UPLOAD_RESULT_NOT_FREE_MEMORY)
                extendedErrorImage = "недостатньо пам'ятi";

            if (resultVideo.hasError == UPLOAD_RESULT_NOT_FREE_MEMORY)
                extendedErrorVideo = "недостатньо пам'ятi";

            if (resultSlide.hasError == UPLOAD_RESULT_NOT_FREE_MEMORY)
                extendedErrorSlide = "недостатньо пам'ятi";

            if (resultScreenImg.hasError == UPLOAD_RESULT_NOT_FREE_MEMORY)
                extendedErrorScreenImg = "недостатньо пам'ятi";

            switch (result) {
                case UPLOAD_RESULT_SUCCESSFULL:
                case UPLOAD_RESULT_SHARE_CONNECTION_ERROR:
                    String status =
                            "<font color=\"blue\"><B>Фоновi та допомiжнi зображення:</B><br></font>[" + (resultScreenImg.hasError > 0 ? extendedErrorScreenImg : ("завантажено : <B>" + resultScreenImg.countFiles + "</B>, iснуючих : <B>" + resultScreenImg.countSkipped + "</B>, видалено : <B>" + resultScreenImg.countDeleted) + "</B>];  <br>")
                                    + "<font color=\"blue\"><B>Зображення товарiв:</B><br></font>[" + (resultImg.hasError > 0 ? extendedErrorImage : ("завантажено : <B>" + resultImg.countFiles + "</B>, iснуючих : <B>" + resultImg.countSkipped + "</B>, видалено : <B>" + resultImg.countDeleted) + "</B>];  <br>")
                                    + "<font color=\"blue\"><B>Вiдео:</B><br></font>[" + (resultVideo.hasError > 0 ? extendedErrorVideo : ("завантажено : <B>" + resultVideo.countFiles + "</B>, iснуючих : <B>" + resultVideo.countSkipped + "</B>, видалено : <B>" + resultVideo.countDeleted) + "</B>];  <br>")
                                    + "<font color=\"blue\"><B>Слайди:</B><br></font>[" + (resultSlide.hasError > 0 ? extendedErrorSlide : ("завантажено : <B>" + resultSlide.countFiles + "</B>, iснуючих : <B>" + resultSlide.countSkipped + "</B>, видалено : <B>" + resultSlide.countDeleted) + "</B>];");
                    changeStatus(status, true);
                    break;
                case UPLOAD_RESULT_NOT_SUPPORT_PROTOCOL:
                    changeStatus("Сервер не підтримує протокол ", true);
                    break;
                case UPLOAD_RESULT_CONNECTION_ERROR:
                    changeStatus("Неможливо пiдключитися до файлового сервера", true);
                    Log.e(TAG, "Неможливо пiдключитися до файлового сервера");
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

    //
    // Определение списка существующих файлов
    // подключение к ресурсу
    // Загрузка файлов
    // Удаление ненужных файлов
    //
    private UploadResult handleFiles(Session session, String shareFolder, String sourceFolder, String destFolder, String[] extensionFiles) {

        UploadResult uploadResult = new UploadResult();
        int divider = 0;

        String connectionStr = shareFolder + (sourceFolder.startsWith("/") ? "" : "/") + sourceFolder;
        UploadMedia.appendToUploadLog("\n(SMB2) " + connectionStr);
        changeStatus(mContext.getString(R.string.connect_to_server) + ": " + connectionStr, true);

        // получим список файлов уже существующих
        listFilesAlreadyExists = null;
        File dirSou = new File(destFolder);
        if (dirSou.isDirectory()) {
            listFilesAlreadyExists = new ArrayList<String>(Arrays.asList(dirSou.list()));
        }

        //  Connect to share
        changeStatus(mContext.getString(R.string.try_connect_to_source), false);
        try {
            DiskShare share = (DiskShare) session.connectShare(shareFolder);
            if (share.isConnected()) {

                changeStatus("Отримання списку файлів...", false);
                UploadMedia.resetMediaPlay(); //остановка демонстрации видео/слайдов
                for (int i = 0; i < extensionFiles.length; i++) {
                    UploadResult tmpUploadResult = downloadFromShareFolder(share, sourceFolder, extensionFiles[i], destFolder);
                    uploadResult.countFiles += tmpUploadResult.countFiles;
                    uploadResult.countSkipped += tmpUploadResult.countSkipped;
                    uploadResult.countDeleted += tmpUploadResult.countDeleted;
                    if (tmpUploadResult.hasError > UPLOAD_RESULT_SUCCESSFULL)
                        uploadResult.hasError = tmpUploadResult.hasError;
                    Log.d(TAG, "Загружено: " + uploadResult.countFiles + " ext:" + extensionFiles[i]);
                }
                UploadMedia.appendToUploadLog("Получено с сервера: " + (uploadResult.countFiles + uploadResult.countSkipped));
                share.close();
            } else {
                changeStatus("Неможливо пiдключитися до " + shareFolder, false);
                uploadResult.hasError = UPLOAD_RESULT_SHARE_CONNECTION_ERROR;
            }
        } catch (Exception e) {
            Log.e(TAG, "Smbj Img Exception: " + e);
            changeStatus("Неможливо пiдключитися до " + shareFolder, false);
            uploadResult.hasError = UPLOAD_RESULT_CONNECTION_ERROR;
        }

        //Удалим файлы, которые были определены на удаление
        changeStatus("Видалення файлiв - " + listFilesAlreadyExists.size(), false);
        for (int i = 0; i < listFilesAlreadyExists.size(); i++) {
            new File(destFolder, listFilesAlreadyExists.get(i)).delete();
            UploadMedia.appendToUploadLog("Удален: " + listFilesAlreadyExists.get(i));
            uploadResult.countDeleted++;
            if ((divider++) >= 50)
                changeStatus("Видалення файлiв - " + uploadResult.countDeleted + " iз " + listFilesAlreadyExists.size(), false);
        }

        return uploadResult;
    }

    private UploadResult downloadFromShareFolder(DiskShare share, String shareSourceFolder, String fileSearchPattern, String destFolder) {
        UploadResult dr = new UploadResult();

        int divider = 0;

        for (FileIdBothDirectoryInformation f : share.list(shareSourceFolder, fileSearchPattern)) {
            divider++;

            com.hierynomus.smbj.share.File remoteSmbjFile = share.openFile(shareSourceFolder + f.getFileName(), EnumSet.of(AccessMask.GENERIC_READ), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null);
            long size = remoteSmbjFile.getFileInformation().getStandardInformation().getEndOfFile();

            // определяем файлы, которые после окончания загрузки будут удалены из каталога
            if (listFilesAlreadyExists.contains(f.getFileName()))
                listFilesAlreadyExists.remove(f.getFileName());

            if (ExtSDSource.getAvailableMemory(MainActivity.context, ExtSDSource.DEFAULT_SD) < size) {
                Log.e(TAG, "Not enough memory");
                UploadMedia.appendToUploadLog("ОШИБКА, недостаточно места на SD карте: " + ExtSDSource.getAvailableMemory(MainActivity.context, ExtSDSource.DEFAULT_SD));
                dr.hasError = UPLOAD_RESULT_NOT_FREE_MEMORY;
                break;
            }

            if (UploadMedia.ifAlreadyExistFile(destFolder, f.getFileName(), size)) {
                Log.d(TAG, "Smbj skip file: " + shareSourceFolder + f.getFileName());
                UploadMedia.appendToUploadLog("Перезаписан: " + f.getFileName());

                if (dr.countSkipped % 10 == 0) {
                    changeStatus("Перезаписан: " + f.getFileName(), true);
                }
                dr.countSkipped++;

                continue;
            }

            Log.d(TAG, "Smbj download file: " + shareSourceFolder + f.getFileName() + " fileSize:" + size);

            java.io.File dest = new File(destFolder, f.getFileName());
            String dstPath = destFolder + f.getFileName();

            InputStream is = null;
            FileOutputStream os = null;
            BufferedOutputStream bos = null;
            try {
                is = remoteSmbjFile.getInputStream();
                os = new FileOutputStream(dest);
                bos = new BufferedOutputStream(os);

                byte[] buffer = new byte[1024 * 500];
                int length = 0;
                Log.d(TAG, "file: " + dstPath);

                while ((length = is.read(buffer)) != -1) {
                    bos.write(buffer, 0, length);
                }
                UploadMedia.appendToUploadLog("Загружен: " + f.getFileName() + " размер: " + size);

            } catch (FileNotFoundException e) {
                Log.e(TAG, "Smbj FileNotFoundException :" + e);
                changeStatus("Помилка, FileNotFoundException", true);
                dr.hasError = UPLOAD_RESULT_FILE_NOT_FOUND;
            } catch (IOException e) {
                Log.e(TAG, "Smbj IOException :" + e);
                changeStatus("Виникли помилки при завантаженнi файлiв", true);
                dr.hasError = UPLOAD_RESULT_IO_ERROR;
            } finally {
                try {
                    remoteSmbjFile.close();
                    if (bos != null) {
                        bos.flush();
                        bos.close();
                    }

                    if (os != null)
                        os.close();
                    if (is != null)
                        is.close();
                } catch (IOException ex) {
                    Log.e(TAG, "Smbj IOException :" + ex);
                    dr.hasError = UPLOAD_RESULT_IO_ERROR;
                }
            }
            dr.countFiles++;

            if ((divider++) >= 10) {
                UploadMedia.resetMediaPlay(); // остановка демонстрации видео/слайдов
                changeStatus("Завантаження: [" + fileSearchPattern + "] " + dr.countFiles, true);
                divider = 0;
            }
        }

        return dr;
    }
}