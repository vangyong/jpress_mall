package io.jpress.admin.controller;

import com.jfinal.aop.Before;
import com.jfinal.plugin.activerecord.Page;
import io.jpress.core.JBaseCRUDController;
import io.jpress.core.interceptor.ActionCacheClearInterceptor;
import io.jpress.model.Transaction;
import io.jpress.model.TransactionItem;
import io.jpress.model.query.TransactionItemQuery;
import io.jpress.model.query.TransactionQuery;
import io.jpress.router.RouterMapping;
import io.jpress.router.RouterNotAllowConvert;
import io.jpress.template.TemplateManager;
import io.jpress.utils.StringUtils;

import java.math.BigInteger;
import java.util.List;

/**
 * @author heguoliang
 * @Description: TODO()
 * @date 2017-6-26 16:20
 */
@RouterMapping(url = "/admin/transaction", viewPath = "/WEB-INF/admin/transaction")
@Before(ActionCacheClearInterceptor.class)
@RouterNotAllowConvert
public class _TransactionController extends JBaseCRUDController<Transaction>{

    @Override
    public void index() {
        String keyword=getPara("k", "").trim();
        String pay_type=getPara("pay_type");
        String status=getPara("status");
        setAttr("pay_type", pay_type);
        setAttr("status", status);
        Page<Transaction> page=TransactionQuery.me().paginate(getPageNumber(), getPageSize(), keyword, status, pay_type);
        setAttr("page", page);
        String templateHtml = "admin_transaction_index.html";
        if (TemplateManager.me().existsFile(templateHtml)) {
            setAttr("include", TemplateManager.me().currentTemplatePath() + "/" + templateHtml);
            return;
        }
        setAttr("include", "_index_include.html");
    }

    public void view() {
        Transaction transaction=TransactionQuery.me().findById(getParaToBigInteger("id"));
        List<TransactionItem> transactionItemList=TransactionItemQuery.me().findList(transaction.getId());
        setAttr("transaction", transaction);
        setAttr("transactionItemList", transactionItemList);
        String templateHtml = "admin_transaction_view.html";
        if (TemplateManager.me().existsFile(templateHtml)) {
            setAttr("include", TemplateManager.me().currentTemplatePath() + "/" + templateHtml);
            return;
        }
        setAttr("include", "_view_include.html");
    }

    public void getTransactionAmountOfMonth(){
        List list=TransactionQuery.me().getTransactionAmountOfMonth();
        renderAjaxResultForSuccess("操作成功", list);
    }

    //发货
    public void delivered(){
        BigInteger id=getParaToBigInteger("id");
        String express=getPara("express");
        if(id==null){
            renderAjaxResultForError("订单id不能为空");
            return;
        }
        if(StringUtils.isBlank(express)){
            renderAjaxResultForError("快递信息不能为空");
            return;
        }
        Transaction transaction=TransactionQuery.me().findById(id);
        if(transaction!=null){
            transaction.setStatus(Transaction.STATUS_3);
            transaction.setExpress(express);
            if(transaction.update()){
                renderAjaxResultForSuccess();
            }else{
                renderAjaxResultForError("操作失败");
            }
        }else{
            renderAjaxResultForError("操作失败");
        }
    }

}
