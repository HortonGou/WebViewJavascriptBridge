package com.gzsll.bridge;

import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by sll on 2016/3/1.
 */
public class WVJBWebViewClient extends WebViewClient {
    private static final String TAG = "WebViewJavascriptClient";
    private static final String INTERFACE = TAG + "Interface";
    private static final String SCHEME = "wvjbscheme";
    private static final String MESSAGE = "__WVJB_QUEUE_MESSAGE__";

    private WebView webView;

    private ArrayList<WVJBMessage> messageQueue = null;
    private Map<String, WVJBResponseCallback> responseCallbacks = null;
    private Map<String, WVJBHandler> messageHandlers = null;
    private long uniqueId = 0;
    private MyJavascriptInterface myInterface = new MyJavascriptInterface();


    public interface WVJBResponseCallback {
        void callback(Object data);
    }

    public interface WVJBHandler {
        void request(Object data, WVJBResponseCallback callback);
    }

    public WVJBWebViewClient(WebView webView) {
        this.webView = webView;
        this.webView.getSettings().setJavaScriptEnabled(true);
        this.webView.setWebViewClient(this);
        this.webView.addJavascriptInterface(myInterface, INTERFACE);
        this.responseCallbacks = new HashMap<>();
        this.messageHandlers = new HashMap<>();
        this.messageQueue = new ArrayList<>();
    }


    public void callHandler(String handlerName) {
        callHandler(handlerName, null, null);
    }

    public void callHandler(String handlerName, Object data) {
        callHandler(handlerName, data, null);
    }

    public void callHandler(String handlerName, Object data,
                            WVJBResponseCallback callback) {
        sendData(data, callback, handlerName);
    }

    public void registerHandler(String handlerName, WVJBHandler handler) {
        if (handlerName == null || handlerName.length() == 0 || handler == null)
            return;
        messageHandlers.put(handlerName, handler);
    }


    private void sendData(Object data, WVJBResponseCallback callback,
                          String handlerName) {
        if (data == null && TextUtils.isEmpty(handlerName))
            return;
        WVJBMessage message = new WVJBMessage();
        if (data != null) {
            message.data = data;
        }
        if (callback != null) {
            String callbackId = "java_cb_" + (++uniqueId);
            responseCallbacks.put(callbackId, callback);
            message.callbackId = callbackId;
        }
        if (handlerName != null) {
            message.handlerName = handlerName;
        }
        queueMessage(message);
    }


    private void queueMessage(WVJBMessage message) {
        if (messageQueue != null) {
            messageQueue.add(message);
        } else {
            dispatchMessage(message);
        }
    }

    private void dispatchMessage(WVJBMessage message) {
        String messageJSON = doubleEscapeString(message2Json(message).toString());

        Log.d(TAG, "SEND:" + messageJSON);

        executeJavascript("WebViewJavascriptBridge._handleMessageFromJava('"
                + messageJSON + "');");
    }


    private String doubleEscapeString(String javascript) {
        String result;
        result = javascript.replace("\\", "\\\\");
        result = result.replace("\"", "\\\"");
        result = result.replace("\'", "\\\'");
        result = result.replace("\n", "\\n");
        result = result.replace("\r", "\\r");
        result = result.replace("\f", "\\f");
        return result;
    }

