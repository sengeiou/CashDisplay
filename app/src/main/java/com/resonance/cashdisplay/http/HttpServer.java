package com.resonance.cashdisplay.http;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Base64;

import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.body.AsyncHttpRequestBody;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;
import com.resonance.cashdisplay.BuildConfig;
import com.resonance.cashdisplay.ExtSDSource;
import com.resonance.cashdisplay.Log;
import com.resonance.cashdisplay.MainActivity;
import com.resonance.cashdisplay.PreferenceParams;
import com.resonance.cashdisplay.PreferencesValues;
import com.resonance.cashdisplay.load.DownloadMedia;
import com.resonance.cashdisplay.sound.Sound;
import com.resonance.cashdisplay.su.Modify_SU_Preferences;

import org.json.JSONException;
import org.json.JSONObject;

import static com.resonance.cashdisplay.http.WebStatus.CLEAR_QUEUE_WEB_MESSAGE;
import static com.resonance.cashdisplay.uart.UartWorker.UART_CHANGE_SETTINGS;

public class HttpServer {

    public static String HTTP_HALT_EVENT = "http_halt_event";
    private AsyncHttpServer mServer = null;
    private AsyncServer mAsyncServer = null;//new AsyncServer();
    private HttpConfig http_config = null;
    private WebStatus webStatus;
    private static final String TAG = "http_Server";
    private final int STAT_IDLE = 0;
    private final int STAT_LOAD_FILES = 1;
    private final int STAT_SAVE = 2;
    private final int STAT_LOAD_FIRMWARE = 3;

    private int iCurStatus = STAT_IDLE;
    private String iCurStatusMsg = "";

    protected boolean isStopped = false;
    private Context mContext;
    private int counttemp = 0;

