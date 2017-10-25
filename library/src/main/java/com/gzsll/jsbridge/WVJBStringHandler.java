package com.gzsll.jsbridge;

import android.text.TextUtils;

/**
 * Created by ght on 2017/9/20.
 */

public abstract class WVJBStringHandler implements WVJBHandler {
    @Override
    public final void request(Object data, WVJBResponseCallback callback) {
        if (data == null) {
            requestString(null, callback);
        } else {
            String str = data.toString();
            if (TextUtils.isEmpty(str)) {
                requestString(null, callback);
            } else {
                requestString(str, callback);
            }
        }
    }

    public abstract void requestString(String string, WVJBResponseCallback callback);
}

