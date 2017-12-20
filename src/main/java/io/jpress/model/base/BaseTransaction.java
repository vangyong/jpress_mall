package io.jpress.model.base;

import com.jfinal.plugin.activerecord.IBean;
import com.jfinal.plugin.ehcache.CacheKit;
import com.jfinal.plugin.ehcache.IDataLoader;
import io.jpress.model.core.JModel;

import java.math.BigInteger;

/**
 * @author heguoliang
 * @Description: TODO()
 * @date 2017-6-23 14:19
 */
public class BaseTransaction<M extends BaseTransaction<M>> extends JModel<M> implements IBean {

    public static final String CACHE_NAME = "transaction";

    public void removeCache(Object key){
        if(key == null) return;
        CacheKit.remove(CACHE_NAME, key);
    }

    public void putCache(Object key,Object value){
        CacheKit.put(CACHE_NAME, key, value);
    }

    public M getCache(Object key){
        return CacheKit.get(CACHE_NAME, key);
    }

    public M getCache(Object key,IDataLoader dataloader){
        return CacheKit.get(CACHE_NAME, key, dataloader);
    }

    @Override
    public boolean equals(Object o) {
        if(o == null){ return false; }
        if(!(o instanceof BaseTransaction<?>)){return false;}

        BaseTransaction<?> m = (BaseTransaction<?>) o;
        if(m.getId() == null){return false;}

        return m.getId().compareTo(this.getId()) == 0;
    }

    public void setId(java.math.BigInteger id) {
        set("id", id);
    }

    public java.math.BigInteger getId() {
        Object id = get("id");
        if (id == null)
            return null;

        return id instanceof BigInteger ? (BigInteger)id : new BigInteger(id.toString());
    }

    public void setUserId(java.math.BigInteger userId) {
        set("user_id", userId);
    }

    public java.math.BigInteger getUserId() {
        return get("user_id");
    }

    public void setUserAddress(java.lang.String userAddress) {
        set("user_address", userAddress);
    }

    public java.lang.String getUserAddress() {
        return get("user_address");
    }

    public void setOrderNo(java.lang.String orderNo) {
        set("order_no", orderNo);
    }

    public java.lang.String getOrderNo() {
        return get("order_no");
    }

    public void setTradeNo(java.lang.String tradeNo) {
        set("trade_no", tradeNo);
    }

    public java.lang.String getTradeNo() {
        return get("trade_no");
    }

    public void setRemark(java.lang.String remark) {
        set("remark", remark);
    }

    public java.lang.String getRemark() {
        return get("remark");
    }

    public void setExpress(java.lang.String express) {
        set("express", express);
    }

    public java.lang.String getExpress() {
        return get("express");
    }

    public void setPayType(java.lang.String payType) {
        set("pay_type", payType);
    }

    public java.lang.String getPayType() {
        return get("pay_type");
    }

    public void setStatus(java.lang.String status) {
        set("status", status);
    }

    public java.lang.String getStatus() {
        return get("status");
    }

    public void setTotleFee(java.math.BigDecimal totleFee) {
        set("totle_fee", totleFee);
    }

    public java.math.BigDecimal getTotleFee() {
        return get("totle_fee");
    }

    public void setPayed(java.util.Date payed) {
        set("payed", payed);
    }

    public java.util.Date getPayed() {
        return get("payed");
    }

    public void setCreated(java.util.Date created) {
        set("created", created);
    }

    public java.util.Date getCreated() {
        return get("created");
    }

}
