package io.jpress.admin.controller;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.CellRangeAddress;

import com.jfinal.aop.Before;
import com.jfinal.kit.PropKit;
import com.jfinal.kit.StrKit;
import com.jfinal.log.Log;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.IAtom;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.upload.UploadFile;
import com.jfinal.weixin.sdk.api.ApiResult;
import com.jfinal.weixin.sdk.api.PaymentApi;
import com.jfinal.weixin.sdk.api.TemplateData;
import com.jfinal.weixin.sdk.api.TemplateMsgApi;

import io.jpress.Consts;
import io.jpress.core.JBaseCRUDController;
import io.jpress.core.interceptor.ActionCacheClearInterceptor;
import io.jpress.front.controller.WechatpayController;
import io.jpress.interceptor.UCodeInterceptor;
import io.jpress.model.Refund;
import io.jpress.model.Transaction;
import io.jpress.model.TransactionItem;
import io.jpress.model.User;
import io.jpress.model.query.OptionQuery;
import io.jpress.model.query.RefundQuery;
import io.jpress.model.query.TransactionItemQuery;
import io.jpress.model.query.TransactionQuery;
import io.jpress.model.query.UserQuery;
import io.jpress.router.RouterMapping;
import io.jpress.router.RouterNotAllowConvert;
import io.jpress.template.TemplateManager;
import io.jpress.utils.DateUtils;
import io.jpress.utils.ExcelKit;
import io.jpress.utils.RandomUtils;
import io.jpress.utils.StringUtils;
import io.jpress.wechat.WechatApiConfigInterceptor;

/**
 * @author heguoliang
 * @Description: TODO()
 * @date 2017-6-26 16:20
 */
@RouterMapping(url = "/admin/transaction", viewPath = "/WEB-INF/admin/transaction")
@Before(ActionCacheClearInterceptor.class)
@RouterNotAllowConvert
public class _TransactionController extends JBaseCRUDController<Transaction>{

    private static final Log log = Log.getLog(_TransactionController.class);
    
    private static final String SUCCESS = "SUCCESS";
    private static final String EXPRESS_NOTICE_TEMP_ID = OptionQuery.me().findValue("wechat_waybill_tempMsg_id");
    private static final String appid = OptionQuery.me().findValue("wechat_appid");
    private static final String partner = OptionQuery.me().findValue("wechat_partner");
    private static final String paternerKey = OptionQuery.me().findValue("wechat_paternerKey");
    private static final String refundNotifyUrl = PropKit.get("wechat_refund_notify_url");
    
    String web_domain = OptionQuery.me().findValue("web_domain");
    
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

    //退款申请-审核
    public void checkRefund() {
        try {
            final BigInteger id=getParaToBigInteger("id");
            final String checkResult=getPara("checkResult");

            if (id==null) {
                renderAjaxResultForError("订单id不能为空");
                return;
            }
            if (StringUtils.isBlank(checkResult)) {
                renderAjaxResultForError("退款审核理由不能为空");
                return;
            }
            final Transaction transaction=TransactionQuery.me().findById(id);
            if (transaction==null) {
                renderAjaxResultForError("订单不存在");
                return;
            }
            Refund refund = RefundQuery.me().findByOrderNo(transaction.getOrderNo());
            if (refund == null) {
                renderAjaxResultForError("退款单不存在");
                return;
            }
            
            if (!"退款申请成功".equals(refund.getStatus()) && !"退款失败".equals(refund.getStatus())) {
                renderAjaxResultForError("退款进行中，不允许重复退款");
                return;
            }
            
            if ("ok".equals(checkResult)) { //审核通过。
                String jsonStr = wechartPreRefund(transaction, refund.getRefundNo(), refund.getDesc());
                if (StringUtils.isBlank(jsonStr)) {
                    renderAjaxResultForError("调起微信退款申请接口失败了");
                    return;
                }
                log.info(jsonStr);
                renderAjaxResult("操作成功", 0, jsonStr);
            } else {
                refund.setStatus("退款失败");
                refund.setDesc(refund.getDesc() + "【审核失败："+checkResult+"】");
                refund.saveOrUpdate();
                renderAjaxResult("操作成功", 0, refund.getDesc());
            }
            
        } catch (Exception e) {
            log.error("系统异常:",e);
            renderAjaxResult("系统异常", Consts.ERROR_CODE_SYSTEM_ERROR, null);
        }
    }

