package io.jpress.model;

import io.jpress.Consts;
import io.jpress.model.base.BaseTransactionItem;
import io.jpress.model.core.Table;
import io.jpress.model.query.ContentQuery;
import io.jpress.model.query.SpecQuery;
import io.jpress.model.query.SpecValueQuery;
import io.jpress.model.router.ContentRouter;
import io.jpress.model.router.PageRouter;

import java.math.BigInteger;

import com.jfinal.core.JFinal;

/**
 * @author heguoliang
 * @Description: TODO()
 * @date 2017-6-23 14:33
 */
@Table(tableName = "transaction_item", primaryKey = "id")
public class TransactionItem extends BaseTransactionItem<TransactionItem> {

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
    
    public String getContentUrl() {
        BigInteger content_id=getContentId();
        if(content_id==null){
            return null;
        }
        Content content=ContentQuery.me().findById(content_id);
        if(content==null){
            return null;
        }
        String baseUrl = null;
        if (Consts.MODULE_PAGE.equals(content.getModule())) {
            baseUrl = PageRouter.getRouter(content);
        } else {
            baseUrl = ContentRouter.getRouter(content);
        }
        return JFinal.me().getContextPath() + baseUrl;
    }

    public Content getContent(){
        BigInteger content_id=getContentId();
        if(content_id==null){
            return null;
        }
        Content content=ContentQuery.me().findById(content_id);
        if(content==null){
            return null;
        }
        return content;
    }

    public String getSpecValueValue(){
        BigInteger specValue_id=getSpecValueId();
        if(specValue_id==null){
            return null;
        }
        SpecValue specValue= SpecValueQuery.me().findById(specValue_id);
        if(specValue==null){
            return null;
        }
        Spec spec=SpecQuery.me().findById(specValue.getSpecId());
        if(spec==null){
            return null;
        }
        return spec.getTitle()+"："+specValue.getValue();
    }

    public java.lang.String getThumbnail() {
        return get("thumbnail");
    }
    public java.lang.String getTitle() {
        return get("title");
    }
    public String getValue() {
        return get("value");
    }

}
