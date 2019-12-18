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
import static com.resonance.cashdisplay.load.UploadMedia.UPLOAD_RESULT_SHARE_CONNECTION_ERROR;
import static com.resonance.cashdisplay.load.UploadMedia.UPLOAD_RESULT_SUCCESSFULL;

public class FtpWorker {

    public final String TAG = "FtpWorker";
    private static FTPClient mFtpClient;

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
//ftp.rasla.ru

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

            int result = 0;
            //int TotalFilesToDownload = 0;

            // String[] FilesAlreadyExists = null;//список файлов уже существующих
            Log.d(TAG, "FtpTask... ");

            if ((user.length() == 0) && (passw.length() == 0)) {
                user = "anonymous";
                passw = "";
            }

            Log.d(TAG, "FtpTask... ");
            changeStatus(mContext.getString(R.string.connect_to_server) + ": " + host, true);
            try {
                Log.d(TAG, "Connect to " + host + ".");
                mFtpClient = new FTPClient();
                mFtpClient.connect(InetAddress.getByName(host));
                boolean status = mFtpClient.login(user, passw);
                mFtpClient.setControlEncoding("UTF-8");
                mFtpClient.setConnectTimeout(10 * 1000);
                mFtpClient.setBufferSize(1024 * 1024);
                mFtpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
                mFtpClient.enterLocalPassiveMode();

                // mFtpClient.changeWorkingDirectory("/SymbolDirectory");
                Log.d(TAG, "FTPConnected " + String.valueOf(status));
                if (!status) {
                    changeStatus("підключення до сервера :" + host + " - не вдалося", true);
                    return UPLOAD_RESULT_CONNECTION_ERROR;
                }

                int replyCode = mFtpClient.getReplyCode();
                Log.d(TAG, "FTP replyCode: " + replyCode);
                if (FTPReply.isPositiveCompletion(replyCode)) {
                    resultImg = downloadRoutine(destImg, shareImg, folderImg, extImg);
                    resultVideo = downloadRoutine(destVideo, shareVideo, folderVideo, extVideo);
                    resultSlide = downloadRoutine(destSlide, shareSlide, folderSlide, extSlide);
                    resultScreenImg = downloadRoutine(destScreenImg, shareScreenImg, folderScreenImg, extScreenImg);
                }
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
                            "<font color=\"blue\"><B>Фоновi та допомiжнi зображення:</B><br></font>[" + (resultScreenImg.hasError > 0 ? extendedErrorScreenImg : "завантажено : <B>" + resultScreenImg.countFiles + "</B>, iснуючих : <B>" + resultScreenImg.countSkipped + "</B>, видалено : <B>" + resultScreenImg.countDeleted) + "</B>];  <br>" +
                                    "<font color=\"blue\"><B>Вiдео:</B><br></font>[" + (resultVideo.hasError > 0 ? extendedErrorVideo : "завантажено : <B>" + resultVideo.countFiles + "</B>, iснуючих : <B>" + resultVideo.countSkipped + "</B>, видалено : <B>" + resultVideo.countDeleted) + "</B>];  <br>" +
                                    "<font color=\"blue\"><B>Зображення товарiв:</B><br></font>[" + (resultImg.hasError > 0 ? extendedErrorImage : "завантажено : <B>" + resultImg.countFiles + "</B>, iснуючих : <B>" + resultImg.countSkipped + "</B>, видалено : <B>" + resultImg.countDeleted) + "</B>];  <br>" +
                                    "<font color=\"blue\"><B>Слайди:</B><br></font>[" + (resultSlide.hasError > 0 ? extendedErrorSlide : "завантажено : <B>" + resultSlide.countFiles + "</B>, iснуючих : <B>" + resultSlide.countSkipped + "</B>, видалено : <B>" + resultSlide.countDeleted) + "</B>];";
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
                    changeStatus("недостатньо пам'яті на носії" + ExtSDSource.getAvailableMemory_SD(), true);
                    break;
            }
            setEventEndDownload(result);
        }
    }

    /***************************************************************************************************************/

    private UploadResult downloadRoutine(String destDir, String share, String folder, String[] extension) {

        UploadResult downResult = new UploadResult();
        String[] filesAlreadyExists = null;//список файлов уже существующих

        //получим список файлов уже существующих
        File dirSou = new File(destDir);
        if (dirSou.isDirectory()) {
            filesAlreadyExists = dirSou.list();
        }

        Log.d(TAG, "Take list Slide files: " + share + "/" + folder);
        changeStatus("отримання списку файлів..." + share + "/" + folder, true);
        downResult.countFiles = 0;
        downResult.countSkipped = 0;
        downResult.countDeleted = 0;

        FTPFile[] fileArray = null;
        try {
            fileArray = mFtpClient.listFiles(share + "/" + folder);
        } catch (IOException e) {
            Log.e(TAG, "IOException " + e);
            e.printStackTrace();
        }
        if (fileArray == null) {
            downResult.hasError = UPLOAD_RESULT_IO_ERROR;
            downResult.countFiles = 0;
            return downResult;
        }

        int totalFilesToDownload = fileArray.length;

        changeStatus("файлiв..." + totalFilesToDownload, false);

        Log.i(TAG, "Size:" + String.valueOf(totalFilesToDownload));
        for (int i = 0; i < totalFilesToDownload; i++) {
            UploadMedia.resetMediaPlay();//остановка демонстрации видео/слайдов
            // Log.d(TAG, "File:"+mFileArray[i].getName()+"  "+mFileArray[i].isDirectory());
            if (fileArray[i].isDirectory()) continue;

            if (ExtSDSource.getAvailableMemory(MainActivity.context, ExtSDSource.DEFAULT_SD) < fileArray[i].getSize()) {
                Log.e(TAG, "Not free memory");
                downResult.hasError = UPLOAD_RESULT_NOT_FREE_MEMORY;
                break;
            }

            if (UploadMedia.ifAlreadyExistFile(destDir, fileArray[i].getName(), fileArray[i].getSize())) {
                Log.d(TAG, "Ftp skip file: " + fileArray[i].getName());
                downResult.countSkipped++;
                continue;
            }
            //фильтр по расширению файла
            boolean isValidExtension = false;
            for (int a = 0; a < extension.length; a++) {
                if (fileArray[i].toString().endsWith(extension[a].substring(2))) {
                    isValidExtension = true;
                    break;
                }
            }
            if (!isValidExtension) {
                Log.d(TAG, "Ftp skip file: " + fileArray[i].getName());
                continue;
            }

            File localFile = new File(destDir + fileArray[i].getName());

            String statusStr = "Завантаження  " + (i + 1) + " з " + totalFilesToDownload + " ";
            changeStatus(statusStr, false);

            if (downloadFtpFile(mFtpClient, share + "/" + folder + fileArray[i].getName(), localFile, fileArray[i].getSize(), statusStr))
                downResult.countFiles++;
        }

        changeStatus("Завантажено : " + downResult.countFiles, true);
        //удаление файлов
        // if (resultSlide.CountFiles > 0)
        {
            //удалим неиспользуемые файлы
            if (filesAlreadyExists != null) {

                for (int i = 0; i < filesAlreadyExists.length; i++) {
                    boolean forDelete = true;
                    for (int y = 0; y < totalFilesToDownload; y++) {
                        if (fileArray[y].getName().equals(filesAlreadyExists[i])) {
                            forDelete = false;
                            break;
                        }
                    }
                    if (forDelete) {
                        Log.w(TAG, "To delete: " + filesAlreadyExists[i]);
                        new File(dirSou, filesAlreadyExists[i]).delete();
                        downResult.countDeleted++;
                    }
                }
            }
        }

        return downResult;
    }


