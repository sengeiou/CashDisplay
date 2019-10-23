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

import static com.resonance.cashdisplay.load.DownloadMedia.DOWNLOAD_RESULT_BAD_ARGUMENTS;
import static com.resonance.cashdisplay.load.DownloadMedia.DOWNLOAD_RESULT_CONNECTION_ERROR;
import static com.resonance.cashdisplay.load.DownloadMedia.DOWNLOAD_RESULT_IO_ERROR;
import static com.resonance.cashdisplay.load.DownloadMedia.DOWNLOAD_RESULT_NOT_FREE_MEMORY;
import static com.resonance.cashdisplay.load.DownloadMedia.DOWNLOAD_RESULT_NOT_SUPPORT_PROTOCOL;
import static com.resonance.cashdisplay.load.DownloadMedia.DOWNLOAD_RESULT_SHARE_CONNECTION_ERROR;
import static com.resonance.cashdisplay.load.DownloadMedia.DOWNLOAD_RESULT_SUCCESSFULL;

//import android.util.Log;


public class FtpWorker{

    public final String TAG = "FtpWorker";
    private static FTPClient mFtpClient;

    private static Context mContext;


    public FtpWorker(Context context) {
        this.mContext = context;
    }


//ftp.rasla.ru

    public void doDownload(HashMap<String , Object> au_hashMap, HashMap<String , Object> img_hashMap, HashMap<String , Object> video_hashMap,  HashMap<String , Object> slide_hashMap) {
        new FtpTask().execute(au_hashMap, img_hashMap, video_hashMap, slide_hashMap);
    }

    private class FtpTask extends AsyncTask<HashMap<String , Object>, Void, Integer> {

