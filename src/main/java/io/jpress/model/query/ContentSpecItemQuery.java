package io.jpress.model.query;

import io.jpress.model.ContentSpecItem;

import java.math.BigInteger;
import java.util.List;

/**
 * @author heguoliang
 * @Description: TODO()
 * @date 2017-6-14 15:19
 */
public class ContentSpecItemQuery extends JBaseQuery{

    protected static final ContentSpecItem DAO = new ContentSpecItem();

    private static final ContentSpecItemQuery QUERY = new ContentSpecItemQuery();

    public static ContentSpecItemQuery me() {
        return QUERY;
    }

    public int deleteByContentId(BigInteger content_id) {
        return DAO.doDelete("content_id = ?", content_id);
    }

    public int deleteBySpecId(BigInteger spec_id) {
        return DAO.doDelete("spec_id = ?", spec_id);
    }

    public long findcount(BigInteger content_id, BigInteger spec_id){
        return DAO.doFindCount(" content_id = ? and spec_id = ? ", content_id, spec_id);
    }

    public long findcount(BigInteger content_id, BigInteger spec_id, BigInteger spec_value_id){
        return DAO.doFindCount(" content_id = ? and spec_id = ? and spec_value_id = ? ", content_id, spec_id, spec_value_id);
    }

    public List<ContentSpecItem> findList(BigInteger content_id, BigInteger spec_id){
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT csi.* ");
        sql.append(" FROM contentspecitem csi ");
        sql.append(" WHERE 1=1 ");
        if(content_id != null){
            sql.append(" AND csi.content_id = "+content_id);
        }
        if(spec_id != null){
            sql.append(" AND csi.spec_id = "+spec_id);
        }
        sql.append(" GROUP BY csi.id ");
        sql.append(" ORDER BY csi.created DESC ");
        return DAO.find(sql.toString());
    }

    public ContentSpecItem findByContentIdAndSpecValueId(BigInteger content_id, BigInteger spec_value_id) {
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT csi.* ");
        sql.append(" FROM contentspecitem csi ");
        sql.append(" WHERE 1=1 ");
        sql.append(" AND csi.content_id = "+content_id);
        sql.append(" AND csi.spec_value_id = "+spec_value_id);
        sql.append(" GROUP BY csi.id ");
        sql.append(" ORDER BY csi.created DESC ");
        return DAO.findFirst(sql.toString());
    }

    /**
     * <b>Description.:检测是否本次下单份数超过了sku的每人限购份数</b><br>
     * <b>Author:jianb.jiang</b>
     * <br><b>Date:</b> 2018年8月1日 下午4:39:25
     * @param userId
     * @param content_id
     * @param spec_value_id
     * @param num
     * @return
     */
    public boolean checkLimitPerUser(BigInteger userId, BigInteger content_id, BigInteger spec_value_id, Integer num) {
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT csi.* ");
        sql.append(" FROM contentspecitem csi ");
        sql.append(" WHERE 1=1 ");
        sql.append(" AND csi.content_id = "+content_id);
        sql.append(" AND csi.spec_value_id = "+spec_value_id);
        sql.append(" AND ifnull(csi.limit_per_user - ?,0) >= (select ifnull(sum(ifnull(ti.quantity,0)),0) from jp_transaction t,jp_transaction_item ti where ti.transaction_id = t.id and t.user_id = ? and ti.content_id = ? and ti.spec_value_id = ?)");
        sql.append(" GROUP BY csi.id ");
        sql.append(" ORDER BY csi.created DESC ");
        ContentSpecItem r = DAO.findFirst(sql.toString(),num,userId,content_id,spec_value_id);
        return r != null;
    }

}
