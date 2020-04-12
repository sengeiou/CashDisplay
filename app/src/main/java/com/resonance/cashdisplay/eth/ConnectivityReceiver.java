package com.resonance.cashdisplay.eth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.os.Bundle;

import com.resonance.cashdisplay.Log;

public class ConnectivityReceiver extends BroadcastReceiver {

    private static final String TAG = ConnectivityReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        NetworkInfo info = (NetworkInfo) extras.getParcelable("networkInfo");
        NetworkInfo.State state = info.getState();
        Log.d(TAG, info.toString() + " " + state.toString());

        if (state == NetworkInfo.State.CONNECTED) {
            Log.d(TAG, "CONNECTED");
            EthernetSettings.ipSettings = EthernetSettings.get_IP_MASK_GW();
        } else {
            Log.d(TAG, "DISCONNECTED");
        }
    }
}
