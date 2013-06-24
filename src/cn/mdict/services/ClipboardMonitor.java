package cn.mdict.services;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Service;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.Gravity;
import cn.mdict.ClipboardPrefs;
import cn.mdict.mdx.MdxEngine;

import java.util.List;

/**
 * Starts a background thread to monitor the states of clipboard and stores any
 * new clips into the SQLite database.
 * <p/>
 * <i>Note:</i> the current android clipboard system service only supports text
 * clips, so in browser, we can just save images to external storage (SD card).
 * This service also monitors the downloads of browser, if any image is
 * detected, it will be stored into SQLite database, too.
 */
@SuppressLint("NewApi")
public class ClipboardMonitor extends Service {

    private android.content.ClipboardManager mClipboard;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("NewApi")
    @Override
    public void onCreate() {
        MdxEngine.setupEnv(getApplicationContext());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mClipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            mClipboard.addPrimaryClipChangedListener(new android.content.ClipboardManager.OnPrimaryClipChangedListener() {
                @SuppressLint("NewApi")
                public void onPrimaryClipChanged() {
                    if (!MdxEngine.getSettings().getPrefGlobalClipboardMonitor())
                        return;
                    ClipData clip = mClipboard.getPrimaryClip();
                    String newClip = "";
                    if (clip == null) {
                        return;
                    }
                    if (clip.getItemAt(0).getText() != null) {
                        newClip = clip.getItemAt(0).getText().toString();
                    }
                    if (newClip.length() > 50)
                        return;
                    Integer gravity = Gravity.TOP;
                    Intent intent = new Intent();
                    intent.setAction("mdict.intent.action.SEARCH");
                    intent.putExtra("EXTRA_QUERY", newClip);//
                    intent.putExtra("EXTRA_FULLSCREEN", true);//
                    intent.putExtra("EXTRA_GRAVITY", gravity);
                    intent.putExtra("EXTRA_HEIGHT", 200);//
                    intent.putExtra("EXTRA_MARGIN_LEFT", 4);//
                    intent.putExtra("EXTRA_MARGIN_RIGHT", 4);
                    intent.putExtra("EXTRA_MARGIN_TOP", 4);
                    intent.putExtra("EXTRA_MARGIN_BOTTOM", 4);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            });
        }
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public void onStart(Intent intent, int startId) {
    }
}
