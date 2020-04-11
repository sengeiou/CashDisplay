package com.resonance.cashdisplay;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.text.method.ScrollingMovementMethod;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import com.resonance.cashdisplay.databinding.ActivityMainBinding;
import com.resonance.cashdisplay.eth.EthernetSettings;
import com.resonance.cashdisplay.http.HttpServer;
import com.resonance.cashdisplay.load.DownloadMedia;
import com.resonance.cashdisplay.product_list.ProductListWorker;
import com.resonance.cashdisplay.settings.PrefValues;
import com.resonance.cashdisplay.settings.PrefWorker;
import com.resonance.cashdisplay.slide_show.VideoSlideService;
import com.resonance.cashdisplay.sound.Sound;
import com.resonance.cashdisplay.su.Modify_SU_Preferences;
import com.resonance.cashdisplay.uart.UartWorker;
import com.resonance.cashdisplay.utils.ImageUtils;

import java.io.File;

import static com.resonance.cashdisplay.eth.EthernetSettings.TIME_CHECK_DHCP_ENABLE;
import static com.resonance.cashdisplay.settings.PrefWorker.LOOK_BASKET;
import static com.resonance.cashdisplay.settings.PrefWorker.LOOK_DMART;
import static com.resonance.cashdisplay.settings.PrefWorker.LOOK_SUBWAY;

public class MainActivity extends Activity {

    private static final String TAG = "Main";
    public static final int CONTEXT_CONNECT = 0;        //слой подключения
    public static final int CONTEXT_THANKS = 1;         //слой спасибо
    public static final int CONTEXT_PRODUCT_LIST = 2;   //Слой список товаров

    public static String CHANGE_SETTINGS = "change_settings";
    public static boolean testMode = false;              // used to specify that app is in test mode

    public static final int MSG_ADD_TOVAR_PRODUCT_LIST = 34;
    public static final int MSG_SET_TOVAR_PRODUCT_LIST = 35;
    public static final int MSG_DEL_TOVAR_PRODUCT_LIST = 36;
    public static final int MSG_CLEAR_PRODUCT_LIST = 37;
    public static final int MSG_SET_SCREEN_PRODUCT_LIST = 38;
    public static final int MSG_SET_SCREEN_NOT_WORK = 39;
    public static final int MSG_SET_SCREEN_THANKS = 40;
    public static final int MSG_ADD_PRODUCT_DEBUG = 1234;

    private PrefValues prefValues;                          // настройки
    private static UartWorker uartWorker;                   // обработчик UART
    public static HttpServer httpServer = null;             // http сервер
    private CommandParser cmdParser;                        // класс обработки команд и данных
    private VideoSlideService videoSlideService;            // класс управления медиа
    private Sound sound;                                    // звук
    private ProductListWorker productListWorker;            // обслуживание списка товаров
    private ViewModel viewModel;
    public static EthernetSettings ethernetSettings = null; // Настройка сети
    public static DownloadMedia downloadMedia;
    public static UpdateFirmware updateFirmware = null;     // обновление ПО

    private static Modify_SU_Preferences su_preferences;

    public static Context context;
    public static RelativeLayout[] relativeLayout;
    public static Point sizeScreen;

    private static ImageView imageSdCardError;
    private boolean lanSetupAlready = false;

    public static View layoutProductListLook = null;    // represent layout_shopping_list_look_x.xml, where x - number of desired look
    // next block of views must be in every layout_shopping_list_look_x.xml to provide compability
    public static ViewGroup layoutTotal;
    public static TextView textViewTotalCount;
    public static TextView textViewTotalSumWithoutDiscount;    // calculated value
    public static TextView textViewTotalDiscount;           // calculated value
    public static TextView textViewTotalSum;             // value for this view received from COM port
    public static ListView listViewProducts;
    public static TextView textViewDebug;
    public static ScrollView scrollViewDebug;
    public static ImageView imageViewProduct;

    private TextView textViewVersion;
    private TextView textViewConnectStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
//                .detectAll()
//                .penaltyLog()
//                .penaltyDeath()
//                .build());
        super.onCreate(savedInstanceState);

        new Log();      // this object is needful to use synchronized methods with lock on it (not on static class)

        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        Log.d(TAG, "androidId: " + androidId);

        lanSetupAlready = false;
        Log.d(TAG, "                        ");
        Log.d(TAG, "***  START SYSTEM  *** " + BuildConfig.BUILD_TYPE + ", build: " + BuildConfig.VERSION_CODE);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//постоянно включен экран

        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_FULLSCREEN;
        getWindow().setAttributes(attributes);

