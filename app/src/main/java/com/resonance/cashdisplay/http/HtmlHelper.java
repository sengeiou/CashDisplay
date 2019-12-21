package com.resonance.cashdisplay.http;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.resonance.cashdisplay.MainActivity;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileReader;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class HtmlHelper {

    private static final String TAG = "HtmlHelper";

    public static String urlDecode(String s) {
        String decoded;
        try {
            decoded = URLDecoder.decode(s, "UTF-8");
        } catch (Exception ignored) {
            //noinspection deprecation
            decoded = URLDecoder.decode(s);
        }
        return decoded;
    }

    public static String urlEncode(String s) {
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
    private static String cleanupPath(String path) {
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

    @NonNull
    public static String loadFileAsString(String path) {
        path = cleanupPath(path);
        return loadFileAsStringInternal(path);
    }

    @Nullable
    private static InputStream loadPathInternal(String path) {
        try {
            return MainActivity.context.getAssets().open(path);
        } catch (Exception exc) {
            Log.e(TAG, "loadPathInternal", exc);
        }
        return null;
    }

    @NonNull
    private static String loadFileAsStringInternal(String path) {
        try {
            if (path.contains("storage"))
                return loadFileFromStorage(path);
            else
                return loadFromAssets(path);
        } catch (Exception exc) {
            Log.e(TAG, "loadPathAsStringInternal", exc);
        }
        return "";
    }

    @NonNull
    private static String loadFileFromStorage(String path) {
        StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                contentBuilder.append(sCurrentLine).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return contentBuilder.toString();
    }

    private static String loadFromAssets(String path) throws Exception {
        final StringBuilder sb = new StringBuilder();

        InputStream htmlStream = null;
        InputStreamReader reader = null;
        BufferedReader br = null;
        try {
            htmlStream = MainActivity.context.getAssets().open(path);
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
            } catch (IOException ignored) {
            }
        }
        if (closeable instanceof Closeable) {
            try {
                ((Closeable) closeable).close();
            } catch (IOException ignored) {
            }
        }
    }
}
