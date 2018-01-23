package io.jpress.front.controller;

import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;

import com.jfinal.aop.Before;
import com.jfinal.aop.Clear;
import com.jfinal.core.ActionKey;
import com.jfinal.upload.UploadFile;

import io.jpress.Consts;
import io.jpress.core.BaseFrontController;
import io.jpress.interceptor.UCodeInterceptor;
import io.jpress.model.User;
import io.jpress.model.query.UserAddressQuery;
import io.jpress.model.query.UserQuery;
import io.jpress.router.RouterMapping;
import io.jpress.ui.freemarker.tag.UserAddressPageTag;
import io.jpress.utils.EncryptUtils;
import io.jpress.utils.StringUtils;
import io.jpress.wechat.WechatUserInterceptor;
import io.jpress.wechat.utils.AuthJsApiUtils;

@RouterMapping(url = "/test")
//@Before(WechatUserInterceptor.class)
public class TestController extends BaseFrontController {

	public void index() {
		if (isMultipartRequest()) {
			UploadFile file = getFile();
			file.getFile().getAbsolutePath();
		}
		renderText("test upload file");
		
	}

	// 测试1
	@ActionKey(Consts.TSET1)
	public void test1() {
		//1、获取AccessToken  
		String accessToken = AuthJsApiUtils.getAccessToken("wx341e1ecdc00e388f", "8d488da99b02d61899f76e3240328139");
		
		//2、获取Ticket 
		String jsapi_ticket = AuthJsApiUtils.getTicket(accessToken);
	    
		Map<String, Object> map = AuthJsApiUtils.sign("wx341e1ecdc00e388f", jsapi_ticket, "http://192.168.0.103:8080/jpress_mall/test/test2");
		
		System.out.println(map);
		
		render("test1.html");
	}
	
	// 测试2
	@ActionKey(Consts.TSET2)
	@Before(WechatUserInterceptor.class)
	public void test2() {
//		String fromUserId=getPara("fromUserId");
//		if(StringUtils.isNotBlank(fromUserId)) {
//			User user=getLoginedUser();
//			user.setPid(BigInteger.valueOf(Long.valueOf(fromUserId)));
//		}
		render("test2.html");
	}

	// 测试3
	@ActionKey(Consts.TSET3)
	public void test3() {
		render("test/test3.html");
	}
	
	
}