        context = this;

        //Получим настройки экрана
        Display display = this.getWindowManager().getDefaultDisplay();
        sizeScreen = new Point();
        display.getSize(sizeScreen);
        sizeScreen.y += 48;
        Log.d(TAG, "Size screen x:" + sizeScreen.x + ", y:" + sizeScreen.y);

        //Получим настройки системы
        prefValues = PrefWorker.getValues();

        //Стартуем активити с биндингом полей
        viewModel = new ViewModel();
        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setViewmodel(viewModel);

        sound = new Sound(this);
        sound.setVolume(prefValues.percentVolume);

        su_preferences = new Modify_SU_Preferences(this);
        su_preferences.setSetupRootCallback(mCallbackRootIsSet);
        su_preferences.verifyRootRights();

        ethernetSettings = new EthernetSettings(this);
        ethernetSettings.setSetupLanCallback(mCallbackLanIsSet);

        downloadMedia = new DownloadMedia(this);

        //обработчик команд и данных
        cmdParser = new CommandParser(viewModel, messageHandler, MainActivity.this);

        //слой для вывода информации по товару
        relativeLayout = new RelativeLayout[]{(RelativeLayout) findViewById(R.id.layout_connect),
                (RelativeLayout) findViewById(R.id.layout_thanks),
                (RelativeLayout) findViewById(R.id.layout_product_list)
        };

        imageSdCardError = (ImageView) findViewById(R.id.imageview_sdcard_error);
        imageSdCardError.setVisibility(View.INVISIBLE);

        textViewVersion = (TextView) findViewById(R.id.textview_version);
        textViewConnectStatus = findViewById(R.id.textview_connect_status);  // widget of viewModel.setStatusConnection
        textViewVersion.setText("build :" + BuildConfig.VERSION_CODE);
        textViewConnectStatus.setHintTextColor(Color.parseColor("#333333"));
        viewModel.setStatusConnection("ініціалізація системи");
        viewModel.setStatusConnection2("");

        //UART
        uartWorker = new UartWorker(uartHandler);
        if (uartWorker.openSerialPort(UartWorker.getCoreNameUart(prefValues.uartName), 0, 0) == 0) {
            viewModel.setStatusConnection("інтерфейс RS232 ініціалізованo");
        } else {
            Log.e(TAG, "ERROR Open Port");
            viewModel.setStatusConnection("ПОМИЛКА інтерфейсу RS232");
        }

        updateFirmware = new UpdateFirmware(this);

        //экран "Список покупок"
        productListWorker = new ProductListWorker(context);
        setProductListLook();

        textViewDebug = (TextView) findViewById(R.id.textview_debug);
        textViewDebug.setMovementMethod(new ScrollingMovementMethod());
        scrollViewDebug = (ScrollView) findViewById(R.id.scrollview_debug);

        registerReceiver(changeSettings, new IntentFilter(CHANGE_SETTINGS));
        setBackgroundScreen();
        setVisibleContext(CONTEXT_CONNECT, null);

        new CheckSystemStart().run();

