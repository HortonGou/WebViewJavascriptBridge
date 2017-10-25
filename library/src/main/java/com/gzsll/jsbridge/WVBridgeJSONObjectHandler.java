package com.gzsll.jsbridge;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ght on 2017/9/20.
 */

public abstract class WVBridgeJSONObjectHandler implements WVJBHandler {
    @Override
    public final void request(Object data, WVJBResponseCallback callback) {
        if (data == null) {
            requestJSONObject(null, callback);
        } else {
            String str = data.toString();
            if (TextUtils.isEmpty(str)) {
                requestJSONObject(null, callback);
            } else {
                try {
                    JSONObject jsonObject = new JSONObject(str);
                    requestJSONObject(jsonObject, callback);
                } catch (JSONException e) {
                    e.printStackTrace();
                    requestJSONObject(null, callback);
                }
            }
        }
    }

    public abstract void requestJSONObject(JSONObject jsonObject, WVJBResponseCallback callback);

}
