package io.jpress.model.base;

import com.jfinal.plugin.activerecord.IBean;
import com.jfinal.plugin.ehcache.CacheKit;
import com.jfinal.plugin.ehcache.IDataLoader;
import io.jpress.model.core.JModel;

import java.math.BigInteger;

/**
 * @author heguoliang
 * @Description: TODO()
 * @date 2017-6-14 15:11
 */
public class BaseShoppingCart<M extends BaseShoppingCart<M>> extends JModel<M> implements IBean {

	public static final String CACHE_NAME = "shopping_cart";

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
        if(!(o instanceof BaseShoppingCart<?>)){return false;}

        BaseShoppingCart<?> m = (BaseShoppingCart<?>) o;
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
