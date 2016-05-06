package com.gzsll.jsbridge;

import android.webkit.WebChromeClient;
import android.webkit.WebView;

/**
 * Created by sll on 2016/5/5.
 */
public class WVJBChromeClient extends WebChromeClient {

    private WVJBWebView mWVJBWebView;

    public WVJBChromeClient(WVJBWebView wvjbWebView) {
        mWVJBWebView = wvjbWebView;
    }


    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        //为什么在这里注入，因为OnPageStarted可能注入不成功，OnPageFinished可以注入成功，但是完成时间有可能太晚，在25%调用刚好是一个折中的办法
        if (newProgress <= 25) {
            mWVJBWebView.setExecuteLocalJs(false);
        } else {
            mWVJBWebView.executeMessage();
        }
        super.onProgressChanged(view, newProgress);
    }
}
