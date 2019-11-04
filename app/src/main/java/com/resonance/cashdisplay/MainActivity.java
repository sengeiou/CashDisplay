//https://console.firebase.google.com/
//Идентификатор проекта: cashdisplay-resonance
package com.resonance.cashdisplay;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.resonance.cashdisplay.databinding.ActivityMainBinding;
import com.resonance.cashdisplay.eth.EthernetSettings;
import com.resonance.cashdisplay.http.HttpServer;
import com.resonance.cashdisplay.http.WebStatus;
import com.resonance.cashdisplay.load.DownloadMedia;
import com.resonance.cashdisplay.shopping_list.ShoppingListWorker;
import com.resonance.cashdisplay.slide_show.VideoSlideService;
import com.resonance.cashdisplay.sound.Sound;
import com.resonance.cashdisplay.su.Modify_SU_Preferences;
import com.resonance.cashdisplay.uart.UartWorker;
import com.resonance.cashdisplay.utils.ImageUtils;

import java.io.File;

//import android.util.Log;

public class MainActivity extends Activity {

    private static final String TAG = "Main";
    public static final int CONTEXT_CONNECT = 0;        //слой подключения
    public static final int CONTEXT_THANKS = 1;         //слой спасибо
    public static final int CONTEXT_SHOPPING_LIST = 2;  //Слой список товаров

    public static String CHANGE_SETTINGS = "change_settings";

    public static final int MSG_ADD_TOVAR_SHOPPING_LIST = 34;
    public static final int MSG_SET_TOVAR_SHOPPING_LIST = 35;
    public static final int MSG_DEL_TOVAR_SHOPPING_LIST = 36;
    public static final int MSG_CLEAR_SHOPPING_LIST = 37;
    public static final int MSG_TOTAL_SUMM_SHOPPING_LIST = 38;
    public static final int MSG_SET_SCREEN_NOT_WORK = 39;
    public static final int MSG_SET_SCREEN_THANKS = 40;
    public static final int MSG_FROM_EKKR = 41;
    public static final int MSG_ADD_PRODUCT_DEBUG = 1234;

    public static PreferencesValues preferenceParams;       //настройки
    private static UartWorker uartWorker;                   //обработчик UART
    public static WebStatus webStatus = null;               //канал передачи сообщений для браузера
    public static HttpServer httpServer = null;             //http сервер
    private CommandParser cmdParser;                        //класс обработки команд и данных
    private VideoSlideService videoSlideService;            //класс управления медиа
    private Sound sound;                                    //звук
    private static ShoppingListWorker shoppingListWorker;   //обслуживание списка товаров
    private ProductInfo productInfo;
    public static EthernetSettings ethernetSettings = null; //Настройка сети
    public static DownloadMedia downloadMedia;
    public static UpdateFirmware updateFirmware = null;     //обновление ПО

    private static Modify_SU_Preferences su_preferences;

    public static Context context;
    private static RelativeLayout[] relativeLayout;
    public static Point sizeScreen;

    private static ImageView imageSdCardError;
    private static boolean lanSetupAlready = false;

    private View layoutShoppingListLook;    // represent layout_shopping_list_look_x.xml, where x - number of desired look
    // next block of views must be in every layout_shopping_list_look_x.xml to provide compability
    public static TextView tv_TotalCount;
    public static TextView textViewTotalSummWithDiscount;   // calculated value
    public static TextView textViewTotalDiscount;           // calculated value
    public static TextView tv_TotalSumm;             // value for this view received from COM port
    public static ListView listView;
    public static TextView textViewDEBUG;
    public static ScrollView mScrollView;
    public static ImageView imageViewTovar;

    TextView tvVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        preferenceParams = PreferenceParams.getParameters();

        //Стартуем активити с биндингом полей
        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        productInfo = new ProductInfo(this);
        binding.setProductinfo(productInfo);

        sound = new Sound(this);
        sound.setVolume(preferenceParams.sPercentVolume);

        su_preferences = new Modify_SU_Preferences(this);
        su_preferences.setSetupRootCallback(mCallbackRootIsSet);
        su_preferences.verifyRootRights();

        webStatus = new WebStatus();

        ethernetSettings = new EthernetSettings(this);
        ethernetSettings.setSetupLanCallback(mCallbackLanIsSet);

        downloadMedia = new DownloadMedia(this);

        //обработчик команд и данных
        String uriImgSource = ExtSDSource.getExternalSdCardPath() + downloadMedia.IMG_URI;
        cmdParser = new CommandParser(productInfo, messageHandler, MainActivity.this, uriImgSource);

        //слой для вывода информации по товару
        relativeLayout = new RelativeLayout[]{(RelativeLayout) findViewById(R.id.idLayoutConnect),
                (RelativeLayout) findViewById(R.id.idLayoutThanks),
                (RelativeLayout) findViewById(R.id.lay_shoppingList)
        };

