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
import com.jfinal.render.Render;
import io.jpress.Consts;
import io.jpress.core.BaseFrontController;
import io.jpress.core.addon.HookInvoker;
import io.jpress.core.cache.ActionCache;
import io.jpress.model.*;
import io.jpress.model.query.*;
import io.jpress.router.RouterMapping;
import io.jpress.template.TemplateManager;
import io.jpress.template.TplModule;
import io.jpress.ui.freemarker.tag.CommentPageTag;
import io.jpress.ui.freemarker.tag.MenusTag;
import io.jpress.ui.freemarker.tag.NextContentTag;
import io.jpress.ui.freemarker.tag.PreviousContentTag;
import io.jpress.utils.StringUtils;
import io.jpress.wechat.WechatUserInterceptor;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@RouterMapping(url = Consts.ROUTER_CONTENT)
@Before(WechatUserInterceptor.class)
public class ContentController extends BaseFrontController {

	private String slug;
	private BigInteger id;
	private int page;

	@ActionCache
	public void index() {
		try {
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
		initRequest();

		Content content = queryContent();
		if (null == content) {
			renderError(404);
			return;
		}

		TplModule module = TemplateManager.me().currentTemplateModule(content.getModule());

		if (module == null) {
			renderError(404);
			return;
		}

		//获取商品介绍图
		List<Metadata> bannerMetadatas = MetaDataQuery.me().findListByTypeAndId(Content.METADATA_TYPE_BANNER, content.getId());
		List<String> banners=new ArrayList<String>();
		if(!bannerMetadatas.isEmpty()){
			for(Metadata bm:bannerMetadatas){
				if(bm.getMetaKey().contains("img")){
					banners.add(bm.getMetaValue());
				}
			}
		}
		setAttr("banners", banners);
		
		Spec spec=SpecQuery.me().findContentSpec(content.getId());
		List<SpecValue> specValueList=SpecValueQuery.me().findContentSpecValueList(content.getId());
		setAttr("spec", spec);
		setAttr("specValueList", specValueList);

		updateContentViewCount(content);
		setGlobleAttrs(content);

		setAttr("p", page);
		setAttr("content", content);
		
		//重新赋值分享参数
		if (StringUtils.isNotBlank(content.getTitle())) {
		    //setAttr(Consts.SHARE_TITLE, content.getTitle());
		    //商品页的分享标题与默认的首页一样
		    //String share_title = OptionQuery.me().findValue("share_title");
		    String share_title = OptionQuery.me().findValue("web_title");
		    setAttr(Consts.SHARE_TITLE, share_title);
		}
		if (StringUtils.isNotBlank(content.getTitle())) {
			setAttr(Consts.SHARE_DESC, content.getTitle());
		}
		if (StringUtils.isNotBlank(content.getThumbnail())) {
			String web_domain = OptionQuery.me().findValue("web_domain");
			setAttr(Consts.SHARE_IMG_URL, web_domain+content.getThumbnail());
		}
		

		setAttr(NextContentTag.TAG_NAME, new NextContentTag(content));
		setAttr(PreviousContentTag.TAG_NAME, new PreviousContentTag(content));

		setAttr(CommentPageTag.TAG_NAME, new CommentPageTag(getRequest(), content, page));

		List<Taxonomy> taxonomys = TaxonomyQuery.me().findListByContentId(content.getId());
		setAttr("taxonomys", taxonomys);
		setAttr(MenusTag.TAG_NAME, new MenusTag(getRequest(), taxonomys, content));

		String style = content.getStyle();
		if (StringUtils.isNotBlank(style)) {
			render(String.format("content_%s_%s.html", module.getName(), style.trim()));
			return;
		}

		style = tryGetTaxonomyTemplate(module, taxonomys);
		if (style != null) {
			render(String.format("content_%s_%s.html", module.getName(), style));
			return;
		}

		render(String.format("content_%s.html", module.getName()));

	}

	private String tryGetTaxonomyTemplate(TplModule module, List<Taxonomy> taxonomys) {
		if (taxonomys == null || taxonomys.isEmpty())
			return null;
		String forSlug = null;
		for (Taxonomy taxonomy : taxonomys) {
			String tFile = String.format("content_%s_%s%s.html", module.getName(), Consts.TAXONOMY_TEMPLATE_PREFIX,taxonomy.getSlug());
			if (templateExists(tFile)) {
				if (forSlug == null) {
					forSlug = Consts.TAXONOMY_TEMPLATE_PREFIX + taxonomy.getSlug();
				} else {
					forSlug = null;
					break;
				}
			}
		}
		return forSlug;
	}

	private void updateContentViewCount(Content content) {
		long visitorCount = VisitorCounter.getVisitorCount(content.getId());
		Long viewCount = content.getViewCount() == null ? visitorCount : content.getViewCount() + visitorCount;
		content.setViewCount(viewCount);
		if (content.update()) {
			VisitorCounter.clearVisitorCount(content.getId());
		}
	}

	private void setGlobleAttrs(Content content) {

		setAttr(Consts.ATTR_GLOBAL_WEB_TITLE, content.getTitle());

		if (StringUtils.isNotBlank(content.getMetaKeywords())) {
			setAttr(Consts.ATTR_GLOBAL_META_KEYWORDS, content.getMetaKeywords());
		} else {
			setAttr(Consts.ATTR_GLOBAL_META_KEYWORDS, content.getTaxonomyAsString(null));
		}

		if (StringUtils.isNotBlank(content.getMetaDescription())) {
			setAttr(Consts.ATTR_GLOBAL_META_DESCRIPTION, content.getMetaDescription());
		} else {
			setAttr(Consts.ATTR_GLOBAL_META_DESCRIPTION, content.getSummary());
		}

	}

	private Content queryContent() {
		if (id != null) {
			return ContentQuery.me().findById(id);
		} else {
			return ContentQuery.me().findBySlug(StringUtils.urlDecode(slug));
		}
	}

	private void initRequest() {
		String para = getPara(0);
		if (StringUtils.isBlank(para)) {
			id = getParaToBigInteger("id");
			slug = getPara("slug");
			page = getParaToInt("p", 1);
			if (id == null && slug == null) {
				renderError(404);
			}
			return;
		}

		if (StringUtils.isNumeric(para)) {
			id = new BigInteger(para);
		} else {
			slug = para;
		}
		page = getParaToInt(1, 1);

	}

	private Render onRenderBefore() {
		return HookInvoker.contentRenderBefore(this);
	}

	private void onRenderAfter() {
		HookInvoker.contentRenderAfter(this);
	}

}
