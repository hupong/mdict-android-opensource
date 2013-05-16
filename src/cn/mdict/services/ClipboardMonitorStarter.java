package cn.mdict.services;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * When booting is completed, it starts {@link ClipboardMonitor} service to
 * monitor the states of clipboard.
 */
public class ClipboardMonitorStarter extends BroadcastReceiver {
    /* This class should be public; otherwise, the system don't have privilege
     * to instantiate it and cause exception occurs.
     */
    private static final String TAG = "MDict.ClipboardMonitorStarter";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            ComponentName service = context.startService(
                    new Intent(context, ClipboardMonitor.class));
            if (service == null) {
                Log.e(TAG, "Can't start service "
                        + ClipboardMonitor.class.getName());
            }
        } else {
            Log.e(TAG, "Received unexpected intent " + intent.toString());
        }
    }
}
