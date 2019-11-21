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

public class Log {
    private static final String NEW_LINE = System.getProperty("line.separator");
    private static boolean mLogcatAppender = true;
    private static final File mLogFile;

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

    static {
        Date currentDate = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(currentDate);

        c.add(Calendar.DATE, -4);

        Date currentDatePlusOne = c.getTime();

        System.out.println(dateFormat.format(currentDatePlusOne));
        File oldLogFile = new File(Environment.getExternalStorageDirectory(), "CashDisplay_" + dateFormat.format(currentDatePlusOne) + ".log");
        if (oldLogFile.exists()) {
            oldLogFile.delete();
        }

        c = Calendar.getInstance();
        c.setTime(currentDate);
        currentDatePlusOne = c.getTime();

        mLogFile = new File(Environment.getExternalStorageDirectory(), "CashDisplay_" + dateFormat.format(currentDatePlusOne) + ".log");

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
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
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