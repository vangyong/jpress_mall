package io.jpress.model;

import io.jpress.model.base.BaseBonus;
import io.jpress.model.core.Table;

/**
 * <b>Description:</b>
 * <br><b>ClassName:</b> Bonus
 * <br><b>Date:</b> 2018年1月25日 下午6:29:34
 * <br>@author <b>jianb.jiang</b>
 */
@Table(tableName="bonus",primaryKey="id")
public class Bonus extends BaseBonus<Bonus> {
    
    private static final long serialVersionUID = -81305671758566183L;

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
