package io.jpress.model;

import io.jpress.model.base.BaseSpecValue;
import io.jpress.model.core.Table;
import io.jpress.model.query.ContentSpecItemQuery;

import java.math.BigInteger;

/**
 * @author heguoliang
 * @Description: TODO()
 * @date 2017-6-14 15:17
 */
@Table(tableName = "spec_value", primaryKey = "id")
public class SpecValue extends BaseSpecValue<SpecValue> {

    public static String STATUS_DELETE = "delete";
    public static String STATUS_NORMAL = "normal";

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

    public ContentSpecItem getContentSpecItem(BigInteger content_id){
        if(content_id==null){
            return null;
        }
        BigInteger id=getId();
        if(id==null){
            return null;
        }
        ContentSpecItem contentSpecItem=ContentSpecItemQuery.me().findByContentIdAndSpecValueId(content_id, id);
        if(contentSpecItem!=null){
            return contentSpecItem;
        }else{
            return null;
        }
    }

}
