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
import io.jpress.interceptor.UCodeInterceptor;
import io.jpress.model.Content;
import io.jpress.model.Metadata;
import io.jpress.model.ModelSorter;
import io.jpress.model.Taxonomy;
import io.jpress.model.query.ContentQuery;
import io.jpress.model.query.MappingQuery;
import io.jpress.model.query.MetaDataQuery;
import io.jpress.model.query.TaxonomyQuery;
import io.jpress.router.RouterMapping;
import io.jpress.router.RouterNotAllowConvert;
import io.jpress.template.TemplateManager;
import io.jpress.template.TplModule;
import io.jpress.template.TplTaxonomyType;
import io.jpress.utils.StringUtils;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RouterMapping(url = "/admin/taxonomy", viewPath = "/WEB-INF/admin/taxonomy")
@Before(ActionCacheClearInterceptor.class)
@RouterNotAllowConvert
public class _TaxonomyController extends JBaseCRUDController<Taxonomy> {

	private String getContentModule() {
		return getPara("m");
	}

	private String getType() {
		return getPara("t");
	}

	public void index() {
		String moduleName = getContentModule();
		TplModule module = TemplateManager.me().currentTemplateModule(moduleName);
		TplTaxonomyType type = module.getTaxonomyTypeByType(getType());
		BigInteger id = getParaToBigInteger("id");

		List<Taxonomy> taxonomys = TaxonomyQuery.me().findListByModuleAndTypeAsSort(moduleName, type.getName());

		if (id != null) {
			Taxonomy taxonomy = TaxonomyQuery.me().findById(id);
			setAttr("taxonomy", taxonomy);
			Content content = ContentQuery.me().findFirstByModuleAndObjectId(Consts.MODULE_MENU, taxonomy.getId());
			if (content != null) {
				setAttr("addToMenuSelete", "checked=\"checked\"");
			}
		}

		if (id != null && taxonomys != null) {
			ModelSorter.removeTreeBranch(taxonomys, id);
		}

		if (TplTaxonomyType.TYPE_SELECT.equals(type.getFormType())) {
			Page<Taxonomy> page = TaxonomyQuery.me().doPaginate(1, Integer.MAX_VALUE, getContentModule(), getType());
			ModelSorter.sort(page.getList());
			setAttr("page", page);
		} else if (TplTaxonomyType.TYPE_INPUT.equals(type.getFormType())) {
			Page<Taxonomy> page = TaxonomyQuery.me().doPaginate(getPageNumber(), getPageSize(), getContentModule(),
					getType());
			setAttr("page", page);
		}

		setAttr("module", module);
		setAttr("type", type);
		setAttr("taxonomys", taxonomys);

		String templateHtml = String.format("admin_taxonomy_index_%s_%s.html", moduleName, getType());
		for (int i = 0; i < 3; i++) {
			if (TemplateManager.me().existsFile(templateHtml)) {
				setAttr("include", TemplateManager.me().currentTemplatePath() + "/" + templateHtml);
				return;
			}
			templateHtml = templateHtml.substring(0, templateHtml.lastIndexOf("_")) + ".html";
		}

		setAttr("include", "_index_include.html");
	}

	public void save() {
		Taxonomy m = getModel(Taxonomy.class);

		if (StringUtils.isBlank(m.getTitle())) {
			renderAjaxResultForError("名称不能为空！");
			return;
		}

		if (StringUtils.isBlank(m.getSlug())) {
			renderAjaxResultForError("别名不能为空！");
			return;
		}

		if (StringUtils.isNumeric(m.getSlug())) {
			renderAjaxResultForError("别名不能全为数字！");
			return;
		}

		// getModel是jfinal通过model.put()设置属性的，不用调用setXXX设置。
		if (m.getSlug() != null) {
			// setSlug内部做了些格式化判断
			m.setSlug(m.getSlug());
		}

		Taxonomy dbTaxonomy = TaxonomyQuery.me().findBySlugAndModule(m.getSlug(), m.getContentModule());
		if (m.getId() != null && dbTaxonomy != null && m.getId().compareTo(dbTaxonomy.getId()) != 0) {
			renderAjaxResultForError("别名已经存在！");
			return;
		}

		if (m.saveOrUpdate()) {

			boolean addToMenu = getParaToBoolean("addToMenu", false);
			if (addToMenu) {
				Content content = ContentQuery.me().findFirstByModuleAndObjectId(Consts.MODULE_MENU, m.getId());
				if (content == null) {
					content = new Content();
					content.setModule(Consts.MODULE_MENU);
				}

				content.setOrderNumber(m.getOrderNumber().longValue());
				content.setTitle(m.getTitle());
				content.setObjectId(m.getId());
				content.setText(m.getUrl());
				content.setThumbnail(m.getIcon());
				content.setStatus(Content.STATUS_NORMAL);
				content.saveOrUpdate();

			} else {
				Content content = ContentQuery.me().findFirstByModuleAndObjectId(Consts.MODULE_MENU, m.getId());
				if (content != null) {
					content.delete();
				}
			}

			m.updateContentCount();

		}
		renderAjaxResultForSuccess("ok");
	}

