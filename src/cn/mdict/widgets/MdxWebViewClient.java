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

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Picture;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.mdict.DictContentProvider;
import cn.mdict.R;
import cn.mdict.mdx.DictEntry;
import cn.mdict.mdx.MdxDictBase;
import cn.mdict.mdx.MdxEngine;

public class MdxWebViewClient extends WebViewClient implements WebView.PictureListener { // implements WebView.PictureListener {
    private MdxView mdxView;
    private Handler jsHandler = new Handler();

    //private ScrollView listContainer;
    private String anchor = null;

    private static final String JavaScriptInterface = "MdxDict";
    private static final String UrlHost = "mdict.cn";
    private static final String UrlScheme = "content";

    private static final Pattern EntryUrlPattern = Pattern.compile("/entry/(\\d+)/(.*)");
    private static final Pattern EntryxUrlPattern = Pattern.compile("/entryx/(\\d+)/(\\d+)");
    private static final Pattern LookupUrlPattern = Pattern.compile("/lookup/(\\d+)/(\\d+)/(.*)");
    private static final Pattern SoundUrlPattern = Pattern.compile("/sound/(\\d+)/(.*)");
    private static final Pattern HeadwordUrlPattern = Pattern.compile("/headword/(.*)"); //Used by word suggestion list


    MdxWebViewClient(MdxView mdxView) {
        this.mdxView = mdxView;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        StringBuffer mimeType = new StringBuffer();
        byte[] data = DictContentProvider.getDataByUrl(mdxView.getDict(), url, mimeType);
        if (data != null && data.length > 0) {
            //Dump css content for debug
            String css;
            if (url.endsWith(".css")){
                try{
                    css=new String(data, "UTF-8");
                    Log.d("css", css);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
            return new WebResourceResponse(mimeType.toString(), null, new ByteArrayInputStream(data));
        } else
            return null;
    }

    @Override
    public void onPageStarted(final WebView view, String url, Bitmap favicon) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            //noinspection AndroidLintNewApi
            view.removeJavascriptInterface(JavaScriptInterface);
        }
        view.addJavascriptInterface(new Object() {
            // This is a call back from javascript
//            @JavascriptInterface
            public void onPageComplete() {
                jsHandler.post(new Runnable() {
                    public void run() {
                        MdxWebViewClient.this.onPageComplete(view);
                    }
                });
            }

            //            @JavascriptInterface
            public void saveSource(String fileContent) {
                String UTF8 = "gb2312";
                BufferedWriter bw;

                try {
                    String strFile = MdxEngine.getTempDir() + "testWeb.html";
                    File f = new File(strFile);
                    if (!f.exists()) {
                        f.createNewFile();
                    }
                    bw = new BufferedWriter(new OutputStreamWriter(
                            new FileOutputStream(strFile),
                            UTF8));
                    bw.write(fileContent);
                    bw.close();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        }, JavaScriptInterface);

    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        Uri uri = Uri.parse(url);
        if (uri.getScheme() == null || uri.getScheme().compareToIgnoreCase(UrlScheme) != 0 || uri.getHost() == null || uri.getHost().compareToIgnoreCase(UrlHost) != 0)
            return false;

        String path = uri.getPath();
        /*
        if (url.compareTo(MdxPageCompleteNotify)==0){
            jumpToAnchor(view);
            return true;
        }
        */
        Matcher matcher = EntryUrlPattern.matcher(path);
        if (matcher.matches() && matcher.groupCount() == 2) {
            //String headWord=uri.getPath();
            int dictId=Integer.parseInt(matcher.group(1));
            String headWord = matcher.group(2);
            anchor = uri.getFragment();
            int fragPos = headWord.indexOf('#');
            if (fragPos > 0)
                headWord = headWord.substring(0, fragPos);
            if (headWord.length() > 0) {
                if (headWord.charAt(0) != '#') {
                    DictEntry entry = new DictEntry(0, "", dictId);
                    if (mdxView.getDict().locateFirst(headWord, false, false, false, true, entry) == MdxDictBase.kMdxSuccess) {
                        mdxView.displayByEntry(entry, true);
                    } else {
                        entry.setDictId(mdxView.getDict().getDictPref().getDictId());
                        if(mdxView.getDict().locateFirst(headWord, false, false, false, true, entry) == MdxDictBase.kMdxSuccess) {
                            mdxView.displayByEntry(entry, true);
                        } else {
                            Toast.makeText(mdxView.getContext(),
                                    String.format(mdxView.getContext().getString(R.string.headword_not_found), headWord),
                                    Toast.LENGTH_SHORT).show();
                            return true;
                        }
                    }
                    return false;
                }
            }
            return true;
        }
        matcher = LookupUrlPattern.matcher(path);
        if (matcher.matches() && matcher.groupCount() == 3) {
            String headWord = matcher.group(3);
            MdxView.MdxViewListener listener = mdxView.getMdxViewListener();
            if (listener != null) {
                try {
                    if (listener.onSearchText(mdxView, headWord, Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))))
                        return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            mdxView.displayByHeadword(headWord, true);
            return true;
        }
        matcher = HeadwordUrlPattern.matcher(path);
        if (matcher.matches() && matcher.groupCount() == 1) {
            String headWord = matcher.group(1);
            mdxView.displayByHeadword(headWord, true);
            return true;
        }

        /*
        if( scheme.compareToIgnoreCase("entryx")==0){
            if (headword.length()!=0 && !dict.isValid()){
                int entryNo=Integer.parseInt(headword);
                DictEntry entry = new DictEntry(entryNo, "", dict.getDictPref().getDictId());
                displayByEntry(entry,false);
            }
            return true;
        }
        */
        matcher = SoundUrlPattern.matcher(path);
        if (matcher.matches() && matcher.groupCount() == 2) {
            String headWord = matcher.group(2);
            if (headWord != null && headWord.length() != 0) {
                headWord = headWord.replace('/', '\\');
                if (headWord.charAt(0) != '\\')
                    headWord = "\\" + headWord;
                mdxView.playAudio(headWord);
            }
            return true;
        }
        return false;
    }

    void jumpToAnchor(WebView view) {
        if (anchor == null || anchor.length() == 0)
            return;
        String js = "javascript:window.location.hash=('" + anchor + "')";
        view.loadUrl(js);
        anchor = null;
    }

    //To be notified of page loaded event
    public void onPageComplete(WebView view) {
        if (mdxView.onPageLoadCompleted(view))
            return;
        jumpToAnchor(view);
    }

    @Override
    public void onNewPicture(final WebView view, Picture picture) {
        jsHandler.post(new Runnable() {
            public void run() {
                onPageComplete(view);
            }
        });
    }

    /*
    @Override
    public void onNewPicture(WebView view, Picture picture) {
        jumpToAnchor(view);
    }
    */
}
