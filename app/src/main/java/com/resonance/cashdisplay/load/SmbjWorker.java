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

import static com.resonance.cashdisplay.load.DownloadMedia.DOWNLOAD_RESULT_BAD_ARGUMENTS;
import static com.resonance.cashdisplay.load.DownloadMedia.DOWNLOAD_RESULT_CONNECTION_ERROR;
import static com.resonance.cashdisplay.load.DownloadMedia.DOWNLOAD_RESULT_ERROR_SMB_SERVER;
import static com.resonance.cashdisplay.load.DownloadMedia.DOWNLOAD_RESULT_FILE_NOT_FOUND;
import static com.resonance.cashdisplay.load.DownloadMedia.DOWNLOAD_RESULT_IO_ERROR;
import static com.resonance.cashdisplay.load.DownloadMedia.DOWNLOAD_RESULT_NOT_FREE_MEMORY;
import static com.resonance.cashdisplay.load.DownloadMedia.DOWNLOAD_RESULT_NOT_SUPPORT_PROTOCOL;
import static com.resonance.cashdisplay.load.DownloadMedia.DOWNLOAD_RESULT_SHARE_CONNECTION_ERROR;
import static com.resonance.cashdisplay.load.DownloadMedia.DOWNLOAD_RESULT_SUCCESSFULL;

//import android.util.Log;



public class SmbjWorker {


    private static List<String> ListFilesAlreadyExists ;
    static {
        Security.addProvider(new BouncyCastleProvider());
    }
  //35.233.122.204
  //admin
  //>zC_+{kdC-Adbe&

    public final String TAG = "SmbjWorker";
    private static final String NEW_LINE =  System.getProperty("line.separator") ;

    private static Context mContext;

    public SmbjWorker(Context context) {
        this.mContext = context;
    }
    public SmbjWorker() { }

       /**** колбэк статуса выполнения загрузки *********************/
        public interface SMBJ_StatusCallback{
            void onSmbjStatus(String status, boolean delRemaininng);
        }
        private static SMBJ_StatusCallback callback_onSmbjStatus;
        public void onChangeStatusCallBack(SMBJ_StatusCallback cback) {
            callback_onSmbjStatus = cback;
        }
        private void ChangeStatus( String status , boolean delRemaininng) {
            callback_onSmbjStatus.onSmbjStatus(status, delRemaininng);
        }
        /**** колбэк окончания загрузки *********************/
        public interface SMBJ_EndDownloadCallback{
            void onSmbjEndDownload(int status );
        }
        private static SMBJ_EndDownloadCallback callback_onEndDownload;

        public void onEndDownloadCallBack(SMBJ_EndDownloadCallback cback) {
            callback_onEndDownload = cback;
        }
        private void setVentEndDownload( int status ) {
            callback_onEndDownload.onSmbjEndDownload(status);
        }



        public void doDownload(HashMap<String , Object> au_hashMap,
                               HashMap<String , Object> img_hashMap,
                               HashMap<String , Object> video_hashMap,
                               HashMap<String , Object> slide_hashMap ,
                               HashMap<String , Object> screen_img_hashMap) {

        new SmbjTask().execute(au_hashMap, img_hashMap, video_hashMap, slide_hashMap, screen_img_hashMap);
    }

    private class SmbjTask extends AsyncTask<HashMap<String , Object>, Void, Integer> {

