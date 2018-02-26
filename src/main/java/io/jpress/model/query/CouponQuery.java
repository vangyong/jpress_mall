/**
 * Copyright (c) 2015-2016, Michael Yang 杨福海 (fuhai999@gmail.com).
 *
 * Licensed under the GNU Lesser General Public License (LGPL) ,Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.jpress.model.query;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

import io.jpress.model.Coupon;

public class CouponQuery extends JBaseQuery {
	public static final Coupon DAO = new Coupon();
	private static final CouponQuery QUERY = new CouponQuery();

	public static CouponQuery me() {
		return QUERY;
	}

	public List<Coupon> findListByUserId(int page, int pagesize, BigInteger userId) {
        StringBuilder sqlBuilder = new StringBuilder("select c.*,cu.id as couponUsedId from coupon c,couponused cu "
                + "where c.id = cu.coupon_id and cu.user_id = ? and cu.used = 0 and c.invalid = 0 "
                + "and DATE(now()) <= c.last_date");
        LinkedList<Object> params = new LinkedList<Object>();


        sqlBuilder.append(" LIMIT ?, ?");
        params.add(userId);
        params.add(page - 1);
        params.add(pagesize);

        if (params.isEmpty()) {
            return DAO.find(sqlBuilder.toString());
        } else {
            return DAO.find(sqlBuilder.toString(), params.toArray());
        }

    }

	public Coupon findByCode(final String code) {
	    return DAO.findFirst("select * from jp_coupon where code = ?" , code);
    }
	
}
