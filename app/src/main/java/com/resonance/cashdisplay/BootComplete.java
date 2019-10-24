package com.resonance.cashdisplay; /**
 * Created by Святослав on 18.05.2016.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

//import android.util.Log;

public class BootComplete extends BroadcastReceiver {
    public static final String TAG = "BootComplete";
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)) {
            Log.e(TAG, "***BootComplete***");
            Intent i = new Intent(context, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }
}