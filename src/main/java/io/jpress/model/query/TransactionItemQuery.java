package io.jpress.model.query;

import io.jpress.model.TransactionItem;

import java.math.BigInteger;
import java.util.List;

/**
 * @author heguoliang
 * @Description: TODO()
 * @date 2017-6-23 14:38
 */
public class TransactionItemQuery extends JBaseQuery{

    protected static final TransactionItem DAO = new TransactionItem();

    private static final TransactionItemQuery QUERY = new TransactionItemQuery();

    public static TransactionItemQuery me() {
        return QUERY;
    }

    public int deleteByTransactionId(BigInteger transaction_id) {
        return DAO.doDelete("transaction_id = ?", transaction_id);
    }

    public List<TransactionItem> findList(BigInteger transaction_id){
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT ti.*,c.title,c.thumbnail,CONCAT(s.title,'：',sv.value) AS value ");
        sql.append(" FROM transactionitem ti ");
        sql.append(" LEFT JOIN content c ON ti.content_id=c.id ");
        sql.append(" LEFT JOIN specvalue sv ON sv.id=ti.spec_value_id ");
        sql.append(" LEFT JOIN spec s ON sv.spec_id=s.id ");
        sql.append(" WHERE 1=1 ");
        if(transaction_id != null){
            sql.append(" AND ti.transaction_id = "+transaction_id);
        }
        sql.append(" GROUP BY ti.id ");
        sql.append(" ORDER BY ti.created DESC ");
        return DAO.find(sql.toString());
    }

    public List<TransactionItem> findListOfNotComment(BigInteger transaction_id){
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT ti.* ");
        sql.append(" FROM transactionitem ti ");
        sql.append(" WHERE 1=1 ");
        if(transaction_id != null){
            sql.append(" AND ti.transaction_id = "+transaction_id);
        }
        sql.append(" AND NOT EXISTS( SELECT c.content_id FROM comment c WHERE c.content_id=ti.content_id ) ");
        sql.append(" GROUP BY ti.id ");
        sql.append(" ORDER BY ti.created DESC ");
        return DAO.find(sql.toString());
    }

}
