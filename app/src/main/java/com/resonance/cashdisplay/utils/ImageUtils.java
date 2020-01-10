package com.resonance.cashdisplay.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;

import com.resonance.cashdisplay.Log;
import com.resonance.cashdisplay.MainActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class ImageUtils {

    public enum RequestSizeOptions {
        RESIZE_FIT,
        RESIZE_INSIDE,
        RESIZE_EXACT
    }

    private final static String TAG = "ImageUtils";

    public ImageUtils() {
    }

    public static Bitmap getImage(File fileImg, Point sizeScreen, boolean fitToWidth) {

        BitmapFactory.Options options;
        options = new BitmapFactory.Options();
        options.inDither = false;
        options.inJustDecodeBounds = false;
        options.inSampleSize = 1;
        options.inScaled = false;
        options.mCancel = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        Bitmap bmp = null;
        try {
            bmp = BitmapFactory.decodeFile(fileImg.getPath(), options);

            if (fitToWidth)
                bmp = scaleToFitWidth(bmp, sizeScreen.x);

        } catch (OutOfMemoryError outOfMemoryError) {
            Log.e(TAG, "" + outOfMemoryError.getMessage() + " " + outOfMemoryError);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "" + e.getMessage());
            return null;
        }
        return bmp;
    }

    public static Bitmap scaleToFitWidth(Bitmap b, int width) {
        float factor = width / (float) b.getWidth();
        return Bitmap.createScaledBitmap(b, width, (int) (b.getHeight() * factor), true);
    }

    /**
     * Resize the given bitmap to the given width/height by the given option.<br>
     */
    public static Bitmap resizeBitmap(Bitmap bitmap, int reqWidth, int reqHeight, RequestSizeOptions options) {
        //масштабируем изображение
        try {
            if (reqWidth > 0 && reqHeight > 0 && (options == RequestSizeOptions.RESIZE_FIT ||
                    options == RequestSizeOptions.RESIZE_INSIDE ||
                    options == RequestSizeOptions.RESIZE_EXACT)) {

                Bitmap resized = null;
                if (options == RequestSizeOptions.RESIZE_FIT) {
                    resized = Bitmap.createScaledBitmap(bitmap, reqWidth, reqHeight, false);
                } else {
                    int width = bitmap.getWidth();
                    int height = bitmap.getHeight();
                    float scale = Math.max(width / (float) reqWidth, height / (float) reqHeight);
                    if (scale > 1 || options == RequestSizeOptions.RESIZE_INSIDE) {
                        resized = Bitmap.createScaledBitmap(bitmap, (int) (width / scale), (int) (height / scale), false);
                    }
                }
                if (resized != null) {
                    if (resized != bitmap) {
                        bitmap.recycle();
                    }
                    return resized;
                }
            }
        } catch (OutOfMemoryError outOfMemoryError) {
            Log.e(TAG, "" + outOfMemoryError.getMessage() + " " + outOfMemoryError);
            return null;
        } catch (Exception e) {
            Log.w(TAG, "Failed to resize cropped image, return bitmap before resize:" + e);
        }
        return bitmap;
    }

    public static void convertBitmapToScreenSize(String pathToImgFile) {

        OutputStream outStream = null;
        File fileImg = new File(pathToImgFile);
        if (!fileImg.exists()) return;

        Bitmap bitmap = getImage(fileImg, MainActivity.sizeScreen, false);

        if ((bitmap.getHeight() != MainActivity.sizeScreen.y) || (bitmap.getWidth() != MainActivity.sizeScreen.x)) {
            try {
                Bitmap OutBitmap = ImageUtils.resizeBitmap(bitmap, MainActivity.sizeScreen.x, MainActivity.sizeScreen.y, ImageUtils.RequestSizeOptions.RESIZE_FIT);

                fileImg.delete();
                fileImg = new File(pathToImgFile);

                outStream = new FileOutputStream(fileImg);
                OutBitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
                outStream.flush();
                outStream.close();
                Log.d(TAG, ">> Convert image: " + pathToImgFile);
            } catch (OutOfMemoryError outOfMemoryError) {
                Log.e(TAG, "" + outOfMemoryError.getMessage() + " " + outOfMemoryError);

            } catch (Exception e) {
                Log.e(TAG, "Failed to resize cropped image, return bitmap before resize:" + e);
                e.printStackTrace();
            }
        }
    }

    public static void freeMemory() {
        System.runFinalization();
        Runtime.getRuntime().gc();
        System.gc();
    }
}
