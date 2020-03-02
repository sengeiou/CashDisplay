package com.resonance.cashdisplay.slide_show;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.resonance.cashdisplay.CommandParser;
import com.resonance.cashdisplay.ExtSDSource;
import com.resonance.cashdisplay.FileOperation;
import com.resonance.cashdisplay.Log;
import com.resonance.cashdisplay.MainActivity;
import com.resonance.cashdisplay.R;
import com.resonance.cashdisplay.load.DownloadMedia;

import java.io.File;

import static com.resonance.cashdisplay.CommandParser.CMD_NWRK;
import static com.resonance.cashdisplay.CommandParser.CMD_THNK;
import static com.resonance.cashdisplay.CommandParser.SYMBOL_SEPARATOR;
import static com.resonance.cashdisplay.Constants.SCAN_BARCODE;
import static com.resonance.cashdisplay.slide_show.SlideViewActivity.FINISH_ALERT;

public class VideoActivity extends AppCompatActivity {

    public final String TAG = "Video";

    private MediaController mediaController;
    VideoView videoView;
    private RelativeLayout videoLayout;
    private static int seekVideoPosition = 0;
    private static String[] videoFilesArray;//массив со списком видео файлов
    private static int indexPlaingFile = 0;
    private final String[] fileExtension = new String[]{"AVI", "avi", "mp4", "MP4"};
    private static String mediaDir = "";

    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();

    private StringBuilder barcodeBuilder = new StringBuilder();

    private static String CurrentPlaingFile = "";

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Log.i(TAG, "onCreate");
        setContentView(R.layout.activity_video);
        delayedHide(1);

        //инитим Видео
        videoView = (VideoView) findViewById(R.id.videoView);
        mediaController = new MediaController(this);
        mediaController.setVisibility(View.GONE);
        mediaController.setAnchorView(videoView);

        videoView.setMediaController(mediaController);
        videoView.setOnCompletionListener(mVideoViewCompletionListener);
        videoView.setOnPreparedListener(mVideoViewPreparedListener);
        videoView.setOnErrorListener(mVideoViewErrorListener);
        videoView.forceLayout();
        videoView.setFitsSystemWindows(true);

        videoLayout = (RelativeLayout) findViewById(R.id.videoLayout);
        this.mediaDir = ExtSDSource.getExternalSdCardPath(this) + DownloadMedia.VIDEO_URI;

        this.registerReceiver(this.finishAlert, new IntentFilter(FINISH_ALERT));

        String tmpFileContinuePlay = "";
        Bundle b = getIntent().getExtras();