        DownloadResult resultImg = new DownloadResult();
        DownloadResult resultVideo = new DownloadResult();
        DownloadResult resultSlide = new DownloadResult();



        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Integer doInBackground(HashMap<String , Object>... params) {

            resultImg.HasError = 0;
            resultVideo.HasError = 0;
            resultSlide.HasError = 0;

            HashMap<String, Object> au_hashMap = params[0];
            HashMap<String, Object> img_hashMap = params[1];
            HashMap<String, Object> video_hashMap = params[2];
            HashMap<String, Object> slide_hashMap = params[3];

            String User =  (String)au_hashMap.get("User");
            String Passw =  (String)au_hashMap.get("Passw");
            String Host =   (String)au_hashMap.get("Host");

            String shareImg = (String)img_hashMap.get("shareImg");
            String folderImg = (String)img_hashMap.get("folderImg");
            String DestinationImg = (String)img_hashMap.get("DestinationImg");
            String[] extensionImg = (String[])img_hashMap.get("extensionArrayImg");

            String shareVideo = (String)video_hashMap.get("shareVideo");
            String folderVideo = (String)video_hashMap.get("folderVideo");
            String DestinationVideo = (String)video_hashMap.get("DestinationVideo");
            String[] extensionVideo = (String[])video_hashMap.get("extensionArrayVideo");

            String shareSlide = (String)slide_hashMap.get("shareSlide");
            String folderSlide = (String)slide_hashMap.get("folderSlide");
            String DestinationSlide = (String)slide_hashMap.get("DestinationSlide");
            String[] extensionSlide = (String[])slide_hashMap.get("extensionArraySlide");

            int error= 0;
            //int TotalFilesToDownload = 0;

           // String[] FilesAlreadyExists = null;//список файлов уже существующих
            Log.d(TAG, "FtpTask... ");

            if ((User.length()==0)&&(Passw.length()==0)){
                User = "anonymous";
                Passw = "";
            }

            Log.d(TAG, "FtpTask... ");
            ChangeStatus(mContext.getString(R.string.connect_to_server)+": "+Host, true);
            try {
                Log.d(TAG, "Connect to " + Host + ".");
                mFtpClient = new FTPClient();
                mFtpClient.setConnectTimeout(10 * 1000);
                mFtpClient.setControlEncoding("UTF-8");
                mFtpClient.setBufferSize(1024*1024);

               // Log.d(TAG, "FTP buffer size:  " + mFtpClient.getBufferSize());
                mFtpClient.connect(InetAddress.getByName(Host));
                boolean status = mFtpClient.login(User, Passw);

                // mFtpClient.changeWorkingDirectory("/SymbolDirectory");
                Log.d(TAG, "FTPConnected "+String.valueOf(status));
                if (!status) {
                    ChangeStatus("підключення до сервера :"+Host+" - не вдалося", true);
                    return DOWNLOAD_RESULT_CONNECTION_ERROR;
                }

                int replyCode = mFtpClient.getReplyCode();
                Log.d(TAG, "FTP replyCode: "+replyCode);

                if (FTPReply.isPositiveCompletion(replyCode))
                {
                    mFtpClient.setFileType(FTPClient.LOCAL_FILE_TYPE);
                    mFtpClient.enterLocalPassiveMode();

                    resultImg   = download_Routine (DestinationImg, shareImg, folderImg, extensionImg);
                    resultVideo = download_Routine (DestinationVideo, shareVideo, folderVideo, extensionVideo);
                    resultSlide = download_Routine (DestinationSlide, shareSlide, folderSlide, extensionSlide);
                }
            } catch (SocketException e) {
                Log.e(TAG, "SocketException "+e);
                error = DOWNLOAD_RESULT_CONNECTION_ERROR;
                e.printStackTrace();
            } catch (UnknownHostException e) {
                Log.e(TAG, "UnknownHostException "+e);
                error = DOWNLOAD_RESULT_CONNECTION_ERROR;
                e.printStackTrace();
            } catch (IOException e) {
                Log.e(TAG, "IOException "+e);
                error = DOWNLOAD_RESULT_IO_ERROR;
                e.printStackTrace();
            }finally {
                try{
                    if (mFtpClient!=null) {
                        mFtpClient.logout();
                        mFtpClient.disconnect();
                    }
                } catch (IOException e)
                {
                    Log.e(TAG, "IOException "+e);
                    e.printStackTrace();
                }
            }
            return error;
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            Log.d(TAG, "FTPTask result : " + result);

            String ExtendedError_image ="помилки" ;
            String ExtendedError_slide ="помилки" ;
            String ExtendedError_video ="помилки" ;

            if (resultImg.HasError == DOWNLOAD_RESULT_NOT_FREE_MEMORY)
                ExtendedError_image = "недостатньо пам'ятi";

            if (resultVideo.HasError == DOWNLOAD_RESULT_NOT_FREE_MEMORY)
                ExtendedError_video = "недостатньо пам'ятi";

            if (resultSlide.HasError == DOWNLOAD_RESULT_NOT_FREE_MEMORY)
                ExtendedError_slide = "недостатньо пам'ятi";

            switch (result){


                case DOWNLOAD_RESULT_SUCCESSFULL:
                case DOWNLOAD_RESULT_SHARE_CONNECTION_ERROR:
                    ChangeStatus( "Вiдео["+(resultVideo.HasError>0?ExtendedError_video:"завантажено : "+resultVideo.CountFiles+", iснуючих : "+resultVideo.CountSkiped+", видалено : "+resultVideo.CountDeleted)+"]; \n "+
                            "Зображення["+(resultImg.HasError>0?ExtendedError_image:"завантажено : "+resultImg.CountFiles+", iснуючих : "+resultImg.CountSkiped+", видалено : "+resultImg.CountDeleted)+"]; \n "+
                            "Слайди["+(resultSlide.HasError>0?ExtendedError_slide:"завантажено : "+resultSlide.CountFiles+", iснуючих : "+resultSlide.CountSkiped+", видалено : "+resultSlide.CountDeleted)+"]", true);

                    break;
                case DOWNLOAD_RESULT_NOT_SUPPORT_PROTOCOL:
                    ChangeStatus("FTP Сервер не підтримує протокол ", true);
                    break;
                case DOWNLOAD_RESULT_CONNECTION_ERROR:
                    ChangeStatus("Неможливо пiдключитися до FTP сервера", true);
                    break;
                case DOWNLOAD_RESULT_BAD_ARGUMENTS:
                    ChangeStatus("Невiрнi параметри пiдключення до FTP сервера", true);
                    break;
                case DOWNLOAD_RESULT_NOT_FREE_MEMORY:
                    ChangeStatus("недостатньо пам'яті на носії"+ExtSDSource.getAvailableMemory_SD(), true);
                    break;
            }
            set_eventEndDownload(result);

        }
    }

    /**** колбэк статуса выполнения загрузки *********************/
    public interface FTP_StatusCallback{
        void onFtpStatus(String status, boolean delRemaininng);
    }
    private static FTP_StatusCallback callback_onFtpStatus;
    public void onChangeStatusCallBack(FTP_StatusCallback cback) {
        callback_onFtpStatus = cback;
    }
    private void ChangeStatus( String status , boolean delRemaininng) {
        callback_onFtpStatus.onFtpStatus(status, delRemaininng);
    }
    /**** колбэк окончания загрузки *********************/
    public interface FTP_EndDownloadCallback{
        void onFtpEndDownload(int status );
    }
    private static FTP_EndDownloadCallback callback_onEndDownload;

    public void onEndDownloadCallBack(FTP_EndDownloadCallback cback) {
        callback_onEndDownload = cback;
    }
    private void set_eventEndDownload( int status ) {
        callback_onEndDownload.onFtpEndDownload(status);
    }

/***************************************************************************************************************/

