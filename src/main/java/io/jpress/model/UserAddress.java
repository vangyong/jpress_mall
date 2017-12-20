package io.jpress.model;

import io.jpress.model.base.BaseUserAddress;
import io.jpress.model.core.Table;

/**
 * @author heguoliang
 * @Description: TODO()
 * @date 2017-6-22 18:30
 */
@Table(tableName = "user_address", primaryKey = "id")
public class UserAddress extends BaseUserAddress<UserAddress>{

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

}
