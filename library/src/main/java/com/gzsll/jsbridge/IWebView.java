package com.gzsll.jsbridge;

/**
 * Created by ght on 2017/9/19.
 */

public interface IWebView {
    /**
     * 注入js建立连接.
     */
    void injectJavascriptFile();

    /**
     * 接收js发送过来的数据.
     */
    void flushMessageQueue();
}
