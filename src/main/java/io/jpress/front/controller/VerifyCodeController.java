package io.jpress.front.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;

import org.json.JSONException;

import com.github.qcloudsms.httpclient.HTTPException;
import com.jfinal.aop.Before;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.IAtom;
import io.jpress.Consts;
import io.jpress.core.BaseFrontController;
import io.jpress.model.VerifyCode;
import io.jpress.model.query.VerifyCodeQuery;
import io.jpress.router.RouterMapping;
import io.jpress.utils.RandomUtils;
import io.jpress.utils.StringUtils;
import io.jpress.wechat.WechatUserInterceptor;
import io.jpress.wechat.utils.QcloudSmsUtil;

@RouterMapping(url = Consts.ROUTER_VERIFYCODE)
//@Before(UserInterceptor.class)
@Before(WechatUserInterceptor.class)
public class VerifyCodeController extends BaseFrontController {

	//获取验证码
	public void getVerifyCode(){
		final String telephone = getPara("telephone");
		final VerifyCode verifyCode = new VerifyCode();
		verifyCode.setTelephone(telephone);
		verifyCode.setStatus(VerifyCode.STATUS_UNVERIFY);
		verifyCode.setCreatedTime(new Date());
		verifyCode.setCode(String.valueOf(RandomUtils.randomSixNum()));
		
		boolean saved = Db.tx(new IAtom() {
			@Override
			public boolean run() throws SQLException {
				boolean flag = verifyCode.saveOrUpdate();
				if(flag) {
					//调用短信接口发送短信
					String content = "尊敬的客户你好,你获取的验证码为:"+verifyCode.getCode()+"【语味果业】";
					try {
						QcloudSmsUtil.sendSingle(telephone, content);
					} catch (JSONException | HTTPException | IOException e) {
						e.printStackTrace();
						throw new SQLException();
					}
				}
				return flag;
			}
		});

		if (saved) {
			renderAjaxResultForSuccess("ok", verifyCode);
		} else {
			renderAjaxResultForError();
		}
	}
	
	//验证验证码正确性
	public void checkVerifyCode() {
		String telephone = getPara("telephone");
		String verifyCode = getPara("verifyCode");
		if (StringUtils.isBlank(telephone)) {
			renderAjaxResultForError("手机号不能为空。");
			return;
		}
		if (StringUtils.isBlank(verifyCode)) {
			renderAjaxResultForError("验证码不能为空。");
			return;
		}
		VerifyCode verifyCodeObj = VerifyCodeQuery.me().checkVerifyCode(telephone,verifyCode);
		
		if (verifyCodeObj!=null) {
			renderAjaxResultForSuccess();
		} else {
			renderAjaxResultForError();
		}
	}
	
}
