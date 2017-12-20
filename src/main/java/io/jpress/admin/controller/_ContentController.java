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
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.IAtom;
import com.jfinal.plugin.activerecord.Page;
import io.jpress.Consts;
import io.jpress.core.JBaseCRUDController;
import io.jpress.core.interceptor.ActionCacheClearInterceptor;
import io.jpress.core.render.AjaxResult;
import io.jpress.interceptor.UCodeInterceptor;
import io.jpress.message.Actions;
import io.jpress.message.MessageKit;
import io.jpress.model.*;
import io.jpress.model.query.*;
import io.jpress.model.router.ContentRouter;
import io.jpress.router.RouterMapping;
import io.jpress.router.RouterNotAllowConvert;
import io.jpress.template.TemplateManager;
import io.jpress.template.TplModule;
import io.jpress.template.TplTaxonomyType;
import io.jpress.utils.JsoupUtils;
import io.jpress.utils.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.*;

@RouterMapping(url = "/admin/content", viewPath = "/WEB-INF/admin/content")
@Before(ActionCacheClearInterceptor.class)
@RouterNotAllowConvert
public class _ContentController extends JBaseCRUDController<Content> {

	private String getModuleName() {
		return getPara("m");
	}

	private String getStatus() {
		return getPara("s");
	}

	@Override
	public void index() {

		TplModule module = TemplateManager.me().currentTemplateModule(getModuleName());
		setAttr("module", module);
		setAttr("delete_count", ContentQuery.me().findCountByModuleAndStatus(getModuleName(), Content.STATUS_DELETE));
		setAttr("draft_count", ContentQuery.me().findCountByModuleAndStatus(getModuleName(), Content.STATUS_DRAFT));
		setAttr("normal_count", ContentQuery.me().findCountByModuleAndStatus(getModuleName(), Content.STATUS_NORMAL));
		setAttr("count", ContentQuery.me().findCountInNormalByModule(getModuleName()));

		setAttr("tids", getPara("tids"));
		BigInteger[] tids = null;
		String[] tidStrings = getPara("tids", "").split(",");

		List<BigInteger> tidList = new ArrayList<BigInteger>();
		for (String stringid : tidStrings) {
			if (StringUtils.isNotBlank(stringid)) {
				tidList.add(new BigInteger(stringid));
			}
		}
		tids = tidList.toArray(new BigInteger[] {});

		String keyword = getPara("k", "").trim();

		Page<Content> page = null;
		if (StringUtils.isNotBlank(getStatus())) {
			page = ContentQuery.me().paginateBySearch(getPageNumber(), getPageSize(), getModuleName(), keyword,
					getStatus(), tids, null);
		} else {
			page = ContentQuery.me().paginateByModuleNotInDelete(getPageNumber(), getPageSize(), getModuleName(),
					keyword, tids, null);
		}

		filterUI(tids);

		setAttr("page", page);

		String templateHtml = String.format("admin_content_index_%s.html", module.getName());
		for (int i = 0; i < 2; i++) {
			if (TemplateManager.me().existsFile(templateHtml)) {
				setAttr("include", TemplateManager.me().currentTemplatePath() + "/" + templateHtml);
				return;
			}
			templateHtml = templateHtml.substring(0, templateHtml.lastIndexOf("_")) + ".html";
		}
		setAttr("include", "_index_include.html");

	}

	private void filterUI(BigInteger[] tids) {
		TplModule module = TemplateManager.me().currentTemplateModule(getModuleName());

		if (module == null) {
			return;
		}

		List<TplTaxonomyType> types = module.getTaxonomyTypes();
		if (types != null && !types.isEmpty()) {
			HashMap<String, List<Taxonomy>> _taxonomyMap = new HashMap<String, List<Taxonomy>>();

			for (TplTaxonomyType type : types) {
				// 排除标签类的分类删选
				if (TplTaxonomyType.TYPE_SELECT.equals(type.getFormType())) {
					List<Taxonomy> taxonomys = TaxonomyQuery.me().findListByModuleAndTypeAsSort(getModuleName(),
							type.getName());
					processSelected(tids, taxonomys);
					_taxonomyMap.put(type.getTitle(), taxonomys);
				}
			}

			setAttr("_taxonomyMap", _taxonomyMap);
		}
	}

