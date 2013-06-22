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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import cn.mdict.R;
import cn.mdict.WebViewGestureFilter;
import cn.mdict.mdx.DictEntry;
import cn.mdict.mdx.MdxEngine;
import cn.mdict.mdx.MdxUtils;

public class EntryViewList implements MdxEntryView {
    EntryViewList(Context context, ScrollView scrollView) {
        this.scrollView = scrollView;
        this.context = context;
        if (scrollView != null)
            viewList = (LinearLayout) scrollView.findViewById(R.id.entryList);
    }

    /*
    public boolean onPageLoadCompleted(WebView view) {
        if (view.getParent() != null) {
            View pv = (View) view.getParent();
            int vPos = view.getScrollY() + view.getTop();
            scrollView.requestChildRectangleOnScreen(pv, new Rect(0, vPos, view.getWidth(), vPos + scrollView.getHeight()), true);
            return mdxView.onPageLoadCompleted(view);
        }

        return false;
    }
    */

    @Override
    public void setMdxView(MdxView mdxView) {
        this.mdxView = mdxView;
        webViewClient = new MdxWebViewClient(mdxView);
        resetAllBlockPlugin();
    }

    @Override
    public void setGestureListener(WebViewGestureFilter.GestureListener listener) {
        gestureListener = listener;
        resetAllBlockPlugin();
    }

    void resetAllBlockPlugin() {
        if (viewList == null)
            return;
        for (int i = 0; i < viewList.getChildCount(); ++i) {
            WebView wv = getWebViewInBlock(i);
            if (wv != null) {
                wv.setOnTouchListener(new WebViewGestureFilter(wv, gestureListener));
                wv.setWebViewClient(webViewClient);
                wv.setPictureListener(webViewClient);
            }
        }
    }

    WebView getWebViewInBlock(int blockNo) {
        if (viewList != null && viewList.getChildCount() > blockNo) {
            return (WebView) viewList.getChildAt(blockNo).findViewById(R.id.entry_content);
        }
        return null;
    }

    @Override
    public void displayEntry(DictEntry entry) {

        //Cleanup used webview
        for (int j = 0; j < viewList.getChildCount(); ++j) {
            ViewGroup item = (ViewGroup) viewList.getChildAt(j);
            WebView wv = getWebViewInBlock(j);
            item.removeView(wv);
            if (wv != null) {
                wv.destroy();
            }
            wv = null; //Release reference
        }

        viewList.removeAllViews();
        for (int i = 0; i < entry.getSiblingCount(); ++i) {
            LinearLayout ll;
            WebView wv;
            if (i >= blockCacheCount) {
                ll = new LinearLayout(context);
                LayoutInflater.from(context).inflate(R.layout.entry_view, ll, true);
                if (blockCache != null && blockCacheCount < blockCache.length) {
                    blockCache[blockCacheCount++] = ll;
                }
                wv = (WebView) ll.findViewById(R.id.entry_content);
                wv.setWebViewClient(webViewClient);
                wv.setOnTouchListener(new WebViewGestureFilter(wv, gestureListener));
                wv.setVerticalScrollbarOverlay(true);
                wv.getSettings().setJavaScriptEnabled(true);
                wv.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
                //wv.getSettings().setEnableSmoothTransition(true);
                //wv.setFocusable(true);
                wv.setMinimumHeight(1);
            } else {
                ll = blockCache[i];
                wv = (WebView) ll.findViewById(R.id.entry_content);
            }
            if (MdxEngine.getSettings().getPrefMultiDictDefaultExpandAll() || i == 0)
                wv.setVisibility(View.VISIBLE);
            else
                wv.setVisibility(View.GONE);

            TextView title = (TextView) ll.findViewById(R.id.entry_title);
            title.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewGroup parentView = (ViewGroup) v.getParent();
                    View webView = parentView.findViewById(R.id.entry_content);
                    if (webView.getVisibility() == View.VISIBLE) {
                        if (parentView.getFocusedChild() != null)
                            parentView.requestChildFocus(v, parentView.getFocusedChild());
                    } else {
                        //Exapnd current, check if we should collapse others
                        if (MdxEngine.getSettings().getPrefMultiDictExpandOnlyOne()) {
                            for (int k = 0; k < viewList.getChildCount(); ++k) {
                                ViewGroup layout = (ViewGroup) viewList.getChildAt(k);
                                View entryView = layout.findViewById(R.id.entry_content);
                                if (entryView != webView) {
                                    entryView.setVisibility(View.GONE);
                                }
                            }
                        }
                    }
                    webView.setVisibility(webView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                }
            });
            //wv.clearView();
            //wv.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            //wv.requestLayout();

            //displayEntryHtml(currentEntry.getSiblingAt(i), wv);
            /*
            ViewGroup.LayoutParams params=wv.getLayoutParams();
            Log.d("WebView","Item "+i+" layout width="+params.width+" height="+params.height);
            params.width=LayoutParams.MATCH_PARENT;
            params.height=LayoutParams.WRAP_CONTENT;
            wv.setLayoutParams(params);
            wv.requestLayout();
            */
            wv.clearView();
            wv.requestLayout();
            viewList.addView(ll);
            DictEntry subEntry=entry.getSiblingAt(i);
            MdxUtils.displayEntry(wv, mdxView.getDict(), subEntry, false, MdxUtils.makeBaseUrl(subEntry));
/*
            ViewGroup.LayoutParams params=wv.getLayoutParams();
            params.width=LayoutParams.MATCH_PARENT;
            params.height=50;
            wv.setLayoutParams(params);
            wv.requestLayout();

            params.height=LayoutParams.WRAP_CONTENT;
            wv.setLayoutParams(params);
            wv.requestLayout();
*/
            title.setText(entry.getHeadword() + " - " + mdxView.getDict().getTitle(entry.getSiblingAt(i).getDictId(), false));
        }
        scrollView.scrollTo(0, 0);
    }

    @Override
    public View getContainer() {
        return scrollView;
    }

    @Override
    public void showAllEntries(boolean show) {
        scrollView.requestFocus();
        if (!show && viewList.getChildCount() > 0) {
            if (viewList.getFocusedChild() != null)
                viewList.requestChildFocus(viewList.getChildAt(0), viewList.getFocusedChild());
        }

        for (int i = 0; i < viewList.getChildCount(); ++i) {
            View childView = getWebViewInBlock(i);
            childView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void zoomIn() {
        for (int j = 0; j < viewList.getChildCount(); ++j) {
            WebView wv = getWebViewInBlock(j);
            if (wv != null) {
                wv.zoomIn();
            }
        }
    }

    @Override
    public void zoomOut() {
        for (int j = 0; j < viewList.getChildCount(); ++j) {
            WebView wv = getWebViewInBlock(j);
            if (wv != null) {
                wv.zoomOut();
            }
        }
    }


    private MdxView mdxView = null;
    private WebViewGestureFilter.GestureListener gestureListener = null;
    private MdxWebViewClient webViewClient = null;
    private LinearLayout viewList = null;
    private ScrollView scrollView = null;
    private LinearLayout[] blockCache = null; //new LinearLayout[30];
    private int blockCacheCount = 0;
    private Context context;

}