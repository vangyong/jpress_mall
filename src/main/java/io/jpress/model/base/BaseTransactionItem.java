package io.jpress.model.base;

import com.jfinal.plugin.activerecord.IBean;
import io.jpress.cache.JCacheKit;
import com.jfinal.plugin.ehcache.IDataLoader;
import io.jpress.model.core.JModel;

import java.math.BigInteger;

/**
 * @author heguoliang
 * @Description: TODO()
 * @date 2017-6-23 14:19
 */
public class BaseTransactionItem<M extends BaseTransactionItem<M>> extends JModel<M> implements IBean {

    public static final String CACHE_NAME = "transaction_item";

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
        if(!(o instanceof BaseTransactionItem<?>)){return false;}

        BaseTransactionItem<?> m = (BaseTransactionItem<?>) o;
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

    public void setTransactionId(java.math.BigInteger transactionId) {
        set("transaction_id", transactionId);
    }

    public java.math.BigInteger getTransactionId() {
        return get("transaction_id");
    }

    public void setContentId(java.math.BigInteger contentId) {
        set("content_id", contentId);
    }

    public java.math.BigInteger getContentId() {
        return get("content_id");
    }

    public void setSpecValueId(java.math.BigInteger specValueId) {
        set("spec_value_id", specValueId);
    }

    public java.math.BigInteger getSpecValueId() {
        return get("spec_value_id");
    }

    public void setPrice(java.math.BigDecimal price) {
        set("price", price);
    }

    public java.math.BigDecimal getPrice() {
        return get("price");
    }

    public void setQuantity(java.lang.Integer quantity) {
        set("quantity", quantity);
    }

    public java.lang.Integer getQuantity() {
        return get("quantity");
    }

    public void setCreated(java.util.Date created) {
        set("created", created);
    }

    public java.util.Date getCreated() {
        return get("created");
    }

}
