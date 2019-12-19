package com.resonance.cashdisplay.load;

import android.content.Context;
import android.os.AsyncTask;

import com.resonance.cashdisplay.ExtSDSource;
import com.resonance.cashdisplay.Log;
import com.resonance.cashdisplay.MainActivity;
import com.resonance.cashdisplay.R;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;

import static com.resonance.cashdisplay.load.UploadMedia.UPLOAD_RESULT_BAD_ARGUMENTS;
import static com.resonance.cashdisplay.load.UploadMedia.UPLOAD_RESULT_CONNECTION_ERROR;
import static com.resonance.cashdisplay.load.UploadMedia.UPLOAD_RESULT_IO_ERROR;
import static com.resonance.cashdisplay.load.UploadMedia.UPLOAD_RESULT_NOT_FREE_MEMORY;
import static com.resonance.cashdisplay.load.UploadMedia.UPLOAD_RESULT_NOT_SUPPORT_PROTOCOL;
import static com.resonance.cashdisplay.load.UploadMedia.UPLOAD_RESULT_SUCCESSFULL;

public class FtpWorker {

    public final String TAG = "FtpWorker";
    private FTPClient mFtpClient;

    private Context mContext;

    public FtpWorker(Context context) {
        this.mContext = context;
    }

    /**** колбэк статуса выполнения загрузки *********************/
    public interface FtpStatusCallback {
        void onFtpStatusChanged(String status, boolean delRemaining);
    }

    private static FtpStatusCallback ftpStatusCallback;

    public void setFtpStatusCallback(FtpStatusCallback cback) {
        ftpStatusCallback = cback;
    }

    private void changeStatus(String status, boolean delRemaining) {
        ftpStatusCallback.onFtpStatusChanged(status, delRemaining);
    }

    /**** колбэк окончания загрузки *********************/
    public interface FtpEndDownloadCallback {
        void onFtpEndDownload(int status);
    }

    private static FtpEndDownloadCallback ftpEndDownloadCallback;

    public void setEndDownloadCallback(FtpEndDownloadCallback cback) {
        ftpEndDownloadCallback = cback;
    }

    private void setEventEndDownload(int status) {
        ftpEndDownloadCallback.onFtpEndDownload(status);
    }

    public void doDownload(HashMap<String, Object> auHashMap,
                           HashMap<String, Object> imgHashMap,
                           HashMap<String, Object> videoHashMap,
                           HashMap<String, Object> slideHashMap,
                           HashMap<String, Object> screenImgHashMap) {
        new FtpTask().execute(auHashMap, imgHashMap, videoHashMap, slideHashMap, screenImgHashMap);
    }

    private class FtpTask extends AsyncTask<HashMap<String, Object>, Void, Integer> {