	@Before(UCodeInterceptor.class)
	public void delete() {
		final BigInteger id = getParaToBigInteger("id");
		if (id == null) {
			renderAjaxResultForError();
			return;
		}

		final Taxonomy taxonomy=TaxonomyQuery.me().findById(id);

		boolean deleted = Db.tx(new IAtom() {
			@Override
			public boolean run() throws SQLException {
				if (taxonomy.delete()) {
					MappingQuery.me().deleteByTaxonomyId(id);

					Content content = ContentQuery.me().findFirstByModuleAndObjectId(Consts.MODULE_MENU, id);
					if (content != null) {
						content.delete();
					}

					List<Taxonomy> list=TaxonomyQuery.me().findListByModuleAndType(null, null, id, null, null);
					if(list != null && list.size() > 0){
						for(Taxonomy t:list){
							t.setParentId(null);
							t.saveOrUpdate();
						}
					}

					MetaDataQuery.me().deleteByTypeAndId(Taxonomy.METADATA_TYPE, id);
					MetaDataQuery.me().deleteByTypeAndId(Taxonomy.METADATA_TYPE_BANNER, id);

					return true;
				}

				return false;
			}
		});

		if (deleted) {
			renderAjaxResultForSuccess();
		} else {
			renderAjaxResultForError();
		}
	}

	public void setting() {
		String moduleName = getContentModule();
		TplModule module = TemplateManager.me().currentTemplateModule(moduleName);
		TplTaxonomyType type = module.getTaxonomyTypeByType(getType());

		final BigInteger id = getParaToBigInteger("id");
		Taxonomy taxonomy = TaxonomyQuery.me().findById(id);
		setAttr("taxonomy", taxonomy);
		setAttr("type", type);

		String templateHtml = String.format("admin_taxonomy_setting_%s_%s.html", moduleName, getType());
		for (int i = 0; i < 3; i++) {
			if (TemplateManager.me().existsFile(templateHtml)) {
				setAttr("include", TemplateManager.me().currentTemplatePath() + "/" + templateHtml);
				return;
			}
			templateHtml = templateHtml.substring(0, templateHtml.lastIndexOf("_")) + ".html";
		}
		setAttr("include", "_setting_include.html");
	}

	@Before(UCodeInterceptor.class)
	public void doSaveSettings() {

		final Map<String, String> metas = getMetas();
		final Taxonomy taxonomy = getModel(Taxonomy.class);

		boolean saved = Db.tx(new IAtom() {
			@Override
			public boolean run() throws SQLException {
				if (!taxonomy.saveOrUpdate()) {
					return false;
				}

				if (metas != null) {
					for (Map.Entry<String, String> entry : metas.entrySet()) {
						taxonomy.saveOrUpdateMetadta(Taxonomy.METADATA_TYPE, entry.getKey(), entry.getValue());
					}
				}

				return true;
			}
		});

		if (saved) {
			renderAjaxResultForSuccess();
		} else {
			renderAjaxResultForError();
		}

	}

	public void banner() {
		String moduleName = getContentModule();

		final BigInteger id = getParaToBigInteger("id");
		Taxonomy taxonomy = TaxonomyQuery.me().findById(id);
		setAttr("taxonomy", taxonomy);

		List<Metadata> metadataList = MetaDataQuery.me().findListByTypeAndId(Taxonomy.METADATA_TYPE_BANNER, id);
		setAttr("metadataSize", metadataList.size());

		String templateHtml = String.format("admin_taxonomy_banner_%s_%s.html", moduleName, getType());
		for (int i = 0; i < 3; i++) {
			if (TemplateManager.me().existsFile(templateHtml)) {
				setAttr("include", TemplateManager.me().currentTemplatePath() + "/" + templateHtml);
				return;
			}
			templateHtml = templateHtml.substring(0, templateHtml.lastIndexOf("_")) + ".html";
		}
		setAttr("include", "_banner_include.html");
	}

	protected boolean validate(Map<String, String> metas){
		for(int x = 0 ; x < metas.size() ; x++) {
			if (metas.get("taxonomy_banner" + x + "_img") == null) break;
			if (metas.get("taxonomy_banner" + x + "_img").isEmpty()) {
				renderAjaxResultForError("banner图不能为空！");
				return false;
			}
			if (metas.get("taxonomy_banner" + x + "_remark") == null) break;
			if (metas.get("taxonomy_banner" + x + "_remark").isEmpty()) {
				renderAjaxResultForError("banner图描述不能为空！");
				return false;
			}
		}

		return true;
	}

	@Before(UCodeInterceptor.class)
	public void doClearBanners() {
		BigInteger id=getParaToBigInteger("id");
		if(id!=null){
			MetaDataQuery.me().deleteByTypeAndId(Taxonomy.METADATA_TYPE_BANNER, id);
			renderAjaxResultForSuccess("操作成功！");
		}else{
			renderAjaxResultForError("操作失败！");
		}
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

		final Taxonomy taxonomy = getModel(Taxonomy.class);

		if (!validate(metas)) return;

		boolean saved = Db.tx(new IAtom() {
			@Override
			public boolean run() throws SQLException {
				//删除
				MetaDataQuery.me().deleteByTypeAndId(Taxonomy.METADATA_TYPE_BANNER, taxonomy.getId());

				//保存
				if (!metas.isEmpty()) {
					for (Map.Entry<String, String> entry : metas.entrySet()) {
						taxonomy.saveOrUpdateMetadta(Taxonomy.METADATA_TYPE_BANNER, entry.getKey(), entry.getValue());
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
