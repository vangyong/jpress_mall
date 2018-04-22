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
package io.jpress.front.controller;

import com.jfinal.aop.Before;
import com.jfinal.aop.Clear;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.IAtom;
import com.jfinal.render.Render;

import io.jpress.Consts;
import io.jpress.core.BaseFrontController;
import io.jpress.core.addon.HookInvoker;
import io.jpress.core.cache.ActionCache;
import io.jpress.model.Coupon;
import io.jpress.model.CouponUsed;
import io.jpress.model.query.CouponQuery;
import io.jpress.model.query.OptionQuery;
import io.jpress.router.RouterMapping;
import io.jpress.ui.freemarker.tag.IndexPageTag;
import io.jpress.utils.StringUtils;
import io.jpress.wechat.WechatUserInterceptor;

import java.math.BigInteger;
import java.sql.SQLException;

@RouterMapping(url = "/")
@Before(WechatUserInterceptor.class)
public class IndexController extends BaseFrontController {

	@ActionCache
	public void index() {
		try {
			/* 新版的首页没有banner了,jianb.jiang,20180212
			//获取首页banner图
			List<Metadata> bannersList = MetaDataQuery.me().findListByTypeAndId(Consts.INDEX_BANNER, Consts.INDEX_BANNER_ID);
			HashMap<String, String> bannersMap = new HashMap<String, String>();
			String[] index_banners_img=new String[bannersList.size()];
			String[] index_banners_url=new String[bannersList.size()];
			String[] index_banners_remark=new String[bannersList.size()];
			if(!bannersList.isEmpty()){
				for(Metadata m:bannersList){
					bannersMap.put(m.getMetaKey(), m.getMetaValue());
				}
			}
			if(!bannersMap.isEmpty()){
				String img=null;
				String url=null;
				String remark=null;
				for(int i=0;i<bannersMap.size();i++){
					img=Consts.INDEX_BANNER+i+"_img";
					url=Consts.INDEX_BANNER+i+"_url";
					remark=Consts.INDEX_BANNER+i+"_remark";
					index_banners_img[i]=bannersMap.get(img);
					index_banners_url[i]=bannersMap.get(url);
					index_banners_remark[i]=bannersMap.get(remark);
				}
			}
			setAttr("index_banners_img", index_banners_img);
			setAttr("index_banners_url", index_banners_url);
			setAttr("index_banners_remark", index_banners_remark);*/

			Render render = onRenderBefore();
			if (render != null) {
				render(render);
			} else {
				doRender();
			}
		} finally {
			onRenderAfter();
		}
	}

	private void doRender() {
		setGlobleAttrs();

		String para = getPara();

		if (StringUtils.isBlank(para)||(!para.contains("-"))) {
			setAttr(IndexPageTag.TAG_NAME, new IndexPageTag(getRequest(), null, 1, null));
			render("index.html");
			return;
		}

		String[] paras = para.split("-");
		if (paras.length == 1) {
			if (StringUtils.isNumeric(para.trim())) {
				setAttr(IndexPageTag.TAG_NAME, new IndexPageTag(getRequest(), null, StringUtils.toInt(para.trim(), 1), null));
				render("index.html");
			} else {
				setAttr(IndexPageTag.TAG_NAME, new IndexPageTag(getRequest(), para.trim(), 1, null));
				render("page_" + para + ".html");
			}
		} else if (paras.length == 2) {
			String pageName = paras[0];
			String pageNumber = paras[1];

			if (!StringUtils.isNumeric(pageNumber)) {
				renderError(404);
			}

			setAttr(IndexPageTag.TAG_NAME, new IndexPageTag(getRequest(), pageName, StringUtils.toInt(pageNumber, 1), null));
			render("page_" + pageName + ".html");
		} else {
			renderError(404);
		}

	}

	private void setGlobleAttrs() {
		String title = OptionQuery.me().findValue("seo_index_title");
		String keywords = OptionQuery.me().findValue("seo_index_keywords");
		String description = OptionQuery.me().findValue("seo_index_description");

		if (StringUtils.isNotBlank(title)) {
			setAttr(Consts.ATTR_GLOBAL_WEB_TITLE, title);
		}

		if (StringUtils.isNotBlank(keywords)) {
			setAttr(Consts.ATTR_GLOBAL_META_KEYWORDS, keywords);
		}

		if (StringUtils.isNotBlank(description)) {
			setAttr(Consts.ATTR_GLOBAL_META_DESCRIPTION, description);
		}
	}

	private Render onRenderBefore() {
		return HookInvoker.indexRenderBefore(this);
	}

	private void onRenderAfter() {
		HookInvoker.indexRenderAfter(this);
	}

	/**
	 * <b>Description.首页加载完成的时候如果访问的地址附有优惠券码则自动抢券，抢券成功后在首页给出提示:</b><br>
	 * <b>Author:jianb.jiang</b>
	 * <br><b>Date:</b> 2018年2月12日 下午8:57:25
	 */
	@Clear(WechatUserInterceptor.class)
	public void getCoupon(){
	    final String couponCode=getPara("cp_code");
	    final BigInteger userId=getLoginedUser().getId();
	    final StringBuilder sqlBuilder = new StringBuilder("UPDATE jp_coupon c SET c.free_num = c.free_num - 1 ");
	    sqlBuilder.append("WHERE c. CODE = ? AND c.invalid = 0 AND DATE(now()) <= c.last_date ");
	    sqlBuilder.append("AND NOT EXISTS ( SELECT 1 FROM jp_coupon_used cu WHERE c.id = cu.coupon_id AND cu.user_id = ?)");
	    if (StringUtils.isBlank(couponCode) || !couponCode.startsWith("Coupon")) {
	        renderAjaxResult("操作成功", 0, "");
	        return;
	    }
	    final Coupon coupon = CouponQuery.me().findByCode(couponCode);
	    boolean saved=Db.tx(new IAtom() {
            @Override
            public boolean run() throws SQLException {
                if (Db.update(sqlBuilder.toString(), couponCode, userId) > 0) { //抢券成功
                    CouponUsed cu = new CouponUsed();
                    cu.setCouponId(coupon.getId());
                    cu.setUserId(userId);
                    if (cu.save()) {
                        return true;
                    }
                }
                return false;
            }
        });

        if(saved){
            renderAjaxResult("操作成功", 0, coupon);
            return;
        }
        renderAjaxResult("系统异常", Consts.ERROR_CODE_SYSTEM_ERROR, null);
	}
}
