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

}
