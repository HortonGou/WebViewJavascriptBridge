package com.gzsll.jsbridge;

import android.graphics.Bitmap;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Created by ght on 2017/9/20.
 */

public class SafeWebViewClient extends WebViewClient {
    private SafeWebView webView;

    public SafeWebViewClient(SafeWebView webView) {
        this.webView = webView;
    }

    @Override
    public void onLoadResource(WebView view, String url) {
        webView.injectJavascriptInterfaces(view);
        super.onLoadResource(view, url);
    }

    @Override
    public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
        webView.injectJavascriptInterfaces(view);
        super.doUpdateVisitedHistory(view, url, isReload);
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        webView.injectJavascriptInterfaces(view);
        super.onPageStarted(view, url, favicon);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        webView.injectJavascriptInterfaces(view);
        super.onPageFinished(view, url);
    }
}
