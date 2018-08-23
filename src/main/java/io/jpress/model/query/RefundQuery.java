package io.jpress.model.query;

import java.math.BigInteger;
import java.util.LinkedList;

import io.jpress.model.Refund;

/**
 * <b>Description:</b>
 * <br><b>ClassName:</b> RefundQuery
 * <br><b>Date:</b> 2018年8月20日 下午4:38:35
 * <br>@author <b>jianb.jiang</b>
 */
public class RefundQuery extends JBaseQuery {
	public static final Refund DAO = new Refund();
	private static final RefundQuery QUERY = new RefundQuery();

	public static RefundQuery me() {
		return QUERY;
	}

	public int deleteByTransactionId(BigInteger transaction_id) {
        return DAO.doDelete("transaction_id = ?", transaction_id);
    }
	
	public Refund findByOrderNo(String orderNo){
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT t.* ");
        sql.append(" FROM refund t ");
        sql.append(" WHERE 1=1 ");
        sql.append(" AND t.order_no = ?");
        sql.append(" ORDER BY t.modified_time DESC ");

        LinkedList<Object> params = new LinkedList<Object>();
        params.add(orderNo);
        return DAO.findFirst(sql.toString(),params.toArray());
    }
    
}
