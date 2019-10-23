package com.resonance.cashdisplay.slide_show;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.resonance.cashdisplay.Log;
import com.resonance.cashdisplay.PreferenceParams;

import java.util.concurrent.TimeUnit;

//import android.util.Log;

public class VideoSlideService {

    private Thread threadWatch = null;
    private static boolean bResetTimeToPlay = false;
    private static boolean bNowPlay = false;
    private static boolean bEnableMediaPlay = false;
    private static Context mContext;
    public final String TAG = "VideoService";

    public static String VIDEO_SLIDE_CHANGE_SETTINGS = "video_slide_change_settings";
    public static String VIDEO_SLIDE_RESET_TIME = "video_slide_reset_time";
    public static String VIDEO_STOP_PLAY = "video_stop_play";
    public static String SLIDE_STOP_PLAY = "slide_stop_play";
    public static String VIDEO_SLIDE_ENABLE = "video_slide_enable";

    private static int seekVideoPosition = 0;
    private static String VideoFileToContinuePlay = "";

    public VideoSlideService(Context context) {

        mContext = context;
        bResetTimeToPlay = false;
        bNowPlay = false;
        bEnableMediaPlay = false;

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(VIDEO_SLIDE_CHANGE_SETTINGS);
        intentFilter.addAction(VIDEO_SLIDE_RESET_TIME);
        intentFilter.addAction(VIDEO_STOP_PLAY);
        intentFilter.addAction(VIDEO_SLIDE_ENABLE);
        mContext.registerReceiver(VideoSlideEvents, intentFilter);
        // WatchVideoSlide();
        Log.d(TAG, "VideoSlideService START");

        //  MediaCodecInfo codecInfo = selectCodec("MIME");
        // codecInfo.getName()
    }

