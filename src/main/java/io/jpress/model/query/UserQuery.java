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

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.ehcache.IDataLoader;
import io.jpress.model.Metadata;
import io.jpress.model.User;
import io.jpress.template.TemplateManager;
import io.jpress.template.TplModule;
import io.jpress.utils.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

public class UserQuery extends JBaseQuery {
	public static final User DAO = new User();
	private static final UserQuery QUERY = new UserQuery();

	public static UserQuery me() {
		return QUERY;
	}

	public List<User> findList(int page, int pagesize, String gender, String role, String status, String orderBy) {
		StringBuilder sqlBuilder = new StringBuilder("select * from user u ");
		LinkedList<Object> params = new LinkedList<Object>();

		boolean needWhere = true;
		needWhere = appendIfNotEmpty(sqlBuilder, "u.gender", gender, params, needWhere);
		needWhere = appendIfNotEmpty(sqlBuilder, "u.role", role, params, needWhere);
		needWhere = appendIfNotEmpty(sqlBuilder, "u.status", status, params, needWhere);

		buildOrderBy(orderBy, sqlBuilder);

		sqlBuilder.append(" LIMIT ?, ?");
		params.add(page - 1);
		params.add(pagesize);

		if (params.isEmpty()) {
			return DAO.find(sqlBuilder.toString());
		} else {
			return DAO.find(sqlBuilder.toString(), params.toArray());
		}

	}

	public User findFirstFromMetadata(String key, Object value) {
//		return DAO.findFirstFromMetadata(key, value);
		Metadata md = MetaDataQuery.me().findFirstByTypeAndValue(User.METADATA_TYPE, key, value);
		if (md != null) {
			BigInteger id = md.getObjectId();
			return findById(id);
		}
		return null;
	}

	public Page<User> paginate(int pageNumber, int pageSize ,String keyword, String flag, String orderby) {
		String select = "select u.*,p.username as pname ";
		StringBuilder fromBuilder = new StringBuilder(" from user u left join user p on u.pid=p.id ");
		fromBuilder.append(" where 1=1 ");
		if(StringUtils.isNotBlank(keyword)){
            fromBuilder.append(" AND ( ");
            fromBuilder.append(" u.username like " +"%'" + keyword + "'%");
            fromBuilder.append(" OR u.realname like " +"%'" + keyword + "'%");
            fromBuilder.append(" ) ");
        }
		if(StringUtils.isNotBlank(flag)){
			fromBuilder.append(" and u.flag = "+"'"+flag+"'");
		}
		buildOrderBy(orderby, fromBuilder);
		return DAO.paginate(pageNumber, pageSize, select, fromBuilder.toString());
	}

	public long findCount() {
		return DAO.doFindCount();
	}

	public long findCount(String flag) {
		return DAO.doFindCount(" flag = ? ", flag);
	}

	public long findAdminCount(String role, String flag) {
		return DAO.doFindCount(" role = ? and flag = ? ", role, flag);
	}

	public long findAdminCount() {
		return DAO.doFindCount(" role = ? ", "administrator");
	}

	public User findById(final BigInteger userId) {
		return DAO.getCache(userId, new IDataLoader() {
			@Override
			public Object load() {
				return DAO.findById(userId);
			}
		});
	}

    public User findByOpenId(final String openId) {
        return DAO.getCache(openId, new IDataLoader() {
            @Override
            public Object load() {
                return DAO.findFirst("select * from jp_user where openid = ?" , openId);
            }
        });
    }

	public User findUserByEmailAndFlag(final String email, final String flag) {
		return DAO.getCache(flag+":"+email, new IDataLoader() {
			@Override
			public Object load() {
				return DAO.doFindFirst(" email = ? and flag = ? ", email, flag);
			}
		});
	}

	public User findUserByUsernameAndFlag(final String username, final String flag) {
		return DAO.getCache(flag+":"+username, new IDataLoader() {
			@Override
			public Object load() {
				return DAO.doFindFirst(" username = ? and flag = ? ", username, flag);
			}
		});
	}

	public User findUserByMobileAndFlag(final String mobile, final String flag) {
		return DAO.getCache(flag+":"+mobile, new IDataLoader() {
			@Override
			public Object load() {
				return DAO.doFindFirst(" mobile = ? and flag = ? ", mobile, flag);
			}
		});
	}

	public boolean updateContentCount(User user) {
		long count = 0;
		List<TplModule> modules = TemplateManager.me().currentTemplateModules();
		if (modules != null && !modules.isEmpty()) {
			for (TplModule m : modules) {
				long moduleCount = ContentQuery.me().findCountInNormalByModuleAndUserId(m.getName(), user.getId());
				count += moduleCount;
			}
		}

		user.setContentCount(count);
		return user.update();
	}

