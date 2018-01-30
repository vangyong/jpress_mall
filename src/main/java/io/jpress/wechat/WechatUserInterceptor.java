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

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import com.jfinal.aop.Interceptor;
import com.jfinal.aop.Invocation;
import com.jfinal.core.Controller;
import com.jfinal.plugin.ehcache.CacheKit;
import com.jfinal.weixin.sdk.api.AccessToken;
import com.jfinal.weixin.sdk.api.AccessTokenApi;
import com.jfinal.weixin.sdk.api.ApiConfig;
import com.jfinal.weixin.sdk.api.ApiConfigKit;
import io.jpress.Consts;
import io.jpress.model.Option;
import io.jpress.model.query.OptionQuery;
import io.jpress.utils.StringUtils;
import io.jpress.wechat.utils.AuthJsApiUtils;

public class WechatUserInterceptor implements Interceptor {

	public static final String AUTHORIZE_URL = "https://open.weixin.qq.com/connect/oauth2/authorize" + "?appid={appid}"
			+ "&redirect_uri={redirecturi}" + "&response_type=code" + "&scope=snsapi_userinfo"
			+ "&state=235#wechat_redirect";

	@Override
	public void intercept(Invocation inv) {

		String appid = OptionQuery.me().findValue(Consts.WECHAT_APPID);
		if (StringUtils.isBlank(appid)) {
			inv.invoke();
			return;
		}
		
		String appsecret = OptionQuery.me().findValue(Consts.WECHAT_APPSECRET);
		if (StringUtils.isBlank(appsecret)) {
			inv.invoke();
			return;
		}
		
		//先从数据库中获取，判断时间戳是否超过2两小时，如果超过则重新获取签名
		String signature = OptionQuery.me().findValue(Consts.WECHAT_SIGNATURE);
		String timestamp = OptionQuery.me().findValue(Consts.WECHAT_TIMESTAMP);
		String nonceStr = OptionQuery.me().findValue(Consts.WECHAT_NONCESTR);
		if(signature!=null&&timestamp!=null) {
			long current_time = System.currentTimeMillis()/1000;
			if(current_time>Long.valueOf(timestamp)+60*60*2) {
				ApiConfig apiConfig = new ApiConfig("token",appid,appsecret); 
				ApiConfigKit.setThreadLocalApiConfig(apiConfig);
				//1、获取AccessToken  
				AccessToken accessToken = AccessTokenApi.getAccessToken();
				//2、获取Ticket 
				String jsapi_ticket = AuthJsApiUtils.getTicket(accessToken.getAccessToken());
				//3、获取签名
				String web_domain = OptionQuery.me().findValue("web_domain");
				Map<String, Object> newSignature = AuthJsApiUtils.sign(appid, jsapi_ticket, web_domain);
				
				signature = String.valueOf(newSignature.get("signature"));
				timestamp = String.valueOf(newSignature.get("timestamp"));
				nonceStr = String.valueOf(newSignature.get("nonceStr"));
				
				//签名
				Option option_signature = OptionQuery.me().findByKey(Consts.WECHAT_SIGNATURE);
				option_signature.setOptionKey(Consts.WECHAT_SIGNATURE);
				option_signature.setOptionValue(String.valueOf(newSignature.get("signature")));
				option_signature.saveOrUpdate();
				//时间戳
				Option option_timestamp = OptionQuery.me().findByKey(Consts.WECHAT_TIMESTAMP);
				option_timestamp.setOptionKey(Consts.WECHAT_TIMESTAMP);
				option_timestamp.setOptionValue(String.valueOf(newSignature.get("timestamp")));
				option_timestamp.saveOrUpdate();
				//随机字符串
				Option option_nonceStr = OptionQuery.me().findByKey(Consts.WECHAT_NONCESTR);
				option_nonceStr.setOptionKey(Consts.WECHAT_NONCESTR);
				option_nonceStr.setOptionValue(String.valueOf(newSignature.get("nonceStr")));
				option_nonceStr.saveOrUpdate();
				//更新缓存
				CacheKit.put(Option.CACHE_NAME, Consts.WECHAT_SIGNATURE, signature);
				CacheKit.put(Option.CACHE_NAME, Consts.WECHAT_TIMESTAMP, timestamp);
				CacheKit.put(Option.CACHE_NAME, Consts.WECHAT_NONCESTR, nonceStr);
			}
		}else {
			ApiConfig apiConfig = new ApiConfig("token",appid,appsecret); 
			ApiConfigKit.setThreadLocalApiConfig(apiConfig);
			//1、获取AccessToken  
			AccessToken accessToken = AccessTokenApi.getAccessToken();
			//2、获取Ticket 
			String jsapi_ticket = AuthJsApiUtils.getTicket(accessToken.getAccessToken());
			//3、获取签名
			String web_domain = OptionQuery.me().findValue("web_domain");
			Map<String, Object> newSignature = AuthJsApiUtils.sign(appid, jsapi_ticket, web_domain);
			
			signature = String.valueOf(newSignature.get("signature"));
			timestamp = String.valueOf(newSignature.get("timestamp"));
			nonceStr = String.valueOf(newSignature.get("nonceStr"));
			
			//签名
			Option option_signature = OptionQuery.me().findByKey(Consts.WECHAT_SIGNATURE);
			option_signature.setOptionKey(Consts.WECHAT_SIGNATURE);
			option_signature.setOptionValue(String.valueOf(newSignature.get("signature")));
			option_signature.saveOrUpdate();
			//时间戳
			Option option_timestamp = OptionQuery.me().findByKey(Consts.WECHAT_TIMESTAMP);
			option_timestamp.setOptionKey(Consts.WECHAT_TIMESTAMP);
			option_timestamp.setOptionValue(String.valueOf(newSignature.get("timestamp")));
			option_timestamp.saveOrUpdate();
			//随机字符串
			Option option_nonceStr = OptionQuery.me().findByKey(Consts.WECHAT_NONCESTR);
			option_nonceStr.setOptionKey(Consts.WECHAT_NONCESTR);
			option_nonceStr.setOptionValue(String.valueOf(newSignature.get("nonceStr")));
			option_nonceStr.saveOrUpdate();
			//更新缓存
			CacheKit.put(Option.CACHE_NAME, Consts.WECHAT_SIGNATURE, signature);
			CacheKit.put(Option.CACHE_NAME, Consts.WECHAT_TIMESTAMP, timestamp);
			CacheKit.put(Option.CACHE_NAME, Consts.WECHAT_NONCESTR, nonceStr);
		}

		Controller controller = inv.getController();
		String userJson = inv.getController().getSessionAttr(Consts.SESSION_WECHAT_USER);
		if (StringUtils.isNotBlank(userJson)) {
			inv.invoke();
			return;
		}
		HttpServletRequest request = controller.getRequest();
		request.setAttribute("signature", signature);
		request.setAttribute("timestamp", timestamp);
		request.setAttribute("nonceStr", nonceStr);
		
		
		// 获取用户将要去的路径
		String queryString = request.getQueryString();
		// 被拦截前的请求URL
		String toUrl = request.getRequestURI();
		String totalUrl = request.getRequestURL().toString();
		if (StringUtils.isNotBlank(queryString)) {
			String newQueryString =queryString;
			if(queryString.contains("?")) {
				newQueryString = queryString.replace("?","&");
			}
 			toUrl =  toUrl.concat("?").concat(newQueryString);
		}
		toUrl = toUrl.replaceAll(request.getContextPath(), "");
		toUrl = StringUtils.urlEncode(toUrl);
		String redirectUrl = request.getScheme() + "://" + 
		        (request.getServerName().startsWith("1") ? request.getServerName()+":"+request.getServerPort() : request.getServerName()) + 
		        request.getContextPath() +
		        "/wechat/callback?goto=" + toUrl;
		redirectUrl = StringUtils.urlEncode(redirectUrl);

		String url = AUTHORIZE_URL.replace("{redirecturi}", redirectUrl).replace("{appid}", appid.trim());
		controller.redirect(url);

	}
	
public static void main(String[] args) {
	String a = "uid=122?from=singlemsg";
	String asd = a.replace("?", "&");
	
	
}
	

}
