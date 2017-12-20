package io.jpress.model.query;

import com.jfinal.plugin.ehcache.IDataLoader;
import io.jpress.model.SpecValue;
import io.jpress.utils.StringUtils;

import java.math.BigInteger;
import java.util.List;

/**
 * @author heguoliang
 * @Description: TODO()
 * @date 2017-6-14 15:19
 */
public class SpecValueQuery extends JBaseQuery{

    protected static final SpecValue DAO = new SpecValue();

    private static final SpecValueQuery QUERY = new SpecValueQuery();

    public static SpecValueQuery me() {
        return QUERY;
    }

    public SpecValue findById(final BigInteger id) {
        return DAO.getCache(id, new IDataLoader() {
            @Override
            public Object load() {
                return DAO.findById(id);
            }
        });
    }
    
    public SpecValue findBySpecIdAndValue(BigInteger spec_id, String value) {
    	return DAO.doFindFirst(" spec_id = ? and value = ? ", spec_id, value);
    }

    public List<SpecValue> findList(BigInteger spec_id, String status){
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT sv.* ");
        sql.append(" FROM specvalue sv ");
        sql.append(" WHERE 1=1 ");
        if(spec_id != null){
            sql.append(" AND sv.spec_id = "+spec_id);
        }
        if(StringUtils.isNotBlank(status)){
            sql.append(" AND sv.status = "+"'"+status+"'");
        }
        sql.append(" GROUP BY sv.id ");
        sql.append(" ORDER BY sv.created DESC ");
        return DAO.find(sql.toString());
    }

    //商品选中的规格，该规格的所有规格值
    public List<SpecValue> findListByContentId(BigInteger content_id){
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT sv.* ");
        sql.append(" FROM specvalue sv ");
        sql.append(" LEFT JOIN contentspecitem csi ON csi.spec_id=sv.spec_id ");
        sql.append(" WHERE 1=1 ");
        sql.append(" AND csi.content_id = "+content_id);
        sql.append(" GROUP BY sv.id ");
        sql.append(" ORDER BY id ");
        return DAO.find(sql.toString());
    }

    //商品选中的规格，该规格选中的规格值
    public List<SpecValue> findContentSpecValueList(BigInteger content_id){
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT sv.* ");
        sql.append(" FROM specvalue sv ");
        sql.append(" LEFT JOIN contentspecitem csi ON csi.spec_value_id=sv.id ");
        sql.append(" WHERE 1=1 ");
        sql.append(" AND csi.content_id = "+content_id);
        sql.append(" GROUP BY sv.id ");
        sql.append(" ORDER BY csi.created DESC ");
        return DAO.find(sql.toString());
    }

    public int deleteBySpecId(BigInteger spec_id) {
        return DAO.doDelete("spec_id = ?", spec_id);
    }

}
