package io.jpress.model.query;

import com.jfinal.plugin.ehcache.IDataLoader;
import io.jpress.model.VerifyCode;
import java.math.BigInteger;

/**
 * @author wangyong
 * @Description: 手机验证码
 * @date 2018-2-24 21:38
 */
public class VerifyCodeQuery extends JBaseQuery{

    protected static final VerifyCode DAO = new VerifyCode();

    private static final VerifyCodeQuery QUERY = new VerifyCodeQuery();

    public static VerifyCodeQuery me() {
        return QUERY;
    }

    public VerifyCode findById(final BigInteger id) {
        return DAO.getCache(id, new IDataLoader() {
            @Override
            public Object load() {
                return DAO.findById(id);
            }
        });
    }

    public int deleteById(BigInteger id) {
        return DAO.doDelete("id = ?", id);
    }

    //校验验证码
    public VerifyCode checkVerifyCode(String telephone,String verifyCode){
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT t.* ");
        sql.append(" FROM verifycode t ");
        sql.append(" WHERE 1=1 ");
        sql.append(" AND t.telephone = "+"'"+telephone+"'");
        sql.append(" AND t.code = "+"'"+verifyCode+"'");
        sql.append(" AND t.status = '"+VerifyCode.STATUS_UNVERIFY+"'");
        sql.append(" GROUP BY t.id ");
        sql.append(" ORDER BY t.created_time DESC ");
        return DAO.findFirst(sql.toString());
    }
    
    
    
}
