package com.resonance.cashdisplay.slide_show;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.resonance.cashdisplay.ExtSDSource;
import com.resonance.cashdisplay.FileOperation;
import com.resonance.cashdisplay.Log;
import com.resonance.cashdisplay.MainActivity;
import com.resonance.cashdisplay.R;
import com.resonance.cashdisplay.settings.PrefWorker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.resonance.cashdisplay.load.DownloadMedia.SLIDE_URI;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class SlideViewActivity extends AppCompatActivity {
    public final String TAG = "Slide";

    private List<Slide> movieList = new ArrayList<>();
    private RecyclerView recyclerView = null;
    private SlideViewAdapter mAdapter = null;
    private Context mContext = null;

    public static final String FINISH_ALERT = "finish_alert";

    private static int indexPicture = -1;
    private static boolean bShowSlide = false;

    private Thread thread = null;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 1;
    private final Handler mHideHandler = new Handler();

    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
        }
    };

    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
        }
    };

    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//постоянно включен экран
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_FULLSCREEN;
        getWindow().setAttributes(attributes);

        setContentView(R.layout.activity_slide_view);

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

        Log.d(TAG, "onCreate");

        recyclerView = findViewById(R.id.recycler_view);
        mAdapter = new SlideViewAdapter(movieList);
    }

    BroadcastReceiver finishAlert = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            stopSlideShow();
        }
    };

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);

        // recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);

        this.registerReceiver(this.finishAlert, new IntentFilter(FINISH_ALERT));
        delayedHide(1);
        startSlideShow();
    }

    @Override
    protected void onStart() {
        super.onStart();
        hideBarNavigation();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(finishAlert);
    }

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

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }


    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    /*******************************************************************************************/

    public void startSlideShow() {
        prepareSlideData();
    }

    public void stopSlideShow() {
        Intent intent = new Intent(VideoSlideService.SLIDE_STOP_PLAY);
        MainActivity.context.sendBroadcast(intent);

        Log.i(TAG, "stopSlideShow ");
        bShowSlide = false;
        this.finish();
    }

    private void prepareSlideData() {

        File dir = new File(ExtSDSource.getExternalSdCardPath(this) + SLIDE_URI);
        Log.i(TAG, "Dir slide files: " + dir.getPath());

        String[] slideFilesArray = null;

        if (dir.exists()) {
            slideFilesArray = dir.list(new FileOperation.FileExtensionFilter("jpg", "png"));

        } else {
            Log.w(TAG, "Источник слайдов не найден: " + dir.getName());
            showToast("Источник слайдов не найден: " + ExtSDSource.getExternalSdCardPath(this) + SLIDE_URI);
            stopSlideShow();
        }

        if (slideFilesArray == null) {
            showToast("нет слайдов для демонстрации: " + ExtSDSource.getExternalSdCardPath(this) + SLIDE_URI);
            return;
        }

        movieList.clear();

        for (int i = 0; i < slideFilesArray.length; i++) {

            Slide slide = new Slide();
            slide.setPathToImgFile(ExtSDSource.getExternalSdCardPath(this) + SLIDE_URI + slideFilesArray[i]);
            movieList.add(slide);


            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mAdapter.notifyDataSetChanged();
                }
            });

        }

        Log.w(TAG, "количество изображений : " + mAdapter.getItemCount());

        if (mAdapter.getItemCount() > 0) {
            ShowSlide();
        } else {
            stopSlideShow();
        }

    }


    private void ShowSlide() {
        if (thread != null)
            thread.interrupt();

        thread = new Thread(SlideThread);
        thread.start();
    }


    private final Runnable SlideThread = new Runnable() {
        @Override
        public void run() {
            bShowSlide = true;

            long StopTime = System.nanoTime() + TimeUnit.SECONDS.toNanos(PrefWorker.getValues().timeSlideImage);
            // Log.w(TAG, "Time to show  : " + PreferenceParams.getValues().sTimeSlideImage+" : "+PreferenceParams.getValues().sVideoOrSlide );
            while (bShowSlide && (PrefWorker.getValues().videoOrSlide == PrefWorker.SLIDE)) {

                if (recyclerView != null) {

                    if (recyclerView.getAdapter().getItemCount() == 0)
                        prepareSlideData();

                    if (recyclerView.getAdapter().getItemCount() > 0) {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                recyclerView.scrollToPosition(indexPicture);
                            }
                        });
                        if (indexPicture++ >= recyclerView.getAdapter().getItemCount()) {
                            indexPicture = 0;
                        }
                    }
                }


                while (StopTime > System.nanoTime()) {
                    //Log.d(TAG, "StopTime:"+StopTime+", System.nanoTime(): "+System.nanoTime());
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
                StopTime = System.nanoTime() + TimeUnit.SECONDS.toNanos(PrefWorker.getValues().timeSlideImage);
                // Log.w(TAG, "2 Time to show  : " + PreferenceParams.getValues().sTimeSlideImage+" : "+PreferenceParams.getValues().sVideoOrSlide );
            }
            ;
            stopSlideShow();
        }

    };


    public void showToast(String message) {
        Toast myToast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG);
        myToast.show();

    }

}
