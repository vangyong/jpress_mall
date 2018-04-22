package io.jpress.admin.controller;

import com.jfinal.aop.Before;
import com.jfinal.plugin.activerecord.Page;
import io.jpress.core.JBaseCRUDController;
import io.jpress.core.interceptor.ActionCacheClearInterceptor;
import io.jpress.interceptor.UCodeInterceptor;
import io.jpress.model.Spec;
import io.jpress.model.SpecValue;
import io.jpress.model.query.ContentSpecItemQuery;
import io.jpress.model.query.SpecQuery;
import io.jpress.model.query.SpecValueQuery;
import io.jpress.router.RouterMapping;
import io.jpress.router.RouterNotAllowConvert;
import io.jpress.template.TemplateManager;
import io.jpress.utils.StringUtils;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;

/**
 * @author heguoliang
 * @Description: TODO()
 * @date 2017-6-14 15:27
 */
@RouterMapping(url = "/admin/spec", viewPath = "/WEB-INF/admin/spec")
@Before(ActionCacheClearInterceptor.class)
@RouterNotAllowConvert
public class _SpecController extends JBaseCRUDController<Spec> {

    @Override
    public void index() {
        String keyword=getPara("k", "").trim();
        Page<Spec> page= SpecQuery.me().paginate(getPageNumber(), getPageSize(), keyword, Spec.STATUS_NORMAL);
        setAttr("page", page);
        String templateHtml = "admin_spec_index.html";
        if (TemplateManager.me().existsFile(templateHtml)) {
            setAttr("include", TemplateManager.me().currentTemplatePath() + "/" + templateHtml);
            return;
        }
        setAttr("include", "_index_include.html");
    }

    @Before(UCodeInterceptor.class)
    public void delete(){
        Spec spec = SpecQuery.me().findById(getParaToBigInteger("id"));
        if(spec != null){
            spec.setStatus(Spec.STATUS_DELETE);
            if(spec.update()){
                List<SpecValue> specValueList=SpecValueQuery.me().findList(spec.getId(), SpecValue.STATUS_NORMAL);
                for(SpecValue specValue:specValueList){
                    specValue.setStatus(SpecValue.STATUS_DELETE);
                    specValue.update();
                }
                ContentSpecItemQuery.me().deleteBySpecId(spec.getId());
                renderAjaxResultForSuccess();
            }else{
                renderAjaxResultForError("删除失败!");
            }
        }else{
            renderAjaxResultForError("删除失败!");
        }
    }

    public void add(){
        setAttr("spec", "");
        render("edit.html");
    }

    public void edit(){
        Spec spec = SpecQuery.me().findById(getParaToBigInteger("id"));
        List<SpecValue> specValueList = SpecValueQuery.me().findList(spec.getId(), SpecValue.STATUS_NORMAL);
        setAttr("spec", spec);
        setAttr("specValueList", specValueList);
        render("edit.html");
    }

    protected boolean validate(Spec spec){
        if (StringUtils.isBlank(spec.getTitle())){
            renderAjaxResultForError("规格名称不能为空！");
            return false;
        }
        return true;
    }

    protected boolean validateSpecValue(String[] spec_value_value, String[] spec_value_order_number){
        for(String value:spec_value_value){
            if(StringUtils.isBlank(value)){
                renderAjaxResultForError("规格值不能为空！");
                return false;
            }
        }
        for(String order_number:spec_value_order_number){
            if(StringUtils.isBlank(order_number)){
                renderAjaxResultForError("排序不能为空！");
                return false;
            }
        }
        return true;
    }

    @Before(UCodeInterceptor.class)
    public void save(){
        String[] spec_value_value=getParaValues("spec_value_value");
        String[] spec_value_order_number=getParaValues("spec_value_order_number");
        BigInteger[] spec_value_id=getParaValuesToBigInteger("spec_value_id");

        if (!validateSpecValue(spec_value_value, spec_value_order_number)) return;

        Spec spec = getModel(Spec.class);
        spec.setStatus(Spec.STATUS_NORMAL);
        spec.setCreated(new Date());

        if (!validate(spec)) return;
        
        boolean add=spec.getId()==null?true:false;

        if(spec.saveOrUpdate()){
        	SpecValue specValue=null;
            for(int i=0;i<spec_value_value.length;i++){
            	if(add){
            		specValue=new SpecValue();
	                specValue.setSpecId(spec.getId());
	                specValue.setValue(spec_value_value[i]);
	                specValue.setOrderNumber(Integer.valueOf(spec_value_order_number[i]));
                    specValue.setStatus(SpecValue.STATUS_NORMAL);
	                specValue.setCreated(new Date());
	                specValue.save();
            	}else{
            		if(spec_value_id[i]!=null){
            			specValue=SpecValueQuery.me().findById(spec_value_id[i]);
                		if(specValue!=null){
                			specValue.setValue(spec_value_value[i]);
        	                specValue.setOrderNumber(Integer.valueOf(spec_value_order_number[i]));
        	                specValue.update();
                		}
            		}else{
            			specValue=new SpecValue();
    	                specValue.setSpecId(spec.getId());
    	                specValue.setValue(spec_value_value[i]);
    	                specValue.setOrderNumber(Integer.valueOf(spec_value_order_number[i]));
                        specValue.setStatus(SpecValue.STATUS_NORMAL);
    	                specValue.setCreated(new Date());
    	                specValue.save();
            		}     
            	}
            }
            renderAjaxResultForSuccess("保存成功!");
        }else{
            renderAjaxResultForError("保存失败!");
        }
    }

    public void getSpecValue(){
        List<SpecValue> specValueList = SpecValueQuery.me().findList(getParaToBigInteger("spec.id"), SpecValue.STATUS_NORMAL);
        renderAjaxResult("操作成功！", 0, specValueList);
    }

    public void deleteSpecValue(){
        SpecValue specValue = SpecValueQuery.me().findById(getParaToBigInteger("specValue.id"));
        if(specValue!=null){
            specValue.setStatus(SpecValue.STATUS_DELETE);
        	specValue.update();
        	renderAjaxResult("操作成功！", 0, null);
        }else{
        	renderAjaxResult("操作失败！", 1, null);
        }              
    }
    
}
