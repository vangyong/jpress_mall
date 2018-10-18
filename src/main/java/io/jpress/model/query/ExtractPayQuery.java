package io.jpress.model.query;

import java.math.BigInteger;

import io.jpress.model.Extract;
import io.jpress.model.ExtractPay;

/**
 * @author wangyong
 * @Description: 提现支付
 * @date 2018-10-13 21:38
 */
public class ExtractPayQuery extends JBaseQuery{

    protected static final ExtractPay DAO = new ExtractPay();

    private static final ExtractPayQuery QUERY = new ExtractPayQuery();

    public static ExtractPayQuery me() {
        return QUERY;
    }

    public ExtractPay findById(final BigInteger id) {
    	return DAO.findById(id);
    }

    public int deleteById(BigInteger id) {
        return DAO.doDelete("id = ?", id);
    }
    
    public int deleteByIds(String ids) {
        return DAO.doDelete("id in ("+ids+")");
    }

    public ExtractPay findByExtractId(BigInteger extractId){
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT t.* ");
        sql.append(" FROM extractpay t ");
        sql.append(" WHERE 1=1 ");
        sql.append(" AND t.extract_id = "+"'"+extractId+"'");
        sql.append(" GROUP BY t.id ");
        sql.append(" ORDER BY t.payed_time DESC ");
        return DAO.findFirst(sql.toString());
    }

}