	private void processSelected(BigInteger[] tids, List<Taxonomy> taxonomys) {
		if (taxonomys == null || taxonomys.isEmpty()) {
			return;
		}
		if (tids == null || tids.length == 0) {
			return;
		}
		for (Taxonomy t : taxonomys) {
			for (BigInteger id : tids) {
				if (t.getId().compareTo(id) == 0) {
					t.put("_selected", "selected=\"selected\"");
				}
			}
		}
	}

	@Before(UCodeInterceptor.class)
	public void trash() {
		Content c = ContentQuery.me().findById(getParaToBigInteger("id"));
		if (c != null) {
			c.setStatus(Content.STATUS_DELETE);
			c.saveOrUpdate();
			renderAjaxResultForSuccess("success");
		} else {
			renderAjaxResultForError("trash error!");
		}
	}

	@Before(UCodeInterceptor.class)
	public void draft() {
		Content c = ContentQuery.me().findById(getParaToBigInteger("id"));
		if (c != null) {
			c.setStatus(Content.STATUS_DRAFT);
			c.saveOrUpdate();
			renderAjaxResultForSuccess("success");
		} else {
			renderAjaxResultForError("trash error!");
		}
	}

	@Before(UCodeInterceptor.class)
	public void batchTrash() {
		BigInteger[] ids = getParaValuesToBigInteger("dataItem");
		int count = ContentQuery.me().batchTrash(ids);
		if (count > 0) {
			renderAjaxResultForSuccess("success");
		} else {
			renderAjaxResultForError("trash error!");
		}
	}

	@Before(UCodeInterceptor.class)
	public void restore() {
		BigInteger id = getParaToBigInteger("id");
		Content c = ContentQuery.me().findById(id);
		if (c != null && c.isDelete()) {
			c.setStatus(Content.STATUS_DRAFT);
			c.setModified(new Date());
			c.saveOrUpdate();
			renderAjaxResultForSuccess("success");
		} else {
			renderAjaxResultForError("restore error!");
		}
	}

	@Before(UCodeInterceptor.class)
	public void doDraft() {
		Content c = ContentQuery.me().findById(getParaToBigInteger("id"));
		if (c != null) {
			c.setStatus(Content.STATUS_NORMAL);
			c.saveOrUpdate();
			renderAjaxResultForSuccess("success");
		} else {
			renderAjaxResultForError("trash error!");
		}
	}

	@Override
	public void edit() {
		String moduleName = getModuleName();
		BigInteger contentId = getParaToBigInteger("id");

		Content content = ContentQuery.me().findById(contentId);
		if (content != null) {
			setAttr("content", content);
			moduleName = content.getModule();
		}

		TplModule module = TemplateManager.me().currentTemplateModule(moduleName);
		setAttr("module", module);

		String _editor = getCookie("_editor", "tinymce");
		setAttr("_editor", _editor);

		setAttr("urlPreffix", ContentRouter.getContentRouterPreffix(module.getName()));
		setAttr("urlSuffix", ContentRouter.getContentRouterSuffix(module.getName()));

		setSlugInputDisplay(moduleName);

		//规格，规格值，价格，库存
		List<Spec> specList=SpecQuery.me().findListByStatus(Spec.STATUS_NORMAL);
		if(contentId!=null){
			//商品选中的规格
			Spec spec=SpecQuery.me().findContentSpec(content.getId());
			//商品选中的规格，该规格的所有规格值
			List<SpecValue> specValueList=SpecValueQuery.me().findListByContentId(content.getId());
			//商品选中的规格，该规格选中的规格值
			List<SpecValue> specValueCheckedList=SpecValueQuery.me().findContentSpecValueList(content.getId());
			setAttr("spec", spec);
			setAttr("specValueList", specValueList);
			setAttr("specValueCheckedList", specValueCheckedList);
		}
		setAttr("specList", specList);

		String templateHtml = String.format("admin_content_edit_%s.html", moduleName);
		for (int i = 0; i < 2; i++) {
			if (TemplateManager.me().existsFile(templateHtml)) {
				setAttr("include", TemplateManager.me().currentTemplatePath() + "/" + templateHtml);
				return;
			}
			templateHtml = templateHtml.substring(0, templateHtml.lastIndexOf("_")) + ".html";
		}

		setAttr("include", "_edit_include.html");
	}

