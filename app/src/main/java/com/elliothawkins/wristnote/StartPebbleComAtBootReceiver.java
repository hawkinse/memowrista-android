package com.elliothawkins.wristnote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class StartPebbleComAtBootReceiver extends BroadcastReceiver {
    public StartPebbleComAtBootReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // an Intent broadcast.
        if(Intent.ACTION_BOOT_COMPLETED.equalsIgnoreCase(intent.getAction())){
            Intent pebbleServiceIntent = new Intent(context, PebbleComService.class);
            context.startService(pebbleServiceIntent);
        }
    }
}
