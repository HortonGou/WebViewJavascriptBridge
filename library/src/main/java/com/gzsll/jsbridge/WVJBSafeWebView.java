package com.gzsll.jsbridge;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by ght on 2017/9/19.
 */

public class WVJBSafeWebView extends SafeWebView implements IWebView{
    private ArrayList<WVJBMessage> messageQueue = new ArrayList<>();
    private HashMap<String, WVJBResponseCallback> responseCallbacks = new HashMap<>();
    private HashMap<String, WVJBHandler> messageHandlers = new HashMap<>();
    private long uniqueId = 0;
    private MyJavascriptInterface myInterface = new MyJavascriptInterface();
    private String script;



    public WVJBSafeWebView(Context context) {
        super(context);
        init();
    }

    public WVJBSafeWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WVJBSafeWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    private void init() {
        getSettings().setJavaScriptEnabled(true);
        addJavascriptInterface(myInterface, WVJBConstants.INTERFACE);
        setWebViewClient(new WVJBWebViewClient(this));
    }
    /**
     * 添加一个浏览器状态监听.
     * 如果为null 会导致jsbridge失效
     *
     * @param client 必须是WVJBWebViewClient的子类
     */
    @Override
    public void setWebViewClient(WebViewClient client) {
        if (client instanceof WVJBSafeWebViewClient) {
            super.setWebViewClient(client);
        } else if (client == null) {
            super.setWebViewClient(null);
        } else {
            throw new IllegalArgumentException(
                    "the \'client\' must be a subclass of the \'WVJBSafeWebViewClient\'");
        }
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

    public void registerHandler(String handlerName,WVJBHandler handler) {
        if (TextUtils.isEmpty(handlerName) || handler == null)
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

    public void dispatchMessage(WVJBMessage message) {
        if (!VerifyMD5()){
            return;
        }
        String messageJSON = doubleEscapeString(message2Json(message).toString());
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


    public void flushMessageQueue() {
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
        if (TextUtils.isEmpty(messageQueueString)) {
            return;
        }
        try {
            JSONArray messages = new JSONArray(messageQueueString);
            for (int i = 0; i < messages.length(); i++) {
                JSONObject jo = messages.getJSONObject(i);
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
                        Log.e(WVJBConstants.TAG, "No handler for message from JS:" + message.handlerName);
                    }
                }
            }
        } catch (JSONException exception) {
            exception.printStackTrace();
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


    public void injectJavascriptFile() {
        try {
            if (TextUtils.isEmpty(script)) {
                InputStream in = getResources().getAssets().open("WebViewJavascriptBridge.js");
                script = convertStreamToString(in);
            }
            executeJavascript(script);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (messageQueue != null) {
            for (WVJBMessage message : messageQueue) {
                dispatchMessage(message);
            }
            messageQueue = null;
        }
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

    private void executeJavascript(String script) {
        executeJavascript(script, null);
    }

    private void executeJavascript(final String script,
                                   final JavascriptCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            evaluateJavascript(script, new ValueCallback<String>() {
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
                post(new Runnable() {
                    @Override
                    public void run() {
                        loadUrl("javascript:window." + WVJBConstants.INTERFACE
                                + ".onResultForScript(" + uniqueId + "," + script + ")");
                    }
                });
            } else {
                post(new Runnable() {
                    @Override
                    public void run() {
                        loadUrl("javascript:" + script);
                    }
                });
            }
        }
    }


    private class MyJavascriptInterface {
        Map<String, JavascriptCallback> map = new HashMap<>();

        public void addCallback(String key, JavascriptCallback callback) {
            map.put(key, callback);
        }

        @JavascriptInterface
        public void onResultForScript(String key, String value) {
            JavascriptCallback callback = map.remove(key);
            if (callback != null)
                callback.onReceiveValue(value);
        }
    }


    public boolean VerifyMD5() {
        String mMD5String = getMD5(getContext());
        if ("b7ec11ed70b8a5862b2efbb41171094d".equals(mMD5String)) {
            return true;
        }
        return false;
    }

    public static String getMD5(Context context) {
        MessageDigest digest = null;
        InputStream in = null;
        byte buffer[] = new byte[1024];
        int len;
        try {
            digest = MessageDigest.getInstance("MD5");
            in = context.getResources().getAssets().open("WebViewJavascriptBridge.js");
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        BigInteger bigInt = new BigInteger(1, digest.digest());
        return bigInt.toString(16);
    }
}
