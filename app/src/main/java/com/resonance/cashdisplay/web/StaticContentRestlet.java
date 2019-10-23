package com.resonance.cashdisplay.web;


import android.content.Intent;
import android.content.res.Resources;

import com.resonance.cashdisplay.BuildConfig;
import com.resonance.cashdisplay.ExtSDSource;
import com.resonance.cashdisplay.Log;
import com.resonance.cashdisplay.MainActivity;
import com.resonance.cashdisplay.PreferenceParams;
import com.resonance.cashdisplay.PreferencesValues;
import com.resonance.cashdisplay.eth.Modify_SU_Preferences;
import com.resonance.cashdisplay.load.DownloadMedia;
import com.resonance.cashdisplay.sound.Sound;

import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.InputRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;

import java.io.IOException;
import java.io.InputStream;

import static com.resonance.cashdisplay.uart.UartWorker.UART_CHANGE_SETTINGS;

//import android.util.Log;


public class StaticContentRestlet extends Restlet {

    private Context context;
    private static final String TAG = "StaticContentRestlet";
    private static final String ROOT_URI = "";
    private static final String FILE_START = "index";

    private static PreferencesValues prefValues;
    private static PreferenceParams prefParams;

    private final int STAT_IDLE = 0;
    private final int STAT_LOAD_FILES = 1;
    private final int STAT_SAVE = 2;
    private final int STAT_LOAD_FIRMWARE = 3;
    //private final int STAT_REBOOT = 4;

    private int iCurStatus = STAT_IDLE;
    private String iCurStatusMsg = "";

    public StaticContentRestlet() {
        prefParams = new PreferenceParams();
        prefValues = prefParams.getParameters();
        new createProducerConsumer().start();
    }

