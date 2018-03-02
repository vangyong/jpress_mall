package io.jpress.wechat.utils;

import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONException;

import com.github.qcloudsms.SmsMultiSender;
import com.github.qcloudsms.SmsMultiSenderResult;
import com.github.qcloudsms.SmsSingleSender;
import com.github.qcloudsms.SmsSingleSenderResult;
import com.github.qcloudsms.SmsStatusPullCallbackResult;
import com.github.qcloudsms.SmsStatusPullReplyResult;
import com.github.qcloudsms.SmsStatusPuller;
import com.github.qcloudsms.SmsVoicePromptSender;
import com.github.qcloudsms.SmsVoicePromptSenderResult;
import com.github.qcloudsms.SmsVoiceVerifyCodeSender;
import com.github.qcloudsms.SmsVoiceVerifyCodeSenderResult;
import com.github.qcloudsms.httpclient.HTTPException;

import io.jpress.Consts;
import io.jpress.model.query.OptionQuery;

public class QcloudSmsUtil {
	
	private static final int TENCENT_SMS_APPID = Integer.valueOf(OptionQuery.me().findValue(Consts.TENCENT_SMS_APPID));
	
	private static final String TENCENT_SMS_APPKEY = OptionQuery.me().findValue(Consts.TENCENT_SMS_APPKEY);
	
	 /**
     * 普通单发
	 * @throws IOException 
	 * @throws HTTPException 
	 * @throws JSONException 
     */
    public static SmsSingleSenderResult sendSingle(String phoneNumber,String content) throws JSONException, HTTPException, IOException{
    	  	//初始化单发
    		SmsSingleSender singleSender = new SmsSingleSender(TENCENT_SMS_APPID, TENCENT_SMS_APPKEY);
    		SmsSingleSenderResult singleSenderResult = singleSender.send(0, "86", phoneNumber, content, "", "");
   	    System.out.println(singleSenderResult);
    		return singleSenderResult;
    }
    
    
    /**
     * 模版单发
     * 模板 id 为 1，模板内容为：测试短信，{1}，{2}，{3}，上学
	 * @throws IOException 
	 * @throws HTTPException 
	 * @throws JSONException 
     */
    public static SmsSingleSenderResult sendSingleTemp(int tempId,String phoneNumber,ArrayList<String> params) throws JSONException, HTTPException, IOException{
    	  	//初始化单发
    		SmsSingleSender singleSender = new SmsSingleSender(TENCENT_SMS_APPID, TENCENT_SMS_APPKEY);
    	    SmsSingleSenderResult singleSenderResult = singleSender.sendWithParam("86", phoneNumber, tempId, params, "", "", "");
    	    System.out.println(singleSenderResult);
    		return singleSenderResult;
    }
    
    /**
     * 普通群发
	 * @throws IOException 
	 * @throws HTTPException 
	 * @throws JSONException 
     */
    public static SmsMultiSenderResult sendMulti( ArrayList<String> phoneNumbers,String content) throws JSONException, HTTPException, IOException{
    	  	//初始化单发
    		SmsMultiSender multiSender = new SmsMultiSender(TENCENT_SMS_APPID, TENCENT_SMS_APPKEY);
    		SmsMultiSenderResult multiSenderResult = multiSender.send(0, "86", phoneNumbers, content, "", "");
   	    System.out.println(multiSenderResult);
    		return multiSenderResult;
    }
    
    /**
     * 模版群发
     * 模板 id 为 1，模板内容为：测试短信，{1}，{2}，{3}，上学
	 * @throws IOException 
	 * @throws HTTPException 
	 * @throws JSONException 
     */
    public static SmsMultiSenderResult sendMultiTemp(int tempId, ArrayList<String> phoneNumbers,ArrayList<String> params) throws JSONException, HTTPException, IOException{
    	  	//初始化单发
    		SmsMultiSender multiSender = new SmsMultiSender(TENCENT_SMS_APPID, TENCENT_SMS_APPKEY);
    		SmsMultiSenderResult multiSenderResult = multiSender.sendWithParam("86", phoneNumbers, tempId, params, "", "", "");
   	    System.out.println(multiSenderResult);
    		return multiSenderResult;
    }
    
    /**
     * 拉取短信回执
	 * @throws IOException 
	 * @throws HTTPException 
	 * @throws JSONException 
     */
    public static SmsStatusPullCallbackResult pullStatus(int tempId, ArrayList<String> phoneNumbers,ArrayList<String> params) throws JSONException, HTTPException, IOException{
    	  	//初始化单发
    		SmsStatusPuller pullstatus = new SmsStatusPuller(TENCENT_SMS_APPID, TENCENT_SMS_APPKEY);
    		SmsStatusPullCallbackResult callbackResult = pullstatus.pullCallback(10);
	    System.out.println(callbackResult);
    		return callbackResult;
    }
    
    /**
     * 拉取短信回复
	 * @throws IOException 
	 * @throws HTTPException 
	 * @throws JSONException 
     */
    public static SmsStatusPullReplyResult pullReply(int tempId, ArrayList<String> phoneNumbers,ArrayList<String> params) throws JSONException, HTTPException, IOException{
    		SmsStatusPuller pullstatus = new SmsStatusPuller(TENCENT_SMS_APPID, TENCENT_SMS_APPKEY);
	    SmsStatusPullReplyResult replyResult = pullstatus.pullReply(10);
	    System.out.println(replyResult);
    		return replyResult;
    }
    
    /**
     * 发送通知内容
	 * @throws IOException 
	 * @throws HTTPException 
	 * @throws JSONException 
     */
    public static SmsVoicePromptSenderResult promtVoice(int tempId, String phoneNumber,String content) throws JSONException, HTTPException, IOException{
    		SmsVoicePromptSender smsVoicePromtSender = new SmsVoicePromptSender(TENCENT_SMS_APPID, TENCENT_SMS_APPKEY);
    	    SmsVoicePromptSenderResult smsSingleVoiceSenderResult = smsVoicePromtSender.send("86", phoneNumber, 2,2,content, "");
    	    System.out.println(smsSingleVoiceSenderResult);
    		return smsSingleVoiceSenderResult;
    }
    
    /**
     * 语音验证码发送
	 * @throws IOException 
	 * @throws HTTPException 
	 * @throws JSONException 
     */
    public static SmsVoiceVerifyCodeSenderResult sendVoiceVerifyCode(int tempId, String phoneNumber) throws JSONException, HTTPException, IOException {
    		SmsVoiceVerifyCodeSender smsVoiceVerifyCodeSender = new SmsVoiceVerifyCodeSender(TENCENT_SMS_APPID, TENCENT_SMS_APPKEY);
    	    SmsVoiceVerifyCodeSenderResult smsVoiceVerifyCodeSenderResult = smsVoiceVerifyCodeSender.send("86",phoneNumber, "123",2,"");
    		return smsVoiceVerifyCodeSenderResult;
    }

}
