/**
 * 
 */
package io.jpress.wechat.utils;

import com.jfinal.log.Log;
import com.jfinal.weixin.sdk.api.ApiResult;
import io.jpress.utils.HttpUtils;
import io.jpress.utils.StringUtils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**  
* @Description: TODO(JS-SDK使用权限签名)
* @author heguoliang  
* @date 2016年8月8日 下午4:54:52
*/
public class AuthJsApiUtils {
	
	private static Log log=Log.getLog(AuthJsApiUtils.class);
	
	// 获取access_token的接口地址（GET） 限200（次/天） 
	private final static String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=APPID&secret=APPSECRET";
	
	//采用http GET方式请求获得jsapi_ticket  
    private final static String JSAPI_TICKET_URL = "https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token=ACCESS_TOKEN&type=jsapi"; 
    
    private static String jsapiTicketTemp = null;
    
    /**js签名
     * 
     * @param url
     * @return
     */
    public static Map<String ,Object> authJsApi(String appId,String url){
    	return sign(appId, jsapiTicketTemp, url);
    } 
    
	/**获取access_token
	 * 
	 * @param appId 凭证
	 * @param appSecret 密钥
	 * @return
	 */
	public static String getAccessToken(String appId, String appSecret){
		String accessToken = null;
		String requestUrl = ACCESS_TOKEN_URL.replace("APPID", appId).replace("APPSECRET", appSecret);
		ApiResult apiResult = null;
		try {
			String jsonResult = HttpUtils.get(requestUrl);
			if (StringUtils.isNotBlank(jsonResult)) {
				apiResult=new ApiResult(jsonResult);
	            accessToken = (apiResult.getStr("access_token"));  
	        }
		} catch (Exception e) {
			accessToken = null;  
            log.warn("获取AccessToken失败errcode:"+apiResult.getInt("errcode")+" errmsg:"+apiResult.getStr("errmsg")); 
			e.printStackTrace();
		} 		
		return accessToken;
	}
	
	/**获取ticket
	 * 
	 * @param accessToken
	 * @return
	 */
	public static String getTicket(String accessToken) {  
		String ticket = null;  
        String requestUrl = JSAPI_TICKET_URL.replace("ACCESS_TOKEN", accessToken);
        ApiResult apiResult = null;
		try {
			String jsonResult = HttpUtils.get(requestUrl);
			if (StringUtils.isNotBlank(jsonResult)) {
				apiResult=new ApiResult(jsonResult);
				ticket = (apiResult.getStr("ticket"));  
	        }
		} catch (Exception e) {
			accessToken = null;  
			e.printStackTrace();
			log.warn("获取JsapiTicket失败 errcode:"+ 
			        (apiResult != null ? (apiResult.getInt("errcode")+"  errmsg:"+apiResult.getStr("errmsg")) : "")); 
		} 
        return ticket;  
    } 
	
	/**
	 * 设置ticket
	 */
    public static void initAndSetTicket(String appId, String appSecret){
		if(StringUtils.isNotBlank(appId) && StringUtils.isNotBlank(appSecret)){
			String accessToken = getAccessToken(appId, appSecret);
			if(StringUtils.isNotBlank(accessToken)){
				String ticket = getTicket(accessToken);
				if(StringUtils.isNotBlank(ticket)){
					jsapiTicketTemp=ticket;
				}else{
					log.warn("execute initAndSetAccessToken ticket为空");
				}
			}else{
				log.warn("execute initAndSetAccessToken accessToken为空");
			}
		}else{
			log.warn("execute initAndSetAccessToken appid或者appsecret为空");
		}
	}
	
	/**签名
	 * 
	 * @param jsapi_ticket 
	 * @param url
	 * @return
	 */
    public static Map<String, Object> sign(String appId, String jsapi_ticket, String url) {  
    	Map<String, Object> map= new HashMap<String, Object>();        
        String nonce_str = create_nonce_str();  
        String timestamp = create_timestamp();  
        String string1;  
        String signature = "";  
        string1 = "jsapi_ticket=" + jsapi_ticket + "&noncestr=" + nonce_str+ "&timestamp=" + timestamp + "&url=" + url;  
        try {          	
            MessageDigest crypt = MessageDigest.getInstance("SHA-1");  
            crypt.reset();  
            crypt.update(string1.getBytes("UTF-8"));  
            signature = byteToHex(crypt.digest());
            
            map.put("appId", appId);   
            map.put("nonceStr", nonce_str);  
            map.put("timestamp", timestamp);  
            map.put("signature", signature); 
        } catch (NoSuchAlgorithmException e) {  
            e.printStackTrace(); 
            log.warn(e.toString());
        } catch (UnsupportedEncodingException e) {  
            e.printStackTrace(); 
            log.warn(e.toString());
        } catch (Exception e) {  
            e.printStackTrace(); 
            log.warn(e.toString());
        }          
        return map;  
    }
	
	private static String byteToHex(final byte[] hash) {  
        Formatter formatter = new Formatter();  
        for (byte b : hash) {  
            formatter.format("%02x", b);  
        }  
        String result = formatter.toString();  
        formatter.close();  
        return result;  
    }  
  
	/*随机字符串*/
    private static String create_nonce_str() {  
        return UUID.randomUUID().toString();  
    }  
  
    /*时间戳*/
    private static String create_timestamp() {  
        return Long.toString(System.currentTimeMillis() / 1000);  
    }
	
}
