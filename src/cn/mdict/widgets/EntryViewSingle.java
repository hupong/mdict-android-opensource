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

package cn.mdict.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import cn.mdict.WebViewGestureFilter;
import cn.mdict.mdx.DictEntry;
import cn.mdict.mdx.MdxUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

/**
 * Created with IntelliJ IDEA.
 * User: Rayman
 * Date: 12-5-18
 * Time: 下午3:19
 * To change this template use File | Settings | File Templates.
 */
public class EntryViewSingle implements MdxEntryView {

    @SuppressLint("NewApi")
	EntryViewSingle(Context context, WebView wv) {        /*
		 * ViewGroup.LayoutParams params= new
		 * ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
		 * ViewGroup.LayoutParams.MATCH_PARENT); htmlView=new WebView(context);
		 * htmlView.setFocusable(true);
		 */
        htmlView = wv;
        htmlView.setVerticalScrollbarOverlay(true);
        htmlView.getSettings().setJavaScriptEnabled(true);
        htmlView.getSettings().setLoadWithOverviewMode(true);
        // htmlView.getSettings().setUseWideViewPort(true);
        // htmlView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        // htmlView.getSettings().setBuiltInZoomControls(true);
        htmlView.getSettings().setSupportZoom(true);
        htmlView.setScrollBarStyle(WebView.SCROLLBARS_INSIDE_OVERLAY);
        // htmlView.setInitialScale(1);
        // (ImageView)
        // htmlView.findViewById(com.android.internal.R.id.zoom_page_overview);
        // htmlView.getSettings().setUseWideViewPort(true);
        // htmlView.getSettings().setLoadWithOverviewMode(true);
        // TODO cancel ZoomControls button in platform 3.0 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            htmlView.getSettings().setDisplayZoomControls(false);
        } else {
            try {
                // There are crash reports due to this call, not sure why
                // Simply ignore errors here.
                //htmlView.getZoomControls().setVisibility(View.GONE); //remarked by alex 2012-11-19
                htmlView.getSettings().setBuiltInZoomControls(false); //added by alex 2012-11-19
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

		/*
		 * htmlView.addJavascriptInterface( new Object(){
		 * 
		 * @SuppressWarnings("unused") //This is a call back from javascript
		 * public void onLookupWord(final String word, final int x, final int
		 * y){ jsHandler.post( new Runnable() { public void run() {
		 * mdxView.displayByHeadword(word, true); }; }); } }, "MdxDict");
		 */
        htmlView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // selectAndCopyText();
                return false;
            }
        });

        htmlView.setWebChromeClient(new WebChromeClient() {
            @SuppressLint("NewApi")
			public boolean onJsAlert(WebView view, String url, String message,
                                     android.webkit.JsResult result) {
                Log.d("JS", message);
                result.confirm();
                return true;
            }

            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                String message = consoleMessage.message();
                Log.d("JS", message);
                return false;
            }
        });

        // setMdxView(mdxView);
        // addView(htmlView);
    }

    @Override
    public void setMdxView(MdxView mdxView) {
        this.mdxView = mdxView;
        wvClient = new MdxWebViewClient(mdxView, null);
        htmlView.setWebViewClient(wvClient);
        // htmlView.addJavascriptInterface(wvClient, "MdxClient");
        htmlView.addJavascriptInterface(new Object() {
            @SuppressWarnings("unused")
            // This is a call back from javascript
            public void onPageComplete() {
                jsHandler.post(new Runnable() {
                    public void run() {
                        if (wvClient != null) {
                            wvClient.onPageComplete(htmlView);
                        }
                    }

                    ;
                });
            }

            public void saveSource(String fileContnet) {
                String UTF8 = "gb2312";
                BufferedWriter bw;

                try {
                    String strFile = "/mnt/sdcard/mdict/temp/testWeb.html";
                    File f = new File(strFile);
                    if (f.exists()) {
                    } else {
                        f.createNewFile();
                    }
                    bw = new BufferedWriter(new OutputStreamWriter(
                            new FileOutputStream(strFile),
                            UTF8));
                    bw.write(fileContnet);
                    bw.close();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        }, "MdxDict");
        // htmlView.setPictureListener(wvClient);
    }

    @Override
    public void setGestureListener(WebViewGestureFilter.GestureListener listener) {
        htmlView.setOnTouchListener(new WebViewGestureFilter(htmlView, listener));
    }

    @Override
    public void displayEntry(DictEntry entry) {
        // MdxUtils.displayEntryHtml(mdxView.getDict(),entry, htmlView);
        // htmlView.loadData("","text/html","");
        // htmlView.clearView();
        MdxUtils.displayEntry(htmlView, mdxView.getDict(), entry);
        htmlView.scrollTo(0, 0);
        //htmlView.loadUrl("javascript:window.MdxDict.saveSource(document.getElementsByTagName('html')[0].innerHTML);");
        // htmlView.//
    }

    @Override
    public View getContainer() {
        return htmlView;
    }

    @Override
    public void showAllEntries(boolean show) {
        if (show)
            htmlView.loadUrl("javascript:ShowAllBlock(true, false, '');");
        else
            htmlView.loadUrl("javascript:ShowAllBlock(false, false, '');");
    }

    @Override
    public void zoomIn() {
        htmlView.zoomIn();
    }

    @Override
    public void zoomOut() {
        htmlView.zoomOut();
    }

    public void displayAssetFile(String filename) {
        if (htmlView != null) {
            htmlView.clearView();
            htmlView.loadUrl("file:///android_asset" + filename);
        }
    }

    public void displayHtml(String html) {
        htmlView.clearView();
        htmlView.loadDataWithBaseURL("", html, "text/html", "utf-8", "");
        htmlView.scrollTo(0, 0);
    }

    public void loadUrl(String url) {
        htmlView.clearView();
        htmlView.loadUrl(url);
    }

    private Handler jsHandler = new Handler();
    private MdxView mdxView = null;
    private WebView htmlView = null;
    MdxWebViewClient wvClient = null;
}
