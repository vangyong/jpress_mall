/**
 * Copyright (c) 2015-2016, Michael Yang 杨福海 (fuhai999@gmail.com).
 *
 * Licensed under the GNU Lesser General Public License (LGPL) ,Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.jpress.wechat;

import com.jfinal.weixin.sdk.api.AccessTokenApi;
import com.jfinal.weixin.sdk.api.ApiConfig;
import com.jfinal.weixin.sdk.api.ApiResult;
import com.jfinal.weixin.sdk.api.MenuApi;
import com.jfinal.weixin.sdk.api.UserApi;
import com.jfinal.weixin.sdk.kit.ParaMap;

import io.jpress.model.query.OptionQuery;
import io.jpress.utils.HttpUtils;
import io.jpress.utils.StringUtils;

public class WechatApi {

    private static String sendApiUrl = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=";
    
	public static ApiConfig getApiConfig() {
		ApiConfig config = new ApiConfig();
		config.setAppId(OptionQuery.me().findValue("wechat_appid"));
		config.setAppSecret(OptionQuery.me().findValue("wechat_appsecret"));
		config.setToken(OptionQuery.me().findValue("wechat_token"));
		return config;
	}


	public static ApiResult createMenu(String jsonString) {
		return MenuApi.createMenu(jsonString);
	}
	
	public static ApiResult getUserInfo(String openId){
		return  UserApi.getUserInfo(openId);
	}
	
	public static ApiResult getUserInfo(String openId, String accessToken) {

	    ParaMap pm = ParaMap.create("access_token", accessToken).put("openid", openId).put("lang", "zh_CN");
	    String jsonResult = null;
	    try {
	        jsonResult = HttpUtils.get("https://api.weixin.qq.com/sns/userinfo", pm.getData());
        } catch (Exception e) {
            e.printStackTrace();
        }
	    if (jsonResult == null)
            return null;

        return new ApiResult(jsonResult);
    }
	
	public static ApiResult getOpenId(String appId, String appSecret, String code) {

		String url = "https://api.weixin.qq.com/sns/oauth2/access_token" + "?appid={appid}"
				+ "&secret={secret}" + "&code={code}" + "&grant_type=authorization_code";

		String getOpenIdUrl = url.replace("{appid}", appId).replace("{secret}", appSecret);
		if (StringUtils.areNotBlank(code)) {
			getOpenIdUrl = getOpenIdUrl.replace("{code}", code);
		}
		String jsonResult = null;
		try {
			jsonResult = HttpUtils.get(getOpenIdUrl);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (jsonResult == null)
			return null;

		return new ApiResult(jsonResult);
	}

	/**
	 * <b>Description.:发送模板消息</b>和TemplateMsgApi.send方法一样的<br>
	 * <b>Author:jianb.jiang</b>
	 * <br><b>Date:</b> 2018年5月4日 下午2:00:28
     * @param jsonStr json字符串
     * @return {ApiResult}
     */
    public static ApiResult send(String jsonStr) {
        String jsonResult = com.jfinal.weixin.sdk.utils.HttpUtils.post(sendApiUrl + AccessTokenApi.getAccessToken().getAccessToken(), jsonStr);
        return new ApiResult(jsonResult);
    }
}
