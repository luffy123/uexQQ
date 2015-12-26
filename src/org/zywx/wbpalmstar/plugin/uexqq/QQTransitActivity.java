package org.zywx.wbpalmstar.plugin.uexqq;

import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import com.tencent.tauth.Tencent;

/**
 * 过度Activity
 * 
 * @Description 更新的QQSDK要求在onActivityResult中设置回调
 * 
 */
@SuppressLint("NewApi")
public class QQTransitActivity extends Activity {
	
	public static final String UEX_QQ_TRANSIT_ACTIVITY_KEY_INTANCE = "uexqqTransitActivityKeyEUExQQ";
	public static final String UEX_QQ_TRANSIT_ACTIVITY_KEY_EVENT = "uexqqTransitActivityKeyEvent";
	public static final String UEX_QQ_TRANSIT_ACTIVITY_KEY_PARAMS = "uexqqTransitActivityKeyParams";
	
	public static final int UEX_QQ_TRANSIT_ACTIVITY_LOGIN = 0;
	public static final int UEX_QQ_TRANSIT_ACTIVITY_SHARE_TO_QQ = 1;
	public static final int UEX_QQ_TRANSIT_ACTIVITY_SHARE_TO_QZONE = 2;
	
	private EUExQQ mIntance;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		//mIntance = (EUExQQ)intent.getParcelableExtra(UEX_QQ_TRANSIT_ACTIVITY_KEY_INTANCE);
		mIntance = EUExQQ.mIntance;
		int event = intent.getIntExtra(UEX_QQ_TRANSIT_ACTIVITY_KEY_EVENT, -1);
		Bundle params = intent.getBundleExtra(UEX_QQ_TRANSIT_ACTIVITY_KEY_PARAMS);
		switch (event) {
		case UEX_QQ_TRANSIT_ACTIVITY_LOGIN:
			if(mIntance != null){
				EUExQQ.mTencent.login(this, "all", mIntance.loginListener);
			}
			break;
		case UEX_QQ_TRANSIT_ACTIVITY_SHARE_TO_QQ:
			if(mIntance != null){
				EUExQQ.mTencent.shareToQQ(this, params, mIntance.qqShareListener);
			}
			break;
		case UEX_QQ_TRANSIT_ACTIVITY_SHARE_TO_QZONE:
			if(mIntance != null){
				EUExQQ.mTencent.shareToQzone(this, params, mIntance.qqShareListener);
			}
			break;
		case -1:
			finish();
			break;

		default:
			finish();
			break;
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(mIntance != null){
			Tencent.onActivityResultData(requestCode, resultCode, data, mIntance.qqShareListener);
		}
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					//过早的finish会导致闪现topActivity 
					ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
			        boolean finish = false;
			        while(!finish){
			        	List<RunningTaskInfo> tasks = am.getRunningTasks(1);
						if (!tasks.isEmpty()) {
							String topActivity = tasks.get(0).topActivity
									.getClassName();
							if ("org.zywx.wbpalmstar.plugin.uexqq.QQTransitActivity"
									.equals(topActivity) || "org.zywx.wbpalmstar.engine.EBrowserActivity".equals(topActivity)) {
								finish = true;
							}
						}
						try {
							Thread.sleep(100);
						} catch (Exception e) {
							e.printStackTrace();
						}
			        }
			        QQTransitActivity.this.finish();
				} catch (Exception e) {
					QQTransitActivity.this.finish();
				}
			}
		}).start();
		//this.finish();
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		Log.i("uexQQ", "onNewIntent");
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return true;
	}
	
	@Override
	protected void onResume() {
		Log.i("uexQQ", "QQTransitActivity onResume");
		super.onResume();
	}
	
	@Override
	protected void onStop() {
		Log.i("uexQQ", "QQTransitActivity onStop");
		super.onStop();
	}
	
	@Override
	protected void onDestroy() {
		mIntance = null;
		super.onDestroy();
		Log.i("uexQQ", "QQTransitActivity onDestroy");
	}
}
