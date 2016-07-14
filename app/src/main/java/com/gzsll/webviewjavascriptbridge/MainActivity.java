package com.gzsll.webviewjavascriptbridge;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import com.gzsll.jsbridge.WVJBWebView;
import com.gzsll.jsbridge.WVJBWebViewClient;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "WebViewJavascriptBridge";


    private WVJBWebView webView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        webView = (WVJBWebView) findViewById(R.id.webView);
        webView.loadUrl("file:///android_asset/ExampleApp.html");
        webView.setWebViewClient(new CustomWebViewClient(webView));

        findViewById(R.id.call).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.callHandler("testJavascriptHandler", "{\"greetingFromJava\": \"Hi there, JS!\" }", new WVJBWebView.WVJBResponseCallback() {

                    @Override
                    public void callback(Object data) {
                        Toast.makeText(MainActivity.this, "testJavascriptHandler responded: " + data, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        webView.registerHandler("testJavaCallback", new WVJBWebView.WVJBHandler() {

            @Override
            public void request(Object data, WVJBWebView.WVJBResponseCallback callback) {
                Toast.makeText(MainActivity.this, "testJavaCallback called:" + data, Toast.LENGTH_LONG).show();
                callback.callback("Response from testJavaCallback!");
            }
        });

        webView.callHandler("testJavascriptHandler", "{\"foo\":\"before ready\" }", new WVJBWebView.WVJBResponseCallback() {

            @Override
            public void callback(Object data) {
                Toast.makeText(MainActivity.this, "Java call testJavascriptHandler got response! :" + data, Toast.LENGTH_LONG).show();
            }
        });

    }

    public class CustomWebViewClient extends WVJBWebViewClient {

        public CustomWebViewClient(WVJBWebView webView) {
            super(webView);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            //  do your work here
            // ...
            return super.shouldOverrideUrlLoading(view, url);
        }
    }


}
