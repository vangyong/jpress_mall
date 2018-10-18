package io.jpress.model.query;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.ehcache.IDataLoader;

import io.jpress.model.Transaction;
import io.jpress.utils.StringUtils;
import io.jpress.wechat.utils.CollectionUtil;

/**
 * @author heguoliang
 * @Description: TODO()
 * @date 2017-6-23 14:38
 */
public class TransactionQuery extends JBaseQuery{

    protected static final Transaction DAO = new Transaction();

    private static final TransactionQuery QUERY = new TransactionQuery();
    
    private static DateFormat DATEFORMAT = new java.text.SimpleDateFormat("yyyy-MM-dd");

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
        String select = " SELECT t.*,r.refund_no,r.status as refund_status ";
        StringBuilder fromBuilder = new StringBuilder(" FROM transaction t LEFT JOIN refund r on t.order_no=r.order_no ");
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
        String select = " SELECT t.*,SUM(ti.quantity*ti.price) AS price,SUM(ti.quantity) AS quantity,GROUP_CONCAT(c.thumbnail SEPARATOR ',') AS contentThumbnails,GROUP_CONCAT(c.title SEPARATOR ',') AS contentTitles,r.refund_no,r.status as refund_status ";
        StringBuilder fromBuilder = new StringBuilder(" FROM transaction t LEFT JOIN refund r on t.order_no=r.order_no ");
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
    
    public long findCount(Integer status, Date createDay){
        return DAO.doFindCount("status >= ? and DATE_FORMAT(created,'%Y-%m-%d') = ? ", status, DATEFORMAT.format(createDay));
    }
    
    public long findCount(Integer status){
        return DAO.doFindCount("status >= ? ", status);
    }
    
    //当日订单金额
    public BigDecimal findAmountOfDay(Date createDay){
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT SUM(t.totle_fee) AS totle_fee");
        sql.append(" FROM transaction t ");
        sql.append(" WHERE 1=1  AND DATE_FORMAT(created,'%Y-%m-%d') = ? ");
        sql.append(" AND t.status >= 2 ");
        BigDecimal total=new BigDecimal("0");
        List list = DAO.find(sql.toString(),DATEFORMAT.format(createDay));
        if(CollectionUtil.isNotEmpty(list)) {
           Transaction map =(Transaction) list.get(0);
           if(map.get("totle_fee")!=null) {
        	   total= map.get("totle_fee");
           }
        }
        return total;
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
