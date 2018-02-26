package io.jpress.wechat.utils;

import java.math.BigDecimal;

/**  
* @Description: 微信企业支付金额转换工具
* @author wangyong  
* @date 2018年2月25日 上午9:39:02
*/
public class MoneyUtil {

    /**
     * 元转分
     * @param yuan
     * @return
     */
    public static Integer Yuan2Fen(Double yuan) {
        return new BigDecimal(String.valueOf(yuan)).movePointRight(2).intValue();
    }

    /**
     * 分转元
     * @param fen
     * @return
     */
    public static Double Fen2Yuan(Integer fen) {
        return new BigDecimal(fen).movePointLeft(2).doubleValue();
    }
}
