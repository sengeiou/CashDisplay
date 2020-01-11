package com.resonance.cashdisplay;

import android.os.Environment;

import com.crashlytics.android.Crashlytics;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class Log {
    public static final String LOG_FILE_PREFIX = "CashDisplay_";

    private static final String NEW_LINE = System.getProperty("line.separator");
    private static boolean mLogcatAppender = true;
    private static File mLogFile;

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

    static {
        logFileProvider();

        TimerTask dayTimerTask = new TimerTask() {
            @Override
            public void run() {
                logFileProvider();
            }
        };

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 24);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 10);
        Date date = calendar.getTime();

        new Timer().scheduleAtFixedRate(dayTimerTask, date, 1000 * 60 * 60 * 24);
    }

    /**
     * Saves 15 last log files including present day. Deletes 60 log files that comes before this 15 days.
     */
    private static void logFileProvider() {
        Date currentDate = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(currentDate);
        c.add(Calendar.DATE, -14);

        for (int i = 1; i < 61; i++) {
            c.add(Calendar.DATE, -1);
            Date oldLogFileDate = c.getTime();
            File oldLogFile = new File(Environment.getExternalStorageDirectory(), LOG_FILE_PREFIX + dateFormat.format(oldLogFileDate) + ".log");
            if (oldLogFile.exists()) {
                oldLogFile.delete();
            }
        }

        mLogFile = new File(Environment.getExternalStorageDirectory(), LOG_FILE_PREFIX + dateFormat.format(currentDate) + ".log");

        if (!mLogFile.exists()) {
            try {
                mLogFile.createNewFile();
            } catch (final IOException e) {
                e.printStackTrace();
            }
            logDeviceInfo();
        }
    }

    public static void i(String TAG, String message) {
        appendLog(TAG + " : " + message);
        if (mLogcatAppender) {
            Crashlytics.log(android.util.Log.INFO, TAG, message);
        }
    }

    public static void d(String TAG, String message) {
        appendLog(TAG + " : " + message);
        if (mLogcatAppender) {
            Crashlytics.log(android.util.Log.DEBUG, TAG, message);
        }
    }

    public static void e(String TAG, String message) {
        appendLog(TAG + " : " + message);
        if (mLogcatAppender) {
            Crashlytics.log(android.util.Log.ERROR, TAG, message);
        }
    }

    public static void v(String TAG, String message) {
        appendLog(TAG + " : " + message);
        if (mLogcatAppender) {
            Crashlytics.log(android.util.Log.VERBOSE, TAG, message);
        }
    }

    public static void w(String TAG, String message) {
        appendLog(TAG + " : " + message);
        if (mLogcatAppender) {
            Crashlytics.log(android.util.Log.WARN, TAG, message);
        }
    }

    private static synchronized void appendLog(String text) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        try {
            final FileWriter fileOut = new FileWriter(mLogFile, true);
            fileOut.append(sdf.format(new Date()) + " : " + text + NEW_LINE);
            fileOut.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    private static void logDeviceInfo() {
        appendLog("Model : " + android.os.Build.MODEL);
        appendLog("Brand : " + android.os.Build.BRAND);
        appendLog("Product : " + android.os.Build.PRODUCT);
        appendLog("Device : " + android.os.Build.DEVICE);
        appendLog("Codename : " + android.os.Build.VERSION.CODENAME);
        appendLog("Release : " + android.os.Build.VERSION.RELEASE);
    }
}