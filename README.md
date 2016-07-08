`WebViewJavascriptBridge for Android` is Cross-platform [WebViewJavascriptBridge](https://github.com/marcuswestin/WebViewJavascriptBridge) for Android Extension,the JavaScript interface compatible with [WebViewJavascriptBridge](https://github.com/marcuswestin/WebViewJavascriptBridge) 。

## Usage ##

1) Add following to the build.gradle of your project.

```
	dependencies {
		compile 'com.gzsll.jsbridge:library:1.1.0'
	}
```

2) Add `com.gzsll.jsbridge.WVJBWebView` to your layout, it is inherited from `WebView`.

```java
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
```

...



3) Copy and paste `setupWebViewJavascriptBridge` into your JS:

```javascript
function setupWebViewJavascriptBridge(callback) {
	if (window.WebViewJavascriptBridge) { return callback(WebViewJavascriptBridge); }
	if (window.WVJBCallbacks) { return window.WVJBCallbacks.push(callback); }
	window.WVJBCallbacks = [callback];
	var WVJBIframe = document.createElement('iframe');
	WVJBIframe.style.display = 'none';
	WVJBIframe.src = 'wvjbscheme://__BRIDGE_LOADED__';
	document.documentElement.appendChild(WVJBIframe);
	setTimeout(function() { document.documentElement.removeChild(WVJBIframe) }, 0)
}
```

4) Finally, call `setupWebViewJavascriptBridge` and then use the bridge to register handlers and call Java handlers:


```javascript
 setupWebViewJavascriptBridge(function(bridge) {
		bridge.registerHandler('testJavascriptHandler', function(data, responseCallback) {
			var responseData = { 'Javascript Says':'Right back atcha!' }
			responseCallback(responseData)
		})

		var callbackButton = document.getElementById('buttons').appendChild(document.createElement('button'))
		callbackButton.innerHTML = 'Fire testJavaCallback'
		callbackButton.onclick = function(e) {
			bridge.callHandler('testJavaCallback', {'foo': 'bar'}, function(response) {
				log('JS got response', response)
			})
		}
	})
```

5) If you want to use `WebViewClient`,you must use like this
 ```java
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

 ```

6) Read Demo for more detail.



API Reference
-------------

### Java API

##### `public void registerHandler(String handlerName, WVJBHandler handler)`

Register a handler called `handlerName`. The javascript can then call this handler with `WebViewJavascriptBridge.callHandler("handlerName")`.


##### `public void callHandler(String handlerName)`
##### `public void callHandler(String handlerName, Object data)`
##### `public void callHandler(String handlerName, Object data,WVJBResponseCallback callback)`

Call the javascript handler called `handlerName`. If a `WVJBResponseCallback` callback is given the javascript handler can respond.


### Javascript API

##### `bridge.registerHandler("handlerName", function(responseData) { ... })`

Register a handler called `handlerName`. The Java can then call this handler with `public void callHandler(String handlerName, Object data)` and `public void callHandler(String handlerName, Object data,WVJBResponseCallback callback)`

Example:

```javascript
bridge.registerHandler("showAlert", function(data) { alert(data) })
bridge.registerHandler("getCurrentPageUrl", function(data, responseCallback) {
	responseCallback(document.location.toString())
})
```


##### `bridge.callHandler("handlerName", data)`
##### `bridge.callHandler("handlerName", data, function responseCallback(responseData) { ... })`

Call an Java handler called `handlerName`. If a `responseCallback` function is given the Java handler can respond.

Example:

```javascript
bridge.callHandler("Log", "Foo")
bridge.callHandler("getScreenHeight", null, function(response) {
	alert('Screen height:' + response)
})
```



