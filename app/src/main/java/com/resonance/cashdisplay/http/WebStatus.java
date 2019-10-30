package com.resonance.cashdisplay.http;

import android.os.Handler;
import android.os.Message;

import com.resonance.cashdisplay.Log;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

//import android.util.Log;


public class WebStatus {

    public static final int SEND_TO_QUEUE_WEB_MESSAGE = 1;
    public static final int CLEAR_QUEUE_WEB_MESSAGE = 2;

    public static final String TAG = "WebStatus";

    private final int SIZE_MESSAGE_QUEUE = 50;
    private BlockingQueue<String> smbMessageQueue;

    public WebStatus() {
        smbMessageQueue = new ArrayBlockingQueue<String>(SIZE_MESSAGE_QUEUE);
        smbMessageQueue.clear();
    }

    public Handler getWebMessageHandler() {
        return webMessageHandler;
    }

    private final Handler webMessageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SEND_TO_QUEUE_WEB_MESSAGE:
                    if (msg.arg1 == CLEAR_QUEUE_WEB_MESSAGE)
                        setEmptyWebStatus();
                    sendWebStatus((String) msg.obj);
                    break;
            }
        }
    };

    private synchronized void sendWebStatus(String msg) {
        if (smbMessageQueue.remainingCapacity() > 0) {
            Log.w(TAG, "SendWebStatus: " + msg);
            smbMessageQueue.add(msg);
        }
    }

    private synchronized void setEmptyWebStatus() {
        smbMessageQueue.clear();
    }

    public synchronized String getStrStatus() {
        String msg = "";
        if (!smbMessageQueue.isEmpty()) {
            try {
                msg = smbMessageQueue.take();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return msg;
    }
}