    private DownloadResult download_Routine (String DestinationDir, String share, String folder, String[] extension){

        DownloadResult down_result =  new DownloadResult();
        String[] FilesAlreadyExists = null;//список файлов уже существующих

        //получим список файлов уже существующих
        File DirSou = new File(DestinationDir);
        if (DirSou.isDirectory()) {
            FilesAlreadyExists = DirSou.list();
        }

        Log.d(TAG, "Take list Slide files: "+share+"/"+folder);
        ChangeStatus("отримання списку файлів..."+share+"/"+folder, true);
        down_result.CountFiles = 0;
        down_result.CountSkiped = 0;
        down_result.CountDeleted = 0;

        FTPFile[] mFileArray = null;
         try {
                mFileArray = mFtpClient.listFiles(share + "/" + folder);
        }catch (IOException e) {
            Log.e(TAG, "IOException "+e);
         e.printStackTrace();
        }
           if(mFileArray==null){
               down_result.HasError = DOWNLOAD_RESULT_IO_ERROR;
               down_result.CountFiles = 0;
               return down_result;
           }

        int TotalFilesToDownload = mFileArray.length;

        ChangeStatus("файлiв..."+TotalFilesToDownload, false);

        Log.i(TAG, "Size:"+String.valueOf(TotalFilesToDownload));
        for (int i=0;i<TotalFilesToDownload;i++)
        {
            DownloadMedia.resetMediaPlay();//остановка демонстрации видео/слайдов
            // Log.d(TAG, "File:"+mFileArray[i].getName()+"  "+mFileArray[i].isDirectory());
            if (mFileArray[i].isDirectory()) continue;

            if (ExtSDSource.getAvailableMemory(MainActivity.context, ExtSDSource.DEFAULT_SD)<mFileArray[i].getSize())
            {
                Log.e(TAG, "Not free memory");
                down_result.HasError = DOWNLOAD_RESULT_NOT_FREE_MEMORY;
                break;
            }

            if (DownloadMedia.ifAlreadyExistFile(DestinationDir, mFileArray[i].getName(), mFileArray[i].getSize())) {
                Log.d(TAG, "Ftp skip file: " +  mFileArray[i].getName() );
                down_result.CountSkiped++;
                continue;
            }
            //фильтр по расширению файла
            boolean isValidExtension = false;
            for (int a=0;a<extension.length;a++) {
                if (mFileArray[i].toString().endsWith(extension[a].substring(2))){
                    isValidExtension = true;
                    break;
                }
            }
            if (!isValidExtension) {
                Log.d(TAG, "Ftp skip file: " + mFileArray[i].getName());
                continue;
            }



            File localFile = new File(DestinationDir + mFileArray[i].getName());

            String statusStr = "Завантаження  "+(i+1)+" з "+TotalFilesToDownload+" ";
            ChangeStatus(statusStr, false);

            if (downloadFtpFile(mFtpClient, share+"/"+folder+mFileArray[i].getName(),localFile,mFileArray[i].getSize(), statusStr))
                down_result.CountFiles++;
        }

        ChangeStatus("Завантажено : "+down_result.CountFiles, true);
        //удаление файлов
        // if (resultSlide.CountFiles > 0)
        {
            //удалим неиспользуемые файлы
            if (FilesAlreadyExists != null) {

                for (int i = 0; i < FilesAlreadyExists.length; i++)
                {
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
                        down_result.CountDeleted++;
                    }
                }
            }
        }

        return down_result;
    }




/***************************************************************************************************************/
    /**
     * @param ftpClient FTPclient object
     * @param remoteFilePath  FTP server file path
     * @param downloadFile   local file path where you want to save after download
     * @return status of downloaded file
     */
    public boolean downloadFtpFile(FTPClient ftpClient, String remoteFilePath, File downloadFile, long sizefile, String status) {

        boolean result  = false;
        File parentDir = downloadFile.getParentFile();
        if (!parentDir.exists())
            parentDir.mkdir();

        Log.d(TAG, "Load ftp file: "+remoteFilePath+" save to: "+downloadFile);
        OutputStream outputStream = null;

        try {

          /*  outputStream = new BufferedOutputStream(new FileOutputStream( downloadFile));
            ftpClient.setFileType(FTPClient.ASCII_FILE_TYPE);
            //ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);

            result = ftpClient.retrieveFile(remoteFilePath, outputStream);
            Log.d(TAG, "Load ftp file check point 4 ");
*/

            int progress=0;

            outputStream = new BufferedOutputStream(new FileOutputStream(downloadFile));
            InputStream inputStream = ftpClient.retrieveFileStream(remoteFilePath);

            byte[] bytesArray = new byte[1024*500];//1024
            int bytesRead = -1;
            long totalBytes = 0;
            while ((bytesRead = inputStream.read(bytesArray)) >0) {
                outputStream.write(bytesArray, 0, bytesRead);
                //Log.d(TAG, ">"+bytesRead);

                totalBytes+=bytesRead;

                if (progress++>150){
                    progress=0;
                    ChangeStatus(status + " " + (int)((100*totalBytes)/sizefile)+"%", true);
                }

            }

            result  = true;
            ftpClient.completePendingCommand();



        } catch (Exception ex) {
            Log.e(TAG, "Exception Load ftp file: "+ex);
            ex.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "IOException ftp file :"+remoteFilePath);
                    e.printStackTrace();
                }
            }
        }
        Log.i(TAG, "result Load ftp file :"+result);
        return result;
    }
    /**
     *
     * @param ftpClient FTPclient object
     * @param downloadFile local file which need to be uploaded.
     */
    public void uploadFile(FTPClient ftpClient, File downloadFile,String serverfilePath) {
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