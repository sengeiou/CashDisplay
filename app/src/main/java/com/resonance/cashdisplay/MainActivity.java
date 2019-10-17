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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

//import android.util.Log;

import android.text.method.ScrollingMovementMethod;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;


import java.io.File;


import com.resonance.cashdisplay.databinding.ActivityMainBinding;


import com.resonance.cashdisplay.eth.Eth_Settings;
import com.resonance.cashdisplay.eth.Modify_SU_Preferences;
import com.resonance.cashdisplay.http.http_Server;
import com.resonance.cashdisplay.load.DownloadMedia;

import com.resonance.cashdisplay.shopping_list.ShoppingListWorker;
import com.resonance.cashdisplay.slide_show.VideoSlideService;
import com.resonance.cashdisplay.sound.Sound;
import com.resonance.cashdisplay.su.RunTimeFunc;
import com.resonance.cashdisplay.uart.UartWorker;
import com.resonance.cashdisplay.utils.ImageUtils;
import com.resonance.cashdisplay.web.WebStatus;


import static com.resonance.cashdisplay.uart.UartWorker.ACTION_UART_OPEN;

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


    public static PreferencesValues preferenceParams;       //настройки
    private static UartWorker uartWorker;                   //обработчик UART
    public static WebStatus webStatus = null;               //канал передачи сообщений для браузера
    public static http_Server httpServer = null;            //http сервер
    private CommandParser cmdParser;                        //класс обработки команд и данных
    private VideoSlideService videoSlideService;            //класс управления медиа
    private Sound sound;                                    //звук
    private static  ShoppingListWorker shoppingListWorker;  //обслуживание списка товаров
    ProductInfo productInfo;
    public static Eth_Settings ethernetSettings = null;     //Настройка сети
    public static DownloadMedia downloadMedia;
    //обновление прошивки
    public static UpdateFirmware updateFirmware = null;     //обновление ПО

    private static Modify_SU_Preferences su_preferences;

    public static Context mContext;
    private static RelativeLayout[] rlay;
    public static Point sizeScreen;


    private static ImageView imageSdCardError;
    private static boolean LanSetupAlready = false;


    public static TextView tv_TotalSumm;
    public static TextView tv_TotalCount;
    public static ListView listView;
    public static TextView textViewDEBUG;
    public static ScrollView mScrollView;
    public static RelativeLayout lay_shoppingList;
    public static ImageView imageViewTovar;

    TextView tvVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LanSetupAlready = false;
        Log.d(TAG, "                        " );
        Log.d(TAG, "***  START SYSTEM  *** " + BuildConfig.BUILD_TYPE+", build: "+BuildConfig.VERSION_CODE);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//постоянно включен экран


        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_FULLSCREEN;
        getWindow().setAttributes(attributes);


        mContext = this;

        //Получим настройки экрана
        Display display = this.getWindowManager().getDefaultDisplay();
        sizeScreen = new Point();
        display.getSize(sizeScreen);
        sizeScreen.y += 48;
        Log.d(TAG, "Size screen x:" + sizeScreen.x +", y:"+sizeScreen.y);

        //Получим настройки системы
        preferenceParams = PreferenceParams.getParameters();

        //Стартуем активити с биндингом полей
        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        productInfo = new ProductInfo(this);
        binding.setProductinfo(productInfo);

        sound = new Sound(this);
        sound.setVolume(preferenceParams.sPercentVolume);

        su_preferences = new Modify_SU_Preferences(mContext);
        su_preferences.callback_onSetupRoot(mCallbackRootIsSet);
        su_preferences.VerifyRootRights();


        webStatus = new WebStatus();

        ethernetSettings = new Eth_Settings(this);
        ethernetSettings.callback_onSetupLAN(mCallbackLanIsSet);


        downloadMedia = new DownloadMedia(this);

        //обработчик команд и данных
        String Uri_ImgSource = ExtSDSource.getExternalSdCardPath() + downloadMedia.IMG_URI;
        cmdParser = new CommandParser(productInfo, messageHandler, MainActivity.this, Uri_ImgSource);

        //слой для вывода информации по товару
        rlay = new RelativeLayout[]{(RelativeLayout) findViewById(R.id.idLayoutConnect),
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

        if (uartWorker.OpenSerialPort(UartWorker.getCoreNameUart(preferenceParams.sUartName), 0, 0) == 0) {
            setVisibleContext(CONTEXT_CONNECT, 0);
            productInfo.setStatusConnection("інтерфейс RS232 ініціалізованo");


        } else {
            Log.e(TAG, "ERROR Open Port");
            setVisibleContext(CONTEXT_CONNECT, 0);
            productInfo.setStatusConnection("ПОМИЛКА інтерфейсу RS232");

        }


        updateFirmware = new UpdateFirmware(this);

        //экран "Список покупок"
        shoppingListWorker = new ShoppingListWorker(this);
        listView = (ListView) findViewById(R.id.listview);
        tv_TotalSumm = (TextView)findViewById(R.id.tv_TotalSumm);
        tv_TotalCount = (TextView)findViewById(R.id.tv_TotalCount);
        imageViewTovar = (ImageView)findViewById(R.id.imageViewTovar);
        textViewDEBUG = (TextView)findViewById(R.id.textViewDEBUG);
        textViewDEBUG.setMovementMethod(new ScrollingMovementMethod());
        mScrollView = (ScrollView)findViewById(R.id.mScrollView);
        lay_shoppingList = (RelativeLayout) findViewById(R.id.lay_shoppingList);
        listView.setAdapter(shoppingListWorker.adapterShoppingList);

        tv_TotalCount.setText("0");
        tv_TotalSumm.setText("0.00");

        Change_settings_register_receiver();
        SetBackgroundScreen();
        setVisibleContext(CONTEXT_CONNECT, 0);

        new CheckSystemStart().run();

        AcceptFullScreen();
    }

    /**
     * Программное нажатие кнопки при переходе в полноэкранный режим
     */
    private void AcceptFullScreen(){
    //
    SharedPreferences sp = getSharedPreferences("LOADDATA", MODE_PRIVATE);
    if (!sp.getBoolean("InputTap", false)) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                //эмулируем нажатие кнопки для подтверждения при переходе в полноэкранный режим
                if (sizeScreen.x==1920) //14"
                    Modify_SU_Preferences.executeCmd("input tap  1060 170", 1000);//3000//
                else //10"
                    Modify_SU_Preferences.executeCmd("input tap 746 157", 1000);//3000//

            }
        }, 30000);
    }
    SharedPreferences.Editor ed = sp.edit();
    ed.putBoolean("InputTap", true);
    ed.commit();

}

    /**
     * Приемник события изменения настроек
     */
    private void Change_settings_register_receiver()
    {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CHANGE_SETTINGS);
        registerReceiver(ChangeSettings,intentFilter);

    }
    BroadcastReceiver ChangeSettings = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {


            if (intent.getAction().equals(CHANGE_SETTINGS))
            {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SetBackgroundScreen();

                    }
                });

            }
        };
    };

    /*********************************************************************************************/


    /**
     * Установка фона экранов
     */
    private void SetBackgroundScreen(){
        //установка фона экрана "Список покупок"
        Bitmap bitmap;
        Drawable drawable;
        String Uri_Background_shoppingList = ExtSDSource.getExternalSdCardPath() + downloadMedia.IMG_SCREEN+((PreferenceParams.getParameters().Background_shoppingList.length()>0)?PreferenceParams.getParameters().Background_shoppingList:"noimg");
        File fileImg =new File(Uri_Background_shoppingList);
        if (fileImg.exists()) {
            bitmap = ImageUtils.getImage(fileImg, MainActivity.sizeScreen, false);
            drawable = new BitmapDrawable(bitmap);
            rlay[CONTEXT_SHOPPING_LIST].setBackground(drawable);
        }else
        {
            rlay[CONTEXT_SHOPPING_LIST].setBackgroundResource(R.drawable.bg);
        }
        rlay[CONTEXT_SHOPPING_LIST].invalidate();


        //Фонове зображення экрану "Каса не працює"
        String Uri_Background_CashNotWork = ExtSDSource.getExternalSdCardPath() + downloadMedia.IMG_SCREEN+((PreferenceParams.getParameters().Background_CashNotWork.length()>0)?PreferenceParams.getParameters().Background_CashNotWork:"noimg");
        fileImg =new File(Uri_Background_CashNotWork);
        if (fileImg.exists()) {
            bitmap = ImageUtils.getImage(fileImg, MainActivity.sizeScreen, false);
            drawable = new BitmapDrawable(bitmap);
            rlay[CONTEXT_CONNECT].setBackground(drawable);
        }else
        {
            rlay[CONTEXT_CONNECT].setBackgroundResource(R.drawable.screen_cache_not_work);
        }
        rlay[CONTEXT_CONNECT].invalidate();


        //Фонове зображення экрану "Дякуємо за покупку"
        String Uri_Background_Thanks = ExtSDSource.getExternalSdCardPath() + downloadMedia.IMG_SCREEN+((PreferenceParams.getParameters().Background_Thanks.length()>0)?PreferenceParams.getParameters().Background_Thanks:"noimg");
        fileImg =new File(Uri_Background_Thanks);
        if (fileImg.exists()) {
            bitmap = ImageUtils.getImage(fileImg, MainActivity.sizeScreen, false);
            drawable = new BitmapDrawable(bitmap);
            rlay[CONTEXT_THANKS].setBackground(drawable);
        }else
        {
            rlay[CONTEXT_THANKS].setBackgroundResource(R.drawable.screen_thanks);
        }
    }

    /************************************************************************************/

    public void setVisibleContext(int TypeContext, Object param) {

        switch (TypeContext) {
            case CONTEXT_SHOPPING_LIST:
                SetVisibleLayer(CONTEXT_SHOPPING_LIST, View.VISIBLE);
                SetVisibleLayer(CONTEXT_CONNECT, View.INVISIBLE);
                SetVisibleLayer(CONTEXT_THANKS, View.INVISIBLE);
                break;
            case CONTEXT_CONNECT:
                SetVisibleLayer(CONTEXT_SHOPPING_LIST, View.INVISIBLE);
                SetVisibleLayer(CONTEXT_CONNECT, View.VISIBLE);
                SetVisibleLayer(CONTEXT_THANKS, View.INVISIBLE);
                break;
            case CONTEXT_THANKS:
                SetVisibleLayer(CONTEXT_SHOPPING_LIST, View.INVISIBLE);
                SetVisibleLayer(CONTEXT_CONNECT, View.INVISIBLE);
                SetVisibleLayer(CONTEXT_THANKS, View.VISIBLE);
                break;

            default:
                break;
        }



    }

    private void SetVisibleLayer(int LayContext, int visible) {
        rlay[LayContext].setVisibility(visible);
        rlay[LayContext].invalidate();
    }

    /************************************************************************************/


    @Override
    public void onStart() {
        super.onStart();
   }


    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(ChangeSettings);
     }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {

       super.onResume();
    }


    /***************************************
     * UART
     ***************************************/
    private final Handler uartHandler = new Handler() {


        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UartWorker.ACTION_UART_READ:
                    cmdParser.ParseInputStr((byte[]) msg.obj, msg.arg2);
                    break;
                case ACTION_UART_OPEN:
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

            switch (msg.what) {

                case MSG_ADD_TOVAR_SHOPPING_LIST:
                    Log.d(TAG, "MSG_ADD_TOVAR_SHOPPING_LIST" );
                    SetEnableMedia(false);
                    shoppingListWorker.Add_TovarList((String) msg.obj);
                    setVisibleContext(msg.arg1, msg.arg2);
                    break;
                case  MSG_SET_TOVAR_SHOPPING_LIST:
                    Log.d(TAG, "MSG_SET_TOVAR_SHOPPING_LIST" );
                    SetEnableMedia(false);
                    shoppingListWorker.SetPosition_TovarList((String) msg.obj);
                    setVisibleContext(msg.arg1, msg.arg2);

                    break;
                case  MSG_DEL_TOVAR_SHOPPING_LIST:
                    Log.d(TAG, "MSG_DEL_TOVAR_SHOPPING_LIST" );
                    shoppingListWorker.DeletePosition_TovarList((String) msg.obj);

                    SetEnableMedia(false);
                    setVisibleContext(msg.arg1, msg.arg2);
                    break;
                case  MSG_CLEAR_SHOPPING_LIST:
                    Log.d(TAG, "MSG_CLEAR_SHOPPING_LIST" );
                    SetEnableMedia(false);
                    shoppingListWorker.Clear_TovarList((String) msg.obj);

                    break;
                case  MSG_SET_SCREEN_NOT_WORK:
                    SetEnableMedia(false);
                    Log.d(TAG, "MSG_SET_SCREEN_NOT_WORK" );
                    shoppingListWorker.CloseDisplayShoppingList();
                    setVisibleContext(msg.arg1, msg.arg2);

                    break;
                case  MSG_SET_SCREEN_THANKS:
                    Log.d(TAG, "MSG_SET_SCREEN_THANKS" );
                    setVisibleContext(msg.arg1, msg.arg2);
                    shoppingListWorker.CloseDisplayShoppingList();
                    SetEnableMedia(true);
                    ResetMediaTime();
                    break;
                case  MSG_FROM_EKKR:
                    Log.d(TAG, "MSG_FROM_EKKR" );
                    break;
                case  1234:
                    shoppingListWorker.ADD_DEBUG((String) msg.obj);
                    break;


                default:
                    Log.e(TAG, "Handler default message:" + String.valueOf(msg.what));
                    break;
            }
        }
    };


    /**
     * Разрешить/запретить демонстрацию медиа
     * @param state
     */
    private void SetEnableMedia(boolean state){
        Intent intent = new Intent(VideoSlideService.VIDEO_SLIDE_ENABLE);
        Bundle mBundle = new Bundle();
        mBundle.putBoolean("enable_video_slide", state);
        intent.putExtras(mBundle);
        sendBroadcast(intent);
    }

    /**
     * Сброс времени, остановка демонстации медиа
     */
    private void ResetMediaTime(){
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
                while (!isInterrupted())
                {
                    if (Modify_SU_Preferences.CheckSystemBootCompleted())
                    {
                        Log.d(TAG, "property set: SYSTEM BOOT COMPLETED");

                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(new Runnable() {
                            public void run() {
                                videoSlideService = new VideoSlideService(MainActivity.this);
                            }
                        });

                        break;
                    }
                    try
                    {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };


    /**
     * Поток мониторит наличие подключение LAN
     */
     private class CheckConnectionEth extends Thread {
        @Override
        public void run() {
            super.run();

            //немного музыки в момент запуска
            if (sound != null) {
                sound.setVolume(80);
                sound.PlaySound(Sound.START_VOICE);
                sound.setVolume(preferenceParams.sPercentVolume);
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String ip = "";
            boolean LoadMediaAtStartSystem = false;

            productInfo.setStatusConnection(ethernetSettings.getCurrentStatus());

            while (!isInterrupted()) {
                try {
                    if (Eth_Settings.isConnected()) {
                        if (!LoadMediaAtStartSystem && preferenceParams.sDownloadAtStart) {
                            LoadMediaAtStartSystem = true;
                            downloadMedia.download();
                        }

                        String stat = ethernetSettings.getCurrentStatus();
                        String NetworkInterfaceIpAddress = Eth_Settings.getNetworkInterfaceIpAddress();
                         productInfo.setStatusConnection(((stat.length() == 0) ? "IP : " + NetworkInterfaceIpAddress : stat));
                        productInfo.setStatusConnection2(((stat.length() == 0) ? "IP : " + NetworkInterfaceIpAddress : stat));

                        if (!ip.equals(NetworkInterfaceIpAddress)) {
                            ip = NetworkInterfaceIpAddress;
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
    };

    //событие об установке прав SU
    private Modify_SU_Preferences.SetupRootCallback mCallbackRootIsSet = new Modify_SU_Preferences.SetupRootCallback() {
        @Override
        public void onSetupRoot(final int result) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "mCallbackRootIsSet :" + result);

                    imageSdCardError.setVisibility(View.INVISIBLE);
                    if (!ExtSDSource.isMounted(mContext))
                    {
                        imageSdCardError.setVisibility(View.VISIBLE);
                        sound.setVolume(80);
                        sound.PlaySound(Sound.WARNING_VOICE);

                        productInfo.setStatusConnection("*** Вiдсутнiй SD носiй ***");
                        productInfo.setStatusConnection2("*** Вiдсутнiй SD носiй ***");
                        uartWorker.CloseSerialPort();
                    }

                    if (result == 1)
                    {
                        if (BuildConfig.BUILD_TYPE.equals("release")) {
                            if (sizeScreen.x!=1920) //только для 10"
                            Modify_SU_Preferences.setSystemUIEnabled(preferenceParams.sShowNavigationBar);//спрячем строку навигации
                        }
                        else
                            Modify_SU_Preferences.setSystemUIEnabled(true);//покажем строку навигации

                        Log.w(TAG, "административные права получены");
                        productInfo.setStatusConnection("ініціалізація системи ");
                        ethernetSettings.ApplyEthernetSettings();//применение параметров
                    } else {
                        Log.w(TAG, "ОШИБКА, нет административных прав:" + result);
                        setVisibleContext(CONTEXT_CONNECT, 0);
                        productInfo.setStatusConnection("ПОМИЛКА, не були надані адміністративні права, " + "IP : " + Eth_Settings.getNetworkInterfaceIpAddress());
                        imageSdCardError.setImageResource(R.drawable.warning);
                        imageSdCardError.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
    };

    //событие об окончании настройки сети
    private Eth_Settings.SetupLanCallback mCallbackLanIsSet = new Eth_Settings.SetupLanCallback() {
        @Override
        public void onSetupLAN(final int result) {
            if (!LanSetupAlready) {//запуск только 1 раз
                LanSetupAlready = true;
                Log.d(TAG, "mCallbackSetupLAN :" + result);
                new CheckConnectionEth().start();//проверка и установка сети

                httpServer = new http_Server(mContext, webStatus);
            }
        }
    };



}