        acceptFullScreen();
    }

    @Override
    protected void onStart() {
        super.onStart();
        hideBarNavigation();
    }

    /**
     * Программное нажатие кнопки при переходе в полноэкранный режим
     */
    private void acceptFullScreen() {

        SharedPreferences sp = getSharedPreferences("LOADDATA", MODE_PRIVATE);
        if (!sp.getBoolean("InputTap", false)) {
            new Handler().postDelayed(() -> {
                //эмулируем нажатие кнопки для подтверждения при переходе в полноэкранный режим
                if (sizeScreen.x == 1920) //14"
                    Modify_SU_Preferences.executeCmd("input tap  1060 170", 1000);//3000//
                else //10"
                    Modify_SU_Preferences.executeCmd("input tap 746 157", 1000);//3000//
            }, 30000);
        }
        SharedPreferences.Editor ed = sp.edit();
        ed.putBoolean("InputTap", true);
        ed.commit();
    }

    /**
     * Приемник события изменения настроек
     */
    BroadcastReceiver changeSettings = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            runOnUiThread(() -> {
                setBackgroundScreen();

                if (testMode)
                    cmdParser = new CommandParser(viewModel, messageHandler, MainActivity.this);

                productListWorker = new ProductListWorker(MainActivity.context);
                setProductListLook();
            });
        }
    };

    /*********************************************************************************************/

    /**
     * Установка фона экранов
     */
    private void setBackgroundScreen() {
        //установка фона экрана "Список покупок"
        Bitmap bitmap;
        Drawable drawable;
        String uriBackgroundShoppingList = ExtSDSource.getExternalSdCardPath(context) + downloadMedia.IMG_SCREEN + ((PrefWorker.getValues().backgroundShoppingList.length() > 0) ? PrefWorker.getValues().backgroundShoppingList : "noimg");
        File fileImg = new File(uriBackgroundShoppingList);
        if (fileImg.exists()) {
            bitmap = ImageUtils.getImage(fileImg, MainActivity.sizeScreen, false);
            drawable = new BitmapDrawable(bitmap);
            relativeLayout[CONTEXT_PRODUCT_LIST].setBackground(drawable);
        } else {
            relativeLayout[CONTEXT_PRODUCT_LIST].setBackgroundResource(R.drawable.bg);
        }
        relativeLayout[CONTEXT_PRODUCT_LIST].invalidate();

        //Фонове зображення экрану "Каса не працює"
        String uriBackgroundCashNotWork = ExtSDSource.getExternalSdCardPath(context) + downloadMedia.IMG_SCREEN + ((PrefWorker.getValues().backgroundCashNotWork.length() > 0) ? PrefWorker.getValues().backgroundCashNotWork : "noimg");
        fileImg = new File(uriBackgroundCashNotWork);
        if (fileImg.exists()) {
            bitmap = ImageUtils.getImage(fileImg, MainActivity.sizeScreen, false);
            drawable = new BitmapDrawable(bitmap);
            relativeLayout[CONTEXT_CONNECT].setBackground(drawable);
        } else {
            relativeLayout[CONTEXT_CONNECT].setBackgroundResource(R.drawable.screen_cache_not_work);
        }
        if (testMode) {
            relativeLayout[CONTEXT_CONNECT].setBackground(null);
            findViewById(R.id.textview_cashbox_not_work).setVisibility(View.VISIBLE);
            findViewById(R.id.textview_emul2line_indicator_1).setVisibility(View.VISIBLE);
            findViewById(R.id.textview_emul2line_indicator_2).setVisibility(View.VISIBLE);
            scrollViewDebug.setVisibility(View.VISIBLE);
            new Handler().post(() -> scrollViewDebug.fullScroll(View.FOCUS_DOWN));
        }
        relativeLayout[CONTEXT_CONNECT].invalidate();

        //Фонове зображення экрану "Дякуємо за покупку"
        String uriBackgroundThanks = ExtSDSource.getExternalSdCardPath(context) + downloadMedia.IMG_SCREEN + ((PrefWorker.getValues().backgroundThanks.length() > 0) ? PrefWorker.getValues().backgroundThanks : "noimg");
        fileImg = new File(uriBackgroundThanks);
        if (fileImg.exists()) {
            bitmap = ImageUtils.getImage(fileImg, MainActivity.sizeScreen, false);
            drawable = new BitmapDrawable(bitmap);
            relativeLayout[CONTEXT_THANKS].setBackground(drawable);
        } else {
            relativeLayout[CONTEXT_THANKS].setBackgroundResource(R.drawable.screen_thanks);
        }
        relativeLayout[CONTEXT_THANKS].invalidate();
    }

    /************************************************************************************/

    /**
     * Method inflates specified layout view in activity_main.xml and get references for actual views.
     * This changes the look of product list for different client's flavours.
     */
    private void setProductListLook() {
        int lookCode = PrefWorker.getValues().productListLookCode;

        int resource;
        switch (lookCode) {
            case LOOK_BASKET:
                resource = R.layout.layout_product_list_look_0;
                break;
            case LOOK_DMART:
                resource = R.layout.layout_product_list_look_1;
                break;
            case LOOK_SUBWAY:
                resource = R.layout.layout_product_list_look_2;
                break;
            default:
                resource = R.layout.layout_product_list_look_0;
                break;
        }
        if (layoutProductListLook != null)
            relativeLayout[CONTEXT_PRODUCT_LIST].removeView(layoutProductListLook);
        LayoutInflater li = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layoutProductListLook = li.inflate(resource, null);
        relativeLayout[CONTEXT_PRODUCT_LIST].addView(layoutProductListLook, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        relativeLayout[CONTEXT_PRODUCT_LIST].invalidate();

        productListWorker.initUniqueComponents();

        listViewProducts = (ListView) layoutProductListLook.findViewById(R.id.listview_products);
        layoutTotal = layoutProductListLook.findViewById(R.id.layout_total);
        textViewTotalCount = (TextView) layoutProductListLook.findViewById(R.id.textview_total_count);
        textViewTotalSumWithoutDiscount = (TextView) layoutProductListLook.findViewById(R.id.textview_total_sum_without_discount);
        textViewTotalDiscount = (TextView) layoutProductListLook.findViewById(R.id.textview_total_discount);
        textViewTotalSum = (TextView) layoutProductListLook.findViewById(R.id.textview_total_sum);
        imageViewProduct = (ImageView) layoutProductListLook.findViewById(R.id.imageview_product);

        listViewProducts.setDividerHeight(0);   // we create our own dividers, because of bug with last divider visibility
        // appropriate adapter must be created everytime for actual listview for specified look of product list
        listViewProducts.setAdapter(productListWorker.createAdapterProductList(lookCode));
        // item click listener we use ot highlight selected item
        listViewProducts.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                new Handler().post(() -> {        // with handler may be small delay before highlighting
                int totalItemCount = parent.getCount();
                if (totalItemCount == 0)
                    return;

                int positionViewPort = position - parent.getFirstVisiblePosition();

                try {
                    View listItem = parent.getChildAt(positionViewPort);
                    AnimationDrawable animDrawable = (AnimationDrawable) listItem.getBackground();
                    // Enter Fade duration is part of duration in xml (starts when xml frame starts, but no longer then xml duration)
                    // Exit Fade is added to duration in xml (xml plays, then this fade starts with next xml item - intersection).
//                    animDrawable.setEnterFadeDuration(0);    // duration of item in xml has priority (if in xml 0, fade in = 0; if in xml 100, fade in = 100)
                    animDrawable.setExitFadeDuration(1000);  // this duration plays always (if in xml 400, then 400 + fade out)
                    animDrawable.start();
                } catch (NullPointerException e) {      // if first time clicked item was not available yet
                    new Handler().post(() -> {          // repeat click action with delay
                        onItemClick(parent, view, position, id);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
//                });
            }
        });

        textViewTotalCount.setText("0");
        textViewTotalSumWithoutDiscount.setText("0.00");
        textViewTotalDiscount.setText("0.00");
        textViewTotalSum.setText("0.00");
        imageViewProduct.setImageBitmap(null);
    }

    /************************************************************************************/

    public void setVisibleContext(int contextType, @Nullable String args) {
        switch (contextType) {
            case CONTEXT_PRODUCT_LIST:
                setVisibleLayer(CONTEXT_PRODUCT_LIST, View.VISIBLE);
                setVisibleLayer(CONTEXT_CONNECT, View.INVISIBLE);
                setVisibleLayer(CONTEXT_THANKS, View.INVISIBLE);
                productListWorker.onProductListShow(args);
                break;
            case CONTEXT_CONNECT:
                setVisibleLayer(CONTEXT_PRODUCT_LIST, View.INVISIBLE);
                setVisibleLayer(CONTEXT_CONNECT, View.VISIBLE);
                setVisibleLayer(CONTEXT_THANKS, View.INVISIBLE);
                productListWorker.onProductListHide();
                break;
            case CONTEXT_THANKS:
                setVisibleLayer(CONTEXT_PRODUCT_LIST, View.INVISIBLE);
                setVisibleLayer(CONTEXT_CONNECT, View.INVISIBLE);
                setVisibleLayer(CONTEXT_THANKS, View.VISIBLE);
                productListWorker.onProductListHide();
                break;
            default:
                break;
        }
    }

    private void setVisibleLayer(int contextLayer, int visibility) {
        relativeLayout[contextLayer].setVisibility(visibility);
        relativeLayout[contextLayer].invalidate();
    }

    /************************************************************************************/

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(changeSettings);
    }

    /***************************************
     * UART
     ***************************************/
    private final Handler uartHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UartWorker.ACTION_UART_READ:
                    cmdParser.parseInputStr((byte[]) msg.obj, msg.arg2);
                    break;
                case UartWorker.ACTION_UART_OPEN:
                    break;
                case UartWorker.ACTION_UART_CLOSED: //  closed
                    setVisibleContext(CONTEXT_CONNECT, null);
                    break;
                case UartWorker.ACTION_UART_ERROR: //  errors
                    Log.d(TAG, "ACTION_UART_ERROR");
                    setVisibleContext(CONTEXT_CONNECT, null);
                    break;
            }
        }
    };

    /**********************
     * SERVICE handler
     ***************************/
    private final Handler messageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            setEnableMedia(false);
            switch (msg.what) {
                case MSG_ADD_TOVAR_PRODUCT_LIST:
                    productListWorker.addProductToList((String) msg.obj);
                    setVisibleContext(CONTEXT_PRODUCT_LIST, null);
                    break;
                case MSG_SET_TOVAR_PRODUCT_LIST:
                    productListWorker.setProductToList((String) msg.obj);
                    setVisibleContext(CONTEXT_PRODUCT_LIST, null);
                    break;
                case MSG_DEL_TOVAR_PRODUCT_LIST:
                    productListWorker.deleteProductFromList((String) msg.obj);
                    setVisibleContext(CONTEXT_PRODUCT_LIST, null);
                    break;
                case MSG_CLEAR_PRODUCT_LIST:
                    productListWorker.clearProductList();
                    break;
                case MSG_SET_SCREEN_PRODUCT_LIST:
                    setVisibleContext(CONTEXT_PRODUCT_LIST, (String) msg.obj);
                    break;
                case MSG_SET_SCREEN_NOT_WORK:
                    productListWorker.clearProductList();
                    setVisibleContext(CONTEXT_CONNECT, null);
                    break;
                case MSG_SET_SCREEN_THANKS:
                    productListWorker.clearProductList();
                    setVisibleContext(CONTEXT_THANKS, null);
                    setEnableMedia(true);
                    resetMediaTime();
                    break;
                case MSG_ADD_PRODUCT_DEBUG:
                    productListWorker.addProductDebug((String) msg.obj);
                    break;
                default:
                    Log.e(TAG, "Handler default message:" + String.valueOf(msg.what));
                    break;
            }
        }
    };

    /**
     * Hide navigation bar
     */
    private void hideBarNavigation() {
        runOnUiThread(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                View decorView = getWindow().getDecorView();
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                                | View.SCREEN_STATE_ON
                                | View.SYSTEM_UI_FLAG_LOW_PROFILE
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                | View.SYSTEM_UI_FLAG_IMMERSIVE);
                decorView.invalidate();
            }
        });
    }

    /**
     * Разрешить/запретить демонстрацию медиа
     *
     * @param state
     */
    private void setEnableMedia(boolean state) {
        Intent intent = new Intent(VideoSlideService.VIDEO_SLIDE_ENABLE);
        Bundle mBundle = new Bundle();
        mBundle.putBoolean("enable_video_slide", state);
        intent.putExtras(mBundle);
        sendBroadcast(intent);
    }

    /**
     * Сброс времени, остановка демонстации медиа
     */
    private void resetMediaTime() {
        Intent intent = new Intent(VideoSlideService.VIDEO_SLIDE_RESET_TIME);
        sendBroadcast(intent);
    }

    /*********************************************************************************************/

    /**
     * Поток отслеживает окончание загрузки системы,
     * чтобы демонстрация медиа не стартовала до показа основного экрана
     */
    private class CheckSystemStart extends Thread {
        @Override
        public void run() {
            while (!isInterrupted()) {
                if (Modify_SU_Preferences.checkSystemBootCompleted()) {
                    Log.d(TAG, "property set: SYSTEM BOOT COMPLETED");
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(() -> videoSlideService = new VideoSlideService(MainActivity.this));
                    break;
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Поток мониторит наличие подключения LAN
     */
    private class CheckConnectionEth extends Thread {
        @Override
        public void run() {
            //немного музыки в момент запуска
            if (sound != null) {
                sound.setVolume(80);
                sound.playSound(Sound.START_VOICE);
                sound.setVolume(prefValues.percentVolume);
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String ip = "";
            boolean loadMediaAtStartSystem = false;

            viewModel.setStatusConnection(ethernetSettings.getCurrentStatus());

            while (!isInterrupted()) {
                try {
                    PrefValues prefValues = PrefWorker.getValues();
                    // not the best place, but good for SD card absence detection
                    runOnUiThread(() -> {
                        if (!ExtSDSource.isMounted(context)) {
                            imageSdCardError.setVisibility(View.VISIBLE);
                            sound.setVolume(80);
                            sound.playSound(Sound.WARNING_VOICE);
                            viewModel.setStatusConnection("*** Вiдсутнiй SD носiй ***");
                            viewModel.setStatusConnection2("*** Вiдсутнiй SD носiй ***");
                            uartWorker.closeSerialPort();
                            Log.w(TAG, "SD-card is absent");
                        } else
                            imageSdCardError.setVisibility(View.INVISIBLE);
                    });

                    String stat = ethernetSettings.getCurrentStatus();
                    if (EthernetSettings.isConnected()) {
                        if (!loadMediaAtStartSystem && prefValues.downloadAtStart) {
                            loadMediaAtStartSystem = true;
                            downloadMedia.download();
                        }
                        String networkInterfaceIpAddress = EthernetSettings.getNetworkInterfaceIpAddress();
                        viewModel.setStatusConnection(((stat.length() == 0) ? "IP : " + networkInterfaceIpAddress : stat));
                        viewModel.setStatusConnection2(((stat.length() == 0) ? "IP : " + networkInterfaceIpAddress : stat));

                        if (!ip.equals(networkInterfaceIpAddress)) {
                            ip = networkInterfaceIpAddress;
                            Log.d(TAG, "Подключение LAN : " + ip);
                            if (!prefValues.dhcp) {     // return static IP, enabled DHCP server may overwrite static settings (e.g. after cable reconnection)
                                ethernetSettings.applyEthernetSettings();
                            }
                        }
                    } else {
                        runOnUiThread(() -> {
                            if (!prefValues.dhcp) {
                                viewModel.setStatusConnection("");
                                textViewConnectStatus.setHint(getString(R.string.not_connected) +" (" + prefValues.ip + ")");
                            } else {
                                textViewConnectStatus.setHint("");
                                viewModel.setStatusConnection(((stat.length() == 0) ? "підключення LAN відсутнє" : stat));
                            }
                        });
                        Log.d(TAG, "Подключение LAN отсутствует");
                        ip = "";
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //событие об установке прав SU
    private Modify_SU_Preferences.SetupRootCallback mCallbackRootIsSet = new Modify_SU_Preferences.SetupRootCallback() {
        @Override
        public void onSetupRoot(int result) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "mCallbackRootIsSet :" + result);

                    imageSdCardError.setVisibility(View.INVISIBLE);
                    if (!ExtSDSource.isMounted(context)) {
                        imageSdCardError.setVisibility(View.VISIBLE);
                        sound.setVolume(80);
                        sound.playSound(Sound.WARNING_VOICE);
                        viewModel.setStatusConnection("*** Вiдсутнiй SD носiй ***");
                        viewModel.setStatusConnection2("*** Вiдсутнiй SD носiй ***");
                        uartWorker.closeSerialPort();
                        Log.w(TAG, "SD-card is absent");
                    }

                    if (result == 1) {
                        if (BuildConfig.BUILD_TYPE.equals("release")) {
                            if (sizeScreen.x != 1920) //только для 10"
                                Modify_SU_Preferences.setSystemUIEnabled(false); //спрячем строку навигации
                        } else
                            Modify_SU_Preferences.setSystemUIEnabled(true); //покажем строку навигации

                        Log.w(TAG, "административные права получены");
                        viewModel.setStatusConnection("ініціалізація системи ");
                        ethernetSettings.applyEthernetSettings(); //применение параметров
                    } else {
                        Log.w(TAG, "ОШИБКА, нет административных прав:" + result);
                        setVisibleContext(CONTEXT_CONNECT, null);
                        viewModel.setStatusConnection("ПОМИЛКА, не були надані адміністративні права, " + "IP : " + EthernetSettings.getNetworkInterfaceIpAddress());
                        imageSdCardError.setImageResource(R.drawable.warning);
                        imageSdCardError.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
    };

    //событие об окончании настройки сети
    private EthernetSettings.SetupLanCallback mCallbackLanIsSet = new EthernetSettings.SetupLanCallback() {
        @Override
        public void onSetupLAN(int result) {
            if (!lanSetupAlready) { //запуск только 1 раз
                lanSetupAlready = true;
                Log.d(TAG, "mCallbackSetupLAN :" + result);
                new CheckConnectionEth().start(); //проверка и установка сети
                httpServer = new HttpServer(context);

                // next block starts set static addresses if DHCP broken (device connected to static LAN)
                if (PrefWorker.getValues().dhcp)
                    new Thread(() -> {
                        try {
                            Thread.sleep(TIME_CHECK_DHCP_ENABLE);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (EthernetSettings.getNetworkInterfaceIpAddress() == null)
                            ethernetSettings.setTempStatic();
                    }).start();
            }
        }
    };
}