    //退款申请-微信
    public void prerefund() {
        try {
            final BigInteger id=getParaToBigInteger("id");
            final String refundDesc=getPara("refundDesc");

            if (id==null) {
                renderAjaxResultForError("订单id不能为空");
                return;
            }
            if (StringUtils.isBlank(refundDesc)) {
                renderAjaxResultForError("退款原因不能为空");
                return;
            }
            final Transaction transaction=TransactionQuery.me().findById(id);
            if (transaction==null) {
                renderAjaxResultForError("订单不存在");
                return;
            }
            
            if (DateUtils.getDayDiff(new Date(), transaction.getPayed()) > 5) { //订单支付超过5天后不允许退款
                renderAjaxResultForError("订单支付超过5天后不允许退款，不允许退款");
                return;
            }
            //一个订单只允许创建一个退款单
            Refund refund = RefundQuery.me().findByOrderNo(transaction.getOrderNo());
            if (refund == null) {
                refund = new Refund();
                refund.setAmount(transaction.getCashFee());//退款金额默认为订单总的现金支付金额（注意：如果退款成功，那么订单产生的奖金就是此笔订单亏损的钱）
                refund.setOrderNo(transaction.getOrderNo());
                refund.setRefundNo(RandomUtils.randomKey("2", transaction.getId().toString()));//退款单编号以2开头，以订单id结尾
                refund.setStatus("退款申请成功");//退款单初始状态为'退款申请成功'
                refund.setTradeNo(transaction.getTradeNo());
            }
            
            if (!"退款申请成功".equals(refund.getStatus()) && !"退款失败".equals(refund.getStatus())) {
                renderAjaxResultForError("退款进行中，不允许重复退款");
                return;
            }
            refund.setDesc(refundDesc);
            
            if(!refund.saveOrUpdate()){
                renderAjaxResultForError("退款单创建失败了");
                return;
            }
            
            String jsonStr = wechartPreRefund(transaction, refund.getRefundNo(), refundDesc);
            if (StringUtils.isBlank(jsonStr)) {
                renderAjaxResultForError("调起微信退款申请接口失败了");
                return;
            }
            refund.setStatus("退款中");
            refund.saveOrUpdate();
            
            log.info(jsonStr);
            renderAjaxResult("操作成功", 0, jsonStr);
        } catch (Exception e) {
            log.error("系统异常:",e);
            renderAjaxResult("系统异常", Consts.ERROR_CODE_SYSTEM_ERROR, null);
        }
    }
    
