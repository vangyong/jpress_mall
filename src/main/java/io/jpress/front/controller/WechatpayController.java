package io.jpress.front.controller;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jfinal.aop.Before;
import com.jfinal.aop.Clear;
import com.jfinal.kit.HttpKit;
import com.jfinal.kit.PropKit;
import com.jfinal.kit.StrKit;
import com.jfinal.log.Log;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.IAtom;
import com.jfinal.weixin.sdk.api.ApiResult;
import com.jfinal.weixin.sdk.api.PaymentApi;
import com.jfinal.weixin.sdk.api.TemplateData;
import com.jfinal.weixin.sdk.api.TemplateMsgApi;
import com.jfinal.weixin.sdk.api.PaymentApi.TradeType;
import com.jfinal.weixin.sdk.kit.IpKit;
import com.jfinal.weixin.sdk.kit.PaymentKit;
import com.jfinal.weixin.sdk.utils.JsonUtils;

import io.jpress.Consts;
import io.jpress.core.BaseFrontController;
import io.jpress.interceptor.UCodeInterceptor;
import io.jpress.model.Bonus;
import io.jpress.model.Content;
import io.jpress.model.ContentSpecItem;
import io.jpress.model.Coupon;
import io.jpress.model.ShoppingCart;
import io.jpress.model.Transaction;
import io.jpress.model.TransactionItem;
import io.jpress.model.User;
import io.jpress.model.query.ContentQuery;
import io.jpress.model.query.ContentSpecItemQuery;
import io.jpress.model.query.CouponQuery;
import io.jpress.model.query.OptionQuery;
import io.jpress.model.query.ShoppingCartQuery;
import io.jpress.model.query.TransactionQuery;
import io.jpress.model.query.UserQuery;
import io.jpress.router.RouterMapping;
import io.jpress.utils.DateUtils;
import io.jpress.utils.RandomUtils;
import io.jpress.utils.StringUtils;
import io.jpress.wechat.WechatApiConfigInterceptor;
import io.jpress.wechat.WechatUserInterceptor;

/**
 * <b>Description:</b>
 * <br><b>ClassName:</b> WechatpayController
 * <br><b>Date:</b> 2018年1月18日 下午9:20:10
 * <br>@author <b>jianb.jiang</b>
 */
@RouterMapping(url = "/wechatpay")
//@Before(UserInterceptor.class)
@Before(WechatUserInterceptor.class)
public class WechatpayController extends BaseFrontController {

    private static final String SUCCESS = "SUCCESS";
    
    /**
     * 支付成功后奖金到账通知  消息的模板ID
     */
    private static final String PAY_SUCCESS_NOTICE_TEMP_ID = OptionQuery.me().findValue("wechat_bonus_tempMsg_id");

    private static final BigDecimal BOUNS_RATIO_LEVEL1 = BigDecimal.valueOf(0.2);

    private static final BigDecimal BOUNS_RATIO_LEVEL2 = BigDecimal.valueOf(0.05);
    
    private static final Log log = Log.getLog(WechatpayController.class);
    
    private static final String appid = OptionQuery.me().findValue("wechat_appid");
    private static final String partner = OptionQuery.me().findValue("wechat_partner");
    private static final String paternerKey = OptionQuery.me().findValue("wechat_paternerKey");
    private static final String notifyUrl = PropKit.get("wechat_notify_url");
    private static final String web_domain = OptionQuery.me().findValue("web_domain");
  
    //待支付-微信支付
    @Before(UCodeInterceptor.class)
    public void prepay() {
        try {
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
            
            String jsonStr = wechartPrePay(openId, transaction.getOrderNo(), transaction.getCashFee());
            
            log.info(jsonStr);
            renderAjaxResult("操作成功", 0, jsonStr);
        } catch (Exception e) {
            log.error("系统异常:",e);
            renderAjaxResult("系统异常", Consts.ERROR_CODE_SYSTEM_ERROR, null);
        }
    }

