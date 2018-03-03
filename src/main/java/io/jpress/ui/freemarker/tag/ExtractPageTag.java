package io.jpress.ui.freemarker.tag;

import com.jfinal.core.JFinal;
import com.jfinal.plugin.activerecord.Page;
import io.jpress.core.render.freemarker.BasePaginateTag;
import io.jpress.core.render.freemarker.JTag;
import io.jpress.model.Extract;
import io.jpress.model.query.ExtractQuery;
import io.jpress.utils.StringUtils;
import javax.servlet.http.HttpServletRequest;
import java.math.BigInteger;

/**
 * @Description:用户提现分页
 * @author wangyong
 * @date 2018-2-26 9:09
 */
public class ExtractPageTag extends JTag{

    public static final String TAG_NAME = "jp.extractPage";

    int pageNumber;
    BigInteger userId;
    String orderBy;
    HttpServletRequest request;

    public ExtractPageTag(HttpServletRequest request, int pageNumber, BigInteger userId, String orderBy) {
        this.request=request;
        this.pageNumber=pageNumber;
        this.userId=userId;
        this.orderBy=orderBy;
    }

    @Override
    public void onRender() {
        int pagesize=getParamToInt("pageSize", 10);
        orderBy=StringUtils.isBlank(orderBy) ? getParam("orderBy") : orderBy;
        Page<Extract> page=ExtractQuery.me().paginate(pageNumber, pagesize, userId, orderBy);
        setVariable("page", page);
        setVariable("accountExtracts", page.getList());

        ExtractPaginateTag pagination=new ExtractPaginateTag(request, page);
        setVariable("pagination", pagination);

        renderBody();
    }

    public static class ExtractPaginateTag extends BasePaginateTag {
        final HttpServletRequest request;

        public ExtractPaginateTag(HttpServletRequest request, Page<Extract> page) {
            super(page);
            this.request=request;
        }

        @Override
        protected String getUrl(int pageNumber) {
            String url=JFinal.me().getContextPath()+"/user/accountExtract";
            url+="?pageNumber="+pageNumber;
            if (StringUtils.isNotBlank(getAnchor())) {
                url+="#"+getAnchor();
            }
            return url;
        }

    }

}