        imageSdCardError = (ImageView) findViewById(R.id.imageSdCardError);
        imageSdCardError.setVisibility(View.INVISIBLE);

        tvVersion = (TextView) findViewById(R.id.tvVersion);
        tvVersion.setText("build :" + BuildConfig.VERSION_CODE);
        productInfo.setStatusConnection("ініціалізація системи");
        productInfo.setStatusConnection2("");

        //UART
        uartWorker = new UartWorker(uartHandler);
        setVisibleContext(CONTEXT_CONNECT, 0);
        if (uartWorker.openSerialPort(UartWorker.getCoreNameUart(preferenceParams.sUartName), 0, 0) == 0) {
            productInfo.setStatusConnection("інтерфейс RS232 ініціалізованo");
        } else {
            Log.e(TAG, "ERROR Open Port");
            productInfo.setStatusConnection("ПОМИЛКА інтерфейсу RS232");
        }

        updateFirmware = new UpdateFirmware(this);

        //экран "Список покупок"
        shoppingListWorker = new ShoppingListWorker(this);
        setProductListLook();

        textViewDEBUG = (TextView) findViewById(R.id.textViewDEBUG);
        textViewDEBUG.setMovementMethod(new ScrollingMovementMethod());
        mScrollView = (ScrollView) findViewById(R.id.mScrollView);

        registerReceiver(changeSettings, new IntentFilter(CHANGE_SETTINGS));
        setBackgroundScreen();
        setVisibleContext(CONTEXT_CONNECT, 0);

        new CheckSystemStart().run();

        acceptFullScreen();
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
                // next need to change look of product list
                shoppingListWorker = new ShoppingListWorker(MainActivity.context);
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
        String uriBackgroundShoppingList = ExtSDSource.getExternalSdCardPath() + downloadMedia.IMG_SCREEN + ((PreferenceParams.getParameters().backgroundShoppingList.length() > 0) ? PreferenceParams.getParameters().backgroundShoppingList : "noimg");
        File fileImg = new File(uriBackgroundShoppingList);
        if (fileImg.exists()) {
            bitmap = ImageUtils.getImage(fileImg, MainActivity.sizeScreen, false);
            drawable = new BitmapDrawable(bitmap);
            relativeLayout[CONTEXT_SHOPPING_LIST].setBackground(drawable);
        } else {
            relativeLayout[CONTEXT_SHOPPING_LIST].setBackgroundResource(R.drawable.bg);
        }
        relativeLayout[CONTEXT_SHOPPING_LIST].invalidate();

        //Фонове зображення экрану "Каса не працює"
        String uriBackgroundCashNotWork = ExtSDSource.getExternalSdCardPath() + downloadMedia.IMG_SCREEN + ((PreferenceParams.getParameters().backgroundCashNotWork.length() > 0) ? PreferenceParams.getParameters().backgroundCashNotWork : "noimg");
        fileImg = new File(uriBackgroundCashNotWork);
        if (fileImg.exists()) {
            bitmap = ImageUtils.getImage(fileImg, MainActivity.sizeScreen, false);
            drawable = new BitmapDrawable(bitmap);
            relativeLayout[CONTEXT_CONNECT].setBackground(drawable);
        } else {
            relativeLayout[CONTEXT_CONNECT].setBackgroundResource(R.drawable.screen_cache_not_work);
        }
        relativeLayout[CONTEXT_CONNECT].invalidate();

