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
import com.jfinal.log.Log;
import com.jfinal.weixin.sdk.api.ApiConfig;
import com.jfinal.weixin.sdk.api.ApiConfigKit;
import com.jfinal.weixin.sdk.api.ApiResult;
import com.jfinal.weixin.sdk.api.JsTicket;
import com.jfinal.weixin.sdk.api.JsTicketApi;
import com.jfinal.weixin.sdk.api.JsTicketApi.JsApiType;

import io.jpress.Consts;
import io.jpress.model.User;
import io.jpress.model.query.OptionQuery;
import io.jpress.model.query.UserQuery;
import io.jpress.utils.CookieUtils;
import io.jpress.utils.StringUtils;
import io.jpress.wechat.utils.AuthJsApiUtils;

public class WechatUserInterceptor implements Interceptor {

	public static final String AUTHORIZE_URL = "https://open.weixin.qq.com/connect/oauth2/authorize" + "?appid={appid}"
			+ "&redirect_uri={redirecturi}" + "&response_type=code" + "&scope=snsapi_userinfo"
			+ "&state=235&connect_redirect=1#wechat_redirect";

	private static final Log log = Log.getLog(WechatUserInterceptor.class);
    
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
		
		Controller controller = inv.getController();
		HttpServletRequest request = controller.getRequest();
		
		// 获取用户将要去的路径
        String queryString = request.getQueryString();
        // 被拦截前的请求URL
        String toUrl = request.getRequestURI();
        toUrl = StringUtils.urlDecode(toUrl);//解码，因为ng中设置的302跳转是编码后的(为了解决安卓微信浏览器不能自动跳转带？的302url),jiangjb,20180912
        if (StringUtils.isNotBlank(queryString)) {
            toUrl =  toUrl.concat("?").concat(queryString);
        }
        toUrl = toUrl.replaceAll(request.getContextPath(), "");
        //当前url
        String currUrl = request.getScheme() + "://" + 
                (request.getServerName().startsWith("1") ? request.getServerName()+":"+request.getServerPort() : request.getServerName()) + 
                request.getContextPath() + request.getRequestURI();//这个地址是浏览器的地址，用来签名的话，必须与浏览器保持一致，对中文是encode了的,jiangjb,20180914
        toUrl = StringUtils.urlEncode(toUrl);
		
		
//		//1、获取AccessToken  
//		String accessToken = AuthJsApiUtils.getAccessToken(OptionQuery.me().findValue(Consts.WECHAT_APPID),  OptionQuery.me().findValue(Consts.WECHAT_APPSECRET));
//		//2、获取Ticket 
//		String jsapi_ticket = AuthJsApiUtils.getTicket(accessToken);
        
		//获取AccessToken和Ticket要使用缓存，每次获取太费时间,jiangjb,2018.06.26
        ApiConfig ac = WechatApi.getApiConfig();
        ApiConfigKit.setThreadLocalApiConfig(ac);
		JsTicket jsTicket = JsTicketApi.getTicket(JsApiType.jsapi);
		String jsapi_ticket = jsTicket.getTicket();
		
		Map<String, Object> newSignature = AuthJsApiUtils.sign(appid, jsapi_ticket, currUrl);
		
		String signature = String.valueOf(newSignature.get("signature"));
		String timestamp = String.valueOf(newSignature.get("timestamp"));
		String nonceStr =  String.valueOf(newSignature.get("nonceStr"));
		
		//先从数据库中获取，判断时间戳是否超过2两小时，如果超过则重新获取签名
		/*String signature = OptionQuery.me().findValue(Consts.WECHAT_SIGNATURE);
		String timestamp = OptionQuery.me().findValue(Consts.WECHAT_TIMESTAMP);
		String nonceStr = OptionQuery.me().findValue(Consts.WECHAT_NONCESTR);
		if(StringUtils.areNotBlank(signature) && StringUtils.areNotBlank(timestamp)) {
			long current_time = System.currentTimeMillis()/1000;
			if(current_time>Long.valueOf(timestamp)+60*60*2) {
				ApiConfig apiConfig = new ApiConfig("token",appid,appsecret); 
				ApiConfigKit.setThreadLocalApiConfig(apiConfig);
				//1、获取AccessToken  
				//AccessToken accessToken = AccessTokenApi.getAccessToken();
				String accessToken = AuthJsApiUtils.getAccessToken(OptionQuery.me().findValue(Consts.WECHAT_APPID),  OptionQuery.me().findValue(Consts.WECHAT_APPSECRET));
				//2、获取Ticket 
				String jsapi_ticket = AuthJsApiUtils.getTicket(accessToken);
				//3、获取签名
				//String web_domain = OptionQuery.me().findValue("web_domain");
				
				String web_domain = request.getRequestURL().toString();
				
				
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
			//AccessToken accessToken = AccessTokenApi.getAccessToken();
			String accessToken = AuthJsApiUtils.getAccessToken(OptionQuery.me().findValue(Consts.WECHAT_APPID),  OptionQuery.me().findValue(Consts.WECHAT_APPSECRET));
			//2、获取Ticket 
			String jsapi_ticket = AuthJsApiUtils.getTicket(accessToken);
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
		}*/

		
		request.setAttribute("signature", signature);
		request.setAttribute("timestamp", timestamp);
		request.setAttribute("nonceStr", nonceStr);
		String userJson = inv.getController().getSessionAttr(Consts.SESSION_WECHAT_USER);
		
		if (StringUtils.isNotBlank(userJson)) {
		    String openid = new ApiResult(userJson).getStr("openid");
		    User currUser = UserQuery.me().findByOpenId(openid);
	        request.setAttribute("userId", currUser.getId());//缓存当前用户id
			inv.invoke();
			return;
		}
		
		String redirectUrl = request.getScheme() + "://" + 
		        (request.getServerName().startsWith("1") ? request.getServerName()+":"+request.getServerPort() : request.getServerName()) + 
		        request.getContextPath() +
		        "/wechat/callback?goto=" + toUrl;
		redirectUrl = StringUtils.urlEncode(redirectUrl);

		String url = AUTHORIZE_URL.replace("{redirecturi}", redirectUrl).replace("{appid}", appid.trim());
		log.info("微信回调地址；" + url);
		controller.redirect(url);

	}

	

}
