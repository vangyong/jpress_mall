package io.jpress.front.controller;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
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
import com.jfinal.weixin.sdk.api.PaymentApi.TradeType;
import com.jfinal.weixin.sdk.kit.IpKit;
import com.jfinal.weixin.sdk.kit.PaymentKit;
import com.jfinal.weixin.sdk.utils.JsonUtils;

import io.jpress.Consts;
import io.jpress.core.BaseFrontController;
import io.jpress.interceptor.UCodeInterceptor;
import io.jpress.interceptor.UserInterceptor;
import io.jpress.model.Bonus;
import io.jpress.model.Content;
import io.jpress.model.ContentSpecItem;
import io.jpress.model.ShoppingCart;
import io.jpress.model.Transaction;
import io.jpress.model.TransactionItem;
import io.jpress.model.User;
import io.jpress.model.UserAddress;
import io.jpress.model.query.ContentQuery;
import io.jpress.model.query.ContentSpecItemQuery;
import io.jpress.model.query.OptionQuery;
import io.jpress.model.query.ShoppingCartQuery;
import io.jpress.model.query.TransactionQuery;
import io.jpress.model.query.UserAddressQuery;
import io.jpress.model.query.UserQuery;
import io.jpress.router.RouterMapping;
import io.jpress.utils.DateUtils;
import io.jpress.utils.RandomUtils;
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

    private static final BigDecimal BOUNS_RATIO_LEVEL1 = BigDecimal.valueOf(0.3);

    private static final BigDecimal BOUNS_RATIO_LEVEL2 = BigDecimal.valueOf(0.05);
    
    private static final Log log = Log.getLog(WechatpayController.class);
    
    String appid = OptionQuery.me().findValue("wechat_appid");
    String partner = OptionQuery.me().findValue("wechat_partner");
    String paternerKey = OptionQuery.me().findValue("wechat_paternerKey");
    String notifyUrl = PropKit.get("wechat_notify_url");
  
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
            
            String jsonStr = wechartPrePay(openId, transaction.getOrderNo(), transaction.getTotleFee());
            
            System.out.println(jsonStr);
            renderAjaxResult("操作成功", 0, jsonStr);
        } catch (Exception e) {
            log.error("系统异常:",e);
            renderAjaxResult("系统异常", Consts.ERROR_CODE_SYSTEM_ERROR, null);
        }
    }

    private String wechartPrePay(String openId, String orderNo, BigDecimal totalFee) {
        // 统一下单文档地址：https://pay.weixin.qq.com/wiki/doc/api/jsapi.php?chapter=9_1
        
        Map<String, String> params = new HashMap<>();
        params.put("appid", appid);
        params.put("mch_id", partner);
        params.put("body", OptionQuery.me().findValue("wechat_transferDesc"));
        params.put("out_trade_no", orderNo);
        params.put("total_fee", Integer.toString(totalFee.multiply(BigDecimal.valueOf(100.00)).intValue()));
        
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
    
    @Clear({UserInterceptor.class})
    public void pay() {
        try {
            // 支付结果通用通知文档: https://pay.weixin.qq.com/wiki/doc/api/jsapi.php?chapter=9_7
            String xmlMsg = HttpKit.readData(getRequest());
            System.out.println("支付通知=" + xmlMsg);
            Map<String, String> params = PaymentKit.xmlToMap(xmlMsg);

            //-----------test-----------------------
//            Map<String, String> params = new HashMap<>();
//            params.put("result_code", "SUCCESS");
//            params.put("total_fee", "10000");
//            params.put("out_trade_no", "1801142043572365216");
//            params.put("transaction_id", "wx_fdskfsalkfalsjdl11");
//            params.put("time_end", "20180125182222");
            //-----------test-----------------------
            
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
              // test -- if (true) {
                if (SUCCESS.equals(resultCode)) {
                    //更新订单信息
                    final Transaction transaction = new Transaction().findFirst("select * from jp_transaction where order_no = ?", orderNo);
                    if (transaction == null) { //订单不存在
                        log.error("微信支付通知错误：orderNo [" + orderNo + "] is not exists!params = []" + params);
                        return;
                    }
                    if (!"0".equals(transaction.getStatus()) && !"1".equals(transaction.getStatus())) {//订单状态不对
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
                    
                    boolean res = Db.tx(new IAtom() {
                        @Override
                        public boolean run() throws SQLException {

                            if (!transaction.update()) { //跟新订单状态
                                return false;
                            }

                            if (!calculateBonus(transaction)) { //计算并分配奖金
                                return false;
                            }

                            return true;
                        }
                    });
                    
                    if (!res) { //订单更新失败
                        log.error("微信支付通知错误：order update failed!params = []" + params);
                        return;
                    }
                    
                    Map<String, String> xml = new HashMap<>();
                    xml.put("return_code", SUCCESS);
                    xml.put("return_msg", "OK");
                    renderText(PaymentKit.toXml(xml));
                } else {
                    log.error("微信支付通知错误：result_code =" + resultCode);
                }
            }
            renderText("");
        } catch (Exception e) {
            log.error("微信支付通知错误：", e);
        }
    }
    
    private boolean calculateBonus(Transaction transaction) {
        User currUser = UserQuery.DAO.findById(transaction.getUserId());//当前消费用户（不从缓存读取，因为缓存中的团队人数和团队金额不是最新值）
        BigDecimal totleFee = transaction.getTotleFee();//本次消费总金额
        BigInteger transactionId = transaction.getId();
        
        //计算当前用户的父亲的奖金
        User pUser = UserQuery.DAO.findById(currUser.getPid());
        if (pUser != null) {
            if (!calculationBounsByUserLevel(totleFee, transactionId, pUser, "C")) {
                return false;
            }
        } else {
            return true;
        }
        
        //计算当前用户的父亲的父亲的奖金
        User ppUser = UserQuery.DAO.findById(pUser.getPid());
        if (ppUser != null) {
            if (!calculationBounsByUserLevel(totleFee, transactionId, ppUser, "B")) {
                return false;
            }
        } else {
            return true;
        }
        
        //计算当前用户的父亲的父亲的父亲的奖金
        User pppUser = UserQuery.DAO.findById(ppUser.getPid());
        if (pppUser != null) {
            if (!calculationBounsByUserLevel(totleFee, transactionId, pppUser, "A")) {
                return false;
            }
        } else {
            return true;
        }
        
        return true;
    }

    private boolean calculationBounsByUserLevel(BigDecimal totleFee, BigInteger transactionId, User user, String level) {
        //此笔消费计入团队总消费金额
        if (Db.update("update jp_user set teamBuyAmount = teamBuyAmount + ? where id = ?", totleFee, user.getId()) <= 0) {
            return false;
        }
        //计算获得的个人直推奖金
        if ("C".equals(level) || "B".equals(level)) {
            Bonus bonusUser = new Bonus();
            BigDecimal bonusUseramount = totleFee.multiply(BOUNS_RATIO_LEVEL2);//间接推广提成5%
            bonusUser.setBonusType(2L);//间接推广
            if ("C".equals(level)) {
                bonusUseramount = totleFee.multiply(BOUNS_RATIO_LEVEL1);//直接推广提成30%
                bonusUser.setBonusType(1L);//直接推广
            }
            bonusUser.setAmount(bonusUseramount);
            bonusUser.setBonusTime(new Date());
            bonusUser.setBonusCycle(1L);//奖金计算周期类型（1 按订单结算也就是实时结算;2 按月结算）
            bonusUser.setTransactionId(transactionId);
            bonusUser.setUserId(user.getId());
            if (!bonusUser.save()) {
                return false;
            }
            if (Db.update("update jp_user set amount = amount + ? where id = ?", bonusUseramount, user.getId()) <= 0) {
                return false;
            }
        }
        
        //计算获得的团队奖金
        BigDecimal bounsTeamAmount = calculationTeamBouns(user,totleFee);
        if (bounsTeamAmount != null) {
            Bonus bounsTeam = new Bonus();
            bounsTeam.setAmount(bounsTeamAmount);
            bounsTeam.setBonusCycle(1L);//奖金计算周期类型（1 按订单结算也就是实时结算;2 按月结算）
            bounsTeam.setBonusTime(new Date());
            bounsTeam.setBonusType(3L);//奖金分类（1 个人提成-直接推广;2 个人提成-间接推广;3 团队提成;4 团队管理费）
            bounsTeam.setTransactionId(transactionId);
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
     * @param totleFee
     * @return
     */
    BigDecimal calculationTeamBouns(User user, BigDecimal totleFee){
        long childNum = user.getChildNum().longValue();
        long teamNum = user.getTeamNum().longValue();
        if (childNum >= 50 && teamNum >= 800) {
            return totleFee.multiply(BigDecimal.valueOf(0.2));//提成20%
        } else if (childNum >= 20 && teamNum >= 200) {
            return totleFee.multiply(BigDecimal.valueOf(0.15));//提成15%
        } else if (childNum >= 12 && teamNum >= 80) {
            return totleFee.multiply(BigDecimal.valueOf(0.1));//提成10%
        } else if (childNum >= 5 && teamNum >= 25) {
            return totleFee.multiply(BigDecimal.valueOf(0.08));//提成8%
        } else if (childNum >= 3 && teamNum >= 10) {
            return totleFee.multiply(BigDecimal.valueOf(0.05));//提成5%
        } else {
            return null;
        }
    }
    
    //购物车结算微信支付
    @Before(UCodeInterceptor.class)
    public void shoppingCartWechatpay(){
        try {
            final String remark=getPara("remark");
            final BigInteger userAddressId=getParaToBigInteger("userAddressId");
            final BigInteger[] shoppingCartIds=getParaValuesToBigInteger("shoppingCartIds");
            if (userAddressId==null) {
                renderAjaxResultForError("收货地址不能为空");
                return;
            }
            if (shoppingCartIds==null) {
                renderAjaxResultForError("结算商品不能为空");
                return;
            }

            final BigInteger userId=getLoginedUser().getId();

            final UserAddress userAddress=UserAddressQuery.me().findById(userAddressId);
            if (userAddress==null) {
                renderAjaxResultForError("收货地址不存在");
                return;
            }

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
                shoppingCartIdSb.append(shoppingCartId).append(",");
            }

            // 商户订单号，商户网站订单系统中唯一订单号，必填
            final String orderNo=RandomUtils.randomKey(null, userId.toString());
            // 付款金额，必填
            final BigDecimal totalFee=ShoppingCartQuery.me().getTotalFee(shoppingCartIdSb.substring(0, shoppingCartIdSb.length()-1), userId);

            boolean saved=Db.tx(new IAtom() {
                @Override
                public boolean run() throws SQLException {

                    Transaction transaction=new Transaction();
                    transaction.setRemark(remark);
                    transaction.setUserId(userId);
                    transaction.setUserAddress(userAddress.getAddress()+" "+userAddress.getName()+" "+userAddress.getMobile());
                    transaction.setOrderNo(orderNo);
                    transaction.setTotleFee(totalFee);
                    transaction.setPayType(Transaction.PAY_TYPE_WECHATPAY);
                    transaction.setStatus(Transaction.STATUS_1);
                    transaction.setCreated(new Date());
                    if(!transaction.save()){
                        return false;
                    }

                    List<ShoppingCart> list=ShoppingCartQuery.me().findList(shoppingCartIdSb.substring(0, shoppingCartIdSb.length()-1), null);
                    if(list.isEmpty()){
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
                                return false;
                            }
                        }
                    }

                    ShoppingCartQuery.me().deleteByIds(shoppingCartIdSb.substring(0, shoppingCartIdSb.length()-1));

                    return true;
                }
            });

            if(saved){
                String userJson = this.getSessionAttr(Consts.SESSION_WECHAT_USER);
                String openId = new ApiResult(userJson).getStr("openid");
                String jsonStr = wechartPrePay(openId, orderNo, totalFee);
                
                System.out.println(jsonStr);
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
    @Before(UCodeInterceptor.class)
    public void contentWechatpay(){
        try {
            final String remark=getPara("remark");
            final BigInteger userAddressId=getParaToBigInteger("userAddressId");
            final BigInteger contentId=getParaToBigInteger("contentId");
            final BigInteger specValueId=getParaToBigInteger("specValueId");
            final Integer quantity=getParaToInt("quantity");

            if (userAddressId==null) {
                renderAjaxResultForError("收货地址不能为空");
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

            final UserAddress userAddress=UserAddressQuery.me().findById(userAddressId);
            if (userAddress==null) {
                renderAjaxResultForError("收货地址不存在");
                return;
            }

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

            // 商户订单号，商户网站订单系统中唯一订单号，必填
            final String orderNo=RandomUtils.randomKey(null, userId.toString());
            // 付款金额，必填
            final BigDecimal totalFee=contentSpecItem.getPrice().multiply(new BigDecimal(quantity));

            boolean saved=Db.tx(new IAtom() {
                @Override
                public boolean run() throws SQLException {
                    Transaction transaction=new Transaction();
                    transaction.setRemark(remark);
                    transaction.setUserId(userId);
                    transaction.setUserAddress(userAddress.getAddress()+" "+userAddress.getName()+" "+userAddress.getMobile());
                    transaction.setOrderNo(orderNo);
                    transaction.setTotleFee(totalFee);
                    transaction.setPayType(Transaction.PAY_TYPE_WECHATPAY);
                    transaction.setStatus(Transaction.STATUS_1);
                    transaction.setCreated(new Date());
                    if(!transaction.save()){
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
                        return false;
                    }

                    return true;
                }
            });

            if(saved){
                String userJson = this.getSessionAttr(Consts.SESSION_WECHAT_USER);
                String openId = new ApiResult(userJson).getStr("openid");
                String jsonStr = wechartPrePay(openId, orderNo, totalFee);
                
                System.out.println(jsonStr);
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