        //Фонове зображення экрану "Дякуємо за покупку"
        String uriBackgroundThanks = ExtSDSource.getExternalSdCardPath() + downloadMedia.IMG_SCREEN + ((PreferenceParams.getParameters().backgroundThanks.length() > 0) ? PreferenceParams.getParameters().backgroundThanks : "noimg");
        fileImg = new File(uriBackgroundThanks);
        if (fileImg.exists()) {
            bitmap = ImageUtils.getImage(fileImg, MainActivity.sizeScreen, false);
            drawable = new BitmapDrawable(bitmap);
            relativeLayout[CONTEXT_THANKS].setBackground(drawable);
        } else {
            relativeLayout[CONTEXT_THANKS].setBackgroundResource(R.drawable.screen_thanks);
        }
    }

    /************************************************************************************/

    /**
     * Method inflates specified layout view in activity_main.xml and get references for actual views.
     * This changes the look of product list for different client's flavours.
     */
    private void setProductListLook() {
        int lookCode = preferenceParams.productListLookCode;
        // appropriate adapter must be created everytime for actual listview for specified look of product list
        shoppingListWorker.createAdapterShoppingList(lookCode);

        RelativeLayout layShoppingList = (RelativeLayout) findViewById(R.id.lay_shoppingList);
        LayoutInflater li = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (layoutShoppingListLook != null)
            layShoppingList.removeView(layoutShoppingListLook);

        int resource;
        switch (lookCode) {
            case 0:
                resource = R.layout.layout_shopping_list_look_0;
                break;
            case 1:
                resource = R.layout.layout_shopping_list_look_1;
                break;
            default:
                resource = R.layout.layout_shopping_list_look_0;
                break;
        }
        layoutShoppingListLook = li.inflate(resource, null);
        layShoppingList.addView(layoutShoppingListLook, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        Log.d(TAG, "");
        listView = (ListView) findViewById(R.id.listview);
        textViewTotalSummWithDiscount = (TextView) findViewById(R.id.textview_sum_without_discount_end);
        textViewTotalDiscount = (TextView) findViewById(R.id.textview_discount_end_sum);
        tv_TotalSumm = (TextView) findViewById(R.id.tv_TotalSumm);
        tv_TotalCount = (TextView) findViewById(R.id.tv_TotalCount);
        imageViewTovar = (ImageView) findViewById(R.id.imageViewTovar);
        listView.setAdapter(shoppingListWorker.getAdapterShoppingList());
        tv_TotalCount.setText("0");
        textViewTotalSummWithDiscount.setText("0.00");
        textViewTotalDiscount.setText("0.00");
        tv_TotalSumm.setText("0.00");
    }

    /************************************************************************************/

    public void setVisibleContext(int contextType, Object param) {
        setVisibleLayer(CONTEXT_SHOPPING_LIST, View.INVISIBLE);
        setVisibleLayer(CONTEXT_CONNECT, View.INVISIBLE);
        setVisibleLayer(CONTEXT_THANKS, View.INVISIBLE);
        switch (contextType) {
            case CONTEXT_SHOPPING_LIST:
                setVisibleLayer(CONTEXT_SHOPPING_LIST, View.VISIBLE);
                break;
            case CONTEXT_CONNECT:
                setVisibleLayer(CONTEXT_CONNECT, View.VISIBLE);
                break;
            case CONTEXT_THANKS:
                setVisibleLayer(CONTEXT_THANKS, View.VISIBLE);
                break;
            default:
                break;
        }
    }

    private void setVisibleLayer(int contextLayer, int visible) {
        relativeLayout[contextLayer].setVisibility(visible);
        relativeLayout[contextLayer].invalidate();
    }

    /************************************************************************************/

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(changeSettings);
        unregisterReceiver(uartWorker.uartChangeSettings);
        unregisterReceiver(videoSlideService.videoSlideEvents);
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
                    setVisibleContext(CONTEXT_CONNECT, 0);
                    break;
                case UartWorker.ACTION_UART_ERROR: //  errors
                    Log.d(TAG, "ACTION_UART_ERROR");
                    setVisibleContext(CONTEXT_CONNECT, 0);
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
                case MSG_ADD_TOVAR_SHOPPING_LIST:
                    Log.d(TAG, "MSG_ADD_TOVAR_SHOPPING_LIST");
                    shoppingListWorker.addTovarList((String) msg.obj);
                    setVisibleContext(CONTEXT_SHOPPING_LIST, msg.arg2);
                    break;
                case MSG_SET_TOVAR_SHOPPING_LIST:
                    Log.d(TAG, "MSG_SET_TOVAR_SHOPPING_LIST");
                    shoppingListWorker.setPositionTovarList((String) msg.obj);
                    setVisibleContext(CONTEXT_SHOPPING_LIST, msg.arg2);
                    break;
                case MSG_DEL_TOVAR_SHOPPING_LIST:
                    Log.d(TAG, "MSG_DEL_TOVAR_SHOPPING_LIST");
                    shoppingListWorker.deletePositionTovarList((String) msg.obj);
                    setVisibleContext(CONTEXT_SHOPPING_LIST, msg.arg2);
                    break;
                case MSG_CLEAR_SHOPPING_LIST:
                    Log.d(TAG, "MSG_CLEAR_SHOPPING_LIST");
                    shoppingListWorker.clearTovarList((String) msg.obj);
                    break;
                case MSG_SET_SCREEN_NOT_WORK:
                    Log.d(TAG, "MSG_SET_SCREEN_NOT_WORK");
                    shoppingListWorker.closeDisplayShoppingList();
                    setVisibleContext(CONTEXT_CONNECT, msg.arg2);
                    break;
                case MSG_SET_SCREEN_THANKS:
                    Log.d(TAG, "MSG_SET_SCREEN_THANKS");
                    setVisibleContext(CONTEXT_THANKS, msg.arg2);
                    shoppingListWorker.closeDisplayShoppingList();
                    setEnableMedia(true);
                    resetMediaTime();
                    break;
                case MSG_FROM_EKKR:
                    Log.d(TAG, "MSG_FROM_EKKR");
                    break;
                case MSG_ADD_PRODUCT_DEBUG:
                    shoppingListWorker.addProductDebug((String) msg.obj);
                    break;
                default:
                    Log.e(TAG, "Handler default message:" + String.valueOf(msg.what));
                    break;
            }
        }
    };

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
            super.run();
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
            super.run();

            //немного музыки в момент запуска
            if (sound != null) {
                sound.setVolume(80);
                sound.playSound(Sound.START_VOICE);
                sound.setVolume(preferenceParams.sPercentVolume);
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String ip = "";
            boolean loadMediaAtStartSystem = false;

            productInfo.setStatusConnection(ethernetSettings.getCurrentStatus());

            while (!isInterrupted()) {
                try {
                    // not the best place, but good for SD card absence detection
                    runOnUiThread(() -> {
                        if (!ExtSDSource.isMounted(context)) {
                            imageSdCardError.setVisibility(View.VISIBLE);
                            sound.setVolume(80);
                            sound.playSound(Sound.WARNING_VOICE);
                            productInfo.setStatusConnection("*** Вiдсутнiй SD носiй ***");
                            productInfo.setStatusConnection2("*** Вiдсутнiй SD носiй ***");
                            uartWorker.closeSerialPort();
                            Log.e(TAG, "Вiдсутнiй SD носiй");
                        } else
                            imageSdCardError.setVisibility(View.INVISIBLE);
                    });

                    if (EthernetSettings.isConnected()) {
                        if (!loadMediaAtStartSystem && preferenceParams.sDownloadAtStart) {
                            loadMediaAtStartSystem = true;
                            downloadMedia.download();
                        }

                        String stat = ethernetSettings.getCurrentStatus();
                        String networkInterfaceIpAddress = EthernetSettings.getNetworkInterfaceIpAddress();
                        productInfo.setStatusConnection(((stat.length() == 0) ? "IP : " + networkInterfaceIpAddress : stat));
                        productInfo.setStatusConnection2(((stat.length() == 0) ? "IP : " + networkInterfaceIpAddress : stat));

                        if (!ip.equals(networkInterfaceIpAddress)) {
                            ip = networkInterfaceIpAddress;
                            Log.d(TAG, "Подключение LAN : " + ip);
                        }
                    } else {
                        String stat = ethernetSettings.getCurrentStatus();
                        productInfo.setStatusConnection(((stat.length() == 0) ? "підключення LAN відсутнє" : stat));
                        productInfo.setStatusConnection2(((stat.length() == 0) ? "підключення LAN відсутнє" : stat));
                        Log.d(TAG, "Подключение LAN отсутствует");
                        ip = "";
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //событие об установке прав SU
    private Modify_SU_Preferences.SetupRootCallback mCallbackRootIsSet = new Modify_SU_Preferences.SetupRootCallback() {
        @Override
        public void onSetupRoot(final int result) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "mCallbackRootIsSet :" + result);

                    imageSdCardError.setVisibility(View.INVISIBLE);
                    if (!ExtSDSource.isMounted(context)) {
                        imageSdCardError.setVisibility(View.VISIBLE);
                        sound.setVolume(80);
                        sound.playSound(Sound.WARNING_VOICE);

                        productInfo.setStatusConnection("*** Вiдсутнiй SD носiй ***");
                        productInfo.setStatusConnection2("*** Вiдсутнiй SD носiй ***");
                        uartWorker.closeSerialPort();
                    }

                    if (result == 1) {
                        if (BuildConfig.BUILD_TYPE.equals("release")) {
                            if (sizeScreen.x != 1920) //только для 10"
                                Modify_SU_Preferences.setSystemUIEnabled(preferenceParams.sShowNavigationBar);//спрячем строку навигации
                        } else
                            Modify_SU_Preferences.setSystemUIEnabled(true);//покажем строку навигации

                        Log.w(TAG, "административные права получены");
                        productInfo.setStatusConnection("ініціалізація системи ");
                        ethernetSettings.applyEthernetSettings();//применение параметров
                    } else {
                        Log.w(TAG, "ОШИБКА, нет административных прав:" + result);
                        setVisibleContext(CONTEXT_CONNECT, 0);
                        productInfo.setStatusConnection("ПОМИЛКА, не були надані адміністративні права, " + "IP : " + EthernetSettings.getNetworkInterfaceIpAddress());
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
        public void onSetupLAN(final int result) {
            if (!lanSetupAlready) {//запуск только 1 раз
                lanSetupAlready = true;
                Log.d(TAG, "mCallbackSetupLAN :" + result);
                new CheckConnectionEth().start();//проверка и установка сети
                httpServer = new HttpServer(context, webStatus);
            }
        }
    };
}

// TODO: 23.10.2019 По ftp протоколу видео файлы могут "ломаться" (видео проигрывается, но картинка искажена)
// TODO: 28.10.2019 Внедрить Crashlytics 