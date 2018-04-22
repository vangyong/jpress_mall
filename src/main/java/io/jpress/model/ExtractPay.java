package io.jpress.model;

import io.jpress.model.base.BaseExtractPay;
import io.jpress.model.core.Table;

/**
 * @Description:提现支付
 * @ClassName:ExtractPay
 * @Date:2018年2月8日 下午9:29:34
 * @author:wangyong
 */
@Table(tableName="extract_pay",primaryKey="id")
public class ExtractPay extends BaseExtractPay<ExtractPay> {
    
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
