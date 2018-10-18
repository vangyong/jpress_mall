package io.jpress.model.query;

import java.math.BigInteger;
import java.util.List;

import io.jpress.model.Bonus;

/**
 * <b>Description:</b>
 * <br><b>ClassName:</b> CouponUsedQuery
 * <br><b>Date:</b> 2018年2月8日 下午9:11:40
 * <br>@author <b>jianb.jiang</b>
 */
public class BonusQuery extends JBaseQuery {
	public static final Bonus DAO = new Bonus();
	private static final BonusQuery QUERY = new BonusQuery();

	public static BonusQuery me() {
		return QUERY;
	}

	public int deleteByTransactionId(BigInteger transaction_id) {
        return DAO.doDelete("transaction_id = ?", transaction_id);
    }
	
	public Bonus findFirstByTransactionId(BigInteger transaction_id){
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT t.* ");
        sql.append(" FROM bonus t ");
        sql.append(" WHERE 1=1 ");
        sql.append(" AND t.transaction_id = "+transaction_id);
        sql.append(" ORDER BY t.bonus_time DESC ");
        return DAO.findFirst(sql.toString());
    }
    
    public List<Bonus> findByTransactionId(BigInteger transaction_id){
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT t.* ");
        sql.append(" FROM bonus t ");
        sql.append(" WHERE 1=1 ");
        sql.append(" AND t.transaction_id = "+transaction_id);
        sql.append(" ORDER BY t.bonus_time DESC ");
        return DAO.find(sql.toString());
    }
    
    public Bonus findByUserId(BigInteger userId){
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT t.* ");
        sql.append(" FROM bonus t ");
        sql.append(" WHERE 1=1 ");
        sql.append(" AND t.user_id = "+userId);
        sql.append(" ORDER BY t.bonus_time DESC ");
        return DAO.findFirst(sql.toString());
    }
    
}
