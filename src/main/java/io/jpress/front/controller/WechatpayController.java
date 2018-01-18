package io.jpress.front.controller;

import com.jfinal.aop.Before;
import com.jfinal.kit.HttpKit;
import com.jfinal.kit.StrKit;
import com.jfinal.log.Log;
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
    String notifyUrl = "http://m.yuweiguoye.com/wechatpay/pay";
  
    //待支付-微信支付
    @Before(UCodeInterceptor.class)
    public void prepay() {
        // openId，采用 网页授权获取 access_token API：SnsAccessTokenApi获取
        String userJson = this.getSessionAttr(Consts.SESSION_WECHAT_USER);
        String openId = new ApiResult(userJson).getStr("openid");

        
        final BigInteger id=getParaToBigInteger("id");

        if (id==null) {
            renderAjaxResultForError("订单id不能为空");
            return;
        }
        final Transaction transaction=TransactionQuery.me().findById(id);
        if (transaction==null) {
            renderAjaxResultForError("订单不存在");
            return;
        }
        
        // 统一下单文档地址：https://pay.weixin.qq.com/wiki/doc/api/jsapi.php?chapter=9_1
        
        Map<String, String> params = new HashMap<>();
        params.put("appid", appid);
        params.put("mch_id", partner);
        params.put("body", OptionQuery.me().findValue("wechat_transferDesc"));
        params.put("out_trade_no", transaction.getOrderNo());
        params.put("total_fee", Integer.toString(transaction.getTotleFee().multiply(BigDecimal.valueOf(100.00)).intValue()));
        
        String ip = IpKit.getRealIp(getRequest());
        if (StrKit.isBlank(ip)) {
            ip = "127.0.0.1";
        }
        
        params.put("spbill_create_ip", ip);
        params.put("trade_type", TradeType.JSAPI.name());
        params.put("nonce_str", Long.toString(System.currentTimeMillis() / 1000));
        params.put("notify_url", notifyUrl);
        params.put("openid", openId);
        System.out.println("微信统一下单输入参数：" + params);
        
        String sign = PaymentKit.createSign(params, paternerKey);
        params.put("sign", sign);
        String xmlResult = PaymentApi.pushOrder(params);
        
        System.out.println("微信统一下单返回参数：" + xmlResult);
        Map<String, String> result = PaymentKit.xmlToMap(xmlResult);
        
        String returnCode = result.get("return_code");
        String returnMsg = result.get("return_msg");
        if (StrKit.isBlank(returnCode) || !SUCCESS.equals(returnCode)) {
            renderText(returnMsg);
            return;
        }
        String resultCode = result.get("result_code");
        if (StrKit.isBlank(resultCode) || !SUCCESS.equals(resultCode)) {
            renderText(returnMsg);
            return;
        }
        // 以下字段在return_code 和result_code都为SUCCESS的时候有返回
        String prepayId = result.get("prepay_id");
        
        Map<String, String> packageParams = new HashMap<>();
        packageParams.put("appId", appid);
        packageParams.put("timeStamp", Long.toString(System.currentTimeMillis() / 1000));
        packageParams.put("nonceStr", Long.toString(System.currentTimeMillis()));
        packageParams.put("package", "prepay_id=" + prepayId);
        packageParams.put("signType", "MD5");
        String packageSign = PaymentKit.createSign(packageParams, paternerKey);
        packageParams.put("paySign", packageSign);
        
        String jsonStr = JsonUtils.toJson(packageParams);
        setAttr("json", jsonStr);
        System.out.println(jsonStr);
        render("weixinpay.html");
    }
    
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
                        log.error("wechat pay notify error：orderNo [" + orderNo + "] is not exists!params = []" + params);
                        return;
                    }
                    if ("1".equals(transaction.getStatus()) || "2".equals(transaction.getStatus())) {//订单状态不对
                        log.error("wechat pay notify error：order status[" + transaction.getStatus() + "] is error!params = []" + params);
                        return;
                    }
                    BigDecimal totalFee = new BigDecimal(totalFeeStr).divide(new BigDecimal("100"));
                    if (transaction.getTotleFee().compareTo(totalFee) != 0) { //订单金额与支付金额不相等
                        log.error("wechat pay notify error：orderTotalFee[" + transaction.getTotleFee() + "] <> payTotalFee[" + totalFee + "]!params = []" + params);
                        return;
                    }
                    transaction.setTradeNo(tradeNo);
                    transaction.setStatus("2");//订单状态修改为已支付
                    transaction.setPayed(DateUtils.parse1(payed));
                    boolean res = transaction.update();
                    if (!res) { //订单更新失败
                        log.error("wechat pay notify error：order update failed!params = []" + params);
                        return;
                    }

                    Map<String, String> xml = new HashMap<>();
                    xml.put("return_code", SUCCESS);
                    xml.put("return_msg", "OK");
                    renderText(PaymentKit.toXml(xml));
                    return;
                } else {
                    log.error("wechat pay notify error：result_code =" + resultCode);
                }
            }
            renderText("");
        } catch (Exception e) {
            log.error("wechat pay notify error：", e);
        }
    }
    
    public void test() {
        render("weixinpay.html");
    }

}
