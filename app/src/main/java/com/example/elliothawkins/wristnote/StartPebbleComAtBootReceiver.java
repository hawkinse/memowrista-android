package com.example.elliothawkins.wristnote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class StartPebbleComAtBootReceiver extends BroadcastReceiver {
    public StartPebbleComAtBootReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        Toast.makeText(context, "WristNote boot service recieved broadcast!", Toast.LENGTH_SHORT).show();
        // an Intent broadcast.
        if(Intent.ACTION_BOOT_COMPLETED.equalsIgnoreCase(intent.getAction())){
            Toast.makeText(context, "WristNote boot service - boot complete!", Toast.LENGTH_SHORT).show();
            Intent pebbleServiceIntent = new Intent(context, PebbleComService.class);
            context.startService(pebbleServiceIntent);
        }
    }
}
