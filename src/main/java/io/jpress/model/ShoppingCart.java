package io.jpress.model;

import com.jfinal.core.JFinal;
import io.jpress.Consts;
import io.jpress.model.base.BaseShoppingCart;
import io.jpress.model.core.Table;
import io.jpress.model.query.ContentQuery;
import io.jpress.model.query.SpecValueQuery;
import io.jpress.model.router.ContentRouter;
import io.jpress.model.router.PageRouter;

import java.math.BigInteger;

/**
 * @author heguoliang
 * @Description: TODO()
 * @date 2017-6-14 15:17
 */
@Table(tableName = "shopping_cart", primaryKey = "id")
public class ShoppingCart extends BaseShoppingCart<ShoppingCart> {

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
    
    public Content getContent(){
    	BigInteger content_id=getContentId();
    	if(content_id==null){
    		return null;
    	}
    	Content content=ContentQuery.me().findById(content_id);
    	if(content==null){
    		return null;
    	}else{
    		return content;
    	}
    }

    public SpecValue getSpecValue(){
    	BigInteger specValue_id=getSpecValueId();
    	if(specValue_id==null){
    		return null;
    	}
    	SpecValue specValue=SpecValueQuery.me().findById(specValue_id);
    	if(specValue==null){
    		return null;
    	}else{
    	    return specValue;
        }
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

    public java.lang.String getThumbnail() {
        return get("thumbnail");
    }
    public java.lang.String getTitle() {
        return get("title");
    }
    public java.math.BigDecimal getPrice() {
        return get("price");
    }
    public String getValue() {
        return get("value");
    }

}
