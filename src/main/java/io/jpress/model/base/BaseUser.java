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
package io.jpress.model.base;

import com.jfinal.plugin.activerecord.IBean;
import com.jfinal.plugin.ehcache.CacheKit;
import com.jfinal.plugin.ehcache.IDataLoader;
import io.jpress.message.MessageKit;
import io.jpress.model.Metadata;
import io.jpress.model.core.JModel;
import io.jpress.model.query.MetaDataQuery;

import java.math.BigInteger;

/**
 *  Auto generated by JPress, do not modify this file.
 */
@SuppressWarnings("serial")
public abstract class BaseUser<M extends BaseUser<M>> extends JModel<M> implements IBean {

	public static final String CACHE_NAME = "user";
	public static final String METADATA_TYPE = "user";

	public static final String ACTION_ADD = "user:add";
	public static final String ACTION_DELETE = "user:delete";
	public static final String ACTION_UPDATE = "user:update";
	public static final String ACTION_ADDORUPDATE = "user:saveOrUpdate";

	public void removeCache(Object key){
		if(key == null) return;
		CacheKit.remove(CACHE_NAME, key);
	}

	public void putCache(Object key,Object value){
		CacheKit.put(CACHE_NAME, key, value);
	}

	public M getCache(Object key){
		return CacheKit.get(CACHE_NAME, key);
	}

	public M getCache(Object key,IDataLoader dataloader){
		return CacheKit.get(CACHE_NAME, key, dataloader);
	}

	public Metadata createMetadata(){
		Metadata md = new Metadata();
		md.setObjectId(getId());
		md.setObjectType(METADATA_TYPE);
		return md;
	}

	public Metadata createMetadata(String key,String value){
		Metadata md = new Metadata();
		md.setObjectId(getId());
		md.setObjectType(METADATA_TYPE);
		md.setMetaKey(key);
		md.setMetaValue(value);
		return md;
	}

	public boolean saveOrUpdateMetadta(String key,String value){
		Metadata metadata = MetaDataQuery.me().findByTypeAndIdAndKey(METADATA_TYPE, getId(), key);
		if (metadata == null) {
			metadata = createMetadata(key, value);
			return metadata.save();
		}
		metadata.setMetaValue(value);
		return metadata.update();
	}

	public String metadata(String key) {
		Metadata m = MetaDataQuery.me().findByTypeAndIdAndKey(METADATA_TYPE, getId(), key);
		if (m != null) {
			return m.getMetaValue();
		}
		return null;
	}

	@Override
	public boolean equals(Object o) {
		if(o == null){ return false; }
		if(!(o instanceof BaseUser<?>)){return false;}

		BaseUser<?> m = (BaseUser<?>) o;
		if(m.getId() == null){return false;}

		return m.getId().compareTo(this.getId()) == 0;
	}

	@Override
	public boolean save() {
		boolean saved = super.save();
		if (saved) { MessageKit.sendMessage(ACTION_ADD, this); }
		return saved;
	}

	@Override
	public boolean delete() {
		boolean deleted = super.delete();
		if (deleted) { MessageKit.sendMessage(ACTION_DELETE, this); }
		return deleted;
	}

	@Override
	public boolean deleteById(Object idValue) {
		boolean deleted = super.deleteById(idValue);
		if (deleted) { MessageKit.sendMessage(ACTION_DELETE, this); }
		return deleted;
	}

	@Override
	public boolean update() {
		boolean update = super.update();
		if (update) { MessageKit.sendMessage(ACTION_UPDATE, this); }
		return update;
	}

	@Override
	public boolean saveOrUpdate() {
		boolean saveOrUpdate = super.saveOrUpdate();
		if (saveOrUpdate) { MessageKit.sendMessage(ACTION_ADDORUPDATE, this); }
		return saveOrUpdate;
	}

	public void setId(java.math.BigInteger id) {
		set("id", id);
	}

	public java.math.BigInteger getId() {
		Object id = get("id");
		if (id == null)
			return null;

		return id instanceof BigInteger ? (BigInteger)id : new BigInteger(id.toString());
	}

	public void setUsername(java.lang.String username) {
		set("username", username);
	}

	public java.lang.String getUsername() {
		return get("username");
	}

	public void setNickname(java.lang.String nickname) {
		set("nickname", nickname);
	}

	public java.lang.String getNickname() {
		return get("nickname");
	}

	public void setRealname(java.lang.String realname) {
		set("realname", realname);
	}

	public java.lang.String getRealname() {
		return get("realname");
	}

	public void setPassword(java.lang.String password) {
		set("password", password);
	}

	public java.lang.String getPassword() {
		return get("password");
	}

	public void setSalt(java.lang.String salt) {
		set("salt", salt);
	}

	public java.lang.String getSalt() {
		return get("salt");
	}

	public void setEmail(java.lang.String email) {
		set("email", email);
	}

	public java.lang.String getEmail() {
		return get("email");
	}

	public void setEmailStatus(java.lang.String emailStatus) {
		set("email_status", emailStatus);
	}

	public java.lang.String getEmailStatus() {
		return get("email_status");
	}

	public void setMobile(java.lang.String mobile) {
		set("mobile", mobile);
	}

	public java.lang.String getMobile() {
		return get("mobile");
	}

	public void setMobileStatus(java.lang.String mobileStatus) {
		set("mobile_status", mobileStatus);
	}

	public java.lang.String getMobileStatus() {
		return get("mobile_status");
	}

	public void setTelephone(java.lang.String telephone) {
		set("telephone", telephone);
	}

