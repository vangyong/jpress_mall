package io.jpress.model.query;

import io.jpress.model.CouponUsed;

/**
 * <b>Description:</b>
 * <br><b>ClassName:</b> CouponUsedQuery
 * <br><b>Date:</b> 2018年2月8日 下午9:11:40
 * <br>@author <b>jianb.jiang</b>
 */
public class CouponUsedQuery extends JBaseQuery {
	public static final CouponUsed DAO = new CouponUsed();
	private static final CouponUsedQuery QUERY = new CouponUsedQuery();

	public static CouponUsedQuery me() {
		return QUERY;
	}

}
