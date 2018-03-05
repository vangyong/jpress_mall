/**
 * Copyright (c) 2015-2016, Michael Yang 杨福海 (fuhai999@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.jpress;

import io.jpress.model.query.OptionQuery;

import java.math.BigInteger;

public class Consts {

	public final static String WEB_DOMAIN=OptionQuery.me().findValue("web_domain");

	//支付宝
	public static class Alipay{
		// 请求网关地址
		public final static String URL = "https://openapi.alipay.com/gateway.do";
		// 返回格式
		public final static String FORMAT = "json";
		// RSA2
		public final static String SIGNTYPE = "RSA2";
		// appid
		public final static String APPID = OptionQuery.me().findValue("alipay_appid");
		// 应用私钥
		public final static String RSA_PRIVATE_KEY = OptionQuery.me().findValue("alipay_rsa_private_key");
		// 应用公钥
		public final static String RSA_PUBLIC_KEY = OptionQuery.me().findValue("alipay_rsa_public_key");
		// 支付宝公钥
		public final static String PUBLIC_KEY = OptionQuery.me().findValue("alipay_public_key");
		// 超时时间
		public final static String TIMEOUT_EXPRESS = OptionQuery.me().findValue("alipay_timeout_express");
		// 同步跳转地址
		public final static String RETURN_URL = WEB_DOMAIN+"/transaction/alipayReturn";
	}

	public static final String COOKIE_LOGINED_USER = "jp_user";

	public static final String CHARTSET_UTF8 = "UTF-8";
	public static final String CHARTSET_ISO_8859_1 = "ISO-8859-1";

	public static final String ROUTER_CONTENT = "/c";
	public static final String ROUTER_TAXONOMY = "/t";
	public static final String ROUTER_USER = "/user";
	public static final String ROUTER_USER_CENTER = ROUTER_USER + "/center";
	public static final String ROUTER_USER_LOGIN = ROUTER_USER + "/login";
	public static final String ROUTER_USER_REGISTER = ROUTER_USER + "/register";
	
	public static final String ROUTER_VERIFYCODE = "/verifyCode";

	public static final int ERROR_CODE_NOT_VALIDATE_CAPTHCHE = 1;
	public static final int ERROR_CODE_USER_EMPTY = 2;
	public static final int ERROR_CODE_SYSTEM_ERROR = 3;
	public static final int ERROR_CODE_USERNAME_EMPTY = 4;
	public static final int ERROR_CODE_USERNAME_EXIST = 5;
	public static final int ERROR_CODE_EMAIL_EMPTY = 6;
	public static final int ERROR_CODE_EMAIL_EXIST = 7;
	public static final int ERROR_CODE_PHONE_EMPTY = 8;
	public static final int ERROR_CODE_PHONE_EXIST = 9;
	public static final int ERROR_CODE_PASSWORD_EMPTY = 10;
	public static final int ERROR_CODE_USER_NOT_EXIST = 11;
	public static final int ERROR_CODE_PASSWORD_ERROR = 12;

	public static final String ATTR_PAGE_NUMBER = "_page_number";
	public static final String ATTR_USER = "USER";
	public static final String ATTR_GLOBAL_WEB_NAME = "WEB_NAME";
	public static final String ATTR_GLOBAL_WEB_TITLE = "WEB_TITLE";
	public static final String ATTR_GLOBAL_WEB_SUBTITLE = "WEB_SUBTITLE";
	public static final String ATTR_GLOBAL_META_KEYWORDS = "META_KEYWORDS";
	public static final String ATTR_GLOBAL_META_DESCRIPTION = "META_DESCRIPTION";

	public static final String SESSION_WECHAT_USER = "_wechat_user";

	public static final String MODULE_MALL = "mall"; // 商城模型
	public static final String MODULE_ARTICLE = "article"; // 文章模型
	public static final String MODULE_PAGE = "page"; // 页面模型
	public static final String MODULE_FOURM = "forum"; // 论坛模型
	public static final String MODULE_MENU = "menu"; // 菜单
	public static final String MODULE_QA = "qa"; // QA问答
	public static final String MODULE_GOODS = "goods"; // 商品
	public static final String MODULE_GOODS_SHOPPING_CART = "goods_shopping_cart"; // 购物车
	public static final String MODULE_GOODS_ORDER = "goods_order"; // 订单
	public static final String MODULE_WECHAT_MENU = "wechat_menu"; // 微信菜单
	public static final String MODULE_WECHAT_REPLY = "wechat_reply"; // 微信自动回复
	public static final String MODULE_USER_COLLECTION = "user_collection"; // 用户搜藏
	public static final String MODULE_USER_RELATIONSHIP = "user_relationship"; // 用户关系（比如：好友，关注等）
	public static final String MODULE_API_APPLICATION = "api_application"; // API应用，可以对应用进行管理
	
	public static final String TAXONOMY_TEMPLATE_PREFIX = "for$";

	public final static String INDEX_BANNER="index_banner";
	public final static BigInteger INDEX_BANNER_ID=BigInteger.valueOf(1);
	
	public static final String WECHAT_APPID = "wechat_appid";
	public static final String WECHAT_APPSECRET = "wechat_appsecret";
	public static final String WECHAT_SIGNATURE = "wechat_signature";
	public static final String WECHAT_TIMESTAMP = "wechat_timestamp";
	public static final String WECHAT_TOKEN = "wechat_timestamp";
	public static final String WECHAT_NONCESTR = "wechat_nonceStr";

	//企业付款到个人
	public static final String WECHAT_PAY_MCHID = "wechat_pay_mchid";
	public static final String WECHAT_MCH_SECRET = "wechat_pay_mchsecret";
	public static final String WECHAT_PAY_SPBILL_CREATE_IP = "wechat_pay_spbill_create_ip";
	
	//腾讯云短信服务
	public static final String TENCENT_SMS_APPID = "tencent_sms_APPID";
	public static final String TENCENT_SMS_APPKEY = "tencent_sms_APPKEY";

}