    BroadcastReceiver VideoSlideEvents = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(VIDEO_SLIDE_CHANGE_SETTINGS)) {
                if (PreferenceParams.getParameters().sCheckEnableVideo) {
                }
            } else if (intent.getAction().equals(VIDEO_SLIDE_RESET_TIME)) {
                bResetTimeToPlay = true;
                // bNowPlay = false;
            } else if (intent.getAction().equals(VIDEO_STOP_PLAY)) {
                //сохраним данные для восстановления воспроизведения видео
                Bundle mBundle = intent.getExtras();
                seekVideoPosition = mBundle.getInt("seekVideoPosition", 0);
                VideoFileToContinuePlay = mBundle.getString("VideoFileToContinuePlay", "");
                bNowPlay = false;
            } else if (intent.getAction().equals(SLIDE_STOP_PLAY)) {
                bNowPlay = false;
            } else if (intent.getAction().equals(VIDEO_SLIDE_ENABLE)) {
                Bundle mBundle = intent.getExtras();
                bEnableMediaPlay = mBundle.getBoolean("enable_video_slide", true);
                Log.d(TAG, "enable_video_slide: " + bEnableMediaPlay);
                if (bEnableMediaPlay)
                    watchVideoSlide();
            }
        }
    };


    private void watchVideoSlide() {

        threadWatch = new Thread(new Runnable() {
            @Override
            public void run() {
                // Log.d(TAG, "WatchVideoSlide start");
                int CurrentSourceForPlay = PreferenceParams.getParameters().sVideoOrSlide;
                long StopTime = System.nanoTime() + TimeUnit.SECONDS.toNanos(PreferenceParams.getParameters().videoTimeout);

                while (bEnableMediaPlay) {

                    if (!bEnableMediaPlay && bNowPlay) {
                        finishSlideAndVideoPlay();
                        Log.d(TAG, "#######2 ");
                        bNowPlay = false;
                        bResetTimeToPlay = false;
                        StopTime = System.nanoTime() + TimeUnit.SECONDS.toNanos(PreferenceParams.getParameters().videoTimeout);
                        continue;
                    }

                    if (!PreferenceParams.getParameters().sCheckEnableVideo) {
                        if (bNowPlay) {
                            finishSlideAndVideoPlay();
                            bNowPlay = false;
                            bResetTimeToPlay = false;
                        }
                        // Log.d(TAG, "#######2,1 ");
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        StopTime = System.nanoTime() + TimeUnit.SECONDS.toNanos(PreferenceParams.getParameters().videoTimeout);
                        continue;
                    }

                    while (StopTime > System.nanoTime()) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        // Log.d(TAG, "----#3,1");
                        //сброс таймера воспроизведения
                        if (bResetTimeToPlay) {
                            bResetTimeToPlay = false;
                            Log.d(TAG, "bResetTimeToPlay ");
                            finishSlideAndVideoPlay();
                            StopTime = System.nanoTime() + TimeUnit.SECONDS.toNanos(PreferenceParams.getParameters().videoTimeout);
                        }
                        if (!bEnableMediaPlay) {
                            bResetTimeToPlay = false;
                            Log.d(TAG, "bEnableMediaPlay is false");
                            finishSlideAndVideoPlay();
                            break;
                        }
                        if (PreferenceParams.getParameters().sVideoOrSlide != CurrentSourceForPlay) {
                            //  Log.d(TAG, "----#3,2");
                            StopTime = System.nanoTime() + TimeUnit.SECONDS.toNanos(PreferenceParams.getParameters().videoTimeout);
                            // Log.d(TAG, "#######1 ");
                            finishSlideAndVideoPlay();
                            bResetTimeToPlay = false;
                            bNowPlay = false;
                            break;
                        }
                        // Log.d(TAG, "----#4");
                    }
                    //Log.d(TAG, "----#4,1");
                    if (!PreferenceParams.getParameters().sCheckEnableVideo || !bEnableMediaPlay) {
                        StopTime = System.nanoTime() + TimeUnit.SECONDS.toNanos(PreferenceParams.getParameters().videoTimeout);
                        continue;
                    }
                    //Log.d(TAG, "----#5");
                    if (!bNowPlay) {
                        //  Log.d(TAG, "----#6");
                        //  Log.d(TAG, "воспроизведение видео");
                        if (PreferenceParams.getParameters().sVideoOrSlide == PreferenceParams._VIDEO) {
                            CurrentSourceForPlay = PreferenceParams._VIDEO;
                            //старт Видео
                            Handler handler = new Handler(Looper.getMainLooper());
                            handler.post(new Runnable() {
                                public void run() {
                                    //передадим данные для восстановления воспроизведения видео
                                    Intent intent = new Intent(mContext, VideoActivity.class);
                                    Bundle mBundle = new Bundle();
                                    mBundle.putInt("seekVideoPosition", seekVideoPosition);
                                    mBundle.putString("VideoFileToContinuePlay", VideoFileToContinuePlay);
                                    intent.putExtras(mBundle);
                                    mContext.startActivity(intent);
                                }
                            });
                            bNowPlay = true;
                        } else {
                            Log.d(TAG, "----#7");
                            CurrentSourceForPlay = PreferenceParams._SLIDE;
                            //старт Слайд шоу

                            Handler handler = new Handler(Looper.getMainLooper());
                            handler.post(new Runnable() {
                                public void run() {
                                    Intent intent = new Intent(mContext, SlideViewActivity.class);
                                    mContext.startActivity(intent);
                                }
                            });
                            bNowPlay = true;
                        }
                    } else {
                        // Log.d(TAG, "----#8");
                        if (PreferenceParams.getParameters().sVideoOrSlide != CurrentSourceForPlay) {
                            //   Log.d(TAG, "----#9");
                            CurrentSourceForPlay = -1;
                            bNowPlay = false;
                            Log.d(TAG, "*** остановлено текущее проигрывание");
                            finishSlideAndVideoPlay();
                            //bResetTimeToPlay=false;
                            StopTime = System.nanoTime() + TimeUnit.SECONDS.toNanos(PreferenceParams.getParameters().videoTimeout);
                            continue;
                        }
                    }
                    StopTime = System.nanoTime() + TimeUnit.SECONDS.toNanos(PreferenceParams.getParameters().videoTimeout);
                }
            }
        });
        threadWatch.start();
    }

    private void finishSlideAndVideoPlay() {
        Log.w(TAG, "FinishSlideAndVideoPlay...");
        Intent intent = new Intent(SlideViewActivity.FINISH_ALERT);
        mContext.sendBroadcast(intent);
        bNowPlay = false;
    }

    private MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        // Log.d(TAG, "MediaCodec count :"+numCodecs);

        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (codecInfo.isEncoder()) {
                continue;
            }

            // MediaCodecInfo.CodecCapabilities cap = codecInfo.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC);

            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].contains("video/"))
                    Log.d(TAG, "codec: " + codecInfo.getName() + ", type:" + types[j]);


                // if (types[j].equalsIgnoreCase() {
                //  Log.d(TAG, "codecInfo:"+codecInfo.getName()+" is encoder:"+codecInfo.isEncoder());
                //  return codecInfo;
                // }
            }
        }
        return null;
    }
}
