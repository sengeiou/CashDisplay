package com.resonance.cashdisplay.sound;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;

import com.resonance.cashdisplay.Log;
import com.resonance.cashdisplay.MainActivity;

import java.io.IOException;

//import android.util.Log;


/**
 * Проигрывание звука и установка громкости
 */

public class Sound implements MediaPlayer.OnCompletionListener {

    public final static String START_VOICE = "mp3/soft-bells.mp3";
    public final static String WARNING_VOICE = "mp3/demonstrative.mp3";

    private final String TAG = "Sound";
    private Context mContext;
    private MediaPlayer player = null;

    public Sound(Context context) {
        mContext = context;
    }

    public void playSound(String filename) {
        AssetFileDescriptor afd = null;

        try {
            Log.d(TAG, "PlaySound");
            afd = mContext.getResources().getAssets().openFd(filename);
            player = new MediaPlayer();

            assert afd != null;
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            player.setOnCompletionListener(this);
            player.setVolume(1f, 1f);

            player.prepare();
            player.start();
            Log.d(TAG, "PlaySound start");
        } catch (IOException e) {
            Log.e(TAG, "IOException PlaySound: " + e.getMessage());
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (player != null) {
            player.stop();
            player.release();
            Log.d(TAG, "PlaySound stop");
        }
    }

    public static void setVolume(int percentVolume) {
        AudioManager mAudioManager = (AudioManager) MainActivity.context.getSystemService(Context.AUDIO_SERVICE);

        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, Math.round(percentVolume * maxVolume / 100), 0);
    }
}
