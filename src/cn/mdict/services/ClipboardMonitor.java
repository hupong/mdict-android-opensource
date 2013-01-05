package cn.mdict.services;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.mdict.ClipboardPrefs;
import cn.mdict.mdx.MdxEngine;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.FileObserver;
import android.os.IBinder;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.Gravity;

/**
 * Starts a background thread to monitor the states of clipboard and stores any
 * new clips into the SQLite database.
 * <p>
 * <i>Note:</i> the current android clipboard system service only supports text
 * clips, so in browser, we can just save images to external storage (SD card).
 * This service also monitors the downloads of browser, if any image is
 * detected, it will be stored into SQLite database, too.
 */
@SuppressLint("NewApi")
public class ClipboardMonitor extends Service {

	/** Image type to be monitored */
	private static final String[] IMAGE_SUFFIXS = new String[] { ".jpg",
			".jpeg", ".gif", ".png" };
	/** Path to browser downloads */
	private static final String BROWSER_DOWNLOAD_PATH = "/sdcard/download";

	private NotificationManager mNM;
	private MonitorTask mTask = new MonitorTask();
	private ClipboardManager mCM;
	private android.content.ClipboardManager mClipboard;

	private SharedPreferences mPrefs;

	android.content.ClipboardManager.OnPrimaryClipChangedListener mPrimaryChangeListener = new android.content.ClipboardManager.OnPrimaryClipChangedListener() {
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
	};

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@SuppressLint("NewApi")
	@Override
	public void onCreate() {
		MdxEngine.initSettings(getApplicationContext());
		// showNotification();
		mCM = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
		mPrefs = getSharedPreferences(ClipboardPrefs.NAME, MODE_PRIVATE);
		ClipboardPrefs.operatingClipboardId = mPrefs.getInt(
				ClipboardPrefs.KEY_OPERATING_CLIPBOARD,
				ClipboardPrefs.DEF_OPERATING_CLIPBOARD);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			mClipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
			mClipboard.addPrimaryClipChangedListener(mPrimaryChangeListener);
		} else {
			mTask.start();
		}
	}

	private void showNotification() {

	}

	@Override
	public void onDestroy() {
		mTask.cancel();
	}

	@Override
	public void onStart(Intent intent, int startId) {
	}

	/**
	 * Monitor task: monitor new text clips in global system clipboard and new
	 * image clips in browser download directory
	 */
	private class MonitorTask extends Thread {

		private volatile boolean mKeepRunning = false;
		private String mOldClip = null;
		private BrowserDownloadMonitor mBDM = new BrowserDownloadMonitor();

		public MonitorTask() {
			super("ClipboardMonitor");
		}

		/** Cancel task */
		public void cancel() {
			mKeepRunning = false;
			interrupt();
		}

		@Override
		public void run() {
			mKeepRunning = true;
			// mBDM.startWatching();
			while (true) {
				if (!MdxEngine.getSettings().getPrefGlobalClipboardMonitor())
					continue;
				doTask();
				try {
					Thread.sleep(mPrefs.getInt(ClipboardPrefs.KEY_MONITOR_INTERVAL,
							ClipboardPrefs.DEF_MONITOR_INTERVAL));
				} catch (InterruptedException ignored) {
				}
				if (!mKeepRunning) {
					break;
				}
			}
			// mBDM.stopWatching();
		}

		private void doTask() {
			if (mCM.hasText()) {
				String newClip = mCM.getText().toString();
				if (!newClip.equals(mOldClip)) {
					Log.i("ClipBoard",
							"detect new text clip: " + newClip.toString());
					mOldClip = newClip;
					if (newClip.length() > 50)
						return;
					searchDict(newClip);
				}
			}
		}

		private void searchDict(String newClip) {
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

		/**
		 * Monitor change of download directory of browser. It listens two
		 * events: <tt>CREATE</tt> and <tt>CLOSE_WRITE</tt>. <tt>CREATE</tt>
		 * event occurs when new file created in download directory. If this
		 * file is image, new image clip will be inserted into database when
		 * receiving <tt>CLOSE_WRITE</tt> event, meaning file is sucessfully
		 * downloaded.
		 */
		private class BrowserDownloadMonitor extends FileObserver {

			private Set<String> mFiles = new HashSet<String>();

			public BrowserDownloadMonitor() {
				super(BROWSER_DOWNLOAD_PATH, CREATE | CLOSE_WRITE);
			}

			private void doDownloadCompleteAction(String path) {
				Log.i("ClipBoard", "new image clip inserted: " + path);
			}

			@Override
			public void onEvent(int event, String path) {
				switch (event) {
				case CREATE:
					for (String s : IMAGE_SUFFIXS) {
						if (path.endsWith(s)) {
							Log.i("ClipBoard", "detect new image: " + path);
							mFiles.add(path);
							break;
						}
					}
					break;
				case CLOSE_WRITE:
					if (mFiles.remove(path)) { // File download completes
						doDownloadCompleteAction(path);
					}
					break;
				default:
					Log.w("ClipBoard",
							"BrowserDownloadMonitor go unexpected event: "
									+ Integer.toHexString(event));
					// throw new RuntimeException("BrowserDownloadMonitor" +
					// " got unexpected event");
				}
			}
		}
	}

	protected static boolean isTopActivity(Activity activity) {

		String packageName = "cn.mdict";
		ActivityManager activityManager = (ActivityManager) activity
				.getSystemService(ACTIVITY_SERVICE);
		List<RunningTaskInfo> tasksInfo = activityManager.getRunningTasks(1);
		if (tasksInfo.size() > 0) {
			System.out.println("---------------package name-----------"
					+ tasksInfo.get(0).topActivity.getPackageName());
			// 应用程序位于堆栈的顶层
			if (packageName.equals(tasksInfo.get(0).topActivity
					.getPackageName())) {
				return true;
			}
		}
		return false;
	}
}
