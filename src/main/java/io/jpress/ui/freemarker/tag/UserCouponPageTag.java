package io.jpress.ui.freemarker.tag;

import io.jpress.core.render.freemarker.BasePaginateTag;
import io.jpress.core.render.freemarker.JTag;
import io.jpress.model.Coupon;
import io.jpress.model.query.CouponQuery;
import io.jpress.utils.StringUtils;

import javax.servlet.http.HttpServletRequest;

import com.jfinal.core.JFinal;
import com.jfinal.plugin.activerecord.Page;

import java.math.BigInteger;

/**
 * <b>Description:</b>
 * <br><b>ClassName:</b> UserCouponPageTag
 * <br><b>Date:</b> 2018年8月30日 下午2:53:20
 * <br>@author <b>jianb.jiang</b>
 */
public class UserCouponPageTag extends JTag{

    public static final String TAG_NAME = "jp.userCouponPage";

    int pageNumber;
    BigInteger userId;
    String orderBy;
    HttpServletRequest request;

    public UserCouponPageTag(HttpServletRequest request, int pageNumber, BigInteger userId, String orderBy) {
        this.request=request;
        this.pageNumber=pageNumber;
        this.userId=userId;
        this.orderBy=orderBy;
    }

    @Override
    public void onRender() {
        int pagesize=getParamToInt("pageSize", 10);
        orderBy=StringUtils.isBlank(orderBy) ? getParam("orderBy") : orderBy;
        Page<Coupon> page = CouponQuery.me().findAllByUserId(pageNumber, pagesize, userId);
        setVariable("page", page);
        setVariable("userCouponList", page.getList());

        UserCouponPaginateTag pagination=new UserCouponPaginateTag(request, page);
        setVariable("pagination", pagination);
        
        renderBody();
    }

    public static class UserCouponPaginateTag extends BasePaginateTag {
        final HttpServletRequest request;

        public UserCouponPaginateTag(HttpServletRequest request, Page<Coupon> page) {
            super(page);
            this.request=request;
        }

        @Override
        protected String getUrl(int pageNumber) {
            String url= JFinal.me().getContextPath()+"/user/userCoupon";
            url+="?pageNumber="+pageNumber;
            if (StringUtils.isNotBlank(getAnchor())) {
                url+="#"+getAnchor();
            }
            return url;
        }

    }

}
