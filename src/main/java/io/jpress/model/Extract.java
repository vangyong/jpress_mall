package io.jpress.model;

import io.jpress.model.base.BaseExtract;
import io.jpress.model.core.Table;

/**
 * @Description:提现申请
 * @ClassName:Extract
 * @Date:2018年2月5日 下午9:29:34
 * @author:wangyong
 */
@Table(tableName="extract",primaryKey="id")
public class Extract extends BaseExtract<Extract> {
    
    private static final long serialVersionUID = -81305671758566183L;
    
	public final static String STATUS_DRAFT = "0";//待审核	
	public final static String STATUS_AGREE = "1";//审核通过	
	public final static String STATUS_UNAGREE = "2";//审核不通过
	public final static String STATUS_PAYED = "3";//支付完成

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
