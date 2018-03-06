package io.jpress.front.controller;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeWapPayModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.jfinal.aop.Before;
import com.jfinal.aop.Clear;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.IAtom;
import io.jpress.Consts;
import io.jpress.core.BaseFrontController;
import io.jpress.interceptor.UCodeInterceptor;
import io.jpress.interceptor.UserInterceptor;
import io.jpress.model.*;
import io.jpress.model.query.*;
import io.jpress.router.RouterMapping;
import io.jpress.utils.RandomUtils;
import io.jpress.utils.StringUtils;
import io.jpress.wechat.WechatUserInterceptor;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.*;

/**
 * @author heguoliang
 * @Description: TODO()
 * @date 2017-6-23 15:07
 */
@RouterMapping(url = "/transaction")
//@Before(UserInterceptor.class)
@Before(WechatUserInterceptor.class)
public class TransactionController extends BaseFrontController {

    //购物车结算支付宝支付
    @Before(UCodeInterceptor.class)
    public void shoppingCartAlipay(){
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
            final String out_trade_no=RandomUtils.randomKey(null, userId.toString());
            // 订单名称，必填
            final String subject="测试商品";
            // 付款金额，必填
            final BigDecimal total_fee=ShoppingCartQuery.me().getTotalFee(shoppingCartIdSb.substring(0, shoppingCartIdSb.length()-1), userId);
            // 商品描述，可空
            final String body="测试商品描述";
            // 销售产品码 必填
            final String product_code="QUICK_WAP_PAY";

            final AlipayClient alipayClient=new DefaultAlipayClient(Consts.Alipay.URL, Consts.Alipay.APPID, Consts.Alipay.RSA_PRIVATE_KEY, Consts.Alipay.FORMAT, Consts.CHARTSET_UTF8, Consts.Alipay.PUBLIC_KEY, Consts.Alipay.SIGNTYPE); //获得初始化的AlipayClient
            final AlipayTradeWapPayRequest alipayRequest=new AlipayTradeWapPayRequest();//创建API对应的request

            boolean saved=Db.tx(new IAtom() {
                @Override
                public boolean run() throws SQLException {
                    // 封装请求支付信息
                    AlipayTradeWapPayModel model=new AlipayTradeWapPayModel();
                    model.setOutTradeNo(out_trade_no);
                    model.setSubject(subject);
                    model.setTotalAmount(total_fee.toString());
                    model.setBody(body);
                    if(StringUtils.isNotBlank(Consts.Alipay.TIMEOUT_EXPRESS)){
                        model.setTimeoutExpress(Consts.Alipay.TIMEOUT_EXPRESS);
                    }
                    model.setProductCode(product_code);
                    alipayRequest.setBizModel(model);
                    // 设置异步通知地址
                    //alipayRequest.setNotifyUrl(Alipay.NOTIFY_URL);
                    // 设置同步地址
                    alipayRequest.setReturnUrl(Consts.Alipay.RETURN_URL);

                    Transaction transaction=new Transaction();
                    transaction.setRemark(remark);
                    transaction.setUserId(userId);
                    transaction.setUserAddress(userAddress.getAddress()+" "+userAddress.getName()+" "+userAddress.getMobile());
                    transaction.setOrderNo(out_trade_no);
                    transaction.setTotleFee(total_fee);
                    transaction.setPayType(Transaction.PAY_TYPE_ALIPAY);
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
                //支付form表单生产
                String form=new String(alipayClient.pageExecute(alipayRequest).getBody().getBytes());
                /*getResponse().setContentType("text/html;charset=" + Consts.CHARTSET_UTF8);
                getResponse().getWriter().write(form);//直接将完整的表单html输出到页面
                getResponse().getWriter().flush();
                getResponse().getWriter().close();
                renderNull();*/
                renderAjaxResult("操作成功", 0, form);
            }else{
                renderAjaxResultForError("操作失败");
            }
        }catch(Exception e) {
            renderAjaxResult("系统异常", Consts.ERROR_CODE_SYSTEM_ERROR, null);
        }
    }

    //商品立即购买支付宝支付
    @Before(UCodeInterceptor.class)
    public void contentAlipay(){
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
            final String out_trade_no=RandomUtils.randomKey(null, userId.toString());
            // 订单名称，必填
            final String subject="测试商品";
            // 付款金额，必填
            final BigDecimal total_fee=contentSpecItem.getPrice().multiply(new BigDecimal(quantity));
            // 商品描述，可空
            final String body="测试商品描述";
            // 销售产品码 必填
            final String product_code="QUICK_WAP_PAY";

            final AlipayClient alipayClient=new DefaultAlipayClient(Consts.Alipay.URL, Consts.Alipay.APPID, Consts.Alipay.RSA_PRIVATE_KEY, Consts.Alipay.FORMAT, Consts.CHARTSET_UTF8, Consts.Alipay.PUBLIC_KEY, Consts.Alipay.SIGNTYPE); //获得初始化的AlipayClient
            final AlipayTradeWapPayRequest alipayRequest=new AlipayTradeWapPayRequest();//创建API对应的request

            boolean saved=Db.tx(new IAtom() {
                @Override
                public boolean run() throws SQLException {
                    // 封装请求支付信息
                    AlipayTradeWapPayModel model=new AlipayTradeWapPayModel();
                    model.setOutTradeNo(out_trade_no);
                    model.setSubject(subject);
                    model.setTotalAmount(total_fee.toString());
                    model.setBody(body);
                    if(StringUtils.isNotBlank(Consts.Alipay.TIMEOUT_EXPRESS)){
                        model.setTimeoutExpress(Consts.Alipay.TIMEOUT_EXPRESS);
                    }
                    model.setProductCode(product_code);
                    alipayRequest.setBizModel(model);
                    // 设置异步通知地址
                    //alipayRequest.setNotifyUrl(Alipay.NOTIFY_URL);
                    // 设置同步地址
                    alipayRequest.setReturnUrl(Consts.Alipay.RETURN_URL);

                    Transaction transaction=new Transaction();
                    transaction.setRemark(remark);
                    transaction.setUserId(userId);
                    transaction.setUserAddress(userAddress.getAddress()+" "+userAddress.getName()+" "+userAddress.getMobile());
                    transaction.setOrderNo(out_trade_no);
                    transaction.setTotleFee(total_fee);
                    transaction.setPayType(Transaction.PAY_TYPE_ALIPAY);
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
                //支付form表单生产
                String form=new String(alipayClient.pageExecute(alipayRequest).getBody().getBytes());
                /*getResponse().setContentType("text/html;charset=" + Consts.CHARTSET_UTF8);
                getResponse().getWriter().write(form);//直接将完整的表单html输出到页面
                getResponse().getWriter().flush();
                getResponse().getWriter().close();
                renderNull();*/
                renderAjaxResult("操作成功", 0, form);
            }else{
                renderAjaxResultForError("操作失败");
            }
        }catch(Exception e) {
            renderAjaxResult("系统异常", Consts.ERROR_CODE_SYSTEM_ERROR, null);
        }
    }

    //待支付-支付宝支付
    @Before(UCodeInterceptor.class)
    public void reAlipay(){
        try {
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

            final BigInteger userId=getLoginedUser().getId();

            // 商户订单号，商户网站订单系统中唯一订单号，必填
            final String out_trade_no=RandomUtils.randomKey(null, userId.toString());
            // 订单名称，必填
            final String subject="测试商品";
            // 付款金额，必填
            final BigDecimal total_fee=transaction.getTotleFee();
            // 商品描述，可空
            final String body="测试商品描述";
            // 销售产品码 必填
            final String product_code="QUICK_WAP_PAY";

            final AlipayClient alipayClient=new DefaultAlipayClient(Consts.Alipay.URL, Consts.Alipay.APPID, Consts.Alipay.RSA_PRIVATE_KEY, Consts.Alipay.FORMAT, Consts.CHARTSET_UTF8, Consts.Alipay.PUBLIC_KEY, Consts.Alipay.SIGNTYPE); //获得初始化的AlipayClient
            final AlipayTradeWapPayRequest alipayRequest=new AlipayTradeWapPayRequest();//创建API对应的request

            boolean saved=Db.tx(new IAtom() {
                @Override
                public boolean run() throws SQLException {
                    // 封装请求支付信息
                    AlipayTradeWapPayModel model=new AlipayTradeWapPayModel();
                    model.setOutTradeNo(out_trade_no);
                    model.setSubject(subject);
                    model.setTotalAmount(total_fee.toString());
                    model.setBody(body);
                    if(StringUtils.isNotBlank(Consts.Alipay.TIMEOUT_EXPRESS)){
                        model.setTimeoutExpress(Consts.Alipay.TIMEOUT_EXPRESS);
                    }
                    model.setProductCode(product_code);
                    alipayRequest.setBizModel(model);
                    // 设置异步通知地址
                    //alipayRequest.setNotifyUrl(Alipay.NOTIFY_URL);
                    // 设置同步地址
                    alipayRequest.setReturnUrl(Consts.Alipay.RETURN_URL);

                    transaction.setOrderNo(out_trade_no);
                    if(!transaction.update()){
                        return false;
                    }

                    return true;
                }
            });

            if(saved){
                //支付form表单生产
                String form=new String(alipayClient.pageExecute(alipayRequest).getBody().getBytes());
                /*getResponse().setContentType("text/html;charset=" + Consts.CHARTSET_UTF8);
                getResponse().getWriter().write(form);//直接将完整的表单html输出到页面
                getResponse().getWriter().flush();
                getResponse().getWriter().close();
                renderNull();*/
                renderAjaxResult("操作成功", 0, form);
            }else{
                renderAjaxResultForError("操作失败");
            }
        }catch(Exception e) {
            renderAjaxResult("系统异常", Consts.ERROR_CODE_SYSTEM_ERROR, null);
        }
    }

    public Map<String, String> getRequestParams(Map<String ,String[]> requestParams, boolean flag) throws UnsupportedEncodingException {
        //获取支付宝过来反馈信息
        Map<String,String> params = new HashMap<String,String>();
        for (Iterator iter = requestParams.keySet().iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            String[] values = (String[]) requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i]
                        : valueStr + values[i] + ",";
            }
            if(flag) {
                //乱码解决，这段代码在出现乱码时使用。如果mysign和sign不相等也可以使用这段代码转化
                valueStr = new String(valueStr.getBytes(Consts.CHARTSET_ISO_8859_1), Consts.CHARTSET_UTF8);
            }
            params.put(name, valueStr);
        }
        return params;
    }

