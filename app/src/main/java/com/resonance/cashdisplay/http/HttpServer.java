package com.resonance.cashdisplay.http;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;
import android.util.Base64;

import com.crashlytics.android.Crashlytics;
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
import com.resonance.cashdisplay.load.UploadMedia;
import com.resonance.cashdisplay.sound.Sound;
import com.resonance.cashdisplay.su.Modify_SU_Preferences;

import org.json.JSONException;
import org.json.JSONObject;

import static com.resonance.cashdisplay.uart.UartWorker.UART_CHANGE_SETTINGS;

public class HttpServer {

    public static String HTTP_HALT_EVENT = "http_halt_event";
    private AsyncHttpServer mServer = null;
    private AsyncServer mAsyncServer = null;//new AsyncServer();
    private HttpConfig httpСonfig = null;
    private static final String TAG = "HttpServer";
    private final int STAT_IDLE = 0;
    private final int STAT_LOAD_FILES = 1;
    private final int STAT_SAVE = 2;
    private final int STAT_LOAD_FIRMWARE = 3;

    private int iCurStatus = STAT_IDLE;
    private String iCurStatusMsg = "";

    protected boolean isStopped = false;
    private Context mContext;
    private int counttemp = 0;

    public HttpServer(Context context) {
        mContext = context;

        createServerAsync();
        new Thread(() -> {
            try {
                createServerAsync();
            } catch (Exception e) {
                e.printStackTrace();
                Crashlytics.logException(e);
            }
        }).start();

        IntentFilter intentFilter = new IntentFilter(HTTP_HALT_EVENT);
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
                new Thread(() -> {
                    try {
                        createServerAsync();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Crashlytics.logException(e);
                    }
                }).start();
            }
        }
    };

    private void createServerAsync() {
        if (mServer != null) {
            return;
        }
        httpСonfig = HttpConfig.get();

        mServer = new AsyncHttpServer();
        mServer.setContext(mContext);
        mAsyncServer = new AsyncServer();
        Log.w(TAG, "[!] Server created");

        mServer.get("/", loginCallback);
        mServer.get("/getsettings", getSettingsCallback);
        mServer.get("/getstatus", getStatusCallback);
        mServer.post("/upload_files", uploadFilesCallback);
        mServer.post("/start_remote_update", startRemoteUpdateCallback);
        mServer.post("/start_reboot", startRebootCallback);
        mServer.post("/setsettings", setSettingsCallback);
        mServer.setErrorCallback(new CompletedCallback() {
            @Override
            public void onCompleted(Exception ex) {
                Log.w(TAG, "**ErrorCallback.onCompleted");
                Crashlytics.logException(ex);
            }
        });
        mServer.listen(mAsyncServer, httpСonfig.port);
    }

    private final HttpServerRequestCallback loginCallback = new HttpServerRequestCallback() {
        @Override
        public void onRequest(final AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
            if (!shouldPass(request, response)) {
                return;
            }
            String requestPath = request.getPath();
            String responsePath = (TextUtils.equals("/", requestPath)) ? "index.html" : requestPath;
            Log.d(TAG, "** loginCallback: \"" + requestPath + "\"" + ", path: \"" + responsePath + "\"");
            response.getHeaders().set("Content-Type", ContentTypes.getInstance().getContentType(responsePath));
            response.send(HtmlHelper.loadFileAsString(responsePath));   // sends all *.html file as string
        }
    };

    private final HttpServerRequestCallback getSettingsCallback = new HttpServerRequestCallback() {
        @Override
        public void onRequest(final AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
            if (!shouldPass(request, response)) {
                return;
            }
            Log.d(TAG, "** /getsettings: \"" + request.getPath() + "\"");
            iCurStatus = STAT_LOAD_FILES;
            iCurStatusMsg = "Пiдключено";

            try {
                JSONObject responseBody = new JSONObject();
                PreferenceParams prefParams = new PreferenceParams();
                PreferencesValues prefValues = prefParams.getParameters();

                responseBody.put("uart_select", prefValues.uartName);
                responseBody.put("host_img", prefValues.smbImg);
                responseBody.put("host_video", prefValues.smbVideo);
                responseBody.put("host_slide", prefValues.smbSlide);
                responseBody.put("host_screen_img", prefValues.pathToScreenImg);
                responseBody.put("host", prefValues.smbHost);
                responseBody.put("ftp_user", prefValues.user);
                responseBody.put("ftp_pass", prefValues.passw);
                responseBody.put("timeout_video", prefValues.videoTimeout);
                responseBody.put("enable_video", prefValues.checkEnableVideo);
                responseBody.put("admin_user", prefValues.admin);
                responseBody.put("admin_pass", prefValues.adminPassw);
                responseBody.put("download_at_start", prefValues.downloadAtStart);
                responseBody.put("volume", prefValues.percentVolume);
                responseBody.put("stat_adress", prefValues.ip);
                responseBody.put("stat_mask", prefValues.mask);
                responseBody.put("stat_gate", prefValues.gateWay);
                responseBody.put("stat_dns", prefValues.dns);

                responseBody.put("dhcp", prefValues.dhcp);
                responseBody.put("lab_current_ver", BuildConfig.VERSION_CODE);
                responseBody.put("protocol", prefValues.transferProtocol);
                responseBody.put("def_background_img", prefValues.defaultBackgroundImage);

                responseBody.put("time_slide_image", prefValues.timeSlideImage);
                responseBody.put("option_video_slide", (prefValues.videoOrSlide == PreferenceParams.VIDEO ? true : false));

                responseBody.put("image_screen_shoppinglist", prefValues.backgroundShoppingList);
                responseBody.put("image_screen_cash_not_work", prefValues.backgroundCashNotWork);
                responseBody.put("image_screen_thanks", prefValues.backgroundThanks);

                Log.d(TAG, "Server responses settings (JSON string): " + responseBody.toString());
                response.send(responseBody.toString());
                System.gc();
            } catch (JSONException e) {
                Log.e(TAG, "JSONException:" + e.getMessage());
                Crashlytics.logException(e);
            }
        }
    };

    private final HttpServerRequestCallback getStatusCallback = new HttpServerRequestCallback() {
        @Override
        public void onRequest(final AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
            if (!shouldPass(request, response)) {
                return;
            }
            Log.d(TAG, "** /getStatus:" + request.getPath() + "\"");
            try {
                JSONObject responseBody = new JSONObject();

                responseBody.put("status", iCurStatus);
                responseBody.put("CurStatusMsg", iCurStatusMsg);
                responseBody.put("lab_current_ver", BuildConfig.VERSION_CODE);
                responseBody.put("sdcard_state", "<font color=\"blue\"><B>пам`ятi вiльно " + ExtSDSource.getAvailableMemory_SD() + "</B></font> ");

                Log.d(TAG, "Server responses status (JSON string): " + responseBody.toString());

                response.send(responseBody.toString());
                System.gc();

                if (iCurStatus == STAT_SAVE) {
                    iCurStatusMsg = "";
                    iCurStatus = STAT_IDLE;
                }
            } catch (JSONException e) {
                Log.e(TAG, "JSONException:" + e.getMessage());
                Crashlytics.logException(e);
            }
        }
    };

    private final HttpServerRequestCallback uploadFilesCallback = new HttpServerRequestCallback() {

        @Override
        public void onRequest(AsyncHttpServerRequest asyncHttpServerRequest, AsyncHttpServerResponse asyncHttpServerResponse) {

            AsyncHttpRequestBody requestBody = asyncHttpServerRequest.getBody();
            asyncHttpServerResponse.code(200);
            Log.d(TAG, "** /upload_files");
            iCurStatus = STAT_LOAD_FILES;
            MainActivity.uploadMedia.upload();
        }
    };

    private final HttpServerRequestCallback startRemoteUpdateCallback = new HttpServerRequestCallback() {

        @Override
        public void onRequest(AsyncHttpServerRequest asyncHttpServerRequest, AsyncHttpServerResponse asyncHttpServerResponse) {

            AsyncHttpRequestBody requestBody = asyncHttpServerRequest.getBody();
            asyncHttpServerResponse.code(200);
            Log.d(TAG, "** /start_remote_update");
            iCurStatus = STAT_LOAD_FIRMWARE;
            MainActivity.updateFirmware.update();
        }
    };

    private final HttpServerRequestCallback startRebootCallback = new HttpServerRequestCallback() {

        @Override
        public void onRequest(AsyncHttpServerRequest asyncHttpServerRequest, AsyncHttpServerResponse asyncHttpServerResponse) {

            AsyncHttpRequestBody requestBody = asyncHttpServerRequest.getBody();
            asyncHttpServerResponse.code(200);
            iCurStatus = STAT_IDLE;
            UploadMedia.resetMediaPlay();
            Log.d(TAG, "** /start_reboot");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
            }
            Modify_SU_Preferences.executeCmd("reboot", 2000);
        }
    };

    private final HttpServerRequestCallback setSettingsCallback = new HttpServerRequestCallback() {

        @Override
        public void onRequest(AsyncHttpServerRequest asyncHttpServerRequest, AsyncHttpServerResponse asyncHttpServerResponse) {

            AsyncHttpRequestBody requestBody = asyncHttpServerRequest.getBody();
            asyncHttpServerResponse.code(200);
            Log.d(TAG, "** /setsettings" + requestBody);
            iCurStatus = STAT_SAVE;
            iCurStatusMsg = "Збереження налаштуваннь ";

            PreferencesValues prefValues = PreferenceParams.getParameters();
            try {
                JSONObject jsonObject = new JSONObject(requestBody.toString());
                Log.d(TAG, "Save settings received from client: " + jsonObject.toString());

                prefValues.uartName = jsonObject.get("uart_select").toString();
                prefValues.smbImg = jsonObject.get("host_img").toString();
                prefValues.smbVideo = jsonObject.get("host_video").toString();
                prefValues.smbSlide = jsonObject.get("host_slide").toString();

                prefValues.smbHost = jsonObject.get("host").toString();
                prefValues.user = jsonObject.get("ftp_user").toString();
                prefValues.passw = jsonObject.get("ftp_pass").toString();
                prefValues.checkEnableVideo = (boolean) jsonObject.get("enable_video");
                String timeout_Str = (String) jsonObject.get("timeout_video");
                prefValues.videoTimeout = Integer.parseInt(timeout_Str.length() > 0 ? timeout_Str : "20");//поставим по умолчанию 5 сек
                if (prefValues.videoTimeout < 5)
                    prefValues.videoTimeout = 5;

                prefValues.downloadAtStart = (boolean) jsonObject.get("download_at_start");
                int prevVolume = prefValues.percentVolume;
                String volume_Str = (String) jsonObject.get("volume");
                prefValues.percentVolume = Integer.parseInt(volume_Str.length() > 0 ? volume_Str : "50");//поставим по умолчанию 50
                if (prefValues.percentVolume != prevVolume) {
                    Sound.setVolume(prefValues.percentVolume);
                }

                prefValues.ip = (String) jsonObject.get("stat_adress");
                prefValues.mask = (String) jsonObject.get("stat_mask");
                prefValues.gateWay = (String) jsonObject.get("stat_gate");
                prefValues.dns = (String) jsonObject.get("stat_dns");
                boolean prevDHCP = prefValues.dhcp;             // save to compare below with new value
                prefValues.dhcp = (boolean) jsonObject.get("dhcp");

                prefValues.transferProtocol = jsonObject.get("protocol").toString();
                prefValues.defaultBackgroundImage = jsonObject.get("def_background_img").toString();

                String tmpTimeSlideImg = jsonObject.get("time_slide_image").toString();
                prefValues.timeSlideImage = Integer.parseInt(tmpTimeSlideImg.length() > 0 ? tmpTimeSlideImg : "10");
                String tmpvideo_slide = jsonObject.get("option_video_slide").toString();
                prefValues.videoOrSlide = (tmpvideo_slide.equals("true") ? PreferenceParams.VIDEO : PreferenceParams.SLIDE);

                prefValues.backgroundShoppingList = (String) jsonObject.get("image_screen_shoppinglist");
                prefValues.backgroundCashNotWork = (String) jsonObject.get("image_screen_cash_not_work");
                prefValues.backgroundThanks = (String) jsonObject.get("image_screen_thanks");

                prefValues.pathToScreenImg = (String) jsonObject.get("host_screen_img");

                PreferenceParams.setParameters(prefValues);

                if (!(prevDHCP && prefValues.dhcp))   // if was DHCP and become DHCP - don't apply network settings
                    MainActivity.ethernetSettings.applyEthernetSettings();  //контроль измененмя сетевых настроек

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

    public void stopHttpServer() {
        mServer.stop();
        mAsyncServer.stop();
        mServer = null;
        isStopped = true;
    }

    private boolean shouldPass(AsyncHttpServerRequest req, AsyncHttpServerResponse res) {
        if (isStopped) {
            Log.w(TAG, "Сервер остановлен!!! (method shouldPass, condition isStopped).");
            res.code(503);
            res.end();
            return false;
        }
        if (!isAuthenticated(req)) {
            Log.w(TAG, "Не пройдена аутентификация! (method shouldPass, condition isAuthenticated).");
            res.getHeaders().add("WWW-Authenticate", "Basic realm=\"DeviceControl\"");
            res.code(401);
            res.end();
            return false;
        }
        return true;
    }

    private boolean isAuthenticated(AsyncHttpServerRequest req) {
        boolean isAuth = !httpСonfig.useAuth;
        String authHeader = req.getHeaders().get("Authorization");

        if (!isAuth && !TextUtils.isEmpty(authHeader)) {

            String[] authData = new String(Base64.decode(authHeader.replace("Basic", "").trim(), Base64.DEFAULT))
                    .split(":");

            switch (authData.length) {
                case 1:                                                     // only login was typed
                    if (httpСonfig.userNameTestMode.equals(authData[0])) {  // tester name
                        if (req.getPath().equals("/")) {       // only if request is from loginCallback)
                            MainActivity.testMode = true;
                            Intent intent = new Intent(MainActivity.CHANGE_SETTINGS);
                            MainActivity.context.sendBroadcast(intent);
                        }
                        return true;
                    }
                    return httpСonfig.userName.equals(authData[0])
                            && httpСonfig.password.equals("");
                case 2:                                             // login and password were typed
                    return httpСonfig.userName.equals(authData[0])
                            && httpСonfig.password.equals(authData[1]);
                default:                    // nor login nor password were typed (or typed mess data)
                    return false;
            }
        }
        return isAuth;
    }

    public synchronized void sendQueWebStatus(String strMsg, boolean clearQueue) {
        iCurStatus = STAT_LOAD_FILES;
        iCurStatusMsg = strMsg;
        Log.w(TAG, "Smb_messageQueue.take: " + iCurStatusMsg + "\"");
    }
}
