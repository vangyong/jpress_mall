/**
 * 
 */
package io.jpress.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**  
* @Description: TODO(随机数工具类)
* @author heguoliang  
* @date 2016年10月10日 上午8:50:13
*/
public class RandomUtils {

    public static final String KeyString = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    
	public static String randomKey(String startaKey, String endKey){
		String simpleDate=new SimpleDateFormat("yyMMddHHmmss").format(new Date());
		String result=simpleDate+randomSixNum();
		if(StringUtils.isNotBlank(startaKey)){
			result=startaKey+result;
		}
		if(StringUtils.isNotBlank(endKey)){
			result=result+endKey;
		}
		return result;
	}
	
	public static Integer randomKey(int count){
		Random random=new Random();
		Integer result=random.nextInt(count);
		return result+1;
	}

	/**
	 * 随机生成六位随机数
	 * @return
	 */
	public static int randomSixNum(){
		int randomNum=(int)((Math.random()*9+1)*100000);
		return randomNum;
	}
	
	//获取指定位数的随机字符串(包含小写字母、大写字母、数字,0<length)
	public static String getRandomString(String preStr, int length) {
	    //随机字符串的随机字符库
	    StringBuilder sb = new StringBuilder();
	    int len = KeyString.length();
	    sb.append(preStr);
	    for (int i = 0; i < length; i++) {
	       sb.append(KeyString.charAt((int) Math.round(Math.random() * (len - 1))));
	    }
	    return sb.toString();
	}
public static void main(String[] args) {
    System.out.println(RandomUtils.getRandomString("Coupon",19));
}
}
