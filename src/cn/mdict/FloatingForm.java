/*
 * Copyright (C) 2012. Rayman Zhang <raymanzhang@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package cn.mdict;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.regex.Pattern;

import cn.mdict.fragments.DictView;
import cn.mdict.mdx.DictEntry;
import cn.mdict.mdx.DictPref;
import cn.mdict.mdx.MdxDictBase;
import cn.mdict.mdx.MdxEngine;
import cn.mdict.mdx.MdxEngineSetting;

//import android.view.WindowManager.LayoutParams;

public class FloatingForm extends SherlockFragmentActivity {

    private static final String TAG = "MDict.FloatingForm";

    public static final int kHistoryIntentId = 0;
    public static final int kFavoritesIntentId = 1;
    public static final int kLibraryIntentId = 2;
    public static final int kSettingIntentId = 3;

    private DictView dictView;
    private MDictApp theApp;

    // private WindowManager wm = null;
    // private WindowManager.LayoutParams wmParams = null;

    // private ViewFlipper flipper;
    // private GestureDetector detector;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Log.d(TAG, "Begin Init");
            //sendBroadcast(new Intent("mdict.cn.KillMainUI"));
            theApp = MDictApp.getInstance();
            theApp.setupAppEnv(getApplicationContext());

            setContentView(R.layout.floating_frame);
            dictView = (DictView) getSupportFragmentManager().findFragmentById(R.id.floating_dict_view_fragment);


            theApp.openPopupDictById(DictPref.kInvalidDictPrefId);
            dictView.changeDict(theApp.getPopupDict(), false);
            // dictView.displayWelcome();
            // setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

            handleIntent(getIntent());

            if (MdxEngine.getSettings().getPrefUseTTS())
                dictView.initTTSEngine();
        } catch (Exception e) {
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(new File(MdxEngine.getDocDir() + "mdict_j.log"));
            } catch (FileNotFoundException e1) {
                MiscUtils.showMessageDialog(this,
                        "Fail to log stack trace to file", "Error");
            }
            if (fos != null) {
                PrintStream ps = new PrintStream(fos);
                e.printStackTrace(ps);
            }
        }
    }

    static Pattern SearchSuggestData = Pattern
            .compile("content://mdx[.]mdict[.]cn/(\\d+)_(-?\\d+)_(.*)");

    private void handleIntent(Intent intent) {
        if (theApp.getMainDict() != null && theApp.getMainDict().isValid()) {

            // Set windows to floating
            WindowManager manager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            Display display = manager.getDefaultDisplay();

            RelativeLayout layout = (RelativeLayout) findViewById(R.id.floating_view);

            int floatingWindowHeight = MdxEngine.getSettings()
                    .getPrefFloatingWindowHeight();
            if (floatingWindowHeight == -1) {
                floatingWindowHeight = display.getHeight() * 3 / 7;
                MdxEngine.getSettings().setPrefFloatingWindowHeight(
                        floatingWindowHeight);
            }
            if (floatingWindowHeight > display.getHeight() * 9 / 10) {
                floatingWindowHeight = display.getHeight() * 9 / 10;
            }
            initFloatingWindow(floatingWindowHeight);

            View localView = findViewById(R.id.floating_frame);
            final Context context = this;            /*
             * localView.setOnClickListener(new View.OnClickListener() {
			 * 
			 * @Override public void onClick(View view) { ((FloatingForm)
			 * context).finish(); } });
			 */

            OnTouchListener gestureListener = new View.OnTouchListener() {
                int lastX
                        ,
                        lastY;
                int initX
                        ,
                        initY;
                int ignoreOffset = 30;
                int webViewHeight = (int) (dictView.getHtmlView().getContentHeight() * dictView.getHtmlView().getScale());
                final int ADJUST_HEIGHT = 1;
                final int SCROLL_WEBVIEW = 2;
                int adjustMode = -1;
                boolean inAdjusting = false;

                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    // event.getp
                    // dictView.sw
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            lastX = (int) event.getRawX();
                            lastY = (int) event.getRawY();
                            initX = lastX;
                            initY = lastY;
                            break;
                        case MotionEvent.ACTION_MOVE:
                            if (Math.abs(initX - (int) event.getRawX()) > ignoreOffset
                                    || Math.abs(initY - (int) event.getRawY()) > ignoreOffset) {

                                if (Math.abs(initX - (int) event.getRawX()) > ignoreOffset) {
                                    if (!inAdjusting) {
                                        adjustMode = ADJUST_HEIGHT;
                                        inAdjusting = true;
                                    }

                                    if (adjustMode == ADJUST_HEIGHT) {
                                        WindowManager manager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                                        Display display = manager
                                                .getDefaultDisplay();
                                        RelativeLayout layout = (RelativeLayout) findViewById(R.id.floating_view);
                                        // Gets the layout params that will allow
                                        // you to
                                        // resize the layout
                                        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) layout
                                                .getLayoutParams();

                                        // int dx = (int) event.getRawX() - lastX;
                                        // int dy = (int) event.getRawY() - lastY;
                                        int height = params.height
                                                + ((int) event.getRawX() - lastX);
                                        if (height < display.getHeight() / 10) {
                                            height = display.getHeight() / 10;
                                        }
                                        if (height > display.getHeight() * 9 / 10) {
                                            height = display.getHeight() * 9 / 10;
                                        }

                                        // Changes the height and width to the
                                        // specified
                                        // *pixels*
                                        MdxEngine
                                                .getSettings()
                                                .setPrefFloatingWindowHeight(height);
                                        params.height = height;
                                        layout.setLayoutParams(params);
                                    }
                                }
                                if (Math.abs(initX - (int) event.getRawX()) < ignoreOffset) {
                                    if (!inAdjusting) {
                                        adjustMode = SCROLL_WEBVIEW;
                                        inAdjusting = true;
                                    }
                                    if (adjustMode == SCROLL_WEBVIEW) {
                                        int scrollOffsetY = (lastY
                                                - (int) event.getRawY()) * 2;
                                        int currentY = dictView.getHtmlView().getScrollY();
                                        if (scrollOffsetY < 0
                                                && Math.abs(scrollOffsetY) > currentY) {
                                            scrollOffsetY = -currentY;
                                        }
                                        dictView.getHtmlView().scrollBy(dictView.getHtmlView().getScrollX(), scrollOffsetY);
                                        //final int actualOffsetY = scrollOffsetY;
                                        // if (scrollOffsetY > 0
                                        // && currentY + scrollOffsetY >
                                        // webViewHeight) {
                                        // scrollOffsetY = webViewHeight - currentY;
                                        // }
                                        // dictView.getHtmlView().get
                                    /*
                                    TimerTask task = new TimerTask() {
							            float t = 0;
							            float sig = -Math.signum(actualOffsetY);
							            float v0 = Math.abs(actualOffsetY)/50;
							           
							            @Override
							            public void run() {                   
							                t += 0.1;
							                double vt = v0- t*t;
							                if (vt >= 0)
							                {
							                	dictView.getHtmlView()
												.scrollBy(
														dictView.getHtmlView()
																.getScrollX(),
																(int) (sig*vt));
							                    //scrollByDeltaY((float) (sig*vt));
							                }
							                else
							                {
							                    mScrollTimer.cancel();
							                    return;
							                }
							            }   
							        };
							       
							        mScrollTimer = new Timer(); 
							        mScrollTimer.schedule(task, 0, 90);
							        */

                                    }
                                }
                                lastX = (int) event.getRawX();
                                lastY = (int) event.getRawY();
                            }
                            break;
                        case MotionEvent.ACTION_UP:
                            adjustMode = -1;
                            inAdjusting = false;
                            if (Math.abs(initX - (int) event.getRawX()) < 10
                                    && Math.abs(initY - (int) event.getRawY()) < 10) {
                                ((FloatingForm) context).finish();
                            }
                            break;
                    }
                    return true;
                }
            };
            localView.setOnTouchListener(gestureListener);

            dictView.setFragmentContainer(layout);

            // Get intent, action and MIME type
            String action = intent.getAction();
            String type = intent.getType();

            if (Intent.ACTION_SEND.equals(action) && type != null) {
                if ("text/plain".equals(type)) {
                    String query = intent.getStringExtra(Intent.EXTRA_TEXT);
                    dictView.displayByHeadword(query, true);
                } else if (type.startsWith("image/")) {
                    // handleSendImage(intent); // Handle single image being
                    // sent
                }
            } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)
                    && type != null) {
                if (type.startsWith("image/")) {
                    // handleSendMultipleImages(intent); // Handle multiple
                    // images being sent
                }
            } else {
                // Handle other intents, such as being started from the home
                // screen
            }
            if (intent.getAction().equals("com.ngc.fora.action.LOOKUP")
                    || intent.getAction().equals(
                    "colordict.intent.action.SEARCH")
                    || intent.getAction().equals("mdict.intent.action.SEARCH")) {
                String query = intent.getStringExtra("EXTRA_QUERY");//
                if (query == null)
                    query = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (query == null)
                    query = intent.getStringExtra("HEADWORD");

                dictView.displayByHeadword(query, true);

            }
        }
    }

    private void initFloatingWindow(Integer height) {
        Boolean fullScreen = getIntent().getBooleanExtra("EXTRA_FULLSCREEN",
                true);//
        Integer layoutGravity = getIntent().getIntExtra("EXTRA_GRAVITY",
                Gravity.BOTTOM);
        Integer leftMargin = 0;// getIntent().getIntExtra("EXTRA_MARGIN_LEFT",
        // 0);//
        Integer rightMargin = 0;// getIntent().getIntExtra("EXTRA_MARGIN_RIGHT",
        // 0);
        Integer topMargin = 0;// getIntent().getIntExtra("EXTRA_MARGIN_TOP", 0);
        Integer bottomMargin = 0;// getIntent().getIntExtra("EXTRA_MARGIN_BOTTOM",
        // 0);
        if (fullScreen)
            getWindow().addFlags(1024); // No title full screen
        else
            getWindow().clearFlags(1024);
        RelativeLayout layout = (RelativeLayout) findViewById(R.id.floating_view);
        // Gets the layout params that will allow you to resize the layout
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) layout
                .getLayoutParams();
        // Changes the height and width to the specified *pixels*
        params.height = height;
        params.gravity = layoutGravity;
        params.leftMargin = leftMargin;
        params.rightMargin = rightMargin;
        params.topMargin = topMargin;
        params.bottomMargin = bottomMargin;

        layout.setLayoutParams(params);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    protected void onPause() {
        MdxEngine.saveEngineSettings();
        super.onPause();
    }

	/*
	 * @Override public void onConfigurationChanged (Configuration newConfig){
	 * super.onConfigurationChanged(newConfig); if
	 * (!MdxEngine.getSettings().getPrefLockRotation())
	 * setRequestedOrientation(currentOrientation); }
	 */

    public void quitProcess() {
        Log.d(TAG, "Quiting process");
        // MdxEngine.unregisterNotification();
        MdxEngine.saveEngineSettings();
        FloatingForm.this.finish();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    public void onQuit() {
        AlertDialog dialog = MiscUtils.buildConfirmDialog(this,
                R.string.confirm_quit, R.string.quit,
                new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                            android.content.DialogInterface dialogInterface,
                            int i) {
                        quitProcess();
                    }
                }, null);
        dialog.show();
    }

    @Override
    public boolean onSearchRequested() {
        dictView.switchToListView();
        return true;
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onQuit();
            return true;
        } else
            return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z) {
            if (!dictView.isInputing()) {
                dictView.switchToListView();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        // if ( !dictView.onBackPressed() ) {
        quitProcess();
        // }
    }

    protected void startIntentForClass(int requestCode, Class<?> cls) {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.setClass(this, cls);
        startActivityForResult(intent, requestCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case kLibraryIntentId:
                if (resultCode == Activity.RESULT_OK) {
                    MdxEngine.saveEngineSettings();
                    int libId = data.getIntExtra(LibraryFrame.SELECTED_LIB_ID,
                            DictPref.kInvalidDictPrefId);
                    if (libId != DictPref.kInvalidDictPrefId) {
                        int result = MDictApp.getInstance().openPopupDictById(libId);
                        if (result == MdxDictBase.kMdxSuccess) {
                            dictView.changeDict(MDictApp.getInstance().getMainDict(), true);
                        } else {
                            String info = String.format(getString(R.string.fail_to_open_dict), result);
                            MiscUtils.showMessageDialog(this, info, getString(R.string.error));
                        }
                    }
                }
                break;
            case kFavoritesIntentId:
                if (resultCode == Activity.RESULT_OK) {
                    DictEntry favEntry = new DictEntry(data.getIntExtra(
                            FavoritesFrame.entryNoName, -1),
                            data.getStringExtra(FavoritesFrame.headwordName),
                            data.getIntExtra(FavoritesFrame.dictIdName,
                                    DictPref.kInvalidDictPrefId));
                    dictView.displayByEntry(favEntry, false);
                }
                break;
            case kHistoryIntentId:
                if (resultCode == Activity.RESULT_OK) {
                    DictEntry histEntry = new DictEntry(data.getIntExtra(
                            HistoryFrame.entryNoName, -1),
                            data.getStringExtra(HistoryFrame.headwordName),
                            data.getIntExtra(HistoryFrame.dictIdName,
                                    DictPref.kInvalidDictPrefId));
                    histEntry.makeJEntry();
                    dictView.displayByEntry(histEntry, false);
                }
                break;
            case kSettingIntentId:
                if (data != null) {
                    ArrayList<String> changePrefs = data
                            .getStringArrayListExtra(SettingFrame.prefChanged);
                    if (changePrefs != null && changePrefs.size() > 0) {
                        for (String pref : changePrefs) {
                            if (pref.compareToIgnoreCase(MdxEngineSetting.prefUseTTS) == 0
                                    || pref.compareToIgnoreCase(MdxEngineSetting.prefPreferredTTSEngine) == 0
                                    || pref.compareToIgnoreCase(MdxEngineSetting.prefTTSLocale) == 0) {
                                dictView.initTTSEngine();
                            } else if (pref
                                    .compareToIgnoreCase(MdxEngineSetting.prefUseFingerGesture) == 0) {
                                dictView.enableFingerGesture(MdxEngine
                                        .getSettings().getPrefUseFingerGesture());
                            }
                        }
                    }
                }
                theApp.rebuildAllDictSetting();
                if (MdxEngine.getSettings().getPrefShowInNotification())
                    MdxEngine.registerNotification();
                else
                    MdxEngine.unregisterNotification();
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        InputMethodManager imm = (InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm.isActive()) {
            MiscUtils.hideSIP(this);
        }
        // Handle item selection
		/*
		 * QuickActionBar qbar=new QuickActionBar(this); qbar.addQuickAction(new
		 * QuickAction(this, R.drawable.ic_search, R.string.quit)); View
		 * itemView
		 * =MiscUtils.getItemViewForActionItem((ActionBarImpl)getSupportActionBar
		 * (), item); qbar.show(itemView);
		 */
        switch (item.getItemId()) {
            // case android.R.id.home:
            // return true;
            case R.id.library:
                startIntentForClass(kLibraryIntentId, LibraryFrame.class);
                return true;
            case R.id.favorites:
                startIntentForClass(kFavoritesIntentId, FavoritesFrame.class);
                return true;
            case R.id.history:
                startIntentForClass(kHistoryIntentId, HistoryFrame.class);
                return true;
            case R.id.settings:
                startIntentForClass(kSettingIntentId, SettingFrame.class);
                return true;
            case R.id.quit:
                onQuit();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (MdxEngine.getSettings().getPrefLockRotation())
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "Destroying");
        MdxEngine.saveEngineSettings();
        super.onDestroy();
    }
}