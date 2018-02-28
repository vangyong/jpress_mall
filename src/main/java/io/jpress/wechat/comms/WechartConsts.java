package io.jpress.wechat.comms;

public class WechartConsts {
	
	//商家支付到个人，校验姓名
	public final static String CHECK_NAME_NO_CHECK = "NO_CHECK";
	public final static String CHECK_NAME_FORCE_CHECK = "FORCE_CHECK";
	
	//商家支付到个人，成功标识
	public final static String RESULT_CODE_SUCCESS = "SUCCESS";
	public final static String RESULT_CODE_FAIL = "FAIL";
	
	//商家支付到个人，错误提示码
	public final static String NO_AUTH = "NO_AUTH"; //没有该接口权限
	public final static String AMOUNT_LIMIT = "AMOUNT_LIMIT"; //付款失败，因你已违反《微信支付商户平台使用协议》，单笔单次付款下限已被调整为5元
	public final static String PARAM_ERROR = "PARAM_ERROR"; //参数错误
	public final static String OPENID_ERROR = "OPENID_ERROR"; //Openid错误
	public final static String SEND_FAILED = "SEND_FAILED"; //付款错误
	public final static String NOTENOUGH = "NOTENOUGH"; //余额不足
	public final static String SYSTEMERROR = "SYSTEMERROR"; //系统繁忙，请稍后再试
	public final static String NAME_MISMATCH = "NAME_MISMATCH"; //姓名校验出错
	public final static String SIGN_ERROR = "SIGN_ERROR"; //签名错误
	public final static String XML_ERROR = "XML_ERROR"; //Post内容出错
	public final static String FATAL_ERROR = "FATAL_ERROR"; //两次请求参数不一致
	public final static String FREQ_LIMIT = "FREQ_LIMIT"; //超过频率限制，请稍后再试
	public final static String MONEY_LIMIT = "MONEY_LIMIT"; //已经达到今日付款总额上限/已达到付款给此用户额度上限
	public final static String CA_ERROR = "CA_ERROR"; //证书出错
	public final static String V2_ACCOUNT_SIMPLE_BAN = "V2_ACCOUNT_SIMPLE_BAN"; //无法给非实名用户付款
	public final static String PARAM_IS_NOT_UTF8 = "PARAM_IS_NOT_UTF8"; //请求参数中包含非utf8编码字符
	
	
	
	
	

}
