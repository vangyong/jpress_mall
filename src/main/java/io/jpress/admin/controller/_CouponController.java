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
package io.jpress.admin.controller;

import com.jfinal.aop.Before;
import com.jfinal.kit.PropKit;
import com.jfinal.plugin.activerecord.Page;
import io.jpress.core.JBaseCRUDController;
import io.jpress.core.interceptor.ActionCacheClearInterceptor;
import io.jpress.model.Attachment;
import io.jpress.model.Comment;
import io.jpress.model.Coupon;
import io.jpress.model.User;
import io.jpress.model.query.AttachmentQuery;
import io.jpress.model.query.CommentQuery;
import io.jpress.model.query.CouponQuery;
import io.jpress.model.query.OptionQuery;
import io.jpress.model.query.UserQuery;
import io.jpress.router.RouterMapping;
import io.jpress.router.RouterNotAllowConvert;
import io.jpress.template.TemplateManager;
import io.jpress.utils.QRCodeUtils;
import io.jpress.utils.RandomUtils;
import io.jpress.utils.StringUtils;

import java.io.File;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RouterMapping(url = "/admin/coupon", viewPath = "/WEB-INF/admin/coupon")
@Before(ActionCacheClearInterceptor.class)
@RouterNotAllowConvert
public class _CouponController extends JBaseCRUDController<Coupon> {

	public void index() {
		setAttr("couponCount", CouponQuery.me().findCount());

		Page<Coupon> page = CouponQuery.me().paginate(getPageNumber(), getPageSize(), null);
		setAttr("page", page);

		String templateHtml = "admin_coupon_index.html";
		if (TemplateManager.me().existsFile(templateHtml)) {
			setAttr("include", TemplateManager.me().currentTemplatePath() + "/" + templateHtml);
			return;
		}
		setAttr("include", "_index_include.html");
	}

	public void edit() {
		BigInteger id = getParaToBigInteger("id");
		if (id != null) {
			setAttr("coupon", CouponQuery.me().findById(id));
		}

		String templateHtml = "admin_coupon_edit.html";
		if (TemplateManager.me().existsFile(templateHtml)) {
			setAttr("include", TemplateManager.me().currentTemplatePath() + "/" + templateHtml);
			return;
		}
		setAttr("include", "_edit_include.html");

	}

	public void info() {
	    BigInteger id = getParaToBigInteger("id");
        Coupon coupon = CouponQuery.me().findById(id);
        setAttr("coupon", coupon);

        String templateHtml = "admin_user_edit.html";
        if (TemplateManager.me().existsFile(templateHtml)) {
            setAttr("include", TemplateManager.me().currentTemplatePath() + "/" + templateHtml);
            render("edit.html");
            return;
        }

        setAttr("include", "_edit_include.html");
        render("edit.html");

    }
	
	public void save() {

	    HashMap<String, String> files = getUploadFilesMap();
        final Map<String, String> metas = getMetas(files);
        
		final Coupon coupon = getModel(Coupon.class);

		if (coupon.getId() == null) {

			// 新增优惠券
			coupon.setCode(RandomUtils.getRandomString("Coupon",19));//自动生成优惠券码
			coupon.setFreeNum(coupon.getTotalNum());//新增的优惠券剩余数量默认为总数
			coupon.setInvalid(0L);//默认有效
			coupon.setCreatedTime(new Date());
			coupon.setModifiedTime(new Date());
		} 

		if (coupon.saveOrUpdate()) {
		    setAttr("coupon", coupon);
            renderAjaxResultForSuccess();
        } else {
            renderAjaxResultForError();
        }
		
	}
	
	public void frozen() {
        BigInteger id = getParaToBigInteger("id");
        if (id != null) {
            Coupon coupon = CouponQuery.me().findById(id);
            coupon.setInvalid(1L);
            coupon.saveOrUpdate();
            renderAjaxResultForSuccess();
        } else {
            renderAjaxResultForError();
        }
    }

    public void restore() {
        BigInteger id = getParaToBigInteger("id");
        if (id != null) {
            Coupon coupon = CouponQuery.me().findById(id);
            coupon.setInvalid(0L);
            coupon.saveOrUpdate();
            renderAjaxResultForSuccess();
        } else {
            renderAjaxResultForError();
        }
    }

    public void couponQRCode(){
        final String couponCode=getPara("cp_code");
        if (StringUtils.isBlank(couponCode) || !couponCode.startsWith("Coupon")) {
            renderAjaxResult("操作成功", 0, "");
            return;
        }
        
        //生成推销二维码
        String webDomain = OptionQuery.me().findValue("web_domain");
        String fileLocalPath = PropKit.get("fileLocalPath");
        StringBuffer text = new StringBuffer();
        text.append(webDomain);
        text.append("/?uid=2&cp_code=");
        text.append(couponCode);
        StringBuffer fileName = new StringBuffer();
        File dir = new File(fileLocalPath + "qcode/");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        fileName.append(fileLocalPath);
        fileName.append("qcode/");
        fileName.append(couponCode);
        fileName.append(".png");
        QRCodeUtils.createQRCode(text.toString(), fileName.toString(), 600, 600, 2);
        StringBuffer img2 = new StringBuffer();
        img2.append(fileLocalPath);
        img2.append("bottom.png");
        StringBuffer outImg = new StringBuffer();
        File dir2 = new File(fileLocalPath + "promotion/");
        if (!dir2.exists()) {
            dir2.mkdirs();
        }
        outImg.append(fileLocalPath);
        outImg.append("promotion/");
        outImg.append(couponCode);
        outImg.append(".png");
        QRCodeUtils.mergeImage(fileName.toString(), img2.toString(), outImg.toString());
        renderAjaxResult("操作成功", 0, getRequest().getContextPath() + "/attachment/promotion/"+couponCode+".png");
    }
}
