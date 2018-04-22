package io.jpress.model.query;

import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.ehcache.IDataLoader;
import io.jpress.model.Transaction;
import io.jpress.utils.StringUtils;

import java.math.BigInteger;
import java.util.List;

/**
 * @author heguoliang
 * @Description: TODO()
 * @date 2017-6-23 14:38
 */
public class TransactionQuery extends JBaseQuery{

    protected static final Transaction DAO = new Transaction();

    private static final TransactionQuery QUERY = new TransactionQuery();

    public static TransactionQuery me() {
        return QUERY;
    }

    public Transaction findById(final BigInteger id) {
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

    public Page<Transaction> paginate(int pageNumber, int pageSize, String keyword, String status, String pay_type) {
        String select = " SELECT t.* ";
        StringBuilder fromBuilder = new StringBuilder(" FROM transaction t ");
        fromBuilder.append(" WHERE 1=1 ");
        if(StringUtils.isNotBlank(keyword)){
            fromBuilder.append(" AND ( ");
            fromBuilder.append(" t.order_no = " +"'" + keyword + "'");
            fromBuilder.append(" OR t.trade_no = " +"'" + keyword + "'");
            fromBuilder.append(" ) ");
        }
        if(StringUtils.isNotBlank(status)){
            fromBuilder.append(" AND t.status = "+"'"+status+"'");
        }
        if(StringUtils.isNotBlank(pay_type)){
            fromBuilder.append(" AND t.pay_type = "+"'"+pay_type+"'");
        }
        fromBuilder.append(" GROUP BY t.id ");
        fromBuilder.append(" ORDER BY t.created DESC ");
        return DAO.paginate(pageNumber, pageSize, select, fromBuilder.toString());
    }

    public Page<Transaction> paginate(int pageNumber, int pageSize, BigInteger user_id, String status, String orderBy) {
        String select = " SELECT t.id,t.status,t.created,SUM(ti.quantity*ti.price) AS price,SUM(ti.quantity) AS quantity,GROUP_CONCAT(c.thumbnail SEPARATOR ',') AS contentThumbnails ";
        StringBuilder fromBuilder = new StringBuilder(" FROM transaction t ");
        fromBuilder.append(" LEFT JOIN transactionitem ti ON ti.transaction_id=t.id ");
        fromBuilder.append(" LEFT JOIN content c ON ti.content_id=c.id ");
        fromBuilder.append(" WHERE 1=1 ");
        if(user_id!=null){
            fromBuilder.append(" AND t.user_id = "+user_id);
        }
        if(StringUtils.isNotBlank(status)){
            fromBuilder.append(" AND t.status = "+"'"+status+"'");
        }
        fromBuilder.append(" GROUP BY t.id ");
        if(StringUtils.isNotBlank(orderBy)){
            fromBuilder.append(" ORDER BY t. "+"'"+orderBy+"'");
        }else{
            fromBuilder.append(" ORDER BY t.created DESC ");
        }
        return DAO.paginate(pageNumber, pageSize, select, fromBuilder.toString());
    }

    public Transaction findByOrderNo(String order_no){
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT t.* ");
        sql.append(" FROM transaction t ");
        sql.append(" WHERE 1=1 ");
        sql.append(" AND t.order_no = "+"'"+order_no+"'");
        sql.append(" GROUP BY t.id ");
        sql.append(" ORDER BY t.created DESC ");
        return DAO.findFirst(sql.toString());
    }

    public long findcount(BigInteger user_id, String status){
        return DAO.doFindCount("user_id = ? and status = ?", user_id, status);
    }

    //获取最近一个月的每天的订单金额
    public List getTransactionAmountOfMonth(){
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT SUM(t.totle_fee) AS totle_fee,DATE_FORMAT(t.`created`,'%Y-%m-%d') AS date ");
        sql.append(" FROM transaction t ");
        sql.append(" WHERE 1=1 ");
        sql.append(" AND t.status >= 2 ");
        sql.append(" GROUP BY DATE_FORMAT(t.`created`,'%Y-%m-%d') ");
        sql.append(" ORDER BY t.created DESC ");
        sql.append(" LIMIT 31 ");
        return DAO.find(sql.toString());
    }

}
