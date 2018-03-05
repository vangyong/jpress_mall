package io.jpress.model;

import io.jpress.model.base.BaseCoupon;
import io.jpress.model.core.Table;

/**
 * Generated by JPress.
 */
@Table(tableName="coupon",primaryKey="id")
public class Coupon extends BaseCoupon<Coupon> {

    private static final long serialVersionUID = 8503033780586478777L;
	
    public void setCouponUsedId(java.math.BigInteger couponUsedId) {
        set("couponUsedId", couponUsedId);
    }

    public java.math.BigInteger getCouponUsedId() {
        return get("couponUsedId");
    }
    
    @Override
    public boolean saveOrUpdate() {
        removeCache(getId());
        return super.saveOrUpdate();
    }
}
