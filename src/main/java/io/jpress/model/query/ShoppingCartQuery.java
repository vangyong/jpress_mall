package io.jpress.model.query;

import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.ehcache.IDataLoader;
import io.jpress.model.ShoppingCart;
import io.jpress.utils.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

/**
 * @author heguoliang
 * @Description: TODO()
 * @date 2017-6-14 15:19
 */
public class ShoppingCartQuery extends JBaseQuery{

    protected static final ShoppingCart DAO = new ShoppingCart();

    private static final ShoppingCartQuery QUERY = new ShoppingCartQuery();

    public static ShoppingCartQuery me() {
        return QUERY;
    }

    public ShoppingCart findById(final BigInteger id) {
        return DAO.getCache(id, new IDataLoader() {
            @Override
            public Object load() {
                return DAO.findById(id);
            }
        });
    }

    public long findcount(BigInteger user_id){
        return DAO.doFindCount("user_id = ?", user_id);
    }

    public int deleteByUserId(BigInteger user_id) {
        return DAO.doDelete("user_id = ?", user_id);
    }

    public int deleteByIds(String ids) {
        return DAO.doDelete("id in ("+ids+")");
    }

    public Page<ShoppingCart> paginate(int pageNumber, int pageSize, String ids, BigInteger user_id, String orderBy, String status) {
        String select = " SELECT sc.*,c.title,c.thumbnail,CONCAT(s.title,'：',sv.value) AS value,csi.price ";
        StringBuilder fromBuilder = new StringBuilder(" FROM shoppingcart sc ");
        fromBuilder.append(" LEFT JOIN content c ON sc.content_id=c.id ");
        fromBuilder.append(" LEFT JOIN specvalue sv ON sv.id=sc.spec_value_id ");
        fromBuilder.append(" LEFT JOIN spec s ON sv.spec_id=s.id ");
        fromBuilder.append(" LEFT JOIN contentspecitem csi ON c.id=csi.content_id AND sc.spec_value_id=csi.spec_value_id ");
        fromBuilder.append(" WHERE 1=1 ");
        if(user_id != null){
            fromBuilder.append(" AND sc.user_id = "+user_id);
        }
        if(StringUtils.isNotBlank(ids)){
            fromBuilder.append(" AND sc.id in ("+ids+") ");
        }
        if(StringUtils.isNotBlank(status)){
            fromBuilder.append(" AND c.status = "+"'"+status+"'");
        }
        fromBuilder.append(" GROUP BY sc.id ");
        if(StringUtils.isNotBlank(orderBy)){
            fromBuilder.append(" ORDER BY sc. "+"'"+orderBy+"'");
        }else{
            fromBuilder.append(" ORDER BY sc.created DESC ");
        }
        return DAO.paginate(pageNumber, pageSize, select, fromBuilder.toString());
    }

    public List<ShoppingCart> findList(String ids, BigInteger user_id){
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT sc.*,c.title,c.thumbnail,CONCAT(s.title,'：',sv.value) AS value,csi.price ");
        sql.append(" FROM shoppingcart sc ");
        sql.append(" LEFT JOIN content c ON sc.content_id=c.id ");
        sql.append(" LEFT JOIN specvalue sv ON sv.id=sc.spec_value_id ");
        sql.append(" LEFT JOIN spec s ON sv.spec_id=s.id ");
        sql.append(" LEFT JOIN contentspecitem csi ON c.id=csi.content_id AND sc.spec_value_id=csi.spec_value_id ");
        sql.append(" WHERE 1=1 ");
        if(user_id != null){
            sql.append(" AND sc.user_id = "+user_id);
        }
        if(StringUtils.isNotBlank(ids)){
            sql.append(" AND sc.id in ("+ids+") ");
        }
        sql.append(" GROUP BY sc.id ");
        sql.append(" ORDER BY sc.created DESC ");
        return DAO.find(sql.toString());
    }

    public ShoppingCart find(BigInteger user_id, BigInteger content_id, BigInteger spec_value_id){
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT sc.* ");
        sql.append(" FROM shoppingcart sc ");
        sql.append(" WHERE 1=1 ");
        sql.append(" AND sc.user_id = "+user_id);
        sql.append(" AND sc.content_id = "+content_id);
        sql.append(" AND sc.spec_value_id = "+spec_value_id);
        sql.append(" GROUP BY sc.id ");
        sql.append(" ORDER BY sc.created DESC ");
        return DAO.findFirst(sql.toString());
    }

    public BigDecimal getTotalFee(String ids, BigInteger user_id){
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT SUM(sc.quantity*csi.price) as price ");
        sql.append(" FROM shoppingcart sc ");
        sql.append(" LEFT JOIN content c ON sc.content_id=c.id ");
        sql.append(" LEFT JOIN contentspecitem csi ON c.id=csi.content_id AND sc.spec_value_id=csi.spec_value_id ");
        sql.append(" WHERE 1=1 ");
        if(StringUtils.isNotBlank(ids)){
            sql.append(" AND sc.id in ("+ids+") ");
        }
        if(user_id != null){
            sql.append(" AND sc.user_id = "+user_id);
        }
        return DAO.findFirst(sql.toString()).getPrice();
    }

    public Object getTotalFeeAndQuantity(String ids, BigInteger user_id){
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT SUM(sc.quantity*csi.price) as price,SUM(sc.quantity) as quantity ");
        sql.append(" FROM shoppingcart sc ");
        sql.append(" LEFT JOIN content c ON sc.content_id=c.id ");
        sql.append(" LEFT JOIN contentspecitem csi ON c.id=csi.content_id AND sc.spec_value_id=csi.spec_value_id ");
        sql.append(" WHERE 1=1 ");
        if(StringUtils.isNotBlank(ids)){
            sql.append(" AND sc.id in ("+ids+") ");
        }
        if(user_id != null){
            sql.append(" AND sc.user_id = "+user_id);
        }
        return DAO.findFirst(sql.toString());
    }

}
