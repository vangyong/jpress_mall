package io.jpress.model;

import io.jpress.model.base.BaseTransaction;
import io.jpress.model.core.Table;
import io.jpress.model.query.RefundQuery;
import io.jpress.model.query.UserQuery;
import io.jpress.utils.StringUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * @author heguoliang
 * @Description: TODO()
 * @date 2017-6-23 14:33
 */
@Table(tableName = "transaction", primaryKey = "id")
public class Transaction extends BaseTransaction<Transaction> {

    public final static String PAY_TYPE_ALIPAY="alipay";
    public final static String PAY_TYPE_WECHATPAY="wechatpay";

    public final static String STATUS_0="0";//支付失败
    public final static String STATUS_1="1";//待支付
    public final static String STATUS_2="2";//已支付/待发货
    public final static String STATUS_3="3";//已发货/待收货
    public final static String STATUS_4="4";//已收货/待评价
    public final static String STATUS_5="5";//完成

    @Override
    public boolean save() {
        removeCache();
        return super.save();
    }

    @Override
    public boolean update() {
        removeCache();
        return super.update();
    }

    @Override
    public boolean saveOrUpdate() {
        removeCache();
        return super.saveOrUpdate();
    }

    @Override
    public boolean delete() {
        removeCache();
        return super.delete();
    }

    private void removeCache(){
        removeCache(getId());
    }

    public String getUserName(){
        BigInteger user_id=getUserId();
        if(user_id==null){
            return null;
        }
        User user=UserQuery.me().findById(user_id);
        if(user==null){
            return null;
        }
        return user.getUsername();
    }

    public List<String> getContentThumbnailList(){
        String data=getContentThumbnails();
        if(StringUtils.isBlank(data)){
            return null;
        }
        String[] contentThumbnails=data.split(",");
        List<String> list=new ArrayList<String>();
        for(String contentThumbnail:contentThumbnails){
            list.add(contentThumbnail);
        }
        return list;
    }

    public java.math.BigDecimal getPrice() {
        return get("price");
    }
    public java.math.BigDecimal getQuantity() {
        return get("quantity");
    }
    public java.lang.String getContentThumbnails() {
        return get("contentThumbnails");
    }

    public List<String> getContentTitleList(){
        String data=getContentTitles();
        if(StringUtils.isBlank(data)){
            return null;
        }
        String[] contentTitles=data.split(",");
        List<String> list=new ArrayList<String>();
        for(String contentTitle:contentTitles){
            list.add(contentTitle);
        }
        return list;
    }
    public java.lang.String getContentTitles() {
        return get("contentTitles");
    }

    public void setRefundNo(java.lang.String refundNo) {
        set("refund_no", refundNo);
    }
    
    public String getRefundNo(){
        String rs = get("refund_no");
        if (StringUtils.isBlank(rs)) {
            String orderNo=getOrderNo();
            if(StringUtils.isBlank(orderNo)){
                return null;
            }
            Refund refund=RefundQuery.me().findByOrderNo(orderNo);
            if(refund==null){
                return null;
            }
            return refund.getRefundNo();
        } else {
            return rs;
        }
    }

    public void setRefundStatus(java.lang.String refundStatus) {
        set("refund_status", refundStatus);
    }
    
    public String getRefundStatus(){
        String rs = get("refund_status");
        if (StringUtils.isBlank(rs)) {
            String orderNo=getOrderNo();
            if(StringUtils.isBlank(orderNo)){
                return null;
            }
            Refund refund=RefundQuery.me().findByOrderNo(orderNo);
            if(refund==null){
                return null;
            }
            return refund.getStatus();
        } else {
            return rs;
        }
    }
}
