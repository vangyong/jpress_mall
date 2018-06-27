package io.jpress.ui.freemarker.tag;

import com.jfinal.plugin.activerecord.Record;

import io.jpress.core.render.freemarker.JTag;
import io.jpress.model.query.UserQuery;
import io.jpress.utils.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.math.BigInteger;
import java.util.List;

/**
 * @author heguoliang
 * @Description: TODO()
 * @date 2017-6-22 9:09
 */
public class UserAmountPageTag extends JTag{

    public static final String TAG_NAME = "jp.userAmountPage";

    int pageNumber;
    BigInteger userId;
    String orderBy;
    HttpServletRequest request;

    public UserAmountPageTag(HttpServletRequest request, int pageNumber, BigInteger userId, String orderBy) {
        this.request=request;
        this.pageNumber=pageNumber;
        this.userId=userId;
        this.orderBy=orderBy;
    }

    @Override
    public void onRender() {
        int pagesize=getParamToInt("pageSize", Integer.MAX_VALUE);//提现记录默认一页查询所有记录,jiangjb,20180627
        orderBy=StringUtils.isBlank(orderBy) ? getParam("orderBy") : orderBy;
        List<Record> accountDetailList = UserQuery.me().findAmountList(pageNumber, pagesize, userId);
        setVariable("accountDetailList", accountDetailList);

        renderBody();
    }

}
