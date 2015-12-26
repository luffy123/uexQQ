package org.zywx.wbpalmstar.plugin.uexqq;

import org.json.JSONException;
import org.json.JSONObject;

public class AnalyzeUserInfo {
	/**
	 * 
	 * @param json 
	 * @return
	 */
	public static ModelUserInfo getModel(String data){
		try {
			ModelUserInfo userInfo = new ModelUserInfo();
			//解析数据
			JSONObject model=new JSONObject(data);
			/*"openid":"C9C9F1D2285220AE46B53774376D16F8","access_token":"2.00fQSXPGyEzLXDa060dad8c7fDuscD","oauth_consumer_key":"3238873984"*/
			String openid= model.optString("openid","");
			userInfo.openid=openid;
			
			String access_token= model.optString("access_token","");
			userInfo.access_token=access_token;
			
			String oauth_consumer_key= model.optString("oauth_consumer_key","");
			userInfo.oauth_consumer_key=oauth_consumer_key;
			
			return userInfo;
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return null;
		
	}

}
