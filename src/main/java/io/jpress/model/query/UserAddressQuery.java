package io.jpress.model.query;

import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.ehcache.IDataLoader;
import io.jpress.model.UserAddress;
import io.jpress.utils.StringUtils;

import java.math.BigInteger;
import java.util.List;

/**
 * @author heguoliang
 * @Description: TODO()
 * @date 2017-6-22 18:32
 */
public class UserAddressQuery extends JBaseQuery{

    protected static final UserAddress DAO = new UserAddress();

    private static final UserAddressQuery QUERY = new UserAddressQuery();

    public static UserAddressQuery me() {
        return QUERY;
    }

    public UserAddress findById(final BigInteger id) {
        return DAO.getCache(id, new IDataLoader() {
            @Override
            public Object load() {
                return DAO.findById(id);
            }
        });
    }

    public int deleteByUserId(BigInteger user_id) {
        return DAO.doDelete("user_id = ?", user_id);
    }

    public int deleteByIds(String ids) {
        return DAO.doDelete("id in ("+ids+")");
    }

    public List<UserAddress> findList(BigInteger user_id){
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT us.* ");
        sql.append(" FROM useraddress ua ");
        sql.append(" WHERE 1=1 ");
        if(user_id != null){
            sql.append(" AND ua.user_id = "+user_id);
        }
        sql.append(" GROUP BY ua.id ");
        sql.append(" ORDER BY ua.created DESC ");
        return DAO.find(sql.toString());
    }

    public Page<UserAddress> paginate(int pageNumber, int pageSize, BigInteger user_id, String orderBy) {
        String select = " SELECT ua.* ";
        StringBuilder fromBuilder = new StringBuilder(" FROM useraddress ua ");
        fromBuilder.append(" WHERE 1=1 ");
        if(user_id != null){
            fromBuilder.append(" AND ua.user_id = "+user_id);
        }
        fromBuilder.append(" GROUP BY ua.id ");
        if(StringUtils.isNotBlank(orderBy)){
            fromBuilder.append(" ORDER BY ua. "+"'"+orderBy+"'");
        }else{
            fromBuilder.append(" ORDER BY ua.created DESC ");
        }
        return DAO.paginate(pageNumber, pageSize, select, fromBuilder.toString());
    }

}