    private String wechartPreRefund(Transaction transaction,String refundNo, String refundDesc) {
            String returnMsg = "退款申请成功，款项将2天内返回用户支付账户";
            // 退款申请文档地址：https://pay.weixin.qq.com/wiki/doc/api/jsapi.php?chapter=9_4
            Map<String, String> params = new HashMap<>();
            params.put("appid", appid);
            params.put("mch_id", partner);
            params.put("out_trade_no", transaction.getOrderNo());
            params.put("out_refund_no", refundNo);
            String totalFee = Integer.toString(transaction.getCashFee().setScale(2, BigDecimal.ROUND_DOWN).multiply(BigDecimal.valueOf(100.00)).intValue());
            params.put("total_fee", totalFee);
            params.put("refund_fee", totalFee);
            params.put("refund_desc", refundDesc);
            params.put("notify_url", refundNotifyUrl);
            String certPath = getSession().getServletContext().getRealPath("/") +"apiclient_cert.p12";
            log.info("微信退款申请输入参数：" + params + "。证书路径：" + certPath);
            // 申请退款，内部添加了随机字符串nonce_str和签名sign
            Map<String, String> result = PaymentApi.refund(params, paternerKey, certPath);
            
            log.info("微信退款申请返回参数：" + result);
            
            String returnCode = result.get("return_code");
            returnMsg = result.get("return_msg");
            if (StrKit.isBlank(returnCode) || !SUCCESS.equals(returnCode)) {
                log.error(returnMsg);
                renderAjaxResult("系统异常", Consts.ERROR_CODE_SYSTEM_ERROR, null);
                return "";
            }
            String resultCode = result.get("result_code");
            if (StrKit.isBlank(resultCode) || !SUCCESS.equals(resultCode)) {
                log.error(returnMsg);
                renderAjaxResult("系统异常", Consts.ERROR_CODE_SYSTEM_ERROR, null);
                return "";
            }
        
        return returnMsg;
    }
    
    
    public void exportExcel(){
        Date now = new Date();
        String fileName = "用户交易记录_"+ DateUtils.format(now, "yyyyMMddHHmmss") +".xls";
        try {
            fileName = new String(fileName.getBytes("utf-8"), "iso8859-1");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }

        HttpServletResponse response = getResponse();
        response.reset();
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        OutputStream output = null;
        try {
            output = response.getOutputStream();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output);

        HSSFWorkbook wb = new HSSFWorkbook();
        // 创建单元格样式
        HSSFCellStyle cellStyleTitle = wb.createCellStyle();
        // 指定单元格居中对齐
        cellStyleTitle.setAlignment(HSSFCellStyle.ALIGN_CENTER);
        // 指定单元格垂直居中对齐
        cellStyleTitle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
        // 指定当单元格内容显示不下时自动换行
        cellStyleTitle.setWrapText(true);
        // ------------------------------------------------------------------
        HSSFCellStyle cellStyle = wb.createCellStyle();
        // 指定单元格居中对齐
        cellStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER);
        // 指定单元格垂直居中对齐
        cellStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
        // 指定当单元格内容显示不下时自动换行
        cellStyle.setWrapText(true);
        // ------------------------------------------------------------------
        // 设置单元格字体
        HSSFFont font = wb.createFont();
        font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
        font.setFontName("宋体");
        font.setFontHeight((short) 250);
        cellStyleTitle.setFont(font);
        // ------------------------------------------------------------------
        HSSFSheet sheet = wb.createSheet();  
        sheet.setDefaultRowHeightInPoints(25);
        sheet.setColumnWidth(0, 280 * 20);
        sheet.setColumnWidth(1, 230 * 30);
        sheet.setColumnWidth(2, 100 * 50);
        sheet.setColumnWidth(3, 100 * 50);
        sheet.setColumnWidth(4, 156 * 30);
        sheet.setColumnWidth(5, 250 * 30);
        sheet.setColumnWidth(6, 256 * 30);
        sheet.setColumnWidth(7, 256 * 30);
        sheet.setColumnWidth(8, 500 * 30);

        // 创建报表头部  
        HSSFRow row0 = sheet.createRow(0);
        //合并单元格
        sheet.addMergedRegion(new CellRangeAddress(0, 0, (short) 0, (short) 3)); //参数1：起始行 参数2：终止行 参数3：起始列 参数4：终止列   
        sheet.addMergedRegion(new CellRangeAddress(0, 0, (short) 4, (short) 5)); //参数1：起始行 参数2：终止行 参数3：起始列 参数4：终止列   
        HSSFCell cell0 = row0.createCell(0);
        cell0.setCellStyle(cellStyleTitle);  
        cell0.setCellValue(new HSSFRichTextString("用户交易记录表")); 

        cell0 = row0.createCell(4);
        cell0.setCellStyle(cellStyleTitle);  
        cell0.setCellValue(new HSSFRichTextString("导出时间：" + DateUtils.format(now))); 
        // 定义第一行  
        HSSFRow row1 = sheet.createRow(1); 