        DownloadResult resultImg = new DownloadResult();
        DownloadResult resultVideo = new DownloadResult();
        DownloadResult resultSlide = new DownloadResult();
        DownloadResult resultScreenImg = new DownloadResult();


        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Integer doInBackground(HashMap<String , Object>... params) {

            HashMap<String, Object> au_hashMap          = params[0];
            HashMap<String, Object> img_hashMap         = params[1];
            HashMap<String, Object> video_hashMap       = params[2];
            HashMap<String, Object> slide_hashMap       = params[3];
            HashMap<String, Object> screenimg_hashMap   = params[4];

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

            String shareScreenImg = (String)screenimg_hashMap.get("shareScreenImg");
            String folderScreenImg = (String)screenimg_hashMap.get("folderScreenImg");
            String DestinationScreenImg = (String)screenimg_hashMap.get("DestinationScreenImg");
            String[] extensionScreenImg = (String[])screenimg_hashMap.get("extensionArrayScreenImg");


            resultImg.HasError = DOWNLOAD_RESULT_SUCCESSFULL;
            resultImg.CountFiles = 0;
            resultImg.CountSkiped = 0;
            resultImg.CountDeleted = 0;

            resultVideo.HasError = DOWNLOAD_RESULT_SUCCESSFULL;
            resultVideo.CountFiles = 0;
            resultVideo.CountSkiped = 0;
            resultVideo.CountDeleted = 0;

            resultSlide.HasError = DOWNLOAD_RESULT_SUCCESSFULL;
            resultSlide.CountFiles = 0;
            resultSlide.CountSkiped = 0;
            resultSlide.CountDeleted = 0;

            resultScreenImg.HasError = DOWNLOAD_RESULT_SUCCESSFULL;
            resultScreenImg.CountFiles = 0;
            resultScreenImg.CountSkiped = 0;
            resultScreenImg.CountDeleted = 0;


            Log.d(TAG, "SmbjTask... ");

            SmbConfig config = SmbConfig.builder()
                    .withMultiProtocolNegotiate(true)
                    .withSecurityProvider(new JceSecurityProvider(new BouncyCastleProvider()))
                    .withSigningRequired(true)
                    //.withDfsEnabled(true)
                    .withAuthenticators(new NtlmAuthenticator.Factory())
                    .build();

            int error = DOWNLOAD_RESULT_SUCCESSFULL;
            Connection connection = null;
            Session session = null;
           // DiskShare share = null;

            try {
                ChangeStatus(mContext.getString(R.string.connect_to_server)+" :"+Host, true);
                SMBClient client = new SMBClient(config);

                Log.d(TAG, "Smbj, try connection : " + Host+" User:"+User+", Passw:"+Passw);
                connection = client.connect(Host);
                AuthenticationContext authenticationContext = new AuthenticationContext(User, Passw.toCharArray(), "");
                session = connection.authenticate(authenticationContext);
                Log.d(TAG, "Smbj connect successfull");


                ChangeStatus(mContext.getString(R.string.get_data_ScreenFiles), false);
                resultScreenImg = HandlerFiles(session, shareScreenImg, folderScreenImg, DestinationScreenImg, extensionScreenImg);

                ChangeStatus(mContext.getString(R.string.get_data_Img), false);
                resultImg = HandlerFiles(session, shareImg, folderImg, DestinationImg, extensionImg);

                ChangeStatus(mContext.getString(R.string.get_data_Video), false);
                resultVideo = HandlerFiles(session, shareVideo, folderVideo, DestinationVideo, extensionVideo);

                ChangeStatus(mContext.getString(R.string.get_data_Slide), false);
                resultSlide = HandlerFiles(session, shareSlide, folderSlide, DestinationSlide, extensionSlide);


            } catch (Exception e) {
                    Log.e(TAG, "Smbj Exception: "+e);
                    if (e.getMessage().contains(" is not supported")){
                        error = DOWNLOAD_RESULT_NOT_SUPPORT_PROTOCOL;
                    }
                    else if  (e.getMessage().contains("Could not connect")||e.getMessage().contains("failed to connect")){
                        error = DOWNLOAD_RESULT_CONNECTION_ERROR;
                    }
                    else if  ((e.getMessage().contains("IllegalArgumentException"))||(e.getMessage().contains("Cannot require message signing when authenticating"))){
                        error = DOWNLOAD_RESULT_BAD_ARGUMENTS;
                    }

                }finally
                {

                    try {
                        //выгрузим на сервер лог загрузки
                        DiskShare share = (DiskShare) session.connectShare(shareScreenImg);
                        if (share.isConnected())
                        {
                            try {
                                String log_dir = "LOG";
                                if (!share.folderExists(log_dir)){
                                  share.mkdir(log_dir);
                                }
                                String remote_log_file = log_dir+"/"+DownloadMedia.mLogFile.getName();
                                if (!share.fileExists(remote_log_file)){


                                   com.hierynomus.smbj.share.File file = share.openFile(remote_log_file, EnumSet.of(AccessMask.GENERIC_ALL), null,SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_CREATE, null);
                                    file.close();
                                   // Log.d(TAG, "LOG >>"+share.fileExists(remote_log_file));
                                }



                                SmbFiles.copy(DownloadMedia.mLogFile, share, remote_log_file, true);
                            } catch (IOException e) {
                                Log.e(TAG, "Smb IOException: " + e);
                            }
                            ;
                        }else
                            Log.w(TAG, "Smb share is not connect " );
                    }
                    catch (Exception e) {
                        Log.e(TAG, "Smb Exception(2): " + e);
                    }
                    //удалим лог, предназначеный для передачи на сервер
                    DownloadMedia.delete_DownloadLog();

                    if (session != null){
                        session = null;
                    }
                    try {
                        if ((connection != null) && (connection.isConnected())) {
                            connection.close(true);
                        }
                    }catch (IOException e1){
                        Log.e(TAG, "Smbj IOException: "+e1);
                    };
                    connection =  null;
                }
            return error;
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            Log.d(TAG, "SmbjTask result : " + result);

            String ExtendedError_image ="помилки" ;
            String ExtendedError_slide ="помилки" ;
            String ExtendedError_video ="помилки" ;
            String ExtendedError_screenImg ="помилки" ;

            if (resultImg.HasError == DOWNLOAD_RESULT_NOT_FREE_MEMORY)
                ExtendedError_image = "недостатньо пам'ятi";

            if (resultVideo.HasError == DOWNLOAD_RESULT_NOT_FREE_MEMORY)
                ExtendedError_video = "недостатньо пам'ятi";

            if (resultSlide.HasError == DOWNLOAD_RESULT_NOT_FREE_MEMORY)
                ExtendedError_slide = "недостатньо пам'ятi";

            if (resultScreenImg.HasError == DOWNLOAD_RESULT_NOT_FREE_MEMORY)
                ExtendedError_screenImg = "недостатньо пам'ятi";

            switch (result){
                case DOWNLOAD_RESULT_SUCCESSFULL:
                case DOWNLOAD_RESULT_SHARE_CONNECTION_ERROR:
                    String status =
                            "<font color=\"blue\"><B>Фоновi зображення:</B><br></font>["+(resultScreenImg.HasError>0?ExtendedError_screenImg:"завантажено : <B>"+resultScreenImg.CountFiles+"</B>, iснуючих : <B>"+resultScreenImg.CountSkiped+"</B>, видалено : <B>"+resultScreenImg.CountDeleted)+"</B>];  <br>"+
                            "<font color=\"blue\"><B>Вiдео:</B><br></font>["+(resultVideo.HasError>0?ExtendedError_video:"завантажено : <B>"+resultVideo.CountFiles+"</B>, iснуючих : <B>"+resultVideo.CountSkiped+"</B>, видалено : <B>"+resultVideo.CountDeleted)+"</B>];  <br>"+
                            "<font color=\"blue\"><B>Зображення товарiв:</B><br></font>["+(resultImg.HasError>0?ExtendedError_image:"завантажено : <B>"+resultImg.CountFiles+"</B>, iснуючих : <B>"+resultImg.CountSkiped+"</B>, видалено : <B>"+resultImg.CountDeleted)+"</B>];  <br>"+
                            "<font color=\"blue\"><B>Слайди:</B><br></font>["+(resultSlide.HasError>0?ExtendedError_slide:"завантажено : <B>"+resultSlide.CountFiles+"</B>, iснуючих : <B>"+resultSlide.CountSkiped+"</B>, видалено : <B>"+resultSlide.CountDeleted)+"</B>];";
                    ChangeStatus(status, true);

                    break;
                case DOWNLOAD_RESULT_NOT_SUPPORT_PROTOCOL:
                    ChangeStatus("Сервер не підтримує протокол ", true);
                    break;
                case DOWNLOAD_RESULT_CONNECTION_ERROR:
                    ChangeStatus("Неможливо пiдключитися до файлового сервера", true);
                    Log.e(TAG, "Неможливо пiдключитися до файлового сервера");
                    break;
                case DOWNLOAD_RESULT_BAD_ARGUMENTS:
                    ChangeStatus("Невiрнi параметри пiдключення до файлового сервера", true);
                    break;
                case DOWNLOAD_RESULT_NOT_FREE_MEMORY:
                    ChangeStatus("недостатньо пам'яті на носії"+ExtSDSource.getAvailableMemory_SD(), true);
                    break;
                case DOWNLOAD_RESULT_ERROR_SMB_SERVER:
                    ChangeStatus("Помилка сервера SMB", true);
                    break;
            }
            setVentEndDownload(result);

        }
    }

