package io.jpress.model;

import io.jpress.model.base.BaseVerifyCode;
import io.jpress.model.core.Table;

/**
 * @Description:手机验证码
 * @ClassName:VerifyCode
 * @Date:2018年2月24日 下午9:29:34
 * @author:wangyong
 */
@Table(tableName="verify_code",primaryKey="id")
public class VerifyCode extends BaseVerifyCode<VerifyCode> {
	private static final long serialVersionUID = -14831733993667437L;
	
	public static final String STATUS_UNVERIFY = "0";//未验证
	public static final String STATUS_VERIFYED = "1";//已经验证

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
