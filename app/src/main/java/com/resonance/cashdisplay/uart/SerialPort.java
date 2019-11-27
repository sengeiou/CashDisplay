/*
 * Copyright 2009 Cedric Priscal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.resonance.cashdisplay.uart;

import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Класс обслуживания UART
 */
public class SerialPort {

    private static final String TAG = "SerialPort";

    private static final String DEFAULT_SU_PATH = "/system/xbin/su";  // originally was "/system/bin/su"

    private static String sSuPath = DEFAULT_SU_PATH;

    /**
     * Set the su binary path, the default su binary path is {@link #DEFAULT_SU_PATH}
     *
     * @param suPath su binary path
     */
    public static void setSuPath(String suPath) {
        if (suPath == null) {
            return;
        }
        sSuPath = suPath;
    }

    /**
     * Get the su binary path
     *
     * @return
     */
    public static String getSuPath() {
        return sSuPath;
    }

    /*
     * Do not remove or rename the field mFd: it is used by native method close();
     */
    private FileDescriptor mFd;
    private FileInputStream mFileInputStream;
    private FileOutputStream mFileOutputStream;

    /**
     * Serial port
     *
     * @param device   Serial device file
     * @param baudrate Baud rate
     * @param dataBits Data bits, default 8
     * @param parity   Parity bit, default 0 (no parity)
     * @param stopBits Stop bit, default 1
     * @param flags    Default 0
     * @throws SecurityException
     * @throws IOException
     */
    private SerialPort(File device, int baudrate, int dataBits, int parity, int stopBits, int flags)
            throws SecurityException, IOException {

//        /* Check access permission */
//        if (!device.canRead() || !device.canWrite()) {
//            try {
//                /* Missing read/write permission, trying to chmod the file */
//                Process su;
//                su = Runtime.getRuntime().exec(sSuPath);
//                String cmd = "chmod 666 " + device.getAbsolutePath() + "\n" + "exit\n";
//                su.getOutputStream().write(cmd.getBytes());
//                if ((su.waitFor() != 0) || !device.canRead() || !device.canWrite()) {
//                    throw new SecurityException();
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//                throw new SecurityException();
//            }
//        }
        mFd = open(device.getAbsolutePath(), baudrate, dataBits, parity, stopBits, flags);
        if (mFd == null) {
            Log.e(TAG, "native open returns null");
            throw new IOException();
        }
        mFileInputStream = new FileInputStream(mFd);
        mFileOutputStream = new FileOutputStream(mFd);
    }

    // Getters and setters
    public InputStream getInputStream() {
        return mFileInputStream;
    }

    public OutputStream getOutputStream() {
        return mFileOutputStream;
    }

    // JNI
    private native FileDescriptor open(String absolutePath, int baudrate, int dataBits, int parity,
                                       int stopBits, int flags);

    public native void close();

    static {
        System.loadLibrary("serial_port");
    }


    public static Builder newBuilder(File device, int baudrate) {
        return new Builder(device, baudrate);
    }

    public static Builder newBuilder(String devicePath, int baudrate) {
        return new Builder(devicePath, baudrate);
    }

    public final static class Builder {

        private File device;
        private int baudrate;
        private int dataBits = 8;
        private int parity = 0;
        private int stopBits = 1;
        private int flags = 0;

        private Builder(File device, int baudrate) {
            this.device = device;
            this.baudrate = baudrate;
        }

        private Builder(String devicePath, int baudrate) {
            this(new File(devicePath), baudrate);
        }

        /**
         * Data bit
         *
         * @param dataBits The default value is 8, the selectable value is 5 ~ 8
         * @return
         */
        public Builder dataBits(int dataBits) {
            this.dataBits = dataBits;
            return this;
        }

        /**
         * Check Digit
         *
         * @param parity 0: No parity bit (NONE, default); 1: Odd parity bit (ODD); 2: Even parity bit (EVEN)
         * @return
         */
        public Builder parity(int parity) {
            this.parity = parity;
            return this;
        }

        /**
         * Stop bit
         *
         * @param stopBits Default 1: 1: 1 stop bit; 2: 2 stop bit
         * @return
         */
        public Builder stopBits(int stopBits) {
            this.stopBits = stopBits;
            return this;
        }

        /**
         * Sign
         *
         * @param flags Default 0
         * @return
         */
        public Builder flags(int flags) {
            this.flags = flags;
            return this;
        }

        /**
         * Open and return to the serial port
         *
         * @return
         * @throws SecurityException
         * @throws IOException
         */
        public SerialPort build() throws SecurityException, IOException {
            return new SerialPort(device, baudrate, dataBits, parity, stopBits, flags);
        }
    }

}
