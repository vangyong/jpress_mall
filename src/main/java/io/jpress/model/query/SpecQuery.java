package io.jpress.model.query;

import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.ehcache.IDataLoader;
import io.jpress.model.Spec;
import io.jpress.utils.StringUtils;

import java.math.BigInteger;
import java.util.List;

/**
 * @author heguoliang
 * @Description: TODO()
 * @date 2017-6-14 15:19
 */
public class SpecQuery extends JBaseQuery{

    protected static final Spec DAO = new Spec();

    private static final SpecQuery QUERY = new SpecQuery();

    public static SpecQuery me() {
        return QUERY;
    }

    public Spec findById(final BigInteger id) {
        return DAO.getCache(id, new IDataLoader() {
            @Override
            public Object load() {
                return DAO.findById(id);
            }
        });
    }

    public Page<Spec> paginate(int pageNumber, int pageSize, String keyword, String status) {
        String select = " SELECT s.*,GROUP_CONCAT(sv.id,':',sv.value,':',sv.order_number SEPARATOR ',') AS spec_value ";
        StringBuilder fromBuilder = new StringBuilder(" FROM spec s ");
        fromBuilder.append(" LEFT JOIN specvalue sv ON s.id=sv.spec_id ");
        fromBuilder.append(" WHERE 1=1 ");
        if(StringUtils.isNotBlank(keyword)){
            fromBuilder.append(" AND ( ");
            fromBuilder.append(" s.title LIKE " +"'%" + keyword + "%'");
            fromBuilder.append(" ) ");
        }
        if(StringUtils.isNotBlank(status)){
            fromBuilder.append(" AND s.status = "+"'"+status+"'");
        }
        fromBuilder.append(" GROUP BY s.id ");
        fromBuilder.append(" ORDER BY s.created DESC ");
        return DAO.paginate(pageNumber, pageSize, select, fromBuilder.toString());
    }

    public List<Spec> findListByStatus(String status){
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT s.* ");
        sql.append(" FROM spec s ");
        sql.append(" WHERE 1=1 ");
        if(StringUtils.isNotBlank(status)){
            sql.append(" AND s.status = "+"'"+status+"'");
        }
        sql.append(" GROUP BY s.id ");
        sql.append(" ORDER BY s.created DESC ");
        return DAO.find(sql.toString());
    }

    //商品选中的规格
    public Spec findContentSpec(BigInteger content_id) {
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT s.* ");
        sql.append(" FROM spec s ");
        sql.append(" LEFT JOIN contentspecitem csi ON csi.spec_id=s.id ");
        sql.append(" WHERE 1=1 ");
        sql.append(" AND csi.content_id = "+content_id);
        sql.append(" GROUP BY s.id ");
        sql.append(" ORDER BY s.created DESC ");
        return DAO.findFirst(sql.toString());
    }

}
