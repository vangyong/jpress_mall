package io.jpress.ui.freemarker.tag;

import com.jfinal.core.JFinal;
import com.jfinal.plugin.activerecord.Page;
import io.jpress.core.render.freemarker.BasePaginateTag;
import io.jpress.core.render.freemarker.JTag;
import io.jpress.model.UserAddress;
import io.jpress.model.query.UserAddressQuery;
import io.jpress.utils.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.math.BigInteger;

/**
 * @author heguoliang
 * @Description: TODO()
 * @date 2017-6-22 9:09
 */
public class UserAddressPageTag extends JTag{

    public static final String TAG_NAME = "jp.userAddressPage";

    int pageNumber;
    BigInteger userId;
    String orderBy;
    HttpServletRequest request;

    public UserAddressPageTag(HttpServletRequest request, int pageNumber, BigInteger userId, String orderBy) {
        this.request=request;
        this.pageNumber=pageNumber;
        this.userId=userId;
        this.orderBy=orderBy;
    }

    @Override
    public void onRender() {
        int pagesize=getParamToInt("pageSize", 10);
        orderBy=StringUtils.isBlank(orderBy) ? getParam("orderBy") : orderBy;
        Page<UserAddress> page=UserAddressQuery.me().paginate(pageNumber, pagesize, userId, orderBy);
        setVariable("page", page);
        setVariable("userAddresss", page.getList());

        UserAddressPaginateTag pagination=new UserAddressPaginateTag(request, page);
        setVariable("pagination", pagination);

        renderBody();
    }

    public static class UserAddressPaginateTag extends BasePaginateTag {
        final HttpServletRequest request;

        public UserAddressPaginateTag(HttpServletRequest request, Page<UserAddress> page) {
            super(page);
            this.request=request;
        }

        @Override
        protected String getUrl(int pageNumber) {
            String url=JFinal.me().getContextPath()+"/user/userAddress";
            url+="?pageNumber="+pageNumber;
            if (StringUtils.isNotBlank(getAnchor())) {
                url+="#"+getAnchor();
            }
            return url;
        }

    }

}