    private String wechartPrePay(String openId, String orderNo, BigDecimal cashFee) {
        // 统一下单文档地址：https://pay.weixin.qq.com/wiki/doc/api/jsapi.php?chapter=9_1
        
        Map<String, String> params = new HashMap<>();
        params.put("appid", appid);
        params.put("mch_id", partner);
        params.put("body", OptionQuery.me().findValue("wechat_transferDesc"));
        params.put("out_trade_no", orderNo);
        params.put("total_fee", Integer.toString(cashFee.setScale(2, BigDecimal.ROUND_DOWN).multiply(BigDecimal.valueOf(100.00)).intValue()));
        
        String ip = IpKit.getRealIp(getRequest());
        if (StrKit.isBlank(ip)) {
            ip = "127.0.0.1";
        }
        
        params.put("spbill_create_ip", ip);
        params.put("trade_type", TradeType.JSAPI.name());
        params.put("nonce_str", Long.toString(System.currentTimeMillis() / 1000));
        params.put("notify_url", notifyUrl);
        params.put("openid", openId);
        log.info("微信统一下单输入参数：" + params);
        
        String sign = PaymentKit.createSign(params, paternerKey);
        params.put("sign", sign);
        String xmlResult = PaymentApi.pushOrder(params);
        
        log.info("微信统一下单返回参数：" + xmlResult);
        Map<String, String> result = PaymentKit.xmlToMap(xmlResult);
        
        String returnCode = result.get("return_code");
        String returnMsg = result.get("return_msg");
        if (StrKit.isBlank(returnCode) || !SUCCESS.equals(returnCode)) {
            log.error(returnMsg);
            renderAjaxResult("系统异常", Consts.ERROR_CODE_SYSTEM_ERROR, null);
        }
        String resultCode = result.get("result_code");
        if (StrKit.isBlank(resultCode) || !SUCCESS.equals(resultCode)) {
            log.error(returnMsg);
            renderAjaxResult("系统异常", Consts.ERROR_CODE_SYSTEM_ERROR, null);
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
        
        return JsonUtils.toJson(packageParams);
    }
    
    @Clear({WechatUserInterceptor.class})
    @Before({WechatApiConfigInterceptor.class})
    public void pay() {
        try {
            // 支付结果通用通知文档: https://pay.weixin.qq.com/wiki/doc/api/jsapi.php?chapter=9_7
            String xmlMsg = HttpKit.readData(getRequest());
            log.info("支付通知=" + xmlMsg);
            Map<String, String> params = PaymentKit.xmlToMap(xmlMsg);
            if (PaymentKit.verifyNotify(params, paternerKey)) {

                //-----------test-----------------------
    //            Map<String, String> params = new HashMap<>();
    //            params.put("result_code", "SUCCESS");
    //            params.put("total_fee", "1522");
    //            params.put("out_trade_no", "18050414283431409212");
    //            params.put("transaction_id", "wx_fdskfsalkfalsjdl11");
    //            params.put("time_end", "20180125182222");
                //-----------test-----------------------
                
                String resultCode = params.get("result_code");
                // 支付总金额
                String cashFeeStr = params.get("total_fee");
                // 商户订单号
                String orderNo = params.get("out_trade_no");
                // 微信支付订单号
                String tradeNo = params.get("transaction_id");
                // 支付完成时间，格式为yyyyMMddHHmmss
                String payed = params.get("time_end");
    
                // 注意重复通知的情况，同一订单号可能收到多次通知，请注意一定先判断订单状态
                // 避免已经成功、关闭、退款的订单被再次更新
                final List<TemplateData> tempMsgList = new ArrayList<TemplateData>();
//              //--test   if (true) {
                if (SUCCESS.equals(resultCode)) {
                    //更新订单信息
                    final Transaction transaction = new Transaction().findFirst("select * from jp_transaction where order_no = ?", orderNo);
                    if (transaction == null) { //订单不存在
                        log.error("微信支付通知错误：orderNo [" + orderNo + "] is not exists!params = []" + params);
                        return;
                    }
                    if ("2".equals(transaction.getStatus())) {//订单已经被处理过，直接返回成功
                        log.error("微信支付通知错误：order status[" + transaction.getStatus() + "] 已经被处理，不用重复处理!params = []" + params);
                        Map<String, String> xml = new HashMap<>();
                        xml.put("return_code", SUCCESS);
                        xml.put("return_msg", "OK");
                        renderText(PaymentKit.toXml(xml));
                    }
                    if (!"0".equals(transaction.getStatus()) && !"1".equals(transaction.getStatus())) {//订单状态不对
                        log.error("微信支付通知错误：order status[" + transaction.getStatus() + "] is error!params = []" + params);
                        return;
                    }
                    BigDecimal cashFee = new BigDecimal(cashFeeStr).divide(new BigDecimal("100"));
                    if (transaction.getCashFee().setScale(2, BigDecimal.ROUND_DOWN).compareTo(cashFee.setScale(2, BigDecimal.ROUND_DOWN)) != 0) { //订单现金支付金额与实际支付金额不相等
                        log.error("微信支付通知错误：orderCashFee[" + transaction.getCashFee() + "] <> payCashFee[" + cashFee + "]!params = []" + params);
                        return;
                    }
                    transaction.setTradeNo(tradeNo);
                    transaction.setStatus("2");//订单状态修改为已支付
                    transaction.setPayed(DateUtils.parse1(payed));
                    
                    boolean res = Db.tx(new IAtom() {
                        @Override
                        public boolean run() throws SQLException {

                            if (!transaction.update()) { //跟新订单状态
                                return false;
                            }

                            if (!calculateBonus(transaction,tempMsgList)) { //计算并分配奖金，同时组装奖金到账通知的模板消息
                                return false;
                            }

                            return true;
                        }
                    });
                    
                    if (!res) { //订单更新失败
                        log.error("微信支付通知错误：order update failed!params = []" + params);
                        return;
                    }
                    
                    List<TemplateData> tempMsgFailList = new ArrayList<>();
                    //推送模板消息
                    for (TemplateData templateData : tempMsgList) {
                        ApiResult result = TemplateMsgApi.send(templateData.build());
                        if (result.getErrorCode() == 40001) { //token失效
                            tempMsgFailList.add(templateData);
                        }
                        log.info("用户("+templateData.getTouser()+")奖金到账消息["+templateData.build()+"]第一次推送结果：" + result.toString());
                    }
                    
                    if (!tempMsgFailList.isEmpty()) {
                        for (TemplateData templateData : tempMsgFailList) {
                            ApiResult result = TemplateMsgApi.send(templateData.build());
                            log.info("用户("+templateData.getTouser()+")奖金到账消息["+templateData.build()+"]第二次推送结果：" + result.toString());
                        }
                    }
                    
                    Map<String, String> xml = new HashMap<>();
                    xml.put("return_code", SUCCESS);
                    xml.put("return_msg", "OK");
                    renderText(PaymentKit.toXml(xml));
                } else {
                    log.error("微信支付通知错误：result_code =" + resultCode);
                }
            } else {
                log.error("微信支付通知错误：签名校验错误");
                renderText("who are you!!!");
            }
            renderText("");
        } catch (Exception e) {
            log.error("微信支付通知错误：", e);
        }
    }
    
    private boolean calculateBonus(Transaction transaction, List<TemplateData> tempMsgList) {
        User currUser = UserQuery.DAO.findById(transaction.getUserId());//当前消费用户（不从缓存读取，因为缓存中的团队人数和团队金额不是最新值）
        BigDecimal spending = transaction.getCashFee().add(transaction.getAmountFee());//本次消费总金额 = 余额消费金额 + 现金消费金额，即不包括优惠券金额
        
        //计算当前用户的父亲的奖金
        User pUser = UserQuery.DAO.findById(currUser.getPid());
        if (pUser != null) {
            if (!calculationBounsByUserLevel(spending, transaction, pUser, "C", tempMsgList)) {
                return false;
            }
        } else {
            return true;
        }
        
        //计算当前用户的父亲的父亲的奖金
        User ppUser = UserQuery.DAO.findById(pUser.getPid());
        if (ppUser != null) {
            if (!calculationBounsByUserLevel(spending, transaction, ppUser, "B", tempMsgList)) {
                return false;
            }
        } else {
            return true;
        }
        
        //计算当前用户的父亲的父亲的父亲的奖金
        User pppUser = UserQuery.DAO.findById(ppUser.getPid());
        if (pppUser != null) {
            if (!calculationBounsByUserLevel(spending, transaction, pppUser, "A", tempMsgList)) {
                return false;
            }
        } else {
            return true;
        }
        
        return true;
    }

    private boolean calculationBounsByUserLevel(BigDecimal spending, Transaction transaction, 
                User user, String level, List<TemplateData> tempMsgList) {
        //此笔消费计入团队总消费金额
        if (Db.update("update jp_user set team_buy_amount = team_buy_amount + ? where id = ?", spending, user.getId()) <= 0) {
            return false;
        }
        //计算获得的个人直推奖金
        if ("C".equals(level) || "B".equals(level)) {
            Bonus bonusUser = new Bonus();
            BigDecimal bonusUseramount = spending.multiply(BOUNS_RATIO_LEVEL2);//间接推广提成5%
            bonusUser.setBonusType(2L);//间接推广
            if ("C".equals(level)) {
                bonusUseramount = spending.multiply(BOUNS_RATIO_LEVEL1);//直接推广提成30%
                bonusUser.setBonusType(1L);//直接推广
            }
            bonusUseramount = bonusUseramount.setScale(2, BigDecimal.ROUND_DOWN);
            bonusUser.setAmount(bonusUseramount);
            bonusUser.setBonusTime(new Date());
            bonusUser.setBonusCycle(1L);//奖金计算周期类型（1 按订单结算也就是实时结算;2 按月结算）
            bonusUser.setTransactionId(transaction.getId());
            bonusUser.setUserId(user.getId());
            if (!bonusUser.save()) {
                return false;
            }
            if (Db.update("update jp_user set amount = amount + ? where id = ?", bonusUseramount, user.getId()) <= 0) {
                return false;
            }
            
            tempMsgList.add(TemplateData.New()
                .setTouser(user.getOpenid()) // 消息接收者
                .setTemplate_id(PAY_SUCCESS_NOTICE_TEMP_ID) //模板id
                .setTopcolor("#eb414a")
                .setUrl(web_domain + "/user/center") //消息链接地址，此为个人中心详情页面也就是查看余额的页面
                // 模板参数
                .add("first", "恭喜您获得一笔推广奖金！\n", "#999")
                .add("keyword1", user.getNickname(), "#999")
                .add("keyword2", transaction.getOrderNo(), "#999")
                .add("keyword3", DateUtils.now(), "#999")
                .add("keyword4", spending.toString() + " 元", "#999")
                .add("keyword5", bonusUseramount.toString() + " 元", "#999")
                .add("remark", "奖金已存入商城账户余额，继续加油！^_^", "#999")
             );
        }
        
        //计算获得的团队奖金
        BigDecimal bounsTeamAmount = calculationTeamBouns(user,spending);
        if (bounsTeamAmount != null) {
            bounsTeamAmount = bounsTeamAmount.setScale(2, BigDecimal.ROUND_DOWN);
            Bonus bounsTeam = new Bonus();
            bounsTeam.setAmount(bounsTeamAmount);
            bounsTeam.setBonusCycle(1L);//奖金计算周期类型（1 按订单结算也就是实时结算;2 按月结算）
            bounsTeam.setBonusTime(new Date());
            bounsTeam.setBonusType(3L);//奖金分类（1 个人提成-直接推广;2 个人提成-间接推广;3 团队提成;4 团队管理费）
            bounsTeam.setTransactionId(transaction.getId());
            bounsTeam.setUserId(user.getId());
            if (!bounsTeam.save()) {
                return false;
            }
            if (Db.update("update jp_user set amount = amount + ? where id = ?", bounsTeamAmount, user.getId()) <= 0) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * <b>Description.根据用户等级计算团队提成:</b><br>
     * <b>Author:jianb.jiang</b>
     * <br><b>Date:</b> 2018年1月29日 下午10:53:14
     * @param user
     * @param spending
     * @return
     */
    BigDecimal calculationTeamBouns(User user, BigDecimal spending){
        long childNum = user.getChildNum().longValue();
        long teamNum = user.getTeamNum().longValue();
        if (childNum >= 50 && teamNum >= 800) {
            return spending.multiply(BigDecimal.valueOf(0.2));//提成20%
        } else if (childNum >= 20 && teamNum >= 200) {
            return spending.multiply(BigDecimal.valueOf(0.15));//提成15%
        } else if (childNum >= 12 && teamNum >= 80) {
            return spending.multiply(BigDecimal.valueOf(0.1));//提成10%
        } else if (childNum >= 5 && teamNum >= 25) {
            return spending.multiply(BigDecimal.valueOf(0.08));//提成8%
        } else if (childNum >= 3 && teamNum >= 10) {
            return spending.multiply(BigDecimal.valueOf(0.05));//提成5%
        } else {
            return null;
        }
    }
    
    //购物车结算微信支付
    @Before(UCodeInterceptor.class)
    public void shoppingCartWechatpay(){
        try {
            final String remark=getPara("remark");
            final String userAddressAddress=getPara("userAddressAddress");
            final String userAddressMobile=getPara("userAddressMobile");
            final String userAddressName=getPara("userAddressName");
            final BigInteger[] shoppingCartIds=getParaValuesToBigInteger("shoppingCartIds");
            final BigDecimal payAmount=getParaToBigDecimal("payAmount");
            final BigInteger couponUsedId=getParaToBigInteger("couponUsedId");
            
            if (StringUtils.isBlank(userAddressName) || StringUtils.isBlank(userAddressMobile) || StringUtils.isBlank(userAddressAddress)) {
                renderAjaxResultForError("收货人、电话及收货地址都不能为空");
                return;
            }
            if (shoppingCartIds==null) {
                renderAjaxResultForError("结算商品不能为空");
                return;
            }

            final BigInteger userId=getLoginedUser().getId();

//            final UserAddress userAddress=UserAddressQuery.me().findById(userAddressId);
//            if (userAddress==null) {
//                renderAjaxResultForError("收货地址不存在");
//                return;
//            }

            final StringBuilder shoppingCartIdSb=new StringBuilder();
            for (BigInteger shoppingCartId:shoppingCartIds) {
                ShoppingCart shoppingCart=ShoppingCartQuery.me().findById(shoppingCartId);
                if (shoppingCart==null) {
                    renderAjaxResultForError("购物车中的结算商品不存在");
                    return;
                }
                Content content=ContentQuery.me().findById(shoppingCart.getContentId());
                if(content==null){
                    renderAjaxResultForError("结算商品不存在");
                    return;
                }
                if(!"normal".equals(content.getStatus())){
                    renderAjaxResultForError("商品已经下架");
                    return;
                }
                ContentSpecItem contentSpecItem=ContentSpecItemQuery.me().findByContentIdAndSpecValueId(shoppingCart.getContentId(), shoppingCart.getSpecValueId());
                if(contentSpecItem==null || shoppingCart.getQuantity()>contentSpecItem.getStock()){
                    renderAjaxResultForError(content.getTitle()+"货存不足");
                    return;
                }

                if (!ContentSpecItemQuery.me().checkLimitPerUser(userId, shoppingCart.getContentId(), shoppingCart.getSpecValueId(), shoppingCart.getQuantity())) {
                    renderAjaxResultForError("商品["+content.getTitle()+"]是限制购买的，不要太贪心噢^_^");
                    return;
                }
                shoppingCartIdSb.append(shoppingCartId).append(",");
            }

            // 商户订单号，商户网站订单系统中唯一订单号，必填
            final String orderNo=RandomUtils.randomKey(null, userId.toString());
            // 商品总金额，必填
            final BigDecimal totalFee=ShoppingCartQuery.me().getTotalFee(shoppingCartIdSb.substring(0, shoppingCartIdSb.length()-1), userId);

            final Transaction transaction=new Transaction();
            
            boolean saved=Db.tx(new IAtom() {
                @Override
                public boolean run() throws SQLException {
                    // 现金支付金额
                    BigDecimal couponFee = BigDecimal.valueOf(0);//默认优惠券支付金额为0。【支付扣减优先级为1】
                    BigDecimal amountFee = BigDecimal.valueOf(0);//默认余额支付金额为0。【支付扣减优先级为2】
                    BigDecimal cashFee = totalFee;//默认情况下商品总金额就为用户现金支付金额。【支付扣减优先级为3】
                    
                    transaction.setRemark(remark);
                    transaction.setUserId(userId);
                    transaction.setUserAddress(userAddressAddress+" "+userAddressName+" "+userAddressMobile);
                    transaction.setOrderNo(orderNo);
                    transaction.setTotleFee(totalFee);
                    transaction.setPayType(Transaction.PAY_TYPE_WECHATPAY);
                    transaction.setStatus(Transaction.STATUS_1);
                    transaction.setCreated(new Date());
                    if(!transaction.save()){
                        log.error("订单[{"+ transaction.getOrderNo() +"}]保存失败..");
                        return false;
                    }

                    List<ShoppingCart> list=ShoppingCartQuery.me().findList(shoppingCartIdSb.substring(0, shoppingCartIdSb.length()-1), null);
                    if(list.isEmpty()){
                        log.error("订单[{"+ transaction.getOrderNo() +"}]没有商品信息..");
                        return false;
                    }else{
                        for(ShoppingCart shoppingCart:list){
                            TransactionItem transactionItem=new TransactionItem();
                            transactionItem.setTransactionId(transaction.getId());
                            transactionItem.setContentId(shoppingCart.getContentId());
                            transactionItem.setSpecValueId(shoppingCart.getSpecValueId());
                            transactionItem.setPrice(shoppingCart.getPrice());
                            transactionItem.setQuantity(shoppingCart.getQuantity());
                            transactionItem.setCreated(new Date());
                            if(!transactionItem.save()){
                                log.error("订单[{"+ transaction.getOrderNo() +"}]的订单项保存失败..");
                                return false;
                            }
                            
                            //进行库存扣减
                            if(Db.update("update jp_content_spec_item set stock = stock - ? where content_id = ? and spec_value_id = ? and (stock - ?) >= 0", 
                                    shoppingCart.getQuantity(), shoppingCart.getContentId(), shoppingCart.getSpecValueId(), shoppingCart.getQuantity()) <= 0) {
                                log.error("库存扣减失败..");
                                return false;
                            }
                        }
                    }
                    
                    ShoppingCartQuery.me().deleteByIds(shoppingCartIdSb.substring(0, shoppingCartIdSb.length()-1));
                    
                    //1、先抵扣优惠券金额
                    if (couponUsedId != null && couponUsedId.compareTo(BigInteger.valueOf(0)) > 0) {
                        Coupon coupon = CouponQuery.me().findByUserIdAndUsedId(userId,couponUsedId);
                        if(coupon != null) {
                            //修改couponUsed的used、transaction_id
                            if(Db.update("update jp_coupon_used set used = 1,transaction_id = ? where id = ? and user_id = ? and used = 0 and transaction_id = 0", 
                                    transaction.getId(), couponUsedId, userId) <= 0) {
                                log.error("优惠券[{"+ couponUsedId +"}]使用状态更新失败..");
                                return false;
                            }
                            
                            couponFee = coupon.getAmount();
                            
                            cashFee = cashFee.subtract(couponFee);
                            if (cashFee.compareTo(BigDecimal.valueOf(0)) <= 0) {
                                cashFee = BigDecimal.valueOf(0);
                                couponFee = totalFee;//优惠券金额比商品金额大时，优惠券支付金额就为商品金额
                                transaction.setStatus(Transaction.STATUS_2); //无需现金支付的时候订单状态为已支付
                            }
                            
                        }
                    }
                    
                    //2、再扣减用户的余额
//                    if (cashFee.compareTo(BigDecimal.valueOf(0)) > 0) {
//                        User currUser = UserQuery.DAO.findById(transaction.getUserId());
//                        BigDecimal userAmount = currUser.getAmount();
//                        if (Db.update("update jp_user set amount = amount - ? where id = ? and (amount - ?) >= 0", cashFee, userId, cashFee) <= 0) {
//                            log.info("用户账户余额不足以支付整个订单...");
//                            //获取用户账户余额
//                            amountFee = userAmount;//账户余额小于抵扣优化券后的商品金额时，余额支付金额就为账户余额
//                            if(Db.update("update jp_user set amount = amount - ? where id = ? and (amount - ?) >= 0", amountFee, userId, amountFee) <= 0){
//                                log.error("用户[{"+ userId +"}]账户余额扣减失败..");
//                                return false;
//                            }
//                            cashFee = cashFee.subtract(amountFee);//扣减账户余额
//                        } else {
//                            //账户余额足以支付整个订单
//                            amountFee = cashFee;//账户余额大于等于抵扣优化券后的商品金额时，余额支付金额就为抵扣优化券后的商品金额
//                            cashFee = BigDecimal.valueOf(0);//即不需要现金支付，现金支付金额为0
//                            transaction.setStatus(Transaction.STATUS_2); //无需现金支付的时候订单状态为已支付
//                        }
//                    }
                    
                    //3、修改订单的cash_fee、amount_fee、coupon_fee
                    transaction.setCouponFee(couponFee.setScale(2, BigDecimal.ROUND_DOWN));
                    transaction.setAmountFee(amountFee.setScale(2, BigDecimal.ROUND_DOWN));
                    transaction.setCashFee(cashFee.setScale(2, BigDecimal.ROUND_DOWN));
                    
                    //4、如果使用了余额支付，需要记录余额支出
                    if (amountFee.compareTo(BigDecimal.valueOf(0)) > 0) {
                        Bonus bonusUser = new Bonus();
                        bonusUser.setBonusType(5L);//订单消费
                        bonusUser.setAmount(amountFee.negate());//负数
                        bonusUser.setBonusTime(new Date());
                        bonusUser.setBonusCycle(1L);//奖金计算周期类型（1 按订单结算也就是实时结算;2 按月结算）
                        bonusUser.setTransactionId(transaction.getId());
                        bonusUser.setUserId(userId);
                        if (!bonusUser.save()) {
                            return false;
                        }
                    }
                    
                    //判断页面计算的支付金额是否与后台计算的支付金额相匹配
                    if(payAmount.compareTo(transaction.getAmountFee().add(transaction.getCashFee())) != 0) {
                        log.error("订单[{"+ transaction.getOrderNo() +"}]支付金额不匹配");
                        return false;
                    }
                    
                    if(!transaction.update()){
                        log.error("订单[{"+ transaction.getOrderNo() +"}]更新金额失败..");
                        return false;
                    }
                    
                    return true;
                }
            });

            if(saved){
                String jsonStr = "0.00";//表示本次无需支付现金
                if (transaction.getCashFee().compareTo(BigDecimal.valueOf(0)) > 0) {
                    String userJson = this.getSessionAttr(Consts.SESSION_WECHAT_USER);
                    String openId = new ApiResult(userJson).getStr("openid");
                    jsonStr = wechartPrePay(openId, orderNo, transaction.getCashFee());
                }
                log.info(jsonStr);
                renderAjaxResult("操作成功", 0, jsonStr);
            }else{
                log.error("保存订单数据失败了!!");
                renderAjaxResultForError("操作失败");
            }
        }catch(Exception e) {
            log.error("系统异常:",e);
            renderAjaxResult("系统异常", Consts.ERROR_CODE_SYSTEM_ERROR, null);
        }
    }

    //商品立即购买微信支付
    @Before({UCodeInterceptor.class,WechatApiConfigInterceptor.class})
    public void contentWechatpay(){
        try {
            final String remark=getPara("remark");
            final String userAddressAddress=getPara("userAddressAddress");
            final String userAddressMobile=getPara("userAddressMobile");
            final String userAddressName=getPara("userAddressName");
            final BigInteger contentId=getParaToBigInteger("contentId");
            final BigInteger specValueId=getParaToBigInteger("specValueId");
            final Integer quantity=getParaToInt("quantity");
            final BigDecimal payAmount=getParaToBigDecimal("payAmount");
            final BigInteger couponUsedId=getParaToBigInteger("couponUsedId");

            if (StringUtils.isBlank(userAddressName) || StringUtils.isBlank(userAddressMobile) || StringUtils.isBlank(userAddressAddress)) {
                renderAjaxResultForError("收货人、电话及收货地址都不能为空");
                return;
            }
            if (contentId==null) {
                renderAjaxResultForError("商品id不能为空");
                return;
            }
            if (specValueId==null) {
                renderAjaxResultForError("规格值id不能为空");
                return;
            }
            if (quantity==null) {
                renderAjaxResultForError("数量不能为空");
                return;
            }

            final BigInteger userId=getLoginedUser().getId();

//            final UserAddress userAddress=UserAddressQuery.me().findById(userAddressId);
//            if (userAddress==null) {
//                renderAjaxResultForError("收货地址不存在");
//                return;
//            }

            final Content content=ContentQuery.me().findById(contentId);
            if(content==null){
                renderAjaxResultForError("商品不存在");
                return;
            }
            if(!"normal".equals(content.getStatus())){
                renderAjaxResultForError("商品已经下架");
                return;
            }

            final ContentSpecItem contentSpecItem=ContentSpecItemQuery.me().findByContentIdAndSpecValueId(content.getId(), specValueId);
            if(contentSpecItem==null || quantity>contentSpecItem.getStock()){
                renderAjaxResultForError("货存不足");
                return;
            }
            if (contentSpecItem.getLimitPerUser() != null && contentSpecItem.getLimitPerUser()>0 
                    && !ContentSpecItemQuery.me().checkLimitPerUser(userId, content.getId(), specValueId, quantity)) {
                renderAjaxResultForError("该商品是限制购买的，不要太贪心噢^_^");
                return;
            }
            // 商户订单号，商户网站订单系统中唯一订单号，必填
            final String orderNo=RandomUtils.randomKey(null, userId.toString());
            // 商品总金额，必填
            final BigDecimal totalFee = contentSpecItem.getPrice().multiply(new BigDecimal(quantity));
            
            final Transaction transaction=new Transaction();

            boolean saved=Db.tx(new IAtom() {
                @Override
                public boolean run() throws SQLException {
                    // 现金支付金额
                    BigDecimal couponFee = BigDecimal.valueOf(0);//默认优惠券支付金额为0。【支付扣减优先级为1】
                    BigDecimal amountFee = BigDecimal.valueOf(0);//默认余额支付金额为0。【支付扣减优先级为2】
                    BigDecimal cashFee = totalFee;//默认情况下商品总金额就为用户现金支付金额。【支付扣减优先级为3】
                    
                    //进行库存扣减
                    if(Db.update("update jp_content_spec_item set stock = stock - ? where content_id = ? and spec_value_id = ? and (stock - ?) >= 0", 
                            quantity, content.getId(), specValueId, quantity) <= 0) {
                        log.error("库存扣减失败..");
                        return false;
                    }
                    
                    transaction.setRemark(remark);
                    transaction.setUserId(userId);
                    transaction.setUserAddress(userAddressAddress+" "+userAddressName+" "+userAddressMobile);
                    transaction.setOrderNo(orderNo);
                    transaction.setTotleFee(totalFee);
                    transaction.setPayType(Transaction.PAY_TYPE_WECHATPAY);
                    transaction.setStatus(Transaction.STATUS_1);
                    transaction.setCreated(new Date());
                    if(!transaction.save()){
                        log.error("订单[{"+ transaction.getOrderNo() +"}]保存失败..");
                        return false;
                    }

                    TransactionItem transactionItem=new TransactionItem();
                    transactionItem.setTransactionId(transaction.getId());
                    transactionItem.setContentId(content.getId());
                    transactionItem.setSpecValueId(contentSpecItem.getSpecValueId());
                    transactionItem.setPrice(contentSpecItem.getPrice());
                    transactionItem.setQuantity(quantity);
                    transactionItem.setCreated(new Date());
                    if(!transactionItem.save()){
                        log.error("订单[{"+ transaction.getOrderNo() +"}]的订单项保存失败..");
                        return false;
                    }
                    
                    //1、先抵扣优惠券金额
                    if (couponUsedId != null && couponUsedId.compareTo(BigInteger.valueOf(0)) > 0) {
                        Coupon coupon = CouponQuery.me().findByUserIdAndUsedId(userId,couponUsedId);
                        if(coupon != null) {
                            //修改couponUsed的used、transaction_id
                            if(Db.update("update jp_coupon_used set used = 1,transaction_id = ? where id = ? and user_id = ? and used = 0 and transaction_id = 0", 
                                    transaction.getId(), couponUsedId, userId) <= 0) {
                                log.error("优惠券[{"+ couponUsedId +"}]使用状态更新失败..");
                                return false;
                            }
                            
                            couponFee = coupon.getAmount();
                            
                            cashFee = cashFee.subtract(couponFee);
                            if (cashFee.compareTo(BigDecimal.valueOf(0)) <= 0) {
                                cashFee = BigDecimal.valueOf(0);
                                couponFee = totalFee;//优惠券金额比商品金额大时，优惠券支付金额就为商品金额
                                transaction.setStatus(Transaction.STATUS_2); //无需现金支付的时候订单状态为已支付
                            }
                            
                        }
                    }
                    
                    //2、再扣减用户的余额
//                    if (cashFee.compareTo(BigDecimal.valueOf(0)) > 0) {
//                        User currUser = UserQuery.DAO.findById(transaction.getUserId());
//                        BigDecimal userAmount = currUser.getAmount();
//                        if (Db.update("update jp_user set amount = amount - ? where id = ? and (amount - ?) >= 0", cashFee, userId, cashFee) <= 0) {
//                            log.info("用户账户余额不足以支付整个订单...");
//                            //获取用户账户余额
//                            amountFee = userAmount;//账户余额小于抵扣优化券后的商品金额时，余额支付金额就为账户余额
//                            if(Db.update("update jp_user set amount = amount - ? where id = ? and (amount - ?) >= 0", amountFee, userId, amountFee) <= 0){
//                                log.error("用户[{"+ userId +"}]账户余额扣减失败..");
//                                return false;
//                            }
//                            cashFee = cashFee.subtract(amountFee);//扣减账户余额
//                        } else {
//                            //账户余额足以支付整个订单
//                            amountFee = cashFee;//账户余额大于等于抵扣优化券后的商品金额时，余额支付金额就为抵扣优化券后的商品金额
//                            cashFee = BigDecimal.valueOf(0);//即不需要现金支付，现金支付金额为0
//                            transaction.setStatus(Transaction.STATUS_2); //无需现金支付的时候订单状态为已支付
//                        }
//                    }
                    
                    //3、修改订单的cash_fee、amount_fee、coupon_fee
                    transaction.setCouponFee(couponFee.setScale(2, BigDecimal.ROUND_DOWN));
                    transaction.setAmountFee(amountFee.setScale(2, BigDecimal.ROUND_DOWN));
                    transaction.setCashFee(cashFee.setScale(2, BigDecimal.ROUND_DOWN));
                    
                    //4、如果使用了余额支付，需要记录余额支出
                    if (amountFee.compareTo(BigDecimal.valueOf(0)) > 0) {
                        Bonus bonusUser = new Bonus();
                        bonusUser.setBonusType(5L);//订单消费
                        bonusUser.setAmount(amountFee.negate());//负数
                        bonusUser.setBonusTime(new Date());
                        bonusUser.setBonusCycle(1L);//奖金计算周期类型（1 按订单结算也就是实时结算;2 按月结算）
                        bonusUser.setTransactionId(transaction.getId());
                        bonusUser.setUserId(userId);
                        if (!bonusUser.save()) {
                            return false;
                        }
                    }
                    
                    //判断页面计算的支付金额是否与后台计算的支付金额相匹配
                    if(payAmount.setScale(2, BigDecimal.ROUND_DOWN).compareTo(
                            transaction.getAmountFee().add(transaction.getCashFee()).setScale(2, BigDecimal.ROUND_DOWN)) != 0) {
                        log.error("订单[{"+ transaction.getOrderNo() +"}]支付金额不匹配");
                        return false;
                    }
                    
                    if(!transaction.update()){
                        log.error("订单[{"+ transaction.getOrderNo() +"}]更新金额失败..");
                        return false;
                    }
                    return true;
                }
            });

            if(saved){
                String jsonStr = "0.00";//表示本次无需支付现金
                if (transaction.getCashFee().compareTo(BigDecimal.valueOf(0)) > 0) {
                    String userJson = this.getSessionAttr(Consts.SESSION_WECHAT_USER);
                    String openId = new ApiResult(userJson).getStr("openid");
                    jsonStr = wechartPrePay(openId, orderNo, transaction.getCashFee());
                }
                log.info(jsonStr);
                renderAjaxResult("操作成功", 0, jsonStr);
            }else{
                log.error("保存订单数据失败了!!");
                renderAjaxResultForError("操作失败");
            }
        }catch(Exception e) {
            log.error("系统异常:",e);
            renderAjaxResult("系统异常", Consts.ERROR_CODE_SYSTEM_ERROR, null);
        }
    }

}
