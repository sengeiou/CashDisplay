package com.resonance.cashdisplay.web;

import android.os.Handler;
import android.os.Message;
//import android.util.Log;

import com.resonance.cashdisplay.Log;
import com.resonance.cashdisplay.uart.UartWorker;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;



public class WebStatus {

    public static final int SEND_TO_QUEUE_WEB_MESSAGE = 1;
    public static final int CLEAR_QUEUE_WEB_MESSAGE = 2;

    public static final String TAG = "WebStatus";

    private final int SIZE_MESSAGE_QUEUE = 50;
    private static BlockingQueue<String> Smb_messageQueue ;

    public WebStatus(){
        Smb_messageQueue = new ArrayBlockingQueue<String>(SIZE_MESSAGE_QUEUE);
        Smb_messageQueue.clear();
    }

    public Handler getWeb_message_handler() {
        return web_message_handler;
    }

    private final Handler web_message_handler = new Handler() {


        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SEND_TO_QUEUE_WEB_MESSAGE:
                    if (msg.arg1==CLEAR_QUEUE_WEB_MESSAGE)
                        SetEmptyWebStatus();
                    SendWebStatus((String)msg.obj);
                    break;

            }
        }
    };

    private synchronized void SendWebStatus(String msg){

        if (Smb_messageQueue.remainingCapacity()>0) {
            Log.w(TAG, "SendWebStatus:"+msg);
            Smb_messageQueue.add(msg);
        }
    }

    private synchronized void SetEmptyWebStatus(){
        Smb_messageQueue.clear();
    }


    public synchronized String getStrStatus(){

        String msg = "";
        if (!Smb_messageQueue.isEmpty())
        {
           try{
            msg = Smb_messageQueue.take();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return msg;
    }
}