	private void setSlugInputDisplay(String moduleName) {
		if (Consts.MODULE_PAGE.equals(moduleName)) {
			setAttr("slugDisplay", "true");
			return;
		}

		String routerType = ContentRouter.getRouterType();
		if (StringUtils.isBlank(routerType)) { // 没设置过，默认id
			return;
		}

		if (ContentRouter.TYPE_DYNAMIC_ID.equals(routerType) || ContentRouter.TYPE_STATIC_MODULE_ID.equals(routerType)
				|| ContentRouter.TYPE_STATIC_DATE_ID.equals(routerType)
				|| ContentRouter.TYPE_STATIC_PREFIX_ID.equals(routerType)) {
			return;
		}
		setAttr("slugDisplay", "true");
	}

	public void changeEditor() {
		String name = getPara();
		setCookie("_editor", name, Integer.MAX_VALUE);
		renderAjaxResultForSuccess();
	}

	public List<BigInteger> getOrCreateTaxonomyIds(String moduleName) {
		TplModule module = TemplateManager.me().currentTemplateModule(moduleName);
		List<TplTaxonomyType> types = module.getTaxonomyTypes();
		List<BigInteger> tIds = new ArrayList<BigInteger>();
		for (TplTaxonomyType type : types) {
			if (type.isInputType()) {
				String slugsData = getPara("_" + type.getName());
				if (StringUtils.isBlank(slugsData)) {
					continue;
				}

				List<Taxonomy> taxonomyList = TaxonomyQuery.me().findListByModuleAndType(moduleName, type.getName());

				String[] slugs = slugsData.split(",");
				for (String slug : slugs) {
					BigInteger id = getTaxonomyIdFromListBySlug(slug, taxonomyList);
					if (id == null) {
						Taxonomy taxonomy = new Taxonomy();
						taxonomy.setTitle(slug);
						taxonomy.setSlug(slug);
						taxonomy.setContentModule(moduleName);
						taxonomy.setType(type.getName());
						if (taxonomy.save()) {
							id = taxonomy.getId();
						}
					}
					tIds.add(id);
				}
			} else if (type.isSelectType()) {
				BigInteger[] ids = getParaValuesToBigInteger("_" + type.getName());
				if (ids != null && ids.length > 0)
					tIds.addAll(Arrays.asList(ids));
			}
		}
		return tIds;
	}

	private BigInteger getTaxonomyIdFromListBySlug(String slug, List<Taxonomy> list) {
		for (Taxonomy taxonomy : list) {
			if (slug.equals(taxonomy.getSlug()))
				return taxonomy.getId();
		}
		return null;
	}

