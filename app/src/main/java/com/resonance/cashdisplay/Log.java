package com.resonance.cashdisplay;

import android.os.Environment;

import com.crashlytics.android.Crashlytics;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Log {
    public static final String LOG_FILE_PREFIX = "CashDisplay_";

    private static final String NEW_LINE = System.getProperty("line.separator");
    private static boolean mLogcatAppender = true;
    private static File mLogFile;

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

    private static final int DAYS_KEEP_ZIP = 14;
    private static final int DAYS_CLEAR_HISTORY = 90;       // clear logs (zips) per last days

    public Log() {
        logFileCreator();

        new Thread(() -> {
            try {
                Thread.sleep(30000);    // need some time for app start (zipping loads CPU)
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            logFileCleaner(logFileCreator());
        }).start();

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 24);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date time = calendar.getTime();

        TimerTask dayTimerTask = new TimerTask() {
            @Override
            public void run() {
                logFileCleaner(logFileCreator());
            }
        };

        new Timer().scheduleAtFixedRate(dayTimerTask, time, 1000 * 60 * 60 * 24);
    }

    /**
     * Creates new log file for current date, if it not exists yet.
     */
    private synchronized Date logFileCreator() {
        Date currentDate = new Date();
        mLogFile = new File(Environment.getExternalStorageDirectory(), LOG_FILE_PREFIX + dateFormat.format(currentDate) + ".log");

        if (!mLogFile.exists()) {
            try {
                mLogFile.createNewFile();
            } catch (final IOException e) {
                e.printStackTrace();
            }
            logDeviceInfo();
        }
        return currentDate;
    }

    /**
     * Packs previous log files into zip, then deletes logs.
     */
    private synchronized void logFileCleaner(Date currentDate) {
        Calendar c = Calendar.getInstance();
        c.setTime(currentDate);

        // handle 1-14 days history logs
        for (int i = 0; i < DAYS_KEEP_ZIP; i++) {
            c.add(Calendar.DATE, -1);
            Date oldLogFileDate = c.getTime();
            File oldLogFile = new File(Environment.getExternalStorageDirectory(), LOG_FILE_PREFIX + dateFormat.format(oldLogFileDate) + ".log");
            if (oldLogFile.exists()) {
                String oldLogFilePath = oldLogFile.toString();
                String oldLogFilePathZip = oldLogFilePath.replace("log", "zip");
                try {
                    zip(new String[]{oldLogFilePath}, oldLogFilePathZip);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                oldLogFile.delete();
            }
        }

        // handle 14-90 days history logs
        for (int i = 0; i < (DAYS_CLEAR_HISTORY - DAYS_KEEP_ZIP); i++) {
            c.add(Calendar.DATE, -1);
            Date oldFileDate = c.getTime();
            File oldLogFile = new File(Environment.getExternalStorageDirectory(), LOG_FILE_PREFIX + dateFormat.format(oldFileDate) + ".log");
            if (oldLogFile.exists()) {          // check for any case
                oldLogFile.delete();
            }
            File oldZipFile = new File(Environment.getExternalStorageDirectory(), LOG_FILE_PREFIX + dateFormat.format(oldFileDate) + ".zip");
            if (oldZipFile.exists()) {
                oldZipFile.delete();
            }
        }
    }

    /**
     * Packs current log file into zip, not delete current log. Used to get current log right now.
     */
    public static void zipCurrentFile() {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());

        Date currentLogFileDate = c.getTime();
        File currentLogFile = new File(Environment.getExternalStorageDirectory(), LOG_FILE_PREFIX + dateFormat.format(currentLogFileDate) + ".log");
        if (currentLogFile.exists()) {
            String currentLogFilePath = currentLogFile.toString();
            String currentLogFilePathZip = currentLogFilePath.replace("log", "zip");
            try {
                zip(new String[]{currentLogFilePath}, currentLogFilePathZip);
            } catch (IOException e) {
                e.printStackTrace();
            }
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

    public static void zip(String[] files, String zipFile) throws IOException {
        int bufferSize = 1024;
        BufferedInputStream origin = null;
        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
        try {
            byte data[] = new byte[bufferSize];

            for (int i = 0; i < files.length; i++) {
                FileInputStream fi = new FileInputStream(files[i]);
                origin = new BufferedInputStream(fi, bufferSize);
                try {
                    ZipEntry entry = new ZipEntry(files[i].substring(files[i].lastIndexOf("/") + 1));
                    out.putNextEntry(entry);
                    int count;
                    while ((count = origin.read(data, 0, bufferSize)) != -1) {
                        out.write(data, 0, count);
                    }
                } finally {
                    origin.close();
                }
            }
        } finally {
            out.close();
        }
    }
}