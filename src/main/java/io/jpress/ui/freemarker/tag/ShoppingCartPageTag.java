package io.jpress.ui.freemarker.tag;

import com.jfinal.core.JFinal;
import com.jfinal.plugin.activerecord.Page;
import io.jpress.core.render.freemarker.BasePaginateTag;
import io.jpress.core.render.freemarker.JTag;
import io.jpress.model.Content;
import io.jpress.model.ShoppingCart;
import io.jpress.model.query.ShoppingCartQuery;
import io.jpress.utils.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.math.BigInteger;

/**
 * @author heguoliang
 * @Description: TODO()
 * @date 2017-6-22 9:09
 */
public class ShoppingCartPageTag extends JTag{

    public static final String TAG_NAME = "jp.shoppingCartPage";

    int pageNumber;
    String ids;
    BigInteger userId;
    String orderBy;
    HttpServletRequest request;

    public ShoppingCartPageTag(HttpServletRequest request, int pageNumber, String ids, BigInteger userId, String orderBy) {
        this.request=request;
        this.pageNumber=pageNumber;
        this.ids=ids;
        this.userId=userId;
        this.orderBy=orderBy;
    }

    @Override
    public void onRender() {
        int pagesize=getParamToInt("pageSize", 10);
        orderBy=StringUtils.isBlank(orderBy) ? getParam("orderBy") : orderBy;
        String status=getParam("status", Content.STATUS_NORMAL);
        Page<ShoppingCart> page=ShoppingCartQuery.me().paginate(pageNumber, pagesize, ids, userId, orderBy, status);
        setVariable("page", page);
        setVariable("shoppingCarts", page.getList());

        ShoppingCartPaginateTag pagination=new ShoppingCartPaginateTag(request, page);
        setVariable("pagination", pagination);

        renderBody();
    }

    public static class ShoppingCartPaginateTag extends BasePaginateTag {
        final HttpServletRequest request;

        public ShoppingCartPaginateTag(HttpServletRequest request, Page<ShoppingCart> page) {
            super(page);
            this.request=request;
        }

        @Override
        protected String getUrl(int pageNumber) {
            String url=JFinal.me().getContextPath()+"/user/shoppingCart";
            url+="?pageNumber="+pageNumber;
            if (StringUtils.isNotBlank(getAnchor())) {
                url+="#"+getAnchor();
            }
            return url;
        }

    }

}