    //
    // Определение списка существующих файлов
    // подключение к ресурсу
    // Загрузка файлов
    // Удаление ненужных файлов
    //
    private DownloadResult HandlerFiles(Session session, String share_folder, String source_folder, String destination_folder, String[] extension_files)
    {

        DownloadResult downloadResult = new DownloadResult();
        downloadResult.HasError = DOWNLOAD_RESULT_SUCCESSFULL;
        downloadResult.CountFiles = 0;
        downloadResult.CountSkiped = 0;
        downloadResult.CountDeleted = 0;
        int divider = 0;

        //получим список файлов уже существующих
        ListFilesAlreadyExists = null;
        File DirSou = new File(destination_folder);
        if (DirSou.isDirectory()) {
            ListFilesAlreadyExists = new ArrayList<String>(Arrays.asList(DirSou.list()));
        }

        //  Connect to share
        ChangeStatus(mContext.getString(R.string.try_connect_to_source), false);
        try {
            DiskShare share = (DiskShare) session.connectShare(share_folder);
            if (share.isConnected())
            {

                for (int i = 0; i < extension_files.length; i++) {
                    DownloadResult tmpresultImg = DownloadFromShareFolder(share, source_folder, extension_files[i], destination_folder);
                    downloadResult.CountFiles+= tmpresultImg.CountFiles;
                    downloadResult.HasError = tmpresultImg.HasError;
                    Log.d(TAG, "IMG Загружено:  " + downloadResult.CountFiles + " ext:" + extension_files[i]);
                }
                share.close();
            } else {
                ChangeStatus("неможливо пiдключитися до " + share_folder, false);
                downloadResult.HasError =DOWNLOAD_RESULT_SHARE_CONNECTION_ERROR;
            }
        }catch (Exception e)
        {
            Log.e(TAG, "Smbj Img Exception: "+e);
            ChangeStatus("Неможливо пiдключитися до "+share_folder, false);
            downloadResult.HasError =DOWNLOAD_RESULT_CONNECTION_ERROR;
        }

        //Удалим файлы, которые были определены на удаление
        ChangeStatus("Видалення файлiв - "+ListFilesAlreadyExists.size(), false);
        for (int i=0;i<ListFilesAlreadyExists.size();i++){
            new File(destination_folder, ListFilesAlreadyExists.get(i)).delete();
            DownloadMedia.append_to_DownloadLog("Удален : "+ListFilesAlreadyExists.get(i));
            downloadResult.CountDeleted++;
            if ((divider++)>=50)
                ChangeStatus("Видалення файлiв - "+downloadResult.CountDeleted+" iз "+ListFilesAlreadyExists.size(), false);
        }
        return downloadResult;
    }

