package io.jpress.model.base;

import com.jfinal.plugin.activerecord.IBean;
import io.jpress.cache.JCacheKit;
import com.jfinal.plugin.ehcache.IDataLoader;
import io.jpress.model.core.JModel;

import java.math.BigInteger;

/**
 * @author heguoliang
 * @Description: TODO()
 * @date 2017-6-14 15:11
 */
public class BaseSpecValue<M extends BaseSpecValue<M>> extends JModel<M> implements IBean {

    public static final String CACHE_NAME = "spec_value";

    public void removeCache(Object key){
        if(key == null) return;
        JCacheKit.remove(CACHE_NAME, key);
    }

    public void putCache(Object key,Object value){
        JCacheKit.put(CACHE_NAME, key, value);
    }

    public M getCache(Object key){
        return JCacheKit.get(CACHE_NAME, key);
    }

    public M getCache(Object key,IDataLoader dataloader){
        return JCacheKit.get(CACHE_NAME, key, dataloader);
    }

    @Override
    public boolean equals(Object o) {
        if(o == null){ return false; }
        if(!(o instanceof BaseSpecValue<?>)){return false;}

        BaseSpecValue<?> m = (BaseSpecValue<?>) o;
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

    public void setSpecId(java.math.BigInteger specId) {
        set("spec_id", specId);
    }

    public java.math.BigInteger getSpecId() {
        return get("spec_id");
    }

    public void setValue(java.lang.String value) {
        set("value", value);
    }

    public java.lang.String getValue() {
        return get("value");
    }

    public void setOrderNumber(java.lang.Integer orderNumber) {
        set("order_number", orderNumber);
    }

    public java.lang.Integer getOrderNumber() {
        return get("order_number");
    }

    public void setStatus(java.lang.String status) {
        set("status", status);
    }

    public java.lang.String getStatus() {
        return get("status");
    }

    public void setCreated(java.util.Date created) {
        set("created", created);
    }

    public java.util.Date getCreated() {
        return get("created");
    }

}