    @Clear(UserInterceptor.class)
    public void alipayReturn(){
        try{
            Map requestParams = getRequest().getParameterMap();

            Map<String,String> params = getRequestParams(requestParams, true);

            //商户订单号
            final String out_trade_no = new String(getPara("out_trade_no").getBytes(Consts.CHARTSET_ISO_8859_1), Consts.CHARTSET_UTF8);
            //支付宝交易号
            final String trade_no = new String(getPara("trade_no").getBytes(Consts.CHARTSET_ISO_8859_1), Consts.CHARTSET_UTF8);

            //计算得出通知验证结果
            boolean verify_result = AlipaySignature.rsaCheckV1(params, Consts.Alipay.PUBLIC_KEY, Consts.CHARTSET_UTF8, Consts.Alipay.SIGNTYPE);

            if(verify_result){//验证成功
                final Transaction transaction=TransactionQuery.me().findByOrderNo(out_trade_no);
                if(transaction!=null){
                    boolean saved=Db.tx(new IAtom() {
                        @Override
                        public boolean run() throws SQLException {
                            transaction.setTradeNo(trade_no);
                            transaction.setStatus(Transaction.STATUS_2);
                            transaction.setPayed(new Date());
                            if(!transaction.update()){
                                return false;
                            }

                            //库存操作
                            List<TransactionItem> transactionItemList=TransactionItemQuery.me().findList(transaction.getId());
                            if(transactionItemList.isEmpty()){
                                return false;
                            }else{
                                for(TransactionItem transactionItem:transactionItemList){
                                    ContentSpecItem contentSpecItem=ContentSpecItemQuery.me().findByContentIdAndSpecValueId(transactionItem.getContentId(), transactionItem.getSpecValueId());
                                    if(contentSpecItem!=null){
                                        Integer stock=contentSpecItem.getStock()-transactionItem.getQuantity();
                                        contentSpecItem.setStock(stock);
                                        if(!contentSpecItem.update()){
                                            return false;
                                        }
                                    }
                                }
                            }

                            return true;
                        }
                    });

                    if(saved){
                        redirect("/user/center");
                    }else{
                        renderError(404);
                    }
                }else{
                    renderError(404);
                }
            }else{
                renderError(404);
            }
        }catch (Exception e) {
            renderError(404);
        }
    }

