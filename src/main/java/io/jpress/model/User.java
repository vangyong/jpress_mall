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
package io.jpress.model;

import io.jpress.model.base.BaseUser;
import io.jpress.model.core.Table;
import io.jpress.model.query.ShoppingCartQuery;
import io.jpress.model.query.UserQuery;

import java.math.BigInteger;
import java.util.Date;

@Table(tableName = "user", primaryKey = "id")
public class User extends BaseUser<User> {
	private static final long serialVersionUID = 1L;

	public static final String ROLE_ADMINISTRATOR = "administrator";//超级管理员
	public static final String ROLE_OPERATOR = "operator";//运营人员
	public static final String ROLE_VISITOR = "visitor";//游客

	public static final String FLAG_ADMIN = "admin";//后台用户
	public static final String FLAG_FRONT = "front";//前端用户

	public static final String STATUS_NORMAL = "normal";
	public static final String STATUS_FROZEN = "frozen";

	public boolean isAdministrator() {
		return ROLE_ADMINISTRATOR.equals(getRole());
	}

	public boolean isOperator() {
		return ROLE_OPERATOR.equals(getRole());
	}

	public boolean isVisitor() {
		return ROLE_VISITOR.equals(getRole());
	}

	public boolean isFrozen() {
		return STATUS_FROZEN.equals(getStatus());
	}

	@Override
	public boolean save() {
		if (getCreated() == null) {
			setCreated(new Date());
		}
		return super.save();
	}

	@Override
	public boolean update() {
		removeCache();
		return super.update();
	}

	@Override
	public boolean saveOrUpdate() {
		removeCache();
		return super.saveOrUpdate();
	}

	@Override
	public boolean delete() {
		removeCache();
		return super.delete();
	}

	private void removeCache(){
		removeCache(getId());
		removeCache(getFlag()+":"+getMobile());
		removeCache(getFlag()+":"+getUsername());
		removeCache(getFlag()+":"+getEmail());
	}

	public String getUrl() {
		return "/user/" + getId();
	}

	//购物车数量
	public String getShoppingCartCount(){
		BigInteger id=getId();
		if(id==null){
			return null;
		}
		User user= UserQuery.me().findById(id);
		if(user!=null){
			long count=ShoppingCartQuery.me().findcount(user.getId());
			return String.valueOf(count);
		}else{
			return null;
		}
	}

}