    public HttpServer(Context context, WebStatus webstat) {
        mContext = context;
        webStatus = webstat;
        // createServerAsync();
        final Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    createServerAsync();
                } catch (final Exception e) {
                    e.printStackTrace();
                }
                super.run();
            }
        };
        t.run();
        new СreateProducerConsumer().start();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(HTTP_HALT_EVENT);
        mContext.registerReceiver(httpHaltEvent, intentFilter);
    }

    public BroadcastReceiver httpHaltEvent = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (counttemp > 0) return;
            counttemp++;
            if (intent.getAction().equals(HTTP_HALT_EVENT)) {
                Log.d(TAG, "** RESTART HTTP SERVER **");
                stopHttpServer();
                final Thread t = new Thread() {
                    @Override
                    public void run() {
                        try {
                            createServerAsync();
                        } catch (final Exception e) {
                            e.printStackTrace();
                        }
                        super.run();
                    }
                };
                t.run();
            }
        }
    };


    private void createServerAsync() {
        if (mServer != null) {
            return;
        }
        http_config = HttpConfig.get();

        mServer = new AsyncHttpServer();
        mServer.setContext(mContext);
        mAsyncServer = new AsyncServer();
        Log.w(TAG, "[!] Server created");

        mServer.get("/", LoginCallback);
        mServer.get("/getsettings", getSettingsCallback);
        mServer.get("/getstatus", getStatusCallback);
        mServer.post("/download_files", download_filesCallback);
        mServer.post("/start_remote_update", start_remote_updateCallback);
        mServer.post("/start_reboot", start_rebootCallback);
        mServer.post("/setsettings", setSettingsCallback);
        mServer.setErrorCallback(new CompletedCallback() {
            @Override
            public void onCompleted(Exception ex) {
                Log.w(TAG, "**CompletedCallback");
            }
        });
        mServer.listen(mAsyncServer, http_config.port);
    }

    private final HttpServerRequestCallback LoginCallback = new HttpServerRequestCallback() {
        @Override
        public void onRequest(final AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
            if (!shouldPass(request, response)) {

                return;
            }
            Log.d(TAG, "** LoginCallback:" + request.getPath());

            final String path = remapPath(request.getPath());
            Log.d(TAG, "** LoginCallback:" + request.getPath() + ", path:" + path);

            response.getHeaders().set("Content-Type", ContentTypes.getInstance().getContentType(path));
            response.send(HtmlHelper.loadPathAsString(path));
        }
    };

    private final HttpServerRequestCallback getSettingsCallback = new HttpServerRequestCallback() {
        @Override
        public void onRequest(final AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
            if (!shouldPass(request, response)) {
                return;
            }
            Log.d(TAG, "** /getsettings:" + request.getPath());
            iCurStatus = STAT_LOAD_FILES;
            iCurStatusMsg = "Пiдключено";

            try {
                JSONObject requestBody = new JSONObject();
                PreferenceParams prefParams = new PreferenceParams();
                PreferencesValues prefValues = prefParams.getParameters();

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
                response.send(requestBody.toString());
                System.gc();
            } catch (JSONException e) {
                Log.e(TAG, "JSONException:" + e.getMessage());
            }
        }
    };

    private final HttpServerRequestCallback setSettingsCallback = new HttpServerRequestCallback() {

        @Override
        public void onRequest(AsyncHttpServerRequest asyncHttpServerRequest, AsyncHttpServerResponse asyncHttpServerResponse) {

            AsyncHttpRequestBody requestBody = asyncHttpServerRequest.getBody();
            asyncHttpServerResponse.code(200);
            Log.d(TAG, "** setsettings:" + requestBody);
            iCurStatus = STAT_SAVE;
            iCurStatusMsg = "Збереження налаштуваннь ";

            PreferenceParams prefParams = new PreferenceParams();
            PreferencesValues prefValues = prefParams.getParameters();
            try {
                JSONObject jsonObject = new JSONObject(requestBody.toString());
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

                MainActivity.ethernetSettings.applyEthernetSettings();//контроль измененмя сетевых настроек

                //сигнал на изменение настройки UART
                Intent intent = new Intent(UART_CHANGE_SETTINGS);
                MainActivity.context.sendBroadcast(intent);
                intent = new Intent(MainActivity.CHANGE_SETTINGS);
                MainActivity.context.sendBroadcast(intent);
                iCurStatusMsg = "Збереження виконано ";
            } catch (JSONException e) {
                Log.e(TAG, "JSONException: " + e.getMessage());
            }
        }
    };

    private final HttpServerRequestCallback getStatusCallback = new HttpServerRequestCallback() {
        @Override
        public void onRequest(final AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
            if (!shouldPass(request, response)) {
                return;
            }
            Log.d(TAG, "** /getStatus:" + request.getPath());
            try {

                JSONObject requestBody = new JSONObject();

                requestBody.put("status", iCurStatus);
                requestBody.put("CurStatusMsg", iCurStatusMsg);
                requestBody.put("lab_current_ver", BuildConfig.VERSION_CODE);
                requestBody.put("sdcard_state", "<font color=\"blue\"><B>пам`ятi вiльно " + ExtSDSource.getAvailableMemory_SD() + "</B></font> ");

                Log.d(TAG, "send status : " + requestBody.toString());

                response.send(requestBody.toString());
                System.gc();

                if (iCurStatus == STAT_SAVE) {
                    iCurStatusMsg = "";
                    iCurStatus = STAT_IDLE;
                }
            } catch (JSONException e) {
                Log.e(TAG, "JSONException:" + e.getMessage());
            }
        }
    };

    private final HttpServerRequestCallback download_filesCallback = new HttpServerRequestCallback() {

        @Override
        public void onRequest(AsyncHttpServerRequest asyncHttpServerRequest, AsyncHttpServerResponse asyncHttpServerResponse) {

            AsyncHttpRequestBody requestBody = asyncHttpServerRequest.getBody();
            asyncHttpServerResponse.code(200);
            Log.d(TAG, "download_files...");
            iCurStatus = STAT_LOAD_FILES;
            MainActivity.downloadMedia.download();
        }
    };

    private final HttpServerRequestCallback start_remote_updateCallback = new HttpServerRequestCallback() {

        @Override
        public void onRequest(AsyncHttpServerRequest asyncHttpServerRequest, AsyncHttpServerResponse asyncHttpServerResponse) {

            AsyncHttpRequestBody requestBody = asyncHttpServerRequest.getBody();
            asyncHttpServerResponse.code(200);
            Log.d(TAG, "set_start_remote_update...");
            iCurStatus = STAT_LOAD_FIRMWARE;
            MainActivity.updateFirmware.update();
        }
    };

    private final HttpServerRequestCallback start_rebootCallback = new HttpServerRequestCallback() {

        @Override
        public void onRequest(AsyncHttpServerRequest asyncHttpServerRequest, AsyncHttpServerResponse asyncHttpServerResponse) {

            AsyncHttpRequestBody requestBody = asyncHttpServerRequest.getBody();
            asyncHttpServerResponse.code(200);
            iCurStatus = STAT_IDLE;
            DownloadMedia.resetMediaPlay();
            Log.d(TAG, "set_start_reboot...");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
            }
            Modify_SU_Preferences.executeCmd("reboot", 2000);
        }
    };


    public void stopHttpServer() {
        mServer.stop();
        mAsyncServer.stop();
        mServer = null;
        isStopped = true;
    }

    private boolean shouldPass(final AsyncHttpServerRequest req, final AsyncHttpServerResponse res) {
        if (isStopped) {
            Log.w(TAG, "isStopped");
            res.code(404);
            res.end();
            Log.w(TAG, "Пароль не подтвержден");
            return false;
        }
        if (!isAuthenticated(req)) {
            Log.w(TAG, "!isAuthenticated");
            res.getHeaders().add("WWW-Authenticate", "Basic realm=\"DeviceControl\"");
            res.code(401);
            res.end();
            return false;
        }
        return true;
    }

    private boolean isAuthenticated(final AsyncHttpServerRequest req) {
        final boolean isAuth = !http_config.useAuth;
        final String authHeader = req.getHeaders().get("Authorization");

        if (!isAuth && !TextUtils.isEmpty(authHeader)) {

            final String[] parts = new String(Base64.decode(authHeader.replace("Basic", "").trim(), Base64.DEFAULT)).split(":");

            // Log.w(TAG, "isAuth:"+isAuth+" authHeader:"+authHeader+" parts[0]:"+parts[0]+" parts[1]:"+parts[1]);

            return parts[0] != null
                    && parts[1] != null
                    && parts[0].equals(http_config.username)
                    && parts[1].equals(http_config.password);
        }
        return isAuth;
    }

    private String remapPath(final String path) {
        if (TextUtils.equals("/", path)) {
            return "index.html";
        }
        return path;
    }

    public synchronized void sendQueWebStatus(String strMsg, boolean clearQueue) {
        Message msg = new Message();
        msg.what = WebStatus.SEND_TO_QUEUE_WEB_MESSAGE;
        msg.obj = strMsg;
        msg.arg1 = (clearQueue ? CLEAR_QUEUE_WEB_MESSAGE : 0);
        msg.arg2 = 0;
        Handler h = webStatus.getWebMessageHandler();
        if (h != null)
            h.sendMessage(msg);
    }

    private class СreateProducerConsumer extends Thread {
        @Override
        public void run() {
            super.run();

            //Log.d(TAG, "createProducerConsumer  started");
            while (!isInterrupted()) {
                try {
                    if (!webStatus.isEmptySmbMessageQueue()) {
                        String msg = webStatus.getStrStatus();
                        iCurStatus = STAT_LOAD_FILES;
                        iCurStatusMsg = msg;
                        Log.w(TAG, "Smb_messageQueue.take:" + iCurStatusMsg);
                    } else {
                        Thread.sleep(500);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Log.w(TAG, "***createProducerConsumer  stoped****");
        }
    }
}
