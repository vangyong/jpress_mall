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
public class BaseUserAddress<M extends BaseUserAddress<M>> extends JModel<M> implements IBean {

	public static final String CACHE_NAME = "user_address";

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
        if(!(o instanceof BaseUserAddress<?>)){return false;}

        BaseUserAddress<?> m = (BaseUserAddress<?>) o;
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

    public void setName(java.lang.String name) {
        set("name", name);
    }

    public java.lang.String getName() {
        return get("name");
    }

    public void setMobile(java.lang.String mobile) {
        set("mobile", mobile);
    }

    public java.lang.String getMobile() {
        return get("mobile");
    }

    public void setAddress(java.lang.String address) {
        set("address", address);
    }

    public java.lang.String getAddress() {
        return get("address");
    }

    public void setZipcode(java.lang.String zipcode) {
        set("zipcode", zipcode);
    }

    public java.lang.String getZipcode() {
        return get("zipcode");
    }

    public void setCreated(java.util.Date created) {
        set("created", created);
    }

    public java.util.Date getCreated() {
        return get("created");
    }

}
