package io.jpress.model;

import io.jpress.model.base.BaseContentSpecItem;
import io.jpress.model.core.Table;

/**
 * @author heguoliang
 * @Description: TODO()
 * @date 2017-6-14 15:17
 */
@Table(tableName = "content_spec_item", primaryKey = "id")
public class ContentSpecItem extends BaseContentSpecItem<ContentSpecItem> {

    /**
	 * 
	 */
	private static final long serialVersionUID = 6802905874862807879L;

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
