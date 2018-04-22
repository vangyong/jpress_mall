package io.jpress.ui.freemarker.tag;

import com.jfinal.core.JFinal;
import com.jfinal.plugin.activerecord.Page;
import io.jpress.core.render.freemarker.BasePaginateTag;
import io.jpress.core.render.freemarker.JTag;
import io.jpress.model.Transaction;
import io.jpress.model.query.TransactionQuery;
import io.jpress.utils.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.math.BigInteger;

/**
 * @author heguoliang
 * @Description: TODO()
 * @date 2017-6-27 15:44
 */
public class TransactionPageTag extends JTag {

    public static final String TAG_NAME = "jp.transactionPage";

    int pageNumber;
    BigInteger userId;
    String status;
    String orderBy;
    HttpServletRequest request;

    public TransactionPageTag(HttpServletRequest request, int pageNumber, String status, BigInteger userId, String orderBy) {
        this.request=request;
        this.pageNumber=pageNumber;
        this.status=status;
        this.userId=userId;
        this.orderBy=orderBy;
    }

    @Override
    public void onRender() {
        int pagesize=getParamToInt("pageSize", 10);
        orderBy= StringUtils.isBlank(orderBy) ? getParam("orderBy") : orderBy;
        Page<Transaction> page=TransactionQuery.me().paginate(pageNumber, pagesize, userId, status, orderBy);
        setVariable("page", page);
        setVariable("transactions", page.getList());

        TransactionPaginateTag pagination=new TransactionPaginateTag(request, page);
        setVariable("pagination", pagination);

        renderBody();
    }

    public static class TransactionPaginateTag extends BasePaginateTag {
        final HttpServletRequest request;

        public TransactionPaginateTag(HttpServletRequest request, Page<Transaction> page) {
            super(page);
            this.request=request;
        }

        @Override
        protected String getUrl(int pageNumber) {
            String url= JFinal.me().getContextPath()+"/user/userTransaction";
            url+="?pageNumber="+pageNumber;
            if (StringUtils.isNotBlank(getAnchor())) {
                url+="#"+getAnchor();
            }
            return url;
        }

    }

}