    private JSONObject message2Json(WVJBMessage message) {
        JSONObject object = new JSONObject();
        try {
            if (message.callbackId != null) {
                object.put("callbackId", message.callbackId);
            }
            if (message.data != null) {
                object.put("data", message.data);
            }
            if (message.handlerName != null) {
                object.put("handlerName", message.handlerName);
            }
            if (message.responseId != null) {
                object.put("responseId", message.responseId);
            }
            if (message.responseData != null) {
                object.put("responseData", message.responseData);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object;
    }

    public void executeJavascript(String script) {
        executeJavascript(script, null);
    }

    public void executeJavascript(String script,
                                  final JavascriptCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(script, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    if (callback != null) {
                        if (value != null && value.startsWith("\"")
                                && value.endsWith("\"")) {
                            value = value.substring(1, value.length() - 1)
                                    .replaceAll("\\\\", "");
                        }
                        callback.onReceiveValue(value);
                    }
                }
            });
        } else {
            if (callback != null) {
                myInterface.addCallback(++uniqueId + "", callback);
                webView.loadUrl("javascript:window." + INTERFACE
                        + ".onResultForScript(" + uniqueId + "," + script + ")");
            } else {
                webView.loadUrl("javascript:" + script);
            }
        }
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        try {
            InputStream in = webView.getResources().getAssets().open("WebViewJavascriptBridge.js");
            String script = convertStreamToString(in);
            executeJavascript(script);
            if (messageQueue != null) {
                for (int i = 0; i < messageQueue.size(); i++) {
                    dispatchMessage(messageQueue.get(i));
                }
                messageQueue = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onPageFinished(view, url);

    }


    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (url.startsWith(SCHEME)) {
            if (url.indexOf(MESSAGE) > 0) {
                flushMessageQueue();
            }
            return true;
        }
        return super.shouldOverrideUrlLoading(view, url);
    }

    private String convertStreamToString(InputStream is) {
        String s = "";
        try {
            Scanner scanner = new Scanner(is, "UTF-8").useDelimiter("\\A");
            if (scanner.hasNext()) s = scanner.next();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return s;
    }

    private void flushMessageQueue() {
        String script = "WebViewJavascriptBridge._fetchQueue()";
        executeJavascript(script, new JavascriptCallback() {
            public void onReceiveValue(String messageQueueString) {
                if (!TextUtils.isEmpty(messageQueueString)) {
                    processMessageQueue(messageQueueString);
                }
            }
        });
    }

    private void processMessageQueue(String messageQueueString) {
        try {
            JSONArray messages = new JSONArray(messageQueueString);
            for (int i = 0; i < messages.length(); i++) {
                JSONObject jo = messages.getJSONObject(i);
                Log.d(TAG, "processMessageQueue:" + jo);

                WVJBMessage message = json2Message(jo);
                if (message.responseId != null) {
                    WVJBResponseCallback responseCallback = responseCallbacks
                            .remove(message.responseId);
                    if (responseCallback != null) {
                        responseCallback.callback(message.responseData);
                    }
                } else {
                    WVJBResponseCallback responseCallback = null;
                    if (message.callbackId != null) {
                        final String callbackId = message.callbackId;
                        responseCallback = new WVJBResponseCallback() {
                            @Override
                            public void callback(Object data) {
                                WVJBMessage msg = new WVJBMessage();
                                msg.responseId = callbackId;
                                msg.responseData = data;
                                queueMessage(msg);
                            }
                        };
                    }

                    WVJBHandler handler = messageHandlers.get(message.handlerName);

                    if (handler != null) {
                        handler.request(message.data, responseCallback);
                    } else {
                        Log.e(TAG, "No handler for message from JS:" + message.handlerName);
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private WVJBMessage json2Message(JSONObject object) {
        WVJBMessage message = new WVJBMessage();
        try {
            if (object.has("callbackId")) {
                message.callbackId = object.getString("callbackId");
            }
            if (object.has("data")) {
                message.data = object.get("data");
            }
            if (object.has("handlerName")) {
                message.handlerName = object.getString("handlerName");
            }
            if (object.has("responseId")) {
                message.responseId = object.getString("responseId");
            }
            if (object.has("responseData")) {
                message.responseData = object.get("responseData");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return message;
    }


    private class WVJBMessage {
        Object data;
        String callbackId;
        String handlerName;
        String responseId;
        Object responseData;
    }

    private class MyJavascriptInterface {
        Map<String, JavascriptCallback> map = new HashMap<String, JavascriptCallback>();

        public void addCallback(String key, JavascriptCallback callback) {
            map.put(key, callback);
        }

        @JavascriptInterface
        public void onResultForScript(String key, String value) {
            Log.i(TAG, "onResultForScript: " + value);
            JavascriptCallback callback = map.remove(key);
            if (callback != null)
                callback.onReceiveValue(value);
        }
    }

    public interface JavascriptCallback {
        void onReceiveValue(String value);
    }

}
