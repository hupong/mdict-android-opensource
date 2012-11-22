package cn.mdict;

import android.graphics.Bitmap;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebViewClientEmb extends WebViewClient {

    public boolean shouldOverrideUrlLoading(WebView view, String url) {

        view.loadUrl(url);
        return true;

    }


    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);

    }


    @Override

    public void onPageFinished(WebView view, String url) {

        super.onPageFinished(view, url);
    }
}
