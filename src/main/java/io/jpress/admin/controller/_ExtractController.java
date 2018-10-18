package io.jpress.admin.controller;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.jfinal.aop.Before;
import com.jfinal.log.Log;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.IAtom;
import com.jfinal.plugin.activerecord.Page;

import io.jpress.Consts;
import io.jpress.core.JBaseCRUDController;
import io.jpress.core.interceptor.ActionCacheClearInterceptor;
import io.jpress.model.Extract;
import io.jpress.model.ExtractPay;
import io.jpress.model.User;
import io.jpress.model.query.ExtractPayQuery;
import io.jpress.model.query.ExtractQuery;
import io.jpress.model.query.OptionQuery;
import io.jpress.model.query.UserQuery;
import io.jpress.router.RouterMapping;
import io.jpress.router.RouterNotAllowConvert;
import io.jpress.template.TemplateManager;
import io.jpress.wechat.comms.WechartConsts;
import io.jpress.wechat.utils.CollectionUtil;
import io.jpress.wechat.utils.HttpUtil;
import io.jpress.wechat.utils.PayUtil;
import io.jpress.wechat.utils.XmlUtil;

@RouterMapping(url = "/admin/extract", viewPath = "/WEB-INF/admin/extract")
@Before(ActionCacheClearInterceptor.class)
@RouterNotAllowConvert
public class _ExtractController extends JBaseCRUDController<Extract> {
	
	private static final Log log = Log.getLog(_ExtractController.class);
	
	private static final String TRANSFERS_PAY = "https://api.mch.weixin.qq.com/mmpaymkttransfers/promotion/transfers"; // 企业付款

	private static final String TRANSFERS_PAY_QUERY = "https://api.mch.weixin.qq.com/mmpaymkttransfers/gettransferinfo"; // 企业付款查询

	private static final String APP_ID = OptionQuery.me().findValue(Consts.WECHAT_APPID);
	private static final String MCH_ID = OptionQuery.me().findValue(Consts.WECHAT_PAY_MCHID);
	private static final String MCH_SECRET = OptionQuery.me().findValue(Consts.WECHAT_MCH_SECRET);
	
	//先测试
//	private static final String APP_ID = "wxd0b33231fc543b7b";
//	private static final String MCH_ID = "1337083401";
//	private static final String MCH_SECRET = "yuweiguoye2018opentmallbanzhangA";
	

	@Override
	public void index() {
		String keyword=getPara("k", "").trim();
        String status=getPara("status");
        setAttr("status", status);
        Page<Extract> page=ExtractQuery.me().paginate(getPageNumber(), getPageSize(), keyword, status);
        setAttr("page", page);
        String templateHtml = "admin_extract_index.html";
        if (TemplateManager.me().existsFile(templateHtml)) {
            setAttr("include", TemplateManager.me().currentTemplatePath() + "/" + templateHtml);
            return;
        }
        setAttr("include", "_index_include.html");
	}
	
	public void changeStatus(){
		BigInteger id = getParaToBigInteger("id");
		String status = getPara("status");
		String desc=getPara("desc");
		Extract extract = ExtractQuery.me().findById(id);
		if(extract != null){
			extract.setStatus(status);
			extract.setRemark(desc);
			extract.saveOrUpdate();
			renderAjaxResultForSuccess("操作成功！");
		}else{
			renderAjaxResultForError("操作失败！");
		}
	}

	@Override
	protected int getPageSize() {
		return 18;
	}
	
	public void view() {
	 	BigInteger id = getParaToBigInteger("id");
		if (id != null) {
			Extract extract = ExtractQuery.me().findById(id);
			
			if(extract!=null) {
				BigInteger userId = extract.getUserId();
				User user = UserQuery.me().findById(userId);
				setAttr("user", user);
				
				//支付信息
				ExtractPay extractPay = ExtractPayQuery.me().findByExtractId(id);
				setAttr("extractPay", extractPay);
			}
			setAttr("extract", extract);
		}
		String templateHtml = "admin_extract_view.html";
		if (TemplateManager.me().existsFile(templateHtml)) {
			setAttr("include", TemplateManager.me().currentTemplatePath() + "/" + templateHtml);
			return;
		}
		setAttr("include", "_view_include.html");
	 }
	
