package com.resonance.cashdisplay.http;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.resonance.cashdisplay.MainActivity;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class html_Helper {

    private static final String TAG = "html_Helper";

    public static String urlDecode(final String s) {
        String decoded;
        try {
            decoded = URLDecoder.decode(s, "UTF-8");
        } catch (Exception ignored) {
            //noinspection deprecation
            decoded = URLDecoder.decode(s);
        }
        return decoded;
    }

    public static String urlEncode(final String s) {
        String encoded;
        try {
            encoded = URLEncoder.encode(s, "UTF-8");
        } catch (Exception ignored) {
            //noinspection deprecation
            encoded = URLEncoder.encode(s);
        }
        return encoded;
    }

    @NonNull
    private static String cleanupPath(final String path) {
        if (TextUtils.isEmpty(path)) {
            return "";
        }
        if (path.startsWith("./")) {
            return path.replaceFirst("./", "");
        }
        if (path.startsWith("/")) {
            return path.replaceFirst("/", "");
        }
        return path;
    }

    @Nullable
    public static InputStream loadPath(String path) {
        path = cleanupPath(path);
        return loadPathInternal(path);
    }

    @NonNull public static String loadPathAsString(String path) {
        path = cleanupPath(path);
        return loadPathAsStringInternal(path);
    }

    @Nullable private static InputStream loadPathInternal(final String path) {
        try {
            return MainActivity.mContext.getAssets().open(path);
        } catch (Exception exc) {
            Log.e(TAG, "loadPathInternal",exc);
        }
        return null;
    }

    @NonNull private static String loadPathAsStringInternal(final String path) {
        try {
            Log.i( TAG,"try to load  file from asset:"+path);
            return loadFromAssets(path);
        } catch (Exception exc) {
            Log.e( TAG,"loadPathAsStringInternal",exc);
        }
        return "";
    }

    private static String loadFromAssets(final String path) throws Exception {
        final StringBuilder sb = new StringBuilder();

        InputStream htmlStream = null;
        InputStreamReader reader = null;
        BufferedReader br = null;
        try {
            htmlStream = MainActivity.mContext.getAssets().open(path);
            reader = new InputStreamReader(htmlStream);
            br = new BufferedReader(reader);
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } finally {
            closeQuietly(br);
            closeQuietly(reader);
            closeQuietly(htmlStream);
        }

        return sb.toString();
    }
    private static void closeQuietly(final Object closeable) {
        if (closeable instanceof Flushable) {
            try {
                ((Flushable) closeable).flush();
            } catch (IOException ignored) { }
        }
        if (closeable instanceof Closeable) {
            try {
                ((Closeable) closeable).close();
            } catch (IOException ignored) { }
        }
    }

}