    private class createProducerConsumer extends Thread {
        @Override
        public void run() {
            super.run();

            //Log.d(TAG, "createProducerConsumer  started");
            while (!isInterrupted()) {
                try {
                    String msg = "";
                    if ((msg = MainActivity.httpServer.webStatus.getStrStatus()).length() > 0) {

                        iCurStatus = STAT_LOAD_FILES;
                        iCurStatusMsg = msg;
                        Log.w(TAG, "Smb_messageQueue.take:" + iCurStatusMsg);


                    } else {
                        Thread.sleep(300);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Log.w(TAG, "***createProducerConsumer  stoped****");
        }
    }

    /**************************************************************/
    @Override
    public void handle(Request request, Response response) {

        String type = request.getMethod().getName();
        String fileName = (String) request.getAttributes().get("uid");

        //Log.d(TAG, "*** request:"+request+" " +request.getRootRef()+" " +request.getAttributes());//getHostRef()

        if (fileName == null) fileName = FILE_START;

        //  Log.d(TAG, "*** "+type+" request :"+request);

        if (type.equalsIgnoreCase("get")) {

            if (("" + request).contains("/getsettings"))//(fileName.equals("getsettings"))//получение параметров
            {
                iCurStatus = STAT_LOAD_FILES;
                iCurStatusMsg = "Пiдключено";

                try {
                    JSONObject requestBody = new JSONObject();
                    prefValues = prefParams.getParameters();

                    requestBody.put("uart_select", prefValues.sUartName);
                    requestBody.put("host_img", prefValues.sSmbImg);
                    requestBody.put("host_video", prefValues.sSmbVideo);
                    requestBody.put("host_slide", prefValues.sSmbSlide);
                    requestBody.put("host_screen_img", prefValues.sPathToScreenImg);

                    requestBody.put("host", prefValues.sSmbHost);
                    requestBody.put("ftp_user", prefValues.sUser);
                    requestBody.put("ftp_pass", prefValues.sPassw);
                    requestBody.put("timeout_video", prefValues.videoTimeout);
                    requestBody.put("enable_video", prefValues.sCheckEnableVideo);
                    requestBody.put("admin_user", prefValues.sAdmin);
                    requestBody.put("admin_pass", prefValues.sAdminPassw);
                    requestBody.put("download_at_start", prefValues.sDownloadAtStart);
                    requestBody.put("volume", prefValues.sPercentVolume);
                    requestBody.put("stat_adress", prefValues.sIP);
                    requestBody.put("stat_mask", prefValues.sMask);
                    requestBody.put("stat_gate", prefValues.sGW);
                    requestBody.put("stat_dns", prefValues.sDNS);

                    requestBody.put("dhcp", prefValues.sDHCP);
                    requestBody.put("lab_current_ver", BuildConfig.VERSION_CODE);
                    requestBody.put("protocol", prefValues.sProtocol);
                    requestBody.put("def_background_img", prefValues.sDefaultBackGroundImage);

                    requestBody.put("time_slide_image", prefValues.sTimeSlideImage);
                    requestBody.put("option_video_slide", (prefValues.sVideoOrSlide == PreferenceParams._VIDEO ? true : false));

                    requestBody.put("image_screen_shoppinglist", prefValues.backgroundShoppingList);
                    requestBody.put("image_screen_cash_not_work", prefValues.backgroundCashNotWork);
                    requestBody.put("image_screen_thanks", prefValues.backgroundThanks);

                    Log.d(TAG, "Send JSON: " + requestBody.toString());
                    response.setEntity(new StringRepresentation(requestBody.toString(), MediaType.APPLICATION_JSON));
                } catch (JSONException e) {
                    Log.e(TAG, "JSONException:" + e.getMessage());
                }

            } else if (("" + request).contains("/getstatus")) {
                // Log.d(TAG, "getstatus...");
                try {
                /*    try {
                        if (!Smb_messageQueue.isEmpty()) {
                            iCurStatus = STAT_LOAD_FILES;
                            iCurStatusMsg =  Smb_messageQueue.take();
                            //Log.w(TAG, "Smb_messageQueue.take:"+iCurStatusMsg);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }*/

                    JSONObject requestBody = new JSONObject();

                    requestBody.put("status", iCurStatus);
                    requestBody.put("CurStatusMsg", iCurStatusMsg);
                    requestBody.put("lab_current_ver", BuildConfig.VERSION_CODE);
                    requestBody.put("sdcard_state", "<font color=\"blue\"><B>пам`ятi вiльно " + ExtSDSource.getAvailableMemory_SD() + "</B></font> ");

                    Log.d(TAG, "restlet send status : " + requestBody.toString());

                    response.setEntity(new StringRepresentation(requestBody.toString(), MediaType.APPLICATION_JSON));

                    if (iCurStatus == STAT_SAVE) {
                        iCurStatusMsg = "";
                        iCurStatus = STAT_IDLE;
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "JSONException:" + e.getMessage());
                }

            } else {// if (("" + request).endsWith("/")){ // (fileName.equals(FILE_START)){//чтение файла

                Log.d(TAG, "handle: " + request + " fileName:" + fileName);

                try {

                    Representation r = readStaticFile(fileName + ".html");
                    response.setEntity(r);
                } catch (Resources.NotFoundException e) {
                    Log.e(TAG, "Resources.NotFoundException:" + e.getMessage());
                    response.setStatus(new Status(Status.CLIENT_ERROR_NOT_FOUND, e.getMessage()));
                } catch (IOException e) {
                    Log.e(TAG, "IOException:" + e.getMessage());
                    response.setStatus(new Status(Status.SERVER_ERROR_INTERNAL, e.getMessage()));
                }
            }
        }

        if (type.equalsIgnoreCase("post")) {

            if (("" + request).contains("/setsettings")) {
                iCurStatus = STAT_SAVE;
                iCurStatusMsg = "Збереження налаштуваннь ";

                try {
                    JsonRepresentation jsonRepresentation = new JsonRepresentation(request.getEntity());
                    JSONObject jsonObject = jsonRepresentation.getJsonObject();

                    response.setEntity(new StringRepresentation(jsonObject.toString(), MediaType.APPLICATION_JSON));

                    Log.d(TAG, "Save settings: " + jsonObject.toString());

                    prefValues.sUartName = jsonObject.get("uart_select").toString();
                    prefValues.sSmbImg = jsonObject.get("host_img").toString();
                    prefValues.sSmbVideo = jsonObject.get("host_video").toString();
                    prefValues.sSmbSlide = jsonObject.get("host_slide").toString();

                    prefValues.sSmbHost = jsonObject.get("host").toString();
                    prefValues.sUser = jsonObject.get("ftp_user").toString();
                    prefValues.sPassw = jsonObject.get("ftp_pass").toString();
                    prefValues.sCheckEnableVideo = (boolean) jsonObject.get("enable_video");
                    String timeout_Str = (String) jsonObject.get("timeout_video");
                    prefValues.videoTimeout = Integer.parseInt(timeout_Str.length() > 0 ? timeout_Str : "20");//поставим по умолчанию 5 сек
                    if (prefValues.videoTimeout < 5)
                        prefValues.videoTimeout = 5;

                    prefValues.sDownloadAtStart = (boolean) jsonObject.get("download_at_start");
                    int prevVolume = prefValues.sPercentVolume;
                    String volume_Str = (String) jsonObject.get("volume");
                    prefValues.sPercentVolume = Integer.parseInt(volume_Str.length() > 0 ? volume_Str : "50");//поставим по умолчанию 50
                    if (prefValues.sPercentVolume != prevVolume) {
                        Sound.setVolume(prefValues.sPercentVolume);
                    }
                    prefValues.sIP = (String) jsonObject.get("stat_adress");
                    prefValues.sMask = (String) jsonObject.get("stat_mask");
                    prefValues.sGW = (String) jsonObject.get("stat_gate");
                    prefValues.sDNS = (String) jsonObject.get("stat_dns");
                    prefValues.sDHCP = (boolean) jsonObject.get("dhcp");

                    // prefValues.sAdmin = jsonObject.get("admin_user").toString();
                    // prefValues.sAdminPassw = jsonObject.get("admin_pass").toString();
                    prefValues.sProtocol = jsonObject.get("protocol").toString();
                    prefValues.sDefaultBackGroundImage = jsonObject.get("def_background_img").toString();

                    String tmpTimeSlideImg = jsonObject.get("time_slide_image").toString();
                    prefValues.sTimeSlideImage = Integer.parseInt(tmpTimeSlideImg.length() > 0 ? tmpTimeSlideImg : "10");
                    String tmpvideo_slide = jsonObject.get("option_video_slide").toString();
                    prefValues.sVideoOrSlide = (tmpvideo_slide.equals("true") ? PreferenceParams._VIDEO : PreferenceParams._SLIDE);

                    prefValues.backgroundShoppingList = (String) jsonObject.get("image_screen_shoppinglist");
                    prefValues.backgroundCashNotWork = (String) jsonObject.get("image_screen_cash_not_work");
                    prefValues.backgroundThanks = (String) jsonObject.get("image_screen_thanks");

                    prefValues.sPathToScreenImg = (String) jsonObject.get("host_screen_img");

                    prefParams.setParameters(prefValues);
                    prefValues = prefParams.getParameters();

                    iCurStatus = STAT_SAVE;
                    iCurStatusMsg = "Збереження виконано ";

                    MainActivity.ethernetSettings.applyEthernetSettings();//контроль измененмя сетевых настроек

                    //сигнал на изменение настройки UART
                    Intent intent = new Intent(UART_CHANGE_SETTINGS);
                    MainActivity.context.sendBroadcast(intent);

                    intent = new Intent(MainActivity.CHANGE_SETTINGS);
                    MainActivity.context.sendBroadcast(intent);

                } catch (IOException e) {
                    Log.e(TAG, "IOException:" + e.getMessage());
                    iCurStatusMsg = "Збереження не виконано, помилки ";
                } catch (JSONException e) {
                    Log.e(TAG, "JSONException:" + e.getMessage());
                } catch (NullPointerException e) {
                    Log.e(TAG, "NullPointerException:" + e.getMessage());
                    iCurStatusMsg = "Збереження не виконано, помилки ";
                }

            } else if (("" + request).contains("/download_files"))////команда на загрузку файлов
            {
                Log.d(TAG, "download_files...");
                iCurStatus = STAT_LOAD_FILES;

                /**************************************/
                MainActivity.downloadMedia.download();

            } else if (("" + request).contains("/start_remote_update")) {
                Log.d(TAG, "set_start_remote_update...");
                iCurStatus = STAT_LOAD_FIRMWARE;
                MainActivity.updateFirmware.update();
            } else if (("" + request).contains("/start_reboot")) {

                iCurStatus = STAT_IDLE;
                DownloadMedia.resetMediaPlay();
                Log.d(TAG, "set_start_reboot...");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                }

                Modify_SU_Preferences.executeCmd("reboot", 2000);
            } else if (("" + request).contains("/set_admin_settings")) {
                iCurStatus = STAT_SAVE;
                iCurStatusMsg = "Збереження налаштуваннь ";

                try {
                    JsonRepresentation jsonRepresentation = new JsonRepresentation(request.getEntity());
                    JSONObject jsonObject = jsonRepresentation.getJsonObject();

                    response.setEntity(new StringRepresentation(jsonObject.toString(), MediaType.APPLICATION_JSON));

                    Log.d(TAG, "Save admin settings: " + jsonObject.toString());

                    prefValues.sAdmin = jsonObject.get("admin_user").toString();
                    prefValues.sAdminPassw = jsonObject.get("admin_pass").toString();
                    prefParams.setParameters(prefValues);
                    prefValues = prefParams.getParameters();

                    Log.d(TAG, " admin_user: " + prefValues.sAdmin);
                    Log.d(TAG, " admin_pass: " + prefValues.sAdminPassw);
                    iCurStatusMsg = "Збереження виконано ";
                    //  WebApplication app = (WebApplication) getApplication();
                    //  app.reauthenticate(request, response);
                    //WebServer.reconnect();
                } catch (IOException e) {
                    Log.e(TAG, "IOException:" + e.getMessage());
                    iCurStatusMsg = "Збереження не виконано, помилки ";
                } catch (JSONException e) {
                    Log.e(TAG, "JSONException:" + e.getMessage());
                    iCurStatusMsg = "Збереження не виконано, помилки ";
                } catch (NullPointerException e) {
                    Log.e(TAG, "NullPointerException:" + e.getMessage());
                    iCurStatusMsg = "Збереження не виконано, помилки ";
                }
            }
        }
    }

    public Representation readStaticFile(String fileName) throws Resources.NotFoundException, IOException {
        InputStream is = MainActivity.context.getAssets().open(fileName);
        Representation representation = new InputRepresentation(is);
        return representation;
    }
}