        HSSFCell cell1 = row1.createCell(0);  
        //第一行第一列    
        cell1.setCellStyle(cellStyleTitle);  
        cell1.setCellValue(new HSSFRichTextString("本地订单号"));  
        //第一行第二列  
        cell1 = row1.createCell(1);  
        cell1.setCellStyle(cellStyleTitle);  
        cell1.setCellValue(new HSSFRichTextString("微信/支付宝订单号"));  
        //第一行第三列  
        cell1 = row1.createCell(2);  
        cell1.setCellStyle(cellStyleTitle);  
        cell1.setCellValue(new HSSFRichTextString("总金额"));   
        //第一行第四列  
        cell1 = row1.createCell(3);  
        cell1.setCellStyle(cellStyleTitle);  
        cell1.setCellValue(new HSSFRichTextString("支付类型"));      
        //第一行第5列  
        cell1 = row1.createCell(4);  
        cell1.setCellStyle(cellStyleTitle);  
        cell1.setCellValue(new HSSFRichTextString("订单状态"));      
        //第一行第6列  
        cell1 = row1.createCell(5);  
        cell1.setCellStyle(cellStyleTitle);  
        cell1.setCellValue(new HSSFRichTextString("创建时间"));      
        //第一行第7列  
        cell1 = row1.createCell(6);  
        cell1.setCellStyle(cellStyleTitle);  
        cell1.setCellValue(new HSSFRichTextString("快递单号"));      
        //第一行第8列  
        cell1 = row1.createCell(7);  
        cell1.setCellStyle(cellStyleTitle);  
        cell1.setCellValue(new HSSFRichTextString("快递信息（快递公司等）"));
        //第一行第9列  
        cell1 = row1.createCell(8);  
        cell1.setCellStyle(cellStyleTitle);  
        cell1.setCellValue(new HSSFRichTextString("收货地址"));   

        String keyword=getPara("k", "").trim();
        String pay_type=getPara("pay_type");
        String status=getPara("status");
        setAttr("pay_type", pay_type);
        setAttr("status", status);
        Page<Transaction> page=TransactionQuery.me().paginate(1, Integer.MAX_VALUE, keyword, status, pay_type);
        List<Transaction> list = page.getList();
        
        for(int x=0;x<list.size();x++){
           Transaction z = list.get(x);
           HSSFRow row = sheet.createRow(x + 2); 
          
           HSSFCell cell = row.createCell(0);  
            cell.setCellStyle(cellStyle);  
            cell.setCellValue(new HSSFRichTextString(z.getOrderNo()));          
            
            cell = row.createCell(1);  
            cell.setCellStyle(cellStyle);  
            cell.setCellValue(new HSSFRichTextString(z.getTradeNo()));               
            
            cell = row.createCell(2);  
            cell.setCellStyle(cellStyle);  
            cell.setCellValue(new HSSFRichTextString(z.getTotleFee().setScale(2, BigDecimal.ROUND_DOWN).toString()));    
            
            cell = row.createCell(3);  
            cell.setCellStyle(cellStyle);  
            String payStr = "";
            String payType = z.getPayType();
            if ("alipay".equals(payType)) {
                payStr = "支付宝支付";
            } else if ("wechatpay".equals(payType)) {
                payStr = "微信支付";
            }
            cell.setCellValue(new HSSFRichTextString(payStr)); 
            
            cell = row.createCell(4);  
            cell.setCellStyle(cellStyle); 
            String stausStr = "";
            String staus = z.getStatus();
            if ("0".equals(staus)) {
                stausStr = "支付失败";
            } else if ("1".equals(staus)) {
                stausStr = "待支付";
            } else if ("2".equals(staus)) {
                stausStr = "已支付/待发货";
            } else if ("3".equals(staus)) {
                stausStr = "已发货/待收货";
            } else if ("4".equals(staus)) {
                stausStr = "已收货/待评价";
            } else if ("5".equals(staus)) {
                stausStr = "完成";
            }
            cell.setCellValue(new HSSFRichTextString(stausStr)); 
            
            cell = row.createCell(5);  
            cell.setCellStyle(cellStyle);  
            cell.setCellValue(new HSSFRichTextString(DateUtils.format(z.getCreated())));    
            
            cell = row.createCell(8);  
            cell.setCellStyle(cellStyle);  
            cell.setCellValue(new HSSFRichTextString(z.getUserAddress()));       
        }
        try {  
            bufferedOutPut.flush();  
            wb.write(bufferedOutPut);  
            bufferedOutPut.close();  
        } catch (IOException e) {  
            e.printStackTrace();
        }   

