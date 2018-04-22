package io.jpress.utils;

import java.math.BigDecimal;

/**
 * @author heguoliang
 * @Description: TODO(金钱转换工具)
 * @date 2017-5-16 16:17
 */
public class AmountUtils {

    /**金额为分的格式 */
    public static final String CURRENCY_FEN_REGEX = "\\-?[0-9]+";

    /**
     * 将元为单位的转换为分 （乘100）
     *
     * @param amount
     * @return
     */
    public static BigDecimal changeY2F(BigDecimal amount) throws Exception{
        if(!amount.toString().matches(CURRENCY_FEN_REGEX)) {
            throw new Exception("金额格式有误");
        }
        return amount.multiply(new BigDecimal(100));
    }

    /**
     * 将分为单位的转换为元 （除100）
     *
     * @param amount
     * @return
     * @throws Exception
     */
    public static BigDecimal changeF2Y(BigDecimal amount) throws Exception{
        if(!amount.toString().matches(CURRENCY_FEN_REGEX)) {
            throw new Exception("金额格式有误");
        }
        return amount.divide(new BigDecimal(100));
    }

}