    //删除订单
    @Before(UCodeInterceptor.class)
    public void delete(){
        final BigInteger id=getParaToBigInteger("id");
        if (id==null) {
            renderAjaxResultForError("订单id不能为空");
            return;
        }
        final Transaction transaction = TransactionQuery.me().findById(id);
        if (!"".equals(transaction.getStatus())) {
            renderAjaxResultForError("当前状态的订单不允许删除");
            return;
        }
        final BigInteger userId = getLoginedUser().getId();
        boolean saved=Db.tx(new IAtom() {
            @Override
            public boolean run() throws SQLException {
                transaction.deleteById(id);
                TransactionItemQuery.me().deleteByTransactionId(id);
                
                Bonus bonus = BonusQuery.me().findByTransactionId(id);
                //退还余额
                if (Db.update("update jp_user set amount = amount - ? where id = ?", bonus.getAmount(), userId) <= 0) {
                    return false;
                }
                //删除余额支付记录
                BonusQuery.me().deleteByTransactionId(id);
                return true;
            }
        });
        if (saved) {
            
        }
        renderAjaxResultForSuccess();
    }

    //确认收货
    @Before(UCodeInterceptor.class)
    public void received(){
        BigInteger id=getParaToBigInteger("id");
        if (id==null) {
            renderAjaxResultForError("订单id不能为空");
            return;
        }
        Transaction transaction=TransactionQuery.me().findById(id);
        if (transaction==null) {
            renderAjaxResultForError("订单不存在");
            return;
        }

        transaction.setStatus(Transaction.STATUS_4);
        if(!transaction.update()){
            renderAjaxResultForError("操作失败");
            return;
        }

        renderAjaxResultForSuccess();
    }

}
