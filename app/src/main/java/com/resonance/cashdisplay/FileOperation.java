package com.resonance.cashdisplay;

//import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by Святослав on 21.09.2016.
 */
public class FileOperation {

    public static final String TAG = "FileOperation";
    final static protected char[] hexArray = "0123456789ABCDEF".toCharArray();

    public boolean createFile(String path) {
        boolean result = true;
        try {
            FileOutputStream fos = new FileOutputStream(path);
        } catch (IOException ex) {
            Log.e(TAG, "Ошибка создания файла: " + path);
            result = false;
        }
        ;
        return result;
    }

    public static boolean writeToPosition(String filename, byte[] buffer, long position, int offset, int len) throws IOException {
        boolean result = false;
        try {
            RandomAccessFile writer = new RandomAccessFile(filename, "rw");
            try {
                writer.seek(position);
                writer.write(buffer, offset, len);
                result = true;
            } finally {
                writer.close();
            }
        } catch (FileNotFoundException ex) {
            Log.e(TAG, "File " + filename + " - not found.");
            result = false;
        } catch (IOException ex) {
            Log.e(TAG, ex.getMessage());
            result = false;
        }
        return result;
    }

    public static String getHashFile(String location) throws IOException {
        String md5 = "";
        InputStream is = new FileInputStream(location);
        try {
            byte[] bytes = new byte[4096];
            int read = 0;
            MessageDigest digest = MessageDigest.getInstance("MD5");

            while ((read = is.read(bytes)) != -1) {
                digest.update(bytes, 0, read);
            }

            byte[] messageDigest = digest.digest();

            StringBuilder sb = new StringBuilder(32);

            for (byte b : messageDigest) {
                sb.append(hexArray[(b >> 4) & 0x0f]);
                sb.append(hexArray[b & 0x0f]);
            }
            md5 = sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return md5;
    }

    public static String getFileExtension(String fileName) {
        String extension = "";

        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i + 1);
        }
        return extension;
    }

    public static String getFileName(String fileName) {
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }


    public String calcMD5qwe(String location) {
        MessageDigest md = null;
        FileInputStream fis = null;
        byte[] dataBytes = new byte[1024];
        try {
            md = MessageDigest.getInstance("MD5");
            fis = new FileInputStream(location);


        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            int nread = 0;
            while ((nread = fis.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, nread);
            }
            ;
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte[] mdbytes = md.digest();

        //convert the byte to hex format method 1
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < mdbytes.length; i++) {
            sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
        }

        Log.d(TAG, "Digest(in hex format):: " + sb.toString());

        //convert the byte to hex format method 2
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < mdbytes.length; i++) {
            String hex = Integer.toHexString(0xff & mdbytes[i]);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        Log.d(TAG, "Digest(in hex format):: " + hexString.toString());
        return hexString.toString();
    }


    public static class FileExtensionFilter implements FilenameFilter {
        private Set<String> exts = new HashSet<String>();

        /**
         * @param extensions a list of allowed extensions, without the dot, e.g.
         *                   <code>"xml","html","rss"</code>
         */
        public FileExtensionFilter(String... extensions) {
            for (String ext : extensions) {
                exts.add("." + ext.toLowerCase().trim());
            }
        }

        public boolean accept(File dir, String name) {
            final Iterator<String> extList = exts.iterator();
            while (extList.hasNext()) {
                if (name.toLowerCase().endsWith(extList.next())) {
                    return true;
                }
            }
            return false;
        }
    }

    public static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();
    }

    public static boolean isFilelocked(File file) {

        return !file.canWrite();

    }

    public static void lockFile(String filepath, boolean state) {

        File videofile = new File(filepath);
        if (videofile.exists()) {

            videofile.setWritable(state);
        }
    }

    public static void deleteFile(String pathToFile) {

        File fdelete = new File(pathToFile);

        if (fdelete.exists()) {
            if (!isFilelocked(fdelete)) {
                if (fdelete.delete()) {
                    Log.d(TAG, "file deleted: " + pathToFile);
                }
            } else {
                Log.d(TAG, "file locked: " + pathToFile);
            }
        }
    }
}