        renderNull();
    }

    @Before({WechatApiConfigInterceptor.class})
    public void importExcel(){
        final List<TemplateData> tempMsgList = new ArrayList<TemplateData>();
        boolean flag=Db.tx(new IAtom() {
            boolean save_flag =true;
            @Override
            public boolean run() throws SQLException {
                try {
                    UploadFile up = getFile("filePath");
                    List<String[]> list = ExcelKit.getExcelData(up.getFile());
                    for (int i = 2; i < list.size(); i ++) { //只读取从第三行开始的数据，应为前两行为表头和标题
                        String[] strings = list.get(i);
                        if (strings.length > 7 && strings[6] != null && !"".equals(strings[6])) {
                            String orderNo = strings[0];
                            String expressNo = strings[6];
                            String expressInfo = strings[7];
                            if (StringUtils.isNotBlank(orderNo) && StringUtils.isNotBlank(expressNo)) {
                                Transaction transaction = TransactionQuery.me().findByOrderNo(orderNo);
                                if (transaction != null) {
                                    String oldExpressNo = transaction.getExpressNo();
                                    String oldStatus = transaction.getStatus();
                                    transaction.setExpressNo(expressNo);  
                                    transaction.setExpress(expressInfo);
                                    transaction.setStatus("3"); //订单状态修改为已经发货
                                    if(transaction.update()) {
                                        if (StringUtils.isBlank(oldExpressNo) && "2".equals(oldStatus)) {//已经导入过快递信息的订单不再发送消息通知
                                            User user = UserQuery.me().findById(transaction.getUserId());
                                            if (user != null) {
                                                tempMsgList.add(TemplateData.New()
                                                        .setTouser(user.getOpenid()) // 消息接收者
                                                        .setTemplate_id(EXPRESS_NOTICE_TEMP_ID) //模板id
                                                        .setTopcolor("#eb414a")
                                                        .setUrl(web_domain + "/user/userTransactionItem?id=" + transaction.getId()) //消息链接地址，此为个人订单详情页面
                                                        // 模板参数
                                                        .add("first", "订单发货通知！\n", "#999")
                                                        .add("keyword1", "语味订单["+transaction.getOrderNo()+"]", "#999")
                                                        .add("keyword2", "已发货", "#999")
                                                        .add("keyword3", transaction.getExpress(), "#999")
                                                        .add("keyword4", transaction.getExpressNo(), "#999")
                                                        .add("remark", "客官，您购买的宝贝已经发货啦！^_^", "#999")
                                                        ); 
                                            }
                                        }
                                    } else {
                                        save_flag = false;
                                        return save_flag;
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    save_flag = false;
                    e.printStackTrace();
                }
                return save_flag;
            }
        });
        if(flag){
        	List<TemplateData> tempMsgFailList = new ArrayList<>();
            //推送模板消息
            for (TemplateData templateData : tempMsgList) {
                ApiResult result = TemplateMsgApi.send(templateData.build());
                if (result.getErrorCode() == 40001) { //token失效
                	tempMsgFailList.add(templateData);
                }
                log.info("用户("+templateData.getTouser()+")订单发货消息["+templateData.build()+"]第一次推送结果：" + result.toString());
            }
            
            if (!tempMsgFailList.isEmpty()) {
            	for (TemplateData templateData : tempMsgFailList) {
            		ApiResult result = TemplateMsgApi.send(templateData.build());
                    log.info("用户("+templateData.getTouser()+")订单发货消息["+templateData.build()+"]第二次推送结果：" + result.toString());
				}
            }
            renderAjaxResult("操作成功", 0);
        }else{
            log.error("导入快递信息数据失败了!!");
            renderAjaxResultForError("操作失败");
        }
    }
}