/***************************************************************************************************************/
    /**
     * @param ftpClient      FTPclient object
     * @param remoteFilePath FTP server file path
     * @param downloadFile   local file path where you want to save after download
     * @return status of downloaded file
     */
    public boolean downloadFtpFile(FTPClient ftpClient, String remoteFilePath, File downloadFile, long sizefile, String status) {

        boolean result = false;
        File parentDir = downloadFile.getParentFile();
        if (!parentDir.exists())
            parentDir.mkdir();

        Log.d(TAG, "Load ftp file: " + remoteFilePath + " save to: " + downloadFile);
        OutputStream outputStream = null;

        try {
            int progress = 0;

            outputStream = new BufferedOutputStream(new FileOutputStream(downloadFile));
            InputStream inputStream = ftpClient.retrieveFileStream(remoteFilePath);

            byte[] bytesArray = new byte[1024 * 500]; //1024
            int bytesRead = -1;
            long totalBytes = 0;
            while ((bytesRead = inputStream.read(bytesArray)) > 0) {
                outputStream.write(bytesArray, 0, bytesRead);

                totalBytes += bytesRead;

                if (progress++ > 150) {
                    progress = 0;
                    changeStatus(status + " " + (int) ((100 * totalBytes) / sizefile) + "%", true);
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
        Log.i(TAG, "result Load ftp file :" + result);
        return result;
    }

    /**
     * @param ftpClient    FTPclient object
     * @param downloadFile local file which need to be uploaded.
     */
    public void uploadFile(FTPClient ftpClient, File downloadFile, String serverfilePath) {
        try {
            FileInputStream srcFileStream = new FileInputStream(downloadFile);
            boolean status = ftpClient.storeFile("remote ftp path",
                    srcFileStream);
            Log.e("Status", String.valueOf(status));
            srcFileStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}