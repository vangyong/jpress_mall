package io.jpress.model;

import io.jpress.model.base.BaseSpec;
import io.jpress.model.core.Table;
import io.jpress.utils.StringUtils;

/**
 * @author heguoliang
 * @Description: TODO()
 * @date 2017-6-14 15:17
 */
@Table(tableName = "spec", primaryKey = "id")
public class Spec extends BaseSpec<Spec> {

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

    public java.lang.String getSpecValue() {
        return get("spec_value");
    }

    public String getSpecValueValue(){
        String specValue=getSpecValue();
        if(StringUtils.isBlank(specValue)){
            return null;
        }
        StringBuilder values=new StringBuilder();
        String[] specValues=specValue.split(",");
        for(String specValueTemp:specValues){
            String[] temp=specValueTemp.split(":");
            values.append(temp[1]).append(",");
        }
        return values.toString().substring(0,values.length()-1);
    }

}
