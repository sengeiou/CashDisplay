package com.resonance.cashdisplay;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;

import androidx.core.content.ContextCompat;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;

/**
 * Created by Святослав on 25.04.2016.
 */
public class ExtSDSource {


    public static final String DEFAULT_SD = "/storage/sdcard2";

    public ExtSDSource() {

    }

    private static String[] getVolumePaths(Context context) {
        String[] volumes = null;
        StorageManager managerStorage = (StorageManager) context.getSystemService("storage");
        if (managerStorage == null) {
            return volumes;
        }
        try {
            return (String[]) managerStorage.getClass().getMethod("getVolumePaths", new Class[0]).invoke(managerStorage, new Object[0]);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return volumes;
        } catch (IllegalArgumentException e2) {
            e2.printStackTrace();
            return volumes;
        } catch (IllegalAccessException e3) {
            e3.printStackTrace();
            return volumes;
        } catch (InvocationTargetException e4) {
            e4.printStackTrace();
            return volumes;
        }
    }

    private static int isSdcardMounted(Context context) {
        StorageManager managerStorage = (StorageManager) context.getSystemService("storage");
        if (managerStorage == null) {
            return -1;
        }

        String path = "";
        String[] paths = getVolumePaths(context);
        for (int i = 0; i < paths.length; i++) {
            if (paths[i].contains(DEFAULT_SD)) {
                path = paths[i];
                break;
            }
        }
        if (path.length() == 0)
            return -1;
        try {
            Method method = managerStorage.getClass().getDeclaredMethod("getVolumeState", String.class);
            method.setAccessible(true);
            String tmp = (String) method.invoke(managerStorage, new Object[]{path});
            if ("mounted".equalsIgnoreCase((String) method.invoke(managerStorage, new Object[]{path}))) {
                return 1;
            }
            return 0;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return -1;
        } catch (IllegalArgumentException e2) {
            e2.printStackTrace();
            return -1;
        } catch (IllegalAccessException e3) {
            e3.printStackTrace();
            return -1;
        } catch (InvocationTargetException e4) {
            e4.printStackTrace();
            return -1;
        }
    }

    public static boolean isMounted(Context context) {
        return (isSdcardMounted(context) == 1);
    }

    public static boolean isReadOnly() {
        String state = Environment.getExternalStorageState();
        return (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state));
    }

    public static String getExternalSdCardPath(Context context) {
        String externalSdPath = null;
        Boolean isSDPresent = android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
        if (isSDPresent) {
            File[] storages = ContextCompat.getExternalFilesDirs(context, null);
            for (File sdCardFile : storages) {
                if (sdCardFile != null && !sdCardFile.toString().contains("emulated")) {
                    externalSdPath = sdCardFile.getPath().replaceFirst("\\/Android.*", "");
                }
            }
        }
        if (externalSdPath == null)
            externalSdPath = Environment.getExternalStorageDirectory().getAbsolutePath();

        return externalSdPath;
    }

    private static String formatSize(long sz) {
        String suffix = null;

        float size = (float) sz;

        if (size >= 1024) {
            suffix = "KB";
            size /= 1024;
            if (size >= 1024) {
                suffix = "MB";
                size /= 1024;

                if (size >= 1024) {
                    suffix = "ГB";
                    size /= 1024;
                }
            }
        }

        DecimalFormat df = new DecimalFormat("#.##");
        df.format(size);
        StringBuilder resultBuffer = new StringBuilder(df.format(size));

        if (suffix != null) resultBuffer.append(suffix);
        return resultBuffer.toString();
    }

    public static String getAvailableMemory_SD() {

        return formatSize(getAvailableMemory(MainActivity.context, DEFAULT_SD));
    }

    public static long getAvailableMemory(Context context, String storage) {

        String path = "";
        String[] paths = getVolumePaths(context);

        for (int i = 0; i < paths.length; i++) {
            if (paths[i].contains(storage)) {
                path = paths[i];
                break;
            }
        }
        if (path.length() == 0)
            return 0;

        StatFs stat = new StatFs(path);
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        return (availableBlocks * blockSize);
    }
}