	public java.lang.String getTelephone() {
		return get("telephone");
	}

	public void setAmount(java.math.BigDecimal amount) {
		set("amount", amount);
	}

	public java.math.BigDecimal getAmount() {
		return get("amount");
	}

	public void setGender(java.lang.String gender) {
		set("gender", gender);
	}

	public java.lang.String getGender() {
		return get("gender");
	}

	public void setRole(java.lang.String role) {
		set("role", role);
	}

	public java.lang.String getRole() {
		return get("role");
	}

	public void setSignature(java.lang.String signature) {
		set("signature", signature);
	}

	public java.lang.String getSignature() {
		return get("signature");
	}

	public void setContentCount(java.lang.Long contentCount) {
		set("content_count", contentCount);
	}

	public java.lang.Long getContentCount() {
		return get("content_count");
	}

	public void setCommentCount(java.lang.Long commentCount) {
		set("comment_count", commentCount);
	}

	public java.lang.Long getCommentCount() {
		return get("comment_count");
	}

	public void setQq(java.lang.String qq) {
		set("qq", qq);
	}

	public java.lang.String getQq() {
		return get("qq");
	}

	public void setWechat(java.lang.String wechat) {
		set("wechat", wechat);
	}

	public java.lang.String getWechat() {
		return get("wechat");
	}

	public void setWeibo(java.lang.String weibo) {
		set("weibo", weibo);
	}

	public java.lang.String getWeibo() {
		return get("weibo");
	}

	public void setFacebook(java.lang.String facebook) {
		set("facebook", facebook);
	}

	public java.lang.String getFacebook() {
		return get("facebook");
	}

	public void setLinkedin(java.lang.String linkedin) {
		set("linkedin", linkedin);
	}

	public java.lang.String getLinkedin() {
		return get("linkedin");
	}

	public void setBirthday(java.util.Date birthday) {
		set("birthday", birthday);
	}

	public java.util.Date getBirthday() {
		return get("birthday");
	}

	public void setCompany(java.lang.String company) {
		set("company", company);
	}

	public java.lang.String getCompany() {
		return get("company");
	}

	public void setOccupation(java.lang.String occupation) {
		set("occupation", occupation);
	}

	public java.lang.String getOccupation() {
		return get("occupation");
	}

	public void setAddress(java.lang.String address) {
		set("address", address);
	}

	public java.lang.String getAddress() {
		return get("address");
	}

	public void setZipcode(java.lang.String zipcode) {
		set("zipcode", zipcode);
	}

	public java.lang.String getZipcode() {
		return get("zipcode");
	}

	public void setSite(java.lang.String site) {
		set("site", site);
	}

	public java.lang.String getSite() {
		return get("site");
	}

	public void setGraduateschool(java.lang.String graduateschool) {
		set("graduateschool", graduateschool);
	}

	public java.lang.String getGraduateschool() {
		return get("graduateschool");
	}

	public void setEducation(java.lang.String education) {
		set("education", education);
	}

	public java.lang.String getEducation() {
		return get("education");
	}

	public void setAvatar(java.lang.String avatar) {
		set("avatar", avatar);
	}

	public java.lang.String getAvatar() {
		return get("avatar");
	}

	public void setIdcardtype(java.lang.String idcardtype) {
		set("idcardtype", idcardtype);
	}

	public java.lang.String getIdcardtype() {
		return get("idcardtype");
	}

	public void setIdcard(java.lang.String idcard) {
		set("idcard", idcard);
	}

	public java.lang.String getIdcard() {
		return get("idcard");
	}

	public void setStatus(java.lang.String status) {
		set("status", status);
	}

	public java.lang.String getStatus() {
		return get("status");
	}

	public void setCreated(java.util.Date created) {
		set("created", created);
	}

	public java.util.Date getCreated() {
		return get("created");
	}

	public void setCreateSource(java.lang.String createSource) {
		set("create_source", createSource);
	}

	public java.lang.String getCreateSource() {
		return get("create_source");
	}

	public void setLogged(java.util.Date logged) {
		set("logged", logged);
	}

	public java.util.Date getLogged() {
		return get("logged");
	}

	public void setActivated(java.util.Date activated) {
		set("activated", activated);
	}

	public java.util.Date getActivated() {
		return get("activated");
	}

	public void setFlag(java.lang.String flag) {
		set("flag", flag);
	}

	public java.lang.String getFlag() {
		return get("flag");
	}
	
	public void setOpenid(java.lang.String openid) {
	    set("openid", openid);
	}

    public java.lang.String getOpenid() {
        return get("openid");
    }
    
    public void setPid(java.math.BigInteger pid) {
        set("pid", pid);
    }

    public java.math.BigInteger getPid() {
        Object pid = get("pid");
        if (pid == null)
            return null;

        return pid instanceof BigInteger ? (BigInteger)pid : new BigInteger(pid.toString());
    }

    public void setChildNum(java.lang.Long childNum) {
        set("child_num", childNum);
    }

    public java.lang.Long getChildNum() {
        return get("child_num");
    }

    public void setTeamNum(java.lang.Long teamNum) {
        set("team_num", teamNum);
    }

    public java.lang.Long getTeamNum() {
        return get("team_num");
    }

    public void setTeamBuyAmount(java.math.BigDecimal teamBuyAmount) {
        set("team_buy_amount", teamBuyAmount);
    }

    public java.math.BigDecimal getTeamBuyAmount() {
        return get("team_buy_amount");
    }

}
