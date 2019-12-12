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

//ftp.rasla.ru

    public void doDownload(HashMap<String, Object> au_hashMap, HashMap<String, Object> img_hashMap, HashMap<String, Object> video_hashMap, HashMap<String, Object> slide_hashMap) {
        new FtpTask().execute(au_hashMap, img_hashMap, video_hashMap, slide_hashMap);
    }

    private class FtpTask extends AsyncTask<HashMap<String, Object>, Void, Integer> {

        UploadResult resultImg = new UploadResult();
        UploadResult resultVideo = new UploadResult();
        UploadResult resultSlide = new UploadResult();

        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Integer doInBackground(HashMap<String, Object>... params) {

            resultImg.hasError = 0;
            resultVideo.hasError = 0;
            resultSlide.hasError = 0;

            HashMap<String, Object> auHashMap = params[0];
            HashMap<String, Object> imgHashMap = params[1];
            HashMap<String, Object> videoHashMap = params[2];
            HashMap<String, Object> slideHashMap = params[3];

            String user = (String) auHashMap.get("User");
            String passw = (String) auHashMap.get("Passw");
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

            int error = 0;
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
                mFtpClient.setConnectTimeout(10 * 1000);
                mFtpClient.setControlEncoding("UTF-8");
                mFtpClient.setBufferSize(1024 * 1024);

                // Log.d(TAG, "FTP buffer size:  " + mFtpClient.getBufferSize());
                mFtpClient.connect(InetAddress.getByName(host));
                boolean status = mFtpClient.login(user, passw);

                // mFtpClient.changeWorkingDirectory("/SymbolDirectory");
                Log.d(TAG, "FTPConnected " + String.valueOf(status));
                if (!status) {
                    changeStatus("підключення до сервера :" + host + " - не вдалося", true);
                    return UPLOAD_RESULT_CONNECTION_ERROR;
                }

                int replyCode = mFtpClient.getReplyCode();
                Log.d(TAG, "FTP replyCode: " + replyCode);

                if (FTPReply.isPositiveCompletion(replyCode)) {
                    mFtpClient.setFileType(FTPClient.LOCAL_FILE_TYPE);
                    mFtpClient.enterLocalPassiveMode();

                    resultImg = downloadRoutine(destinationImg, shareImg, folderImg, extensionImg);
                    resultVideo = downloadRoutine(destinationVideo, shareVideo, folderVideo, extensionVideo);
                    resultSlide = downloadRoutine(destinationSlide, shareSlide, folderSlide, extensionSlide);
                }
            } catch (SocketException e) {
                Log.e(TAG, "SocketException " + e);
                error = UPLOAD_RESULT_CONNECTION_ERROR;
                e.printStackTrace();
            } catch (UnknownHostException e) {
                Log.e(TAG, "UnknownHostException " + e);
                error = UPLOAD_RESULT_CONNECTION_ERROR;
                e.printStackTrace();
            } catch (IOException e) {
                Log.e(TAG, "IOException " + e);
                error = UPLOAD_RESULT_IO_ERROR;
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
            return error;
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            Log.d(TAG, "FTPTask result : " + result);

            String ExtendedError_image = "помилки";
            String ExtendedError_slide = "помилки";
            String ExtendedError_video = "помилки";

            if (resultImg.hasError == UPLOAD_RESULT_NOT_FREE_MEMORY)
                ExtendedError_image = "недостатньо пам'ятi";

            if (resultVideo.hasError == UPLOAD_RESULT_NOT_FREE_MEMORY)
                ExtendedError_video = "недостатньо пам'ятi";

            if (resultSlide.hasError == UPLOAD_RESULT_NOT_FREE_MEMORY)
                ExtendedError_slide = "недостатньо пам'ятi";

            switch (result) {


                case UPLOAD_RESULT_SUCCESSFULL:
                case UPLOAD_RESULT_SHARE_CONNECTION_ERROR:
                    changeStatus("Вiдео[" + (resultVideo.hasError > 0 ? ExtendedError_video : "завантажено : " + resultVideo.countFiles + ", iснуючих : " + resultVideo.countSkipped + ", видалено : " + resultVideo.countDeleted) + "]; \n " +
                            "Зображення[" + (resultImg.hasError > 0 ? ExtendedError_image : "завантажено : " + resultImg.countFiles + ", iснуючих : " + resultImg.countSkipped + ", видалено : " + resultImg.countDeleted) + "]; \n " +
                            "Слайди[" + (resultSlide.hasError > 0 ? ExtendedError_slide : "завантажено : " + resultSlide.countFiles + ", iснуючих : " + resultSlide.countSkipped + ", видалено : " + resultSlide.countDeleted) + "]", true);

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

    /**** колбэк статуса выполнения загрузки *********************/
    public interface FTP_StatusCallback {
        void onFtpStatus(String status, boolean delRemaininng);
    }

    private static FTP_StatusCallback callback_onFtpStatus;

    public void onChangeStatusCallBack(FTP_StatusCallback cback) {
        callback_onFtpStatus = cback;
    }

    private void changeStatus(String status, boolean delRemaininng) {
        callback_onFtpStatus.onFtpStatus(status, delRemaininng);
    }

    /**** колбэк окончания загрузки *********************/
    public interface FTP_EndDownloadCallback {
        void onFtpEndDownload(int status);
    }

    private static FTP_EndDownloadCallback callback_onEndDownload;

    public void onEndDownloadCallBack(FTP_EndDownloadCallback cback) {
        callback_onEndDownload = cback;
    }

    private void setEventEndDownload(int status) {
        callback_onEndDownload.onFtpEndDownload(status);
    }

    /***************************************************************************************************************/

    private UploadResult downloadRoutine(String DestinationDir, String share, String folder, String[] extension) {

        UploadResult down_result = new UploadResult();
        String[] FilesAlreadyExists = null;//список файлов уже существующих

        //получим список файлов уже существующих
        File DirSou = new File(DestinationDir);
        if (DirSou.isDirectory()) {
            FilesAlreadyExists = DirSou.list();
        }

        Log.d(TAG, "Take list Slide files: " + share + "/" + folder);
        changeStatus("отримання списку файлів..." + share + "/" + folder, true);
        down_result.countFiles = 0;
        down_result.countSkipped = 0;
        down_result.countDeleted = 0;

        FTPFile[] mFileArray = null;
        try {
            mFileArray = mFtpClient.listFiles(share + "/" + folder);
        } catch (IOException e) {
            Log.e(TAG, "IOException " + e);
            e.printStackTrace();
        }
        if (mFileArray == null) {
            down_result.hasError = UPLOAD_RESULT_IO_ERROR;
            down_result.countFiles = 0;
            return down_result;
        }

        int TotalFilesToDownload = mFileArray.length;

        changeStatus("файлiв..." + TotalFilesToDownload, false);

        Log.i(TAG, "Size:" + String.valueOf(TotalFilesToDownload));
        for (int i = 0; i < TotalFilesToDownload; i++) {
            UploadMedia.resetMediaPlay();//остановка демонстрации видео/слайдов
            // Log.d(TAG, "File:"+mFileArray[i].getName()+"  "+mFileArray[i].isDirectory());
            if (mFileArray[i].isDirectory()) continue;

            if (ExtSDSource.getAvailableMemory(MainActivity.context, ExtSDSource.DEFAULT_SD) < mFileArray[i].getSize()) {
                Log.e(TAG, "Not free memory");
                down_result.hasError = UPLOAD_RESULT_NOT_FREE_MEMORY;
                break;
            }

            if (UploadMedia.ifAlreadyExistFile(DestinationDir, mFileArray[i].getName(), mFileArray[i].getSize())) {
                Log.d(TAG, "Ftp skip file: " + mFileArray[i].getName());
                down_result.countSkipped++;
                continue;
            }
            //фильтр по расширению файла
            boolean isValidExtension = false;
            for (int a = 0; a < extension.length; a++) {
                if (mFileArray[i].toString().endsWith(extension[a].substring(2))) {
                    isValidExtension = true;
                    break;
                }
            }
            if (!isValidExtension) {
                Log.d(TAG, "Ftp skip file: " + mFileArray[i].getName());
                continue;
            }


            File localFile = new File(DestinationDir + mFileArray[i].getName());

            String statusStr = "Завантаження  " + (i + 1) + " з " + TotalFilesToDownload + " ";
            changeStatus(statusStr, false);

            if (downloadFtpFile(mFtpClient, share + "/" + folder + mFileArray[i].getName(), localFile, mFileArray[i].getSize(), statusStr))
                down_result.countFiles++;
        }

        changeStatus("Завантажено : " + down_result.countFiles, true);
        //удаление файлов
        // if (resultSlide.CountFiles > 0)
        {
            //удалим неиспользуемые файлы
            if (FilesAlreadyExists != null) {

                for (int i = 0; i < FilesAlreadyExists.length; i++) {
                    boolean forDelete = true;
                    for (int y = 0; y < TotalFilesToDownload; y++) {
                        if (mFileArray[y].getName().equals(FilesAlreadyExists[i])) {
                            forDelete = false;
                            break;
                        }
                    }
                    if (forDelete) {
                        Log.w(TAG, "To delete: " + FilesAlreadyExists[i]);
                        new File(DirSou, FilesAlreadyExists[i]).delete();
                        down_result.countDeleted++;
                    }
                }
            }
        }

        return down_result;
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

          /*  outputStream = new BufferedOutputStream(new FileOutputStream( downloadFile));
            ftpClient.setFileType(FTPClient.ASCII_FILE_TYPE);
            //ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);

            result = ftpClient.retrieveFile(remoteFilePath, outputStream);
            Log.d(TAG, "Load ftp file check point 4 ");
*/

            int progress = 0;

            outputStream = new BufferedOutputStream(new FileOutputStream(downloadFile));
            InputStream inputStream = ftpClient.retrieveFileStream(remoteFilePath);

            byte[] bytesArray = new byte[1024 * 500];//1024
            int bytesRead = -1;
            long totalBytes = 0;
            while ((bytesRead = inputStream.read(bytesArray)) > 0) {
                outputStream.write(bytesArray, 0, bytesRead);
                //Log.d(TAG, ">"+bytesRead);

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