        if (b != null) {
            // Log.i(TAG, "onCreate savedInstanceState!= null");
            seekVideoPosition = b.getInt("seekVideoPosition");//начальная позиция проигрывания видео
            tmpFileContinuePlay = b.getString("VideoFileToContinuePlay");
        }
        updateListMediaFiles(tmpFileContinuePlay);
        startPlay();
    }

    @Override
    protected void onStart() {
        super.onStart();
        hideBarNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        videoLayout.setFocusable(true);                                // for barcode scanner
        videoLayout.setFocusableInTouchMode(true);                     // for barcode scanner
        videoLayout.requestFocus();                                    // for barcode scanner
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(finishAlert);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            char pressedKey = (char) event.getUnicodeChar();
            barcodeBuilder.append(pressedKey);
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
            CommandParser.handleCommand(CMD_NWRK, null);
            CommandParser.handleCommand(CMD_THNK, SCAN_BARCODE + (char) SYMBOL_SEPARATOR + barcodeBuilder.toString());
        }
        return super.dispatchKeyEvent(event);
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

    BroadcastReceiver finishAlert = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            stopPlay();
        }
    };

    MediaPlayer.OnCompletionListener mVideoViewCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer arg0) {
            seekVideoPosition = 0;
            // Log.i(TAG, "videoFilesArray.length " +videoFilesArray.length);
            if (videoFilesArray.length > 0) {
                indexPlaingFile++;
                indexPlaingFile %= videoFilesArray.length;
            }
            Log.i(TAG, "Проигрывание видео завершено ");
            if (videoView != null)
                videoView.stopPlayback();
            startPlay();
        }
    };

    MediaPlayer.OnPreparedListener mVideoViewPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            Log.i(TAG, "Медиа файл загружен и готов для воспроизведения");

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    videoView.start();
                }
            });
        }
    };

    MediaPlayer.OnErrorListener mVideoViewErrorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {

            if (what == MediaPlayer.MEDIA_ERROR_UNKNOWN)
                showToast("Не підтримується формат медiа файлу : " + videoFilesArray[indexPlaingFile]);
            else
                showToast("Помилка завантаження медіа файлу : " + videoFilesArray[indexPlaingFile]);
            Log.i(TAG, "Ошибка загрузки медиа файла: " + videoFilesArray[indexPlaingFile] + " what:" + what + ", extra:" + extra);
            indexPlaingFile++;
            indexPlaingFile %= videoFilesArray.length;
            seekVideoPosition = 0;


            if ((indexPlaingFile == 0) && (videoFilesArray.length == 1)) {
                updateListMediaFiles("");
                stopPlay();
            } else
                startPlay();

            return true;
        }
    };

    private void updateListMediaFiles(String FileToContinue) {
        File dir = new File(this.mediaDir);
        if (dir.exists()) {
            indexPlaingFile = 0;
            videoFilesArray = dir.list(new FileOperation.FileExtensionFilter(fileExtension[0], fileExtension[1], fileExtension[2]));
            boolean isFound = false;
            for (int i = 0; i < videoFilesArray.length; i++) {
                if (videoFilesArray[i].equals(FileToContinue)) {
                    indexPlaingFile = i;
                    isFound = true;
                    break;
                }
            }
            if (!isFound)
                seekVideoPosition = 0;

            Log.i(TAG, "Dir VIDEO files: " + videoFilesArray.length + " dir:" + mediaDir);
        } else {
            seekVideoPosition = 0;
            Log.e(TAG, "Источник VIDEO не найден: " + this.mediaDir);
            showToast("Источник VIDEO не найден: " + this.mediaDir);
        }
    }


    private void startPlay() {
        // Log.d(TAG, "StartPlay");
        if (videoFilesArray == null) {
            stopPlay();
            return;
        }

        try {
            if (videoFilesArray.length > 0) {
                if ((indexPlaingFile < videoFilesArray.length) && (indexPlaingFile >= 0)) {
                    if (videoView != null) {
                        Log.d(TAG, "2 StartPlay: " + videoView.isPlaying());
                        if (!videoView.isPlaying()) {
                            File fileTmp = new File(mediaDir + videoFilesArray[indexPlaingFile]);
                            if (fileTmp.exists()) {
                                if (!FileOperation.isFileLocked(fileTmp)) {
                                    Log.d(TAG, "Start play video: " + mediaDir + videoFilesArray[indexPlaingFile]);
                                    Uri uriVideo = Uri.parse(mediaDir + videoFilesArray[indexPlaingFile]);
                                    videoView.setVideoURI(uriVideo);
                                    videoView.seekTo(seekVideoPosition);
                                }
                            } else {
                                stopPlay();
                            }
                        }
                    }
                } else {
                    indexPlaingFile = 0;
                }
            } else {
                Log.i(TAG, "Видео файлов:" + videoFilesArray.length + ", тек. файл: " + indexPlaingFile + " - воспроизведение отложено");
                stopPlay();
            }
        } catch (Exception e) {
            stopPlay();
        }


    }

    public void stopPlay() {
        // Log.d(TAG, "StopPlay");
        if (videoView != null) {
            if (videoView.isPlaying()) {
                seekVideoPosition = videoView.getCurrentPosition();//получим позицию воспроизведения
                //  Log.w(TAG, "ОСТАНОВКА ВИДЕО");
                videoView.stopPlayback();
            } else
                seekVideoPosition = 0;
            //передадим данные для последующего воспроизведения видео
            Intent intent = new Intent(VideoSlideService.VIDEO_STOP_PLAY);
            Bundle mBundle = new Bundle();
            mBundle.putInt("seekVideoPosition", seekVideoPosition);
            mBundle.putString("VideoFileToContinuePlay", ((videoFilesArray != null) ? (videoFilesArray.length > 0 ? videoFilesArray[indexPlaingFile] : "") : ""));
            intent.putExtras(mBundle);
            MainActivity.context.sendBroadcast(intent);

        }
        this.finish();
    }


    public void showToast(String message) {
        Toast myToast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
        myToast.show();
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

    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };


    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
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
    };
}
