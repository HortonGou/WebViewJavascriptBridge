package com.gzsll.jsbridge;

import android.webkit.WebView;


/**
 * Created by ght on 2017/9/20.
 */

public class WVJBSafeWebViewClient extends SafeWebViewClient {
    private WVJBSafeWebView mWVJBWebView;


    public WVJBSafeWebViewClient(WVJBSafeWebView wvjbWebView) {
        super(wvjbWebView);
        mWVJBWebView = wvjbWebView;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (url.startsWith(WVJBConstants.SCHEME)) {
            if (url.indexOf(WVJBConstants.BRIDGE_LOADED) > 0) {
                mWVJBWebView.injectJavascriptFile();
            } else if (url.indexOf(WVJBConstants.MESSAGE) > 0) {
                mWVJBWebView.flushMessageQueue();
            } else {
                Logger.d("UnkownMessage:" + url);
            }
            return true;
        }
        return super.shouldOverrideUrlLoading(view, url);
    }
}