    private DownloadResult DownloadFromShareFolder(DiskShare share, String shareSourceFolder, String file_search_pattern, String DestinationFolder )
    {
        DownloadResult dr = new DownloadResult();
        dr.HasError = 0;
        dr.CountFiles = 0;
        dr.CountSkiped = 0;
        int divider = 0;

        ChangeStatus("отримання списку файлів...", false);
        DownloadMedia.append_to_DownloadLog("(SMB2) Получение списка файлов..."+shareSourceFolder);
        DownloadMedia.resetMediaPlay();//остановка демонстрации видео/слайдов

        for (FileIdBothDirectoryInformation f : share.list(shareSourceFolder, file_search_pattern))
        {
            divider++;

            com.hierynomus.smbj.share.File remoteSmbjFile = share.openFile(shareSourceFolder + f.getFileName(), EnumSet.of(AccessMask.GENERIC_READ), null,SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null);
            long size = remoteSmbjFile.getFileInformation().getStandardInformation().getEndOfFile();



            //определяем файлы, которые после окончания загрузки будут удалены из каталога
            if (ListFilesAlreadyExists.contains(f.getFileName()))
                ListFilesAlreadyExists.remove(f.getFileName());

            if (ExtSDSource.getAvailableMemory(MainActivity.context, ExtSDSource.DEFAULT_SD)<size)
            {
                Log.e(TAG, "Not enough memory");
                DownloadMedia.append_to_DownloadLog("ОШИБКА, недостаточно места на SD карте: "+ExtSDSource.getAvailableMemory(MainActivity.context, ExtSDSource.DEFAULT_SD));
                dr.HasError = DOWNLOAD_RESULT_NOT_FREE_MEMORY;
                break;
            }

            if (DownloadMedia.ifAlreadyExistFile(DestinationFolder, f.getFileName(), size)) {
                Log.d(TAG, "Smbj skip file: " + shareSourceFolder + f.getFileName() );

                if (dr.CountSkiped%10 ==0)
                    ChangeStatus("Пропущено :"+f.getFileName(), true);
                dr.CountSkiped++;
                continue;
            }

            Log.d(TAG, "Smbj download file: " + shareSourceFolder + f.getFileName()+" fileSize:"+size );

            java.io.File dest = new File(DestinationFolder, f.getFileName());
            String dstPath = DestinationFolder + f.getFileName();

            InputStream is = null;
            FileOutputStream  os = null;
            BufferedOutputStream bos = null;
            try {
                is = remoteSmbjFile.getInputStream();
                os = new FileOutputStream(dest);
                bos = new BufferedOutputStream(os);

                byte[] buffer = new byte[1024 * 500];
                int length = 0;
                Log.d(TAG, "file: " + dstPath );

                while ((length = is.read(buffer)) !=-1) {
                    bos.write(buffer, 0, length);
                }
               DownloadMedia.append_to_DownloadLog("загружен : "+shareSourceFolder + f.getFileName()+" размер: "+size);

            } catch (FileNotFoundException e) {
                Log.e(TAG, "Smbj FileNotFoundException :" + e);
                ChangeStatus("Помилка, FileNotFoundException", true);
                dr.HasError = DOWNLOAD_RESULT_FILE_NOT_FOUND;
            } catch (IOException e) {
                Log.e(TAG, "Smbj IOException :" + e);
                ChangeStatus("Виникли помилки при завантаженнi файлiв", true);
                dr.HasError = DOWNLOAD_RESULT_IO_ERROR;
            } finally {
                try {

                    remoteSmbjFile.close();
                    if (bos!=null) {
                        bos.flush();
                        bos.close();
                    }

                    if (os != null)
                        os.close();
                    if (is != null)
                        is.close();
                }catch (IOException ex){
                    Log.e(TAG, "Smbj IOException :" + ex);
                    dr.HasError = DOWNLOAD_RESULT_IO_ERROR;
                }
            }
            dr.CountFiles++;

            if ((divider++)>=10) {
                DownloadMedia.resetMediaPlay();//остановка демонстрации видео/слайдов
                ChangeStatus("Завантаження: ["+file_search_pattern+"] "+dr.CountFiles, true);
                divider = 0;
            }



        }
        return dr;
    }




}