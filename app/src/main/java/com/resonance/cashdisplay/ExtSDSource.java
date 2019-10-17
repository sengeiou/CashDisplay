package com.resonance.cashdisplay;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.os.Environment;
import android.os.RemoteException;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
//import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by Святослав on 25.04.2016.
 */
public class ExtSDSource {


    public static final String DEFAULT_SD = "/storage/sdcard2";

    public ExtSDSource(){

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
        for (int i=0;i<paths.length;i++){
            if (paths[i].contains(DEFAULT_SD)) {
                path =paths[i];
                break;
            }
        }
        if (path.length()==0)
            return -1;
        try {
            Method method = managerStorage.getClass().getDeclaredMethod("getVolumeState", new Class[]{String.class});
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

        return (isSdcardMounted(context)==1?true:false);
    }


    public static boolean isReadOnly() {
        String state = Environment.getExternalStorageState();

        return (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) ;
    }


    public static String getExternalSdCardPath() {
        String path = null;

        File sdCardFile = null;
        List<String> sdCardPossiblePath = Arrays.asList("external_sd", "ext_sd", "external", "extSdCard","storage/sdcard2", "storage/sdcard1");

        for (String sdPath : sdCardPossiblePath) {
            // File file = new File("/mnt/"+sdPath);
            File file = new File("/"+sdPath);
            if (file.isDirectory() && file.canWrite()) {
                path = file.getAbsolutePath();

                String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmmss").format(new Date());
                File testWritable = new File(path, "test_" + timeStamp);

                if (testWritable.mkdirs()) {
                    testWritable.delete();

                }
                else {
                    path = null;
                }
            }
        }

        if (path != null) {
            sdCardFile = new File(path);
        }
        else {
            sdCardFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
        }

        return sdCardFile.getAbsolutePath();
    }



    private static String formatSize(long sz) {
        String suffix = null;

        float size = (float)sz;

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

        return formatSize(getAvailableMemory(MainActivity.mContext, DEFAULT_SD));

    }

    public static long getAvailableMemory(Context context, String storage) {

        String path = "";
        String[] paths = getVolumePaths(context);

        for (int i=0;i<paths.length;i++)
        {
            if (paths[i].contains(storage))
            {
                path =paths[i];
                break;
            }
        }
        if (path.length()==0)
            return 0;

        StatFs stat = new StatFs(path);
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        return (availableBlocks * blockSize);
    }





}
