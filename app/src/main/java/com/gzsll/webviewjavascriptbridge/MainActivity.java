package com.gzsll.webviewjavascriptbridge;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Toast;

import com.gzsll.bridge.WVJBWebViewClient;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "WebViewJavascriptBridge";


    private WebView webView;
    private WVJBWebViewClient webViewClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        webView = (WebView) findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("file:///android_asset/ExampleApp.html");
        webViewClient = new MyWebViewClient(webView);
        webView.setWebChromeClient(new MyWebChromeClient());

        findViewById(R.id.call).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webViewClient.callHandler("testJavascriptHandler", "{\"greetingFromJava\": \"Hi there, JS!\" }", new WVJBWebViewClient.WVJBResponseCallback() {

                    @Override
                    public void callback(Object data) {
                        Toast.makeText(MainActivity.this, "testJavascriptHandler responded: " + data, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

    }

    public class MyWebViewClient extends WVJBWebViewClient {

        public MyWebViewClient(WebView webView) {
            super(webView);
            registerHandler("testJavaCallback", new WVJBWebViewClient.WVJBHandler() {

                @Override
                public void request(Object data, WVJBResponseCallback callback) {
                    Toast.makeText(MainActivity.this, "testJavaCallback called:" + data, Toast.LENGTH_LONG).show();
                    callback.callback("Response from testJavaCallback!");
                }
            });

            callHandler("testJavascriptHandler", "{\"foo\":\"before ready\" }", new WVJBResponseCallback() {

                @Override
                public void callback(Object data) {
                    Toast.makeText(MainActivity.this, "Java call testJavascriptHandler got response! :" + data, Toast.LENGTH_LONG).show();
                }
            });
        }

    }


    class MyWebChromeClient extends WebChromeClient {
        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            Log.d(TAG, "onConsoleMessage:" + consoleMessage.message() + ":" + consoleMessage.lineNumber());
            return true;
        }
    }

}