	@Before(UCodeInterceptor.class)
	@Override
	public void save() {
		final Map<String, String> metas = getMetas();
		final Content content = getContent();

		if (StringUtils.isBlank(content.getTitle())) {
			renderAjaxResultForError("商品标题不能为空！");
			return;
		}
		
		if (StringUtils.isBlank(content.getText())) {
			renderAjaxResultForError("商品介绍不能为空！");
			return;
		}
		
		if (StringUtils.isBlank(content.getThumbnail())) {
			renderAjaxResultForError("缩略图不能为空！");
			return;
		}
		
		//规格，规格值，库存，价钱
		BigInteger specId=getParaToBigInteger("specId");
		BigInteger[] specValueIds=getParaValuesToBigInteger("specValueIds");
		if(specId==null){
			renderAjaxResultForError("规格不能为空！");
			return;
		}
		if(specValueIds==null){
			renderAjaxResultForError("规格值不能为空！");
			return;
		}
		final List<ContentSpecItem> contentSpecItemList=new ArrayList<ContentSpecItem>();
		for(BigInteger specValueId:specValueIds){
			BigDecimal price=getParaToBigDecimal("price"+specValueId);
			Integer stock=getParaToInt("stock"+specValueId);
			if(price==null){
				renderAjaxResultForError("价格不能为空！");
				return;
			}
			if(stock==null){
				renderAjaxResultForError("库存不能为空！");
				return;
			}
			ContentSpecItem contentSpecItem=new ContentSpecItem();
			contentSpecItem.setSpecId(specId);
			contentSpecItem.setSpecValueId(specValueId);
			contentSpecItem.setPrice(price);
			contentSpecItem.setStock(stock);
			contentSpecItemList.add(contentSpecItem);
		}

		String slug = StringUtils.isBlank(content.getSlug()) ? content.getTitle() : content.getSlug();
		content.setSlug(slug);
		
		/*String text=content.getText();
		content.setText(text);*/

		Content dbContent = ContentQuery.me().findBySlug(content.getSlug());
		if (dbContent != null && content.getId() != null && dbContent.getId().compareTo(content.getId()) != 0) {
			renderAjaxResultForError();
			return;
		}
		
		//审核
		String contentStatus = Content.STATUS_NORMAL;
		Boolean content_must_audited = OptionQuery.me().findValueAsBool("content_must_audited");
		if (content_must_audited != null && content_must_audited) {
			contentStatus = Content.STATUS_DRAFT;
		}else if(StringUtils.isNotBlank(content.getStatus()) && Content.STATUS_DRAFT.equals(content.getStatus())){
			contentStatus = Content.STATUS_DRAFT;
		}
		content.setStatus(contentStatus);

		boolean saved = Db.tx(new IAtom() {
			@Override
			public boolean run() throws SQLException {

				Content oldContent = null;
				if (content.getId() != null) {
					oldContent = ContentQuery.me().findById(content.getId());

					ContentSpecItemQuery.me().deleteByContentId(content.getId());
				}

				if (!content.saveOrUpdate()) {
					return false;
				}
				
				if(!contentSpecItemList.isEmpty()){
					for(ContentSpecItem contentSpecItem:contentSpecItemList){
						contentSpecItem.setContentId(content.getId());
						contentSpecItem.setCreated(new Date());
						if(!contentSpecItem.save()){
							return false;
						}
					}
				}

				content.updateCommentCount();

				List<BigInteger> ids = getOrCreateTaxonomyIds(content.getModule());
				if (ids == null || ids.size() == 0) {
					MappingQuery.me().deleteByContentId(content.getId());
				} else {
					if (!MappingQuery.me().doBatchUpdate(content.getId(), ids.toArray(new BigInteger[0]))) {
						return false;
					}
				}

				if (metas != null) {
					for (Map.Entry<String, String> entry : metas.entrySet()) {
						content.saveOrUpdateMetadta(Content.METADATA_TYPE, entry.getKey(), entry.getValue());
					}
				}

				MessageKit.sendMessage(Actions.CONTENT_COUNT_UPDATE, ids.toArray(new BigInteger[] {}));

				if (oldContent != null && oldContent.getTaxonomys() != null) {
					List<Taxonomy> taxonomys = oldContent.getTaxonomys();
					BigInteger[] taxonomyIds = new BigInteger[taxonomys.size()];
					for (int i = 0; i < taxonomys.size(); i++) {
						taxonomyIds[i] = taxonomys.get(i).getId();
					}
					MessageKit.sendMessage(Actions.CONTENT_COUNT_UPDATE, taxonomyIds);
				}

				return true;
			}
		});

		if (!saved) {
			renderAjaxResultForError();
			return;
		}

		AjaxResult ar = new AjaxResult();
		ar.setErrorCode(0);
		ar.setData(content.getId());
		renderAjaxResult("save ok", 0, content.getId());
	}

