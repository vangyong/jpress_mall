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

import io.jpress.message.MessageKit;
import io.jpress.model.Metadata;
import io.jpress.model.core.JModel;
import io.jpress.model.query.MetaDataQuery;
import java.math.BigInteger;

import com.jfinal.plugin.activerecord.IBean;
import io.jpress.cache.JCacheKit;
import com.jfinal.plugin.ehcache.IDataLoader;

/**
 *  Auto generated by JPress, do not modify this file.
 */
@SuppressWarnings("serial")
public abstract class BaseRefund<M extends BaseRefund<M>> extends JModel<M> implements IBean {

	public static final String CACHE_NAME = "jp_refund";
	public static final String METADATA_TYPE = "jp_refund";

	public static final String ACTION_ADD = "jp_refund:add";
	public static final String ACTION_DELETE = "jp_refund:delete";
	public static final String ACTION_UPDATE = "jp_refund:update";

	public void removeCache(Object key){
		if(key == null) return;
		JCacheKit.remove(CACHE_NAME, key);
	}

	public void putCache(Object key,Object value){
		JCacheKit.put(CACHE_NAME, key, value);
	}

	public M getCache(Object key){
		return JCacheKit.get(CACHE_NAME, key);
	}

	public M getCache(Object key,IDataLoader dataloader){
		return JCacheKit.get(CACHE_NAME, key, dataloader);
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
		if(!(o instanceof BaseRefund<?>)){return false;}

		BaseRefund<?> m = (BaseRefund<?>) o;
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

	public void setId(java.math.BigInteger id) {
		set("id", id);
	}

	public java.math.BigInteger getId() {
		Object id = get("id");
		if (id == null)
			return null;

		return id instanceof BigInteger ? (BigInteger)id : new BigInteger(id.toString());
	}

	public void setRefundNo(java.lang.String refundNo) {
		set("refund_no", refundNo);
	}

	public java.lang.String getRefundNo() {
		return get("refund_no");
	}

	public void setOrderNo(java.lang.String orderNo) {
		set("order_no", orderNo);
	}

	public java.lang.String getOrderNo() {
		return get("order_no");
	}

	public void setTradeNo(java.lang.String tradeNo) {
		set("trade_no", tradeNo);
	}

	public java.lang.String getTradeNo() {
		return get("trade_no");
	}

	public void setTradeRefundNo(java.lang.String tradeRefundNo) {
		set("trade_refund_no", tradeRefundNo);
	}

	public java.lang.String getTradeRefundNo() {
		return get("trade_refund_no");
	}

	public void setStatus(java.lang.String status) {
		set("status", status);
	}

	public java.lang.String getStatus() {
		return get("status");
	}

	public void setAmount(java.math.BigDecimal amount) {
		set("amount", amount);
	}

	public java.math.BigDecimal getAmount() {
		return get("amount");
	}

    public void setDesc(java.lang.String desc) {
        set("desc", desc);
    }

    public java.lang.String getDesc() {
        return get("desc");
    }

	public void setCreatedTime(java.util.Date createdTime) {
		set("created_time", createdTime);
	}

	public java.util.Date getCreatedTime() {
		return get("created_time");
	}

	public void setModifiedTime(java.util.Date modifiedTime) {
		set("modified_time", modifiedTime);
	}

	public java.util.Date getModifiedTime() {
		return get("modified_time");
	}

}