        UploadResult resultImg = new UploadResult();
        UploadResult resultVideo = new UploadResult();
        UploadResult resultSlide = new UploadResult();
        UploadResult resultScreenImg = new UploadResult();

        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Integer doInBackground(HashMap<String, Object>... params) {

            Log.d(TAG, "FtpTask started...");
            int result = UPLOAD_RESULT_SUCCESSFULL;

            HashMap<String, Object> auHashMap = params[0];
            HashMap<String, Object> imgHashMap = params[1];
            HashMap<String, Object> videoHashMap = params[2];
            HashMap<String, Object> slideHashMap = params[3];
            HashMap<String, Object> screenImgHashMap = params[4];

            String user = (String) auHashMap.get("user");
            String password = (String) auHashMap.get("passw");
            String host = (String) auHashMap.get("host");

            String shareServerImg = (String) imgHashMap.get("shareImg");
            String shareDirImg = (String) imgHashMap.get("folderImg");
            String destDirImg = (String) imgHashMap.get("destImg");
            String[] extImg = (String[]) imgHashMap.get("extArrayImg");

            String shareServerVideo = (String) videoHashMap.get("shareVideo");
            String shareDirVideo = (String) videoHashMap.get("folderVideo");
            String destDirVideo = (String) videoHashMap.get("destVideo");
            String[] extVideo = (String[]) videoHashMap.get("extArrayVideo");

            String shareServerSlide = (String) slideHashMap.get("shareSlide");
            String shareDirSlide = (String) slideHashMap.get("folderSlide");
            String destDirSlide = (String) slideHashMap.get("destSlide");
            String[] extSlide = (String[]) slideHashMap.get("extArraySlide");

            String shareServerScreenImg = (String) screenImgHashMap.get("shareScreenImg");
            String shareDirScreenImg = (String) screenImgHashMap.get("folderScreenImg");
            String destDirScreenImg = (String) screenImgHashMap.get("destScreenImg");
            String[] extScreenImg = (String[]) screenImgHashMap.get("extArrayScreenImg");

            if ((user.length() == 0) && (password.length() == 0)) {
                user = "anonymous";
                password = "";
            }

            changeStatus(mContext.getString(R.string.connect_to_server) + ": " + host, true);

            try {
                Log.d(TAG, "Connect to " + host + " ...");
                mFtpClient = new FTPClient();
                mFtpClient.connect(InetAddress.getByName(host));
                boolean status = mFtpClient.login(user, password);
                mFtpClient.setControlEncoding("UTF-8");
                mFtpClient.setConnectTimeout(10 * 1000);
                mFtpClient.setBufferSize(1024 * 1024);
                mFtpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
                mFtpClient.enterLocalPassiveMode();

                Log.d(TAG, "FTPConnected status: " + status);
                if (!status) {
                    changeStatus(mContext.getString(R.string.connect_to_server) + ": " + host + " - не вдалося", true);
                    return UPLOAD_RESULT_CONNECTION_ERROR;
                }
                int replyCode = mFtpClient.getReplyCode();
                Log.d(TAG, "FTP replyCode: " + replyCode);
                if (FTPReply.isPositiveCompletion(replyCode)) {
                    resultImg = downloadFiles(destDirImg, shareServerImg, shareDirImg, extImg);
                    resultVideo = downloadFiles(destDirVideo, shareServerVideo, shareDirVideo, extVideo);
                    resultSlide = downloadFiles(destDirSlide, shareServerSlide, shareDirSlide, extSlide);
                    resultScreenImg = downloadFiles(destDirScreenImg, shareServerScreenImg, shareDirScreenImg, extScreenImg);
                }

                //выгрузим на сервер лог загрузки
                String ftpRemotePath = (shareServerImg + "/LOG/");
                uploadFileToFtp(mFtpClient, UploadMedia.logFile, ftpRemotePath);
                UploadMedia.deleteUploadLog();

            } catch (SocketException e) {
                Log.e(TAG, "SocketException " + e);
                result = UPLOAD_RESULT_CONNECTION_ERROR;
                e.printStackTrace();
            } catch (UnknownHostException e) {
                Log.e(TAG, "UnknownHostException " + e);
                result = UPLOAD_RESULT_CONNECTION_ERROR;
                e.printStackTrace();
            } catch (IOException e) {
                Log.e(TAG, "IOException " + e);
                result = UPLOAD_RESULT_IO_ERROR;
                e.printStackTrace();
            } finally {
                try {
                    if (mFtpClient != null) {
                        mFtpClient.logout();
                        mFtpClient.disconnect();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "IOException " + e);
                    e.printStackTrace();
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            Log.d(TAG, "FTPTask result : " + result);

            String errorImage = "помилка";
            String errorVideo = "помилка";
            String errorSlide = "помилка";
            String errorScreenImg = "помилка";

            if (resultImg.hasError == UPLOAD_RESULT_NOT_FREE_MEMORY)
                errorImage = "недостатньо пам'ятi";

            if (resultVideo.hasError == UPLOAD_RESULT_NOT_FREE_MEMORY)
                errorVideo = "недостатньо пам'ятi";

            if (resultSlide.hasError == UPLOAD_RESULT_NOT_FREE_MEMORY)
                errorSlide = "недостатньо пам'ятi";

            if (resultScreenImg.hasError == UPLOAD_RESULT_NOT_FREE_MEMORY)
                errorScreenImg = "недостатньо пам'ятi";

            switch (result) {
                case UPLOAD_RESULT_SUCCESSFULL:
                    String status =
                            "<font color=\"blue\"><B>Фоновi та допомiжнi зображення:</B><br></font>[" + (resultScreenImg.hasError > 0 ? errorScreenImg : ("завантажено : <B>" + resultScreenImg.countFiles + "</B>, iснуючих : <B>" + resultScreenImg.countSkipped + "</B>, видалено : <B>" + resultScreenImg.countDeleted) + "</B>];  <br>") +
                                    "<font color=\"blue\"><B>Зображення товарiв:</B><br></font>[" + (resultImg.hasError > 0 ? errorImage : ("завантажено : <B>" + resultImg.countFiles + "</B>, iснуючих : <B>" + resultImg.countSkipped + "</B>, видалено : <B>" + resultImg.countDeleted) + "</B>];  <br>") +
                                    "<font color=\"blue\"><B>Вiдео:</B><br></font>[" + (resultVideo.hasError > 0 ? errorVideo : ("завантажено : <B>" + resultVideo.countFiles + "</B>, iснуючих : <B>" + resultVideo.countSkipped + "</B>, видалено : <B>" + resultVideo.countDeleted) + "</B>];  <br>") +
                                    "<font color=\"blue\"><B>Слайди:</B><br></font>[" + (resultSlide.hasError > 0 ? errorSlide : ("завантажено : <B>" + resultSlide.countFiles + "</B>, iснуючих : <B>" + resultSlide.countSkipped + "</B>, видалено : <B>" + resultSlide.countDeleted) + "</B>];");
                    changeStatus(status, true);
                    break;
                case UPLOAD_RESULT_NOT_SUPPORT_PROTOCOL:
                    changeStatus("FTP Сервер не підтримує протокол ", true);
                    break;
                case UPLOAD_RESULT_CONNECTION_ERROR:
                    changeStatus("Неможливо пiдключитися до FTP сервера", true);
                    break;
                case UPLOAD_RESULT_BAD_ARGUMENTS:
                    changeStatus("Невiрнi параметри пiдключення до FTP сервера", true);
                    break;
                case UPLOAD_RESULT_NOT_FREE_MEMORY:
                    changeStatus("Недостатньо пам'яті на носії" + ExtSDSource.getAvailableMemory_SD(), true);
                    break;
            }
            setEventEndDownload(result);
        }
    }

    /***************************************************************************************************************/

    private UploadResult downloadFiles(String destDirPath, String share, String shareDir, String[] extArray) {

        UploadResult result = new UploadResult();

        String ftpRemotePath = (share.startsWith("/") ? "" : "/") + share + (shareDir.startsWith("/") ? "" : "/") + shareDir;
        UploadMedia.appendToUploadLog("\n(FTP) " + ftpRemotePath);
        ftpRemotePath = (ftpRemotePath.startsWith("/") ? ftpRemotePath.substring(1) : ftpRemotePath);

        //получим список файлов уже существующих
        String[] filesAlreadyExists = null;
        File destDir = new File(destDirPath);
        if (destDir.isDirectory()) {
            filesAlreadyExists = destDir.list();
        }

        Log.d(TAG, "Take list of files: " + ftpRemotePath);
        changeStatus("Отримання списку файлів... " + ftpRemotePath, true);

        FTPFile[] ftpFileArray = null;
        try {
            ftpFileArray = mFtpClient.listFiles(ftpRemotePath);
        } catch (IOException e) {
            Log.e(TAG, "IOException " + e);
            e.printStackTrace();
        }
        if (ftpFileArray == null) {
            result.hasError = UPLOAD_RESULT_IO_ERROR;
            result.countFiles = 0;
            return result;
        }
        int totalFilesToDownload = ftpFileArray.length;
        UploadMedia.appendToUploadLog("*** Файлов для загрузки :" + totalFilesToDownload + " ***");
        changeStatus("Файлiв... " + totalFilesToDownload, false);
        Log.d(TAG, "Size:" + totalFilesToDownload);

        for (int i = 0; i < totalFilesToDownload; i++) {

            UploadMedia.resetMediaPlay(); //остановка демонстрации видео/слайдов

            if (ftpFileArray[i].isDirectory()) continue;

            if (ExtSDSource.getAvailableMemory(MainActivity.context, ExtSDSource.DEFAULT_SD) < ftpFileArray[i].getSize()) {
                Log.e(TAG, "No free memory");
                UploadMedia.appendToUploadLog("Недостаточно памяти на SD карте: " + ExtSDSource.getAvailableMemory(MainActivity.context, ExtSDSource.DEFAULT_SD));
                result.hasError = UPLOAD_RESULT_NOT_FREE_MEMORY;
                break;
            }

            if (UploadMedia.ifAlreadyExistFile(destDirPath, ftpFileArray[i].getName(), ftpFileArray[i].getSize())) {
                Log.d(TAG, "Ftp skip file: " + ftpFileArray[i].getName());
                UploadMedia.appendToUploadLog("Перезаписан: " + ftpFileArray[i].getName());
                result.countSkipped++;
                continue;
            }
            //фильтр по расширению файла
            boolean isValidExtension = false;
            for (int j = 0; j < extArray.length; j++) {
                if (ftpFileArray[i].toString().endsWith(extArray[j].substring(2))) {
                    isValidExtension = true;
                    break;
                }
            }
            if (!isValidExtension) {
                Log.d(TAG, "Ftp skip file: " + ftpFileArray[i].getName());
                continue;
            }

            String statusStr = "Завантаження  " + (i + 1) + " з " + totalFilesToDownload + " ";
            changeStatus(statusStr, false);

            File localFile = new File(destDirPath + ftpFileArray[i].getName());
            if (downloadFtpFile(mFtpClient, ftpRemotePath + ftpFileArray[i].getName(), localFile, ftpFileArray[i].getSize(), statusStr))
                result.countFiles++;

            if (!localFile.exists())
                Log.e(TAG, "File [" + localFile + "] NOT CREATED");
            else
                UploadMedia.appendToUploadLog("Загружен:    " + ftpFileArray[i].getName() + ", размер : " + ftpFileArray[i].getSize());
        }

        changeStatus("Завантажено : " + result.countFiles, true);

        // удаление лишних файлов
        if (filesAlreadyExists != null) {
            for (int i = 0; i < filesAlreadyExists.length; i++) {
                boolean forDelete = true;
                for (int j = 0; j < totalFilesToDownload; j++) {
                    if (ftpFileArray[j].getName().equals(filesAlreadyExists[i])) {
                        forDelete = false;
                        break;
                    }
                }
                if (forDelete) {
                    Log.w(TAG, "To delete: " + filesAlreadyExists[i]);
                    new File(destDir, filesAlreadyExists[i]).delete();
                    UploadMedia.appendToUploadLog("Удалён:      " + filesAlreadyExists[i]);
                    result.countDeleted++;
                }
            }
        }

        return result;
    }


/***************************************************************************************************************/
    /**
     * @param ftpClient      FTPclient object
     * @param remoteFilePath FTP server file path
     * @param downloadFile   local file path where you want to save after download
     * @return status of downloaded file
     */
    public boolean downloadFtpFile(FTPClient ftpClient, String remoteFilePath, File downloadFile, long fileSize, String status) {

        boolean result = false;
        File parentDir = downloadFile.getParentFile();
        if (!parentDir.exists())
            parentDir.mkdir();

        Log.d(TAG, "Load ftp file: " + remoteFilePath + " , save to: " + downloadFile);
        OutputStream outputStream = null;

        try {
            int progress = 0;

            outputStream = new BufferedOutputStream(new FileOutputStream(downloadFile));
            InputStream inputStream = ftpClient.retrieveFileStream(remoteFilePath);

            byte[] bytesArray = new byte[1024 * 500]; //1024
            int bytesAmount;
            long totalBytes = 0;
            while ((bytesAmount = inputStream.read(bytesArray)) > 0) {
                outputStream.write(bytesArray, 0, bytesAmount);

                totalBytes += bytesAmount;

                if (progress++ > 150) {
                    progress = 0;
                    changeStatus(status + " " + (int) ((100 * totalBytes) / fileSize) + "%", true);
                }
            }
            result = true;
            ftpClient.completePendingCommand();
        } catch (Exception ex) {
            Log.e(TAG, "Exception Load ftp file: " + ex);
            ex.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "IOException ftp file :" + remoteFilePath);
                    e.printStackTrace();
                }
            }
        }
        Log.i(TAG, "Result Load ftp file :" + result);
        return result;
    }

    /**
     * @param ftpClient  FTP client object
     * @param uploadFile local file which need to be uploaded.
     */
    public void uploadFileToFtp(FTPClient ftpClient, File uploadFile, String serverFilePath) {
        try {
            ftpClient.makeDirectory(serverFilePath);
            FileInputStream srcFileStream = new FileInputStream(uploadFile);
            boolean status = ftpClient.storeFile(serverFilePath + uploadFile.getName(),
                    srcFileStream);
            Log.d(TAG, "Saving log file of FTP server: " + status);
            srcFileStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}