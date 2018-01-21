package io.jpress.front.controller;

import com.jfinal.aop.Before;
import com.jfinal.kit.HttpKit;
import com.jfinal.kit.PropKit;
import com.jfinal.kit.StrKit;
import com.jfinal.log.Log;
import com.jfinal.plugin.activerecord.tx.Tx;
import com.jfinal.weixin.sdk.api.ApiResult;
import com.jfinal.weixin.sdk.api.PaymentApi;
import com.jfinal.weixin.sdk.api.PaymentApi.TradeType;
import com.jfinal.weixin.sdk.kit.IpKit;
import com.jfinal.weixin.sdk.kit.PaymentKit;
import com.jfinal.weixin.sdk.utils.JsonUtils;

import io.jpress.Consts;
import io.jpress.core.BaseFrontController;
import io.jpress.interceptor.UCodeInterceptor;
import io.jpress.interceptor.UserInterceptor;
import io.jpress.model.*;
import io.jpress.model.query.*;
import io.jpress.router.RouterMapping;
import io.jpress.utils.DateUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * <b>Description:</b>
 * <br><b>ClassName:</b> WechatpayController
 * <br><b>Date:</b> 2018年1月18日 下午9:20:10
 * <br>@author <b>jianb.jiang</b>
 */
@RouterMapping(url = "/wechatpay")
@Before(UserInterceptor.class)
public class WechatpayController extends BaseFrontController {

    private static final String SUCCESS = "SUCCESS";

    private static final Log log = Log.getLog(WechatpayController.class);
    
    String appid = OptionQuery.me().findValue("wechat_appid");
    String partner = OptionQuery.me().findValue("wechat_partner");
    String paternerKey = OptionQuery.me().findValue("wechat_paternerKey");
    String notifyUrl = PropKit.get("wechat_notify_url");
  
    //待支付-微信支付
    @Before(UCodeInterceptor.class)
    public void prepay() {
        try {
            
            String jsonStr = "{\"timeStamp\":\"1516526141\",\"package\":\"prepay_id=wx2018012117150606ac003fb20827751109\",\"paySign\":\"FCFF07F3A6E55C3989840420A59ADF3F\",\"appId\":\"wxd0b33231fc543b7b\",\"signType\":\"MD5\",\"nonceStr\":\"1516526141486\"}";
//            setAttr("json", jsonStr);
            System.out.println(jsonStr);
            renderAjaxResult("操作成功", 0, jsonStr);
        } catch (Exception e) {
            log.error("系统异常:",e);
            renderAjaxResult("系统异常", Consts.ERROR_CODE_SYSTEM_ERROR, null);
        }
    }
    
    @Before(Tx.class)
    public void pay() {
        try {
            // 支付结果通用通知文档: https://pay.weixin.qq.com/wiki/doc/api/jsapi.php?chapter=9_7
            String xmlMsg = HttpKit.readData(getRequest());
            System.out.println("支付通知=" + xmlMsg);
            Map<String, String> params = PaymentKit.xmlToMap(xmlMsg);

            String resultCode = params.get("result_code");
            // 总金额
            String totalFeeStr = params.get("total_fee");
            // 商户订单号
            String orderNo = params.get("out_trade_no");
            // 微信支付订单号
            String tradeNo = params.get("transaction_id");
            // 支付完成时间，格式为yyyyMMddHHmmss
            String payed = params.get("time_end");

            // 注意重复通知的情况，同一订单号可能收到多次通知，请注意一定先判断订单状态
            // 避免已经成功、关闭、退款的订单被再次更新

            if (PaymentKit.verifyNotify(params, paternerKey)) {
                if (SUCCESS.equals(resultCode)) {
                    //更新订单信息
                    Transaction transaction = new Transaction().findFirst("select * from jp_transaction where order_no = ?", orderNo);
                    if (transaction == null) { //订单不存在
                        log.error("微信支付通知错误：orderNo [" + orderNo + "] is not exists!params = []" + params);
                        return;
                    }
                    if ("1".equals(transaction.getStatus()) || "2".equals(transaction.getStatus())) {//订单状态不对
                        log.error("微信支付通知错误：order status[" + transaction.getStatus() + "] is error!params = []" + params);
                        return;
                    }
                    BigDecimal totalFee = new BigDecimal(totalFeeStr).divide(new BigDecimal("100"));
                    if (transaction.getTotleFee().compareTo(totalFee) != 0) { //订单金额与支付金额不相等
                        log.error("微信支付通知错误：orderTotalFee[" + transaction.getTotleFee() + "] <> payTotalFee[" + totalFee + "]!params = []" + params);
                        return;
                    }
                    transaction.setTradeNo(tradeNo);
                    transaction.setStatus("2");//订单状态修改为已支付
                    transaction.setPayed(DateUtils.parse1(payed));
                    boolean res = transaction.update();
                    if (!res) { //订单更新失败
                        log.error("微信支付通知错误：order update failed!params = []" + params);
                        return;
                    }

                    Map<String, String> xml = new HashMap<>();
                    xml.put("return_code", SUCCESS);
                    xml.put("return_msg", "OK");
                    renderText(PaymentKit.toXml(xml));
                    return;
                } else {
                    log.error("微信支付通知错误：result_code =" + resultCode);
                }
            }
            renderText("");
        } catch (Exception e) {
            log.error("微信支付通知错误：", e);
        }
    }
    
    public void test() {
        render("weixinpay.html");
    }

}