	private Content getContent() {
		Content content = getModel(Content.class);

		content.setText(JsoupUtils.getBodyHtml(content.getText()));

		if (content.getCreated() == null) {
			content.setCreated(new Date());
		}
		content.setModified(new Date());

		User user = getLoginedUser();
		content.setUserId(user.getId());

		return content;
	}

	public void banner() {
		String moduleName = getModuleName();

		final BigInteger id = getParaToBigInteger("id");
		Content content = ContentQuery.me().findById(id);
		setAttr("content", content);

		List<Metadata> metadataList = MetaDataQuery.me().findListByTypeAndId(Content.METADATA_TYPE_BANNER, id);
		setAttr("metadataSize", metadataList.size());

		String templateHtml = String.format("admin_content_banner_%s.html", moduleName);
		for (int i = 0; i < 2; i++) {
			if (TemplateManager.me().existsFile(templateHtml)) {
				setAttr("include", TemplateManager.me().currentTemplatePath() + "/" + templateHtml);
				return;
			}
			templateHtml = templateHtml.substring(0, templateHtml.lastIndexOf("_")) + ".html";
		}
		setAttr("include", "_banner_include.html");
	}

	@Before(UCodeInterceptor.class)
	public void doClearBanners() {
		BigInteger id=getParaToBigInteger("id");
		if(id!=null){
			MetaDataQuery.me().deleteByTypeAndId(Content.METADATA_TYPE_BANNER, id);
			renderAjaxResultForSuccess("操作成功！");
		}else{
			renderAjaxResultForError("操作失败！");
		}
	}

	protected boolean validate(Map<String, String> metas){
		for(int x = 0 ; x < metas.size() ; x++) {
			if (metas.get("content_banner" + x + "_img") == null) break;
			if (metas.get("content_banner" + x + "_img").isEmpty()) {
				renderAjaxResultForError("商品介绍图不能为空！");
				return false;
			}
			if (metas.get("content_banner" + x + "_remark") == null) break;
			if (metas.get("content_banner" + x + "_remark").isEmpty()) {
				renderAjaxResultForError("商品介绍图描述不能为空！");
				return false;
			}
		}

		return true;
	}

	@Before(UCodeInterceptor.class)
	public void doSaveBanners() {
		final HashMap<String, String> metas = new HashMap<String, String>();
		Map<String, String[]> requestMap = getParaMap();
		if (requestMap != null) {
			for (Map.Entry<String, String[]> entry : requestMap.entrySet()) {
				if (entry.getKey().startsWith("meta_")) {
					metas.put(entry.getKey().substring(5), entry.getValue()[0]);
				}
			}
		}

		final Content content = getModel(Content.class);

		if (!validate(metas)) return;

		boolean saved = Db.tx(new IAtom() {
			@Override
			public boolean run() throws SQLException {
				//删除
				MetaDataQuery.me().deleteByTypeAndId(Content.METADATA_TYPE_BANNER, content.getId());

				//保存
				if (!metas.isEmpty()) {
					for (Map.Entry<String, String> entry : metas.entrySet()) {
						content.saveOrUpdateMetadta(Content.METADATA_TYPE_BANNER, entry.getKey(), entry.getValue());
					}
				}

				return true;
			}
		});

		if (saved) {
			renderAjaxResultForSuccess("操作成功！");
		} else {
			renderAjaxResultForError("操作失败！");
		}
	}

}
