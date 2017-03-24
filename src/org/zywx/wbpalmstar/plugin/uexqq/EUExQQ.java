package org.zywx.wbpalmstar.plugin.uexqq;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.tencent.connect.UserInfo;
import com.tencent.connect.common.Constants;
import com.tencent.connect.share.QQShare;
import com.tencent.connect.share.QzoneShare;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;
import org.zywx.wbpalmstar.engine.universalex.EUExCallback;
import org.zywx.wbpalmstar.plugin.uexqq.util.ConstantUtils;
import org.zywx.wbpalmstar.plugin.uexqq.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EUExQQ extends EUExBase {
    private static final String TAG = "EUExQQ";
    private Context mContext;
    private PackageManager pm;
    public static Tencent mTencent;
    public static EUExQQ mIntance;
    public static String mAppid = "222222";
    private static final String CB_LOGIN = "uexQQ.cbLogin";
    public static final String CB_INSTALLIED = "uexQQ.cbIsQQInstalled";
    private static final String CB_SHARE_QQ = "uexQQ.cbShareQQ";
    private static final String CB_USER_INFO_QQ = "uexQQ.cbGetUserInfo";
    private String loginFuncId;
    private String shareQQFunId;
    private String getUserInfoFunId;
    private static final String TAG_RET = "ret";
    private static final String TAG_DATA = "data";

    public EUExQQ(Context context, EBrowserView eBrw) {
        super(context, eBrw);
        this.mContext = context;
        pm = mContext.getPackageManager();
    }

    @Override
    protected boolean clean() {
        Log.i(TAG, "clean");
        return false;
    }

    public void login(String[] params) {
        if (params.length < 1) {
            return;
        }
        mAppid = params[0];
        if (params.length == 2) {
            loginFuncId = params[1];
        }
        Log.i(TAG, "login->mAppid = " + mAppid);
        initTencent(mAppid);
        //mTencent.login((Activity )mContext, "all", loginListener);
        doToStartTransitActivity(QQTransitActivity.UEX_QQ_TRANSIT_ACTIVITY_LOGIN, null);
    }

    private void initTencent(String appId) {
        if (mTencent == null) {
            mTencent = Tencent.createInstance(appId, mContext);
        }
    }

    IUiListener loginListener = new BaseUiListener() {
        @Override
        protected void doComplete(JSONObject values) {
            initOpenidAndToken(values);
        }
    };

    private class BaseUiListener implements IUiListener {
        @Override
        public void onComplete(Object response) {
            Log.i(TAG, "BaseUiListener->onComplete->response = " + response);
            if (null == response) {
                loginCallBack(EUExCallback.F_C_FAILED, "server no response!");
                return;
            }
            JSONObject jsonResponse = (JSONObject) response;
            if (null != jsonResponse && jsonResponse.length() == 0) {
                loginCallBack(EUExCallback.F_C_FAILED, "server no response!");
                return;
            }
            doComplete((JSONObject) response);
        }

        protected void doComplete(JSONObject values) {

        }

        @Override
        public void onError(UiError e) {
            loginCallBack(EUExCallback.F_C_FAILED, "errorCode:" + e.errorCode + "," + e
                    .errorDetail);
        }

        /**
         * 如果没有安装qq,调用Tencent.login,
         * 再返回应用时，不会走QQTransActivity的onActivityResult，所以需要发广播，通知QQTransActivity关闭自己
         */
        private void finishQQTransActivityNotify() {
            Intent intent = new Intent(ConstantUtils.UEX_QQ_FINISH_QQ_TRANSIT_ACTIVITY_ACTION);
            intent.setPackage(mContext.getPackageName());
            mContext.sendBroadcast(intent);
        }

        @Override
        public void onCancel() {
            finishQQTransActivityNotify();
            loginCallBack(EUExCallback.F_C_FAILED, "user cancel");
        }
    }

    IUiListener qqShareListener = new IUiListener() {
        @Override
        public void onCancel() {
            HashMap<String, Object> result = new HashMap<String, Object>();
            result.put("errCode", EUExCallback.F_C_FAILED);
            result.put("errStr", "user cancel");
            JSONObject jsonObject = new JSONObject(result);
            if (null != shareQQFunId) {
                callbackToJs(Integer.parseInt(shareQQFunId), false, EUExCallback.F_C_FAILED,
                        jsonObject);
            } else {
                jsCallbackAsyn(CB_SHARE_QQ, 0, EUExCallback.F_C_INT,
                        jsonObject.toString());
            }
        }

        @Override
        public void onComplete(Object response) {
            HashMap<String, Object> result = new HashMap<String, Object>();
            result.put("errCode", EUExCallback.F_C_SUCCESS);
            result.put("errStr", response);
            JSONObject jsonObject = new JSONObject(result);
            Log.i(TAG, "qqShareListener->onComplete->response = " + response);

            if (null != shareQQFunId) {
                callbackToJs(Integer.parseInt(shareQQFunId), false, EUExCallback.F_C_SUCCESS,
                        jsonObject);
            } else {
                jsCallbackAsyn(CB_SHARE_QQ, 0, EUExCallback.F_C_INT,
                        jsonObject.toString());
            }
        }

        @Override
        public void onError(UiError e) {
            HashMap<String, Object> result = new HashMap<String, Object>();
            result.put("errCode", EUExCallback.F_C_FAILED);
            result.put("errStr", e.errorMessage);
            JSONObject jsonObject = new JSONObject(result);
            Log.i(TAG, "qqShareListener->onError = " + e.errorMessage);

            if (null != shareQQFunId) {
                callbackToJs(Integer.parseInt(shareQQFunId), false, EUExCallback.F_C_FAILED,
                        jsonObject);
            } else {
                jsCallbackAsyn(CB_SHARE_QQ, 0, EUExCallback.F_C_INT,
                        jsonObject.toString());
            }
        }
    };

    public void initOpenidAndToken(JSONObject jsonObject) {
        try {
            String token = jsonObject.getString(Constants.PARAM_ACCESS_TOKEN);
            String expires = jsonObject.getString(Constants.PARAM_EXPIRES_IN);
            String openId = jsonObject.getString(Constants.PARAM_OPEN_ID);
            if (!TextUtils.isEmpty(token) && !TextUtils.isEmpty(expires)
                    && !TextUtils.isEmpty(openId)) {
                mTencent.setAccessToken(token, expires);
                mTencent.setOpenId(openId);
            }
            JSONObject dataJson = new JSONObject();
            dataJson.put(Constants.PARAM_ACCESS_TOKEN, token);
            dataJson.put(Constants.PARAM_OPEN_ID, openId);
            dataJson.put(Constants.PARAM_EXPIRES_IN, expires);
            loginCallBack(EUExCallback.F_C_SUCCESS, dataJson);
        } catch (Exception e) {
            loginCallBack(EUExCallback.F_C_FAILED, e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean isQQInstalled(String[] params) {
        boolean isInstalled = false;
        List<PackageInfo> installedPackages = pm.getInstalledPackages(PackageManager
                .GET_UNINSTALLED_PACKAGES);
        for (PackageInfo info : installedPackages) {
            if ("com.tencent.mobileqq".equalsIgnoreCase(info.packageName)) {
                isInstalled = true;
            }
        }
        jsCallback(CB_INSTALLIED, 0, EUExCallback.F_C_TEXT,
                isInstalled ? 0 : 1);
        return isInstalled;
    }

    public void loginCallBack(int ret, Object data) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(TAG_RET, String.valueOf(ret));
        map.put(TAG_DATA, data);
        JSONObject json = new JSONObject(map);
        if (loginFuncId != null) {
            callbackToJs(Integer.parseInt(loginFuncId), false, EUExCallback.F_C_SUCCESS, data);
            //4.0直接返回data
        } else {
            jsCallbackAsyn(CB_LOGIN, 0, EUExCallback.F_C_JSON,
                    json.toString());
        }
    }

    public void shareWebImgTextToQQ(String[] params) {
        if (params.length < 2) {
            return;
        }
        String appId = params[0];
        initTencent(appId);
        String jsonData = params[1];
        if (params.length == 3) {
            shareQQFunId = params[2];
        }
        parseWebImgData(jsonData);
    }

    private void parseWebImgData(String jsonData) {
        try {
            final Bundle params = new Bundle();
            params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_DEFAULT);
            //分享的类型。图文分享(普通分享)填Tencent.SHARE_TO_QQ_TYPE_DEFAULT
            JSONObject json = new JSONObject(jsonData);
            //必选
            String title = json.get(QQShare.SHARE_TO_QQ_TITLE).toString();
            params.putString(QQShare.SHARE_TO_QQ_TITLE, title);//分享的标题, 最长30个字符。
            String targetUrl = json.get(QQShare.SHARE_TO_QQ_TARGET_URL).toString();
            params.putString(QQShare.SHARE_TO_QQ_TARGET_URL, targetUrl);//这条分享消息被好友点击后的跳转URL。

            //可选
            if (json.has(QQShare.SHARE_TO_QQ_SUMMARY)) {
                String summary = json.get(QQShare.SHARE_TO_QQ_SUMMARY).toString();
                params.putString(QQShare.SHARE_TO_QQ_SUMMARY, summary);//分享的消息摘要，最长40个字。可选
            }
            if (json.has(QQShare.SHARE_TO_QQ_IMAGE_URL)) {
                String imageUrl = getImageUrl(json.get(QQShare.SHARE_TO_QQ_IMAGE_URL).toString());
                params.putString(QQShare.SHARE_TO_QQ_IMAGE_URL, imageUrl);//分享图片的URL或者本地路径
            }
            if (json.has(QQShare.SHARE_TO_QQ_APP_NAME)) {
                String appName = json.get(QQShare.SHARE_TO_QQ_APP_NAME).toString();
                params.putString(QQShare.SHARE_TO_QQ_APP_NAME, appName);
                //手Q客户端顶部，替换“返回”按钮文字，如果为空，用返回代替
            }
            if (json.has(QQShare.SHARE_TO_QQ_EXT_INT)) {
                params.putInt(QQShare.SHARE_TO_QQ_EXT_INT, Integer.valueOf(json.get(QQShare
                        .SHARE_TO_QQ_EXT_INT).toString()));
            }
            //QQShare.SHARE_TO_QQ_FLAG_QZONE_AUTO_OPEN，分享时自动打开分享到QZone的对话框。
            //QQShare.SHARE_TO_QQ_FLAG_QZONE_ITEM_HIDE，分享时隐藏分享到QZone按钮
            doToQQShare(params);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String getImageUrl(String url) {
        if (url.startsWith(BUtility.F_HTTP_PATH)) {
            return url;
        }
        String imgPath = BUtility.makeRealPath(
                BUtility.makeUrl(mBrwView.getCurrentUrl(), url),
                mBrwView.getCurrentWidget().m_widgetPath,
                mBrwView.getCurrentWidget().m_wgtType);
        if (imgPath.startsWith("/") && !imgPath.startsWith("/data")) {
            return imgPath;
        }
        return Utils.copyImage(mContext, imgPath);
    }

    private void doToQQShare(Bundle params) {
        //mTencent.shareToQQ((Activity)mContext, params, qqShareListener);
        doToStartTransitActivity(QQTransitActivity.UEX_QQ_TRANSIT_ACTIVITY_SHARE_TO_QQ, params);
    }

    public void shareLocalImgToQQ(String[] params) {
        if (params.length < 2) {
            return;
        }
        String appId = params[0];
        initTencent(appId);
        String jsonData = params[1];
        if (params.length == 3) {
            shareQQFunId = params[2];
        }
        parseLocalImgData(jsonData);
    }

    private void parseLocalImgData(String jsonData) {
        try {
            Bundle params = new Bundle();
            //分享类型，分享纯图片时填写QQShare.SHARE_TO_QQ_TYPE_IMAGE。
            params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_IMAGE);
            JSONObject json = new JSONObject(jsonData);
            String imageLocalUrl = json.get(QQShare.SHARE_TO_QQ_IMAGE_LOCAL_URL).toString();

            //需要分享的本地图片路径。
            params.putString(QQShare.SHARE_TO_QQ_IMAGE_LOCAL_URL, getImageUrl(imageLocalUrl));

            if (json.has(QQShare.SHARE_TO_QQ_APP_NAME)) {
                String appName = json.get(QQShare.SHARE_TO_QQ_APP_NAME).toString();
                //手Q客户端顶部，替换“返回”按钮文字，如果为空，用返回代替。
                params.putString(QQShare.SHARE_TO_QQ_APP_NAME, appName);
            }

            //分享额外选项，两种类型可选（默认是不隐藏分享到QZone按钮且不自动打开分享到QZone的对话框）：
            //QQShare.SHARE_TO_QQ_FLAG_QZONE_AUTO_OPEN，分享时自动打开分享到QZone的对话框。
            //QQShare.SHARE_TO_QQ_FLAG_QZONE_ITEM_HIDE，分享时隐藏分享到QZone按钮。
            if (json.has(QQShare.SHARE_TO_QQ_EXT_INT)) {
                params.putInt(QQShare.SHARE_TO_QQ_EXT_INT, Integer.valueOf(json.get(QQShare
                        .SHARE_TO_QQ_EXT_INT).toString()));
            }
            doToQQShare(params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void shareAudioToQQ(String[] params) {
        if (params.length < 2) {
            return;
        }
        String appId = params[0];
        initTencent(appId);
        String jsonData = params[1];
        if (params.length == 3) {
            shareQQFunId = params[2];
        }
        parseAudioData(jsonData);
    }

    private void parseAudioData(String jsonData) {
        try {
            Bundle params = new Bundle();
            //分享类型，分享纯图片时填写QQShare.SHARE_TO_QQ_TYPE_IMAGE。
            params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_AUDIO);
            JSONObject json = new JSONObject(jsonData);
            //必选
            String title = json.get(QQShare.SHARE_TO_QQ_TITLE).toString();
            params.putString(QQShare.SHARE_TO_QQ_TITLE, title);//分享的标题, 最长30个字符。
            String targetUrl = json.get(QQShare.SHARE_TO_QQ_TARGET_URL).toString();
            params.putString(QQShare.SHARE_TO_QQ_TARGET_URL, targetUrl);//这条分享消息被好友点击后的跳转URL。
            String audio_url = json.get(QQShare.SHARE_TO_QQ_AUDIO_URL).toString();
            params.putString(QQShare.SHARE_TO_QQ_AUDIO_URL, audio_url);
            //可选
            if (json.has(QQShare.SHARE_TO_QQ_SUMMARY)) {
                String summary = json.get(QQShare.SHARE_TO_QQ_SUMMARY).toString();
                params.putString(QQShare.SHARE_TO_QQ_SUMMARY, summary);//分享的消息摘要，最长40个字。可选
            }
            if (json.has(QQShare.SHARE_TO_QQ_IMAGE_URL)) {
                String imageUrl = getImageUrl(json.get(QQShare.SHARE_TO_QQ_IMAGE_URL).toString());
                params.putString(QQShare.SHARE_TO_QQ_IMAGE_URL, imageUrl);//分享图片的URL或者本地路径
            }
            if (json.has(QQShare.SHARE_TO_QQ_APP_NAME)) {
                String appName = json.get(QQShare.SHARE_TO_QQ_APP_NAME).toString();
                params.putString(QQShare.SHARE_TO_QQ_APP_NAME, appName);
                //手Q客户端顶部，替换“返回”按钮文字，如果为空，用返回代替
            }

            //分享额外选项，两种类型可选（默认是不隐藏分享到QZone按钮且不自动打开分享到QZone的对话框）：
            //QQShare.SHARE_TO_QQ_FLAG_QZONE_AUTO_OPEN，分享时自动打开分享到QZone的对话框。
            //QQShare.SHARE_TO_QQ_FLAG_QZONE_ITEM_HIDE，分享时隐藏分享到QZone按钮。
            if (json.has(QQShare.SHARE_TO_QQ_EXT_INT)) {
                params.putInt(QQShare.SHARE_TO_QQ_EXT_INT, Integer.valueOf(json.get(QQShare
                        .SHARE_TO_QQ_EXT_INT).toString()));
            }
            doToQQShare(params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void shareAppToQQ(String[] params) {
        if (params.length < 2) {
            return;
        }
        String appId = params[0];
        initTencent(appId);
        String jsonData = params[1];
        if (params.length == 3) {
            shareQQFunId = params[2];
        }
        parseAppData(jsonData);
    }

    private void parseAppData(String jsonData) {
        try {
            Bundle params = new Bundle();
            params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_APP);
            JSONObject json = new JSONObject(jsonData);
            //必选
            String title = json.get(QQShare.SHARE_TO_QQ_TITLE).toString();
            params.putString(QQShare.SHARE_TO_QQ_TITLE, title);//分享的标题, 最长30个字符。

            //可选
            if (json.has(QQShare.SHARE_TO_QQ_SUMMARY)) {
                String summary = json.get(QQShare.SHARE_TO_QQ_SUMMARY).toString();
                params.putString(QQShare.SHARE_TO_QQ_SUMMARY, summary);//分享的消息摘要，最长40个字。可选
            }
            if (json.has(QQShare.SHARE_TO_QQ_IMAGE_URL)) {
                String imageUrl = getImageUrl(json.get(QQShare.SHARE_TO_QQ_IMAGE_URL).toString());
                params.putString(QQShare.SHARE_TO_QQ_IMAGE_URL, imageUrl);//分享图片的URL或者本地路径
            }
            if (json.has(QQShare.SHARE_TO_QQ_APP_NAME)) {
                String appName = json.get(QQShare.SHARE_TO_QQ_APP_NAME).toString();
                params.putString(QQShare.SHARE_TO_QQ_APP_NAME, appName);
                //手Q客户端顶部，替换“返回”按钮文字，如果为空，用返回代替
            }

            //分享额外选项，两种类型可选（默认是不隐藏分享到QZone按钮且不自动打开分享到QZone的对话框）：
            //QQShare.SHARE_TO_QQ_FLAG_QZONE_AUTO_OPEN，分享时自动打开分享到QZone的对话框。
            //QQShare.SHARE_TO_QQ_FLAG_QZONE_ITEM_HIDE，分享时隐藏分享到QZone按钮。
            if (json.has(QQShare.SHARE_TO_QQ_EXT_INT)) {
                params.putInt(QQShare.SHARE_TO_QQ_EXT_INT, Integer.valueOf(json.get(QQShare
                        .SHARE_TO_QQ_EXT_INT).toString()));
            }
            doToQQShare(params);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void shareImgTextToQZone(String[] params) {
        if (params.length < 2) {
            return;
        }
        String appId = params[0];
        initTencent(appId);
        String jsonData = params[1];
        if (params.length == 3) {
            shareQQFunId = params[2];
        }
        parseImgTextToQZoneData(jsonData);
    }

    private void parseImgTextToQZoneData(String jsonData) {
        try {
            Bundle params = new Bundle();
            params.putInt(QzoneShare.SHARE_TO_QZONE_KEY_TYPE, QzoneShare
                    .SHARE_TO_QZONE_TYPE_IMAGE_TEXT);
            JSONObject json = new JSONObject(jsonData);
            //必选
            String title = json.get(QzoneShare.SHARE_TO_QQ_TITLE).toString();
            params.putString(QzoneShare.SHARE_TO_QQ_TITLE, title);//分享的标题, 最长30个字符。
            String targetUrl = json.get(QzoneShare.SHARE_TO_QQ_TARGET_URL).toString();
            params.putString(QzoneShare.SHARE_TO_QQ_TARGET_URL, targetUrl);//这条分享消息被好友点击后的跳转URL。
            //可选
            if (json.has(QzoneShare.SHARE_TO_QQ_SUMMARY)) {
                String summary = json.get(QzoneShare.SHARE_TO_QQ_SUMMARY).toString();
                params.putString(QzoneShare.SHARE_TO_QQ_SUMMARY, summary);//分享的消息摘要，最长40个字。可选
            }
            if (json.has(QzoneShare.SHARE_TO_QQ_IMAGE_URL)) {
                ArrayList<String> paths = new ArrayList<String>();
                JSONArray jsonArray = json.getJSONArray(QzoneShare.SHARE_TO_QQ_IMAGE_URL);
                for (int i = 0; i < jsonArray.length(); i++) {
                    String path = jsonArray.opt(i).toString();
                    paths.add(getImageUrl(path));
                }
                params.putStringArrayList(QzoneShare.SHARE_TO_QQ_IMAGE_URL, paths);//分享图片的URL或者本地路径
            }
            doToQZoneShare(params);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void doToQZoneShare(Bundle params) {
        //mTencent.shareToQzone((Activity)mContext, params, qqShareListener);
        doToStartTransitActivity(QQTransitActivity.UEX_QQ_TRANSIT_ACTIVITY_SHARE_TO_QZONE, params);
    }

    public void getUserInfo(String[] param) {
        if (param.length < 1) {
            return;
        }
        String appId = param[0];
        if (param.length == 2) {
            getUserInfoFunId = param[1];
        }
        initTencent(appId);
        UserInfo mInfo = new UserInfo(mContext, mTencent.getQQToken());
        mInfo.getUserInfo(new BaseUiListener() {
            @Override
            protected void doComplete(JSONObject values) {
                // json 类型回调
                String js = SCRIPT_HEADER + "if(" + CB_USER_INFO_QQ + "){"
                        + CB_USER_INFO_QQ + "(" + 0 + ","
                        + EUExCallback.F_C_JSON + "," + values.toString()
                        + ");}";
                Log.i(TAG, "cbGetUserInfo :" + js);
                onCallback(js);
                if (getUserInfoFunId != null) {
                    callbackToJs(Integer.parseInt(getUserInfoFunId), false, values);
                }
            }
        });
    }

    //SDK升级,需要onActivityResult接收回调数据
    private void doToStartTransitActivity(int event, Bundle params) {
        Intent intent = new Intent(mContext, QQTransitActivity.class);
        //intent.putExtra(QQTransitActivity.UEX_QQ_TRANSIT_ACTIVITY_KEY_INTANCE, this);
        mIntance = this;
        intent.putExtra(ConstantUtils.UEX_QQ_TRANSIT_ACTIVITY_KEY_EVENT, event);
        intent.putExtra(ConstantUtils.UEX_QQ_TRANSIT_ACTIVITY_KEY_PARAMS, params);
        startActivity(intent);
    }
}