	public boolean updateCommentCount(User user) {
		long count = CommentQuery.me().findCountByUserIdInNormal(user.getId());
		user.setCommentCount(count);
		return user.update();
	}

	protected static void buildOrderBy(String orderBy, StringBuilder fromBuilder) {
		if ("content_count".equals(orderBy)) {
			fromBuilder.append(" ORDER BY u.content_count DESC");
		}

		else if ("comment_count".equals(orderBy)) {
			fromBuilder.append(" ORDER BY u.comment_count DESC");
		}

		else if ("username".equals(orderBy)) {
			fromBuilder.append(" ORDER BY u.username DESC");
		}

		else if ("nickname".equals(orderBy)) {
			fromBuilder.append(" ORDER BY u.nickname DESC");
		}

		else if ("amount".equals(orderBy)) {
			fromBuilder.append(" ORDER BY u.amount DESC");
		}

		else if ("logged".equals(orderBy)) {
			fromBuilder.append(" ORDER BY u.logged DESC");
		}

		else if ("activated".equals(orderBy)) {
			fromBuilder.append(" ORDER BY u.activated DESC");
		}

		else {
			fromBuilder.append(" ORDER BY u.created DESC");
		}
	}

	/**
	 * <b>Description.:查找用户的余额明细</b><br>
	 * <b>Author:jianb.jiang</b>
	 * <br><b>Date:</b> 2018年3月6日 下午9:56:14
	 * @param page
	 * @param pagesize
	 * @param gender
	 * @param role
	 * @param status
	 * @param orderBy
	 * @return
	 */
    public List<Record> findAmountList(int page, int pagesize, BigInteger userId) {
        StringBuilder sqlBuilder = new StringBuilder("select * from ( " +
            "select  " +
            " case when b.bonus_type=1 then '个人直接推广奖励' " +
            "    when b.bonus_type=2 then '个人间接推广奖励' " +
            "    when b.bonus_type=3 then '团队消费达标奖励' " +
            "    when b.bonus_type=4 then '团队管理达标奖励' " +
            "    when b.bonus_type=5 then '购买商品付款' " +
            "    when b.bonus_type=6 then '订单退款扣减' END as amountName, " +
            "    b.bonus_time as amountTime, " +
            "    b.amount as amount " +
            " from jp_bonus b  " +
            "where b.user_id = ? " +
            "union  " +
            "select  " +
            "    '提现支付' as amountName, " +
            "    ep.payed_time as amountTime, " +
            "    -ep.pay_money as amount " +
            "from jp_extract e,jp_extract_pay ep " +
            "where e.id = ep.extract_id " +
            "and e.`status` = 3 and e.user_id = ? " +
            ") a  " +
            "order by a.amountTime desc");
        LinkedList<Object> params = new LinkedList<Object>();

        sqlBuilder.append(" LIMIT ?, ?");
        params.add(userId);
        params.add(userId);
        params.add(page - 1);
        params.add(pagesize);

        if (params.isEmpty()) {
            return Db.find(sqlBuilder.toString());
        } else {
            return Db.find(sqlBuilder.toString(), params.toArray());
        }

    }

    /**
     * <b>Description.:查询当前用户的可提现金额：用户可提现金额 = 用户入账15天后的奖金收入 - 用户的奖金扣除(消费和退款扣除) - 已经提现金额</b><br>
     * <b>Author:jianb.jiang</b>
     * <br><b>Date:</b> 2018年6月27日 下午2:17:03
     * @return
     */
    public Record getExtractAvailableAmount(BigInteger userId) {
        StringBuilder sqlBuilder = new StringBuilder(
                "SELECT "+
                "( SELECT "+
                "       IFNULL(SUM(b.amount), 0) AS all15 "+
                "   FROM jp_bonus b "+
                "   WHERE b.user_id = ? AND (b.bonus_type != 5 AND b.bonus_type != 6"+
                "   AND DATE(b.bonus_time) <= DATE_SUB(CURDATE(), INTERVAL 15 DAY) OR b.bonus_type in (5,6))"+
                ") - ( "+
                "   SELECT "+
                "       IFNULL(SUM(ep.pay_money), 0) AS extractPayed "+
                "   FROM "+
                "       jp_extract e, "+
                "       jp_extract_pay ep "+
                "   WHERE "+
                "       e.id = ep.extract_id "+
                "   AND e.user_id = ? "+
                "   AND e.`status` = 3 "+
                ") AS extractAvailableAmount,"+ 
                "(SELECT IFNULL(u.amount, 0) FROM jp_user u where u.id = ?) AS userAmount FROM DUAL");
            LinkedList<Object> params = new LinkedList<Object>();

            params.add(userId);
            params.add(userId);
            params.add(userId);
            
            if (params.isEmpty()) {
                return Db.findFirst(sqlBuilder.toString());
            } else {
                return Db.findFirst(sqlBuilder.toString(), params.toArray());
            }
    }

    
}