	//支付页面
	public void pay() {
	 	BigInteger id = getParaToBigInteger("id");
		if (id != null) {
			Extract extract = ExtractQuery.me().findById(id);
			if(extract!=null) {
				setAttr("user", UserQuery.me().findById(extract.getUserId()));
				setAttr("extract", extract);
			}
		}
		String templateHtml = "admin_extract_pay.html";
		if (TemplateManager.me().existsFile(templateHtml)) {
			setAttr("include", TemplateManager.me().currentTemplatePath() + "/" + templateHtml);
			return;
		}
		setAttr("include", "_pay_include.html");
	 }
	
	//支付
	public void paymoney() {
		final ExtractPay extractPay = getModel(ExtractPay.class);
		final Extract extract = ExtractQuery.me().findById(extractPay.getExtractId());
		boolean saved = Db.tx(new IAtom() {
			@Override
			public boolean run() throws SQLException {
				if (Db.update("update jp_extract set status = ? where status = 1 and id = ?", Extract.STATUS_PAYED, extract.getId()) <= 0) {
					log.info("次提现申请已经支付过了。。。");
	                return false;
	            }
				//本地保存支付明细,更新提现申请支付状态
//				extract.setPayedMoney(extract.getPayedMoney()==null ? new BigDecimal("0").add(extractPay.getPayMoney()) : extract.getPayedMoney().add(extractPay.getPayMoney()));
			    extract.setPayedMoney(extractPay.getPayMoney());
				extract.setPayedTime(new Date());
				extract.setStatus(Extract.STATUS_PAYED);
				extractPay.saveOrUpdate();
				extract.saveOrUpdate();
				
				//扣减用户账户余额
				User user = UserQuery.me().findById(extract.getUserId());
				if (Db.update("update jp_user set amount = amount - ? where id = ?", extractPay.getPayMoney(), user.getId()) <= 0) {
	                return false;
	            }
				
				if(user!=null&&user.getOpenid()!=null) {
					String partner_trade_no = String.valueOf(extractPay.getId());
					String openid = user.getOpenid();
					String re_user_name = user.getRealname();
					int amount = (extractPay.getPayMoney().multiply(new BigDecimal(100))).intValue();
					
					log.info("调用支付接口");
					Map<String, String> restmap = null;
					try {
						Map<String, Object> parm = new HashMap<String, Object>();
						parm.put("mch_appid", APP_ID); //公众账号appid
						parm.put("mchid", MCH_ID); //商户号
						parm.put("nonce_str", PayUtil.getNonceStr()); //随机字符串
						parm.put("partner_trade_no", partner_trade_no); //商户订单号
						parm.put("openid", openid); //用户openid	
						parm.put("check_name", WechartConsts.CHECK_NAME_FORCE_CHECK); //校验用户姓名选项 OPTION_CHECK
						parm.put("re_user_name", re_user_name); //check_name设置为FORCE_CHECK或OPTION_CHECK，则必填
						parm.put("amount", amount); //转账金额
						parm.put("desc", "奖励提现"); //企业付款描述信息
						parm.put("spbill_create_ip", OptionQuery.me().findValue(Consts.WECHAT_PAY_SPBILL_CREATE_IP)); //Ip地址
						parm.put("sign", PayUtil.getSign(parm, MCH_SECRET));
						String restxml = HttpUtil.posts(TRANSFERS_PAY, XmlUtil.xmlFormat(parm, false));
						restmap = XmlUtil.xmlParse(restxml);
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
					
					if (CollectionUtil.isNotEmpty(restmap) && WechartConsts.RESULT_CODE_SUCCESS.equals(restmap.get("result_code"))) {
						log.info("转账成功");
						Map<String, String> transferMap = new HashMap<>();
						transferMap.put("partner_trade_no", restmap.get("partner_trade_no"));//商户转账订单号
						transferMap.put("payment_no", restmap.get("payment_no")); //微信订单号
						transferMap.put("payment_time", restmap.get("payment_time")); //微信支付成功时间
						
					}else {
						if (CollectionUtil.isNotEmpty(restmap) && WechartConsts.RESULT_CODE_FAIL.equals(restmap.get("result_code"))) {
							log.info("转账失败：" + restmap.get("err_code") + ":" + restmap.get("err_code_des"));
							throw new  SQLException();
						}
					}
				}else {
					return false;
				}
				return true;
			}
		});

		if (saved) {
			renderAjaxResultForSuccess();
		} else {
			renderAjaxResultForError();
		}
		renderAjaxResultForSuccess("ok");
	}
	
}