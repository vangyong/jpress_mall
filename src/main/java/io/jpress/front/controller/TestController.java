package io.jpress.front.controller;

import com.jfinal.aop.Before;
import com.jfinal.aop.Clear;
import com.jfinal.core.ActionKey;
import com.jfinal.upload.UploadFile;

import io.jpress.Consts;
import io.jpress.core.BaseFrontController;
import io.jpress.router.RouterMapping;
import io.jpress.wechat.WechatUserInterceptor;

@RouterMapping(url = "/test")
public class TestController extends BaseFrontController {

	public void index() {
		if (isMultipartRequest()) {
			UploadFile file = getFile();
			file.getFile().getAbsolutePath();
		}
		renderText("test upload file");

	}

	// 登录
	@Before(WechatUserInterceptor.class)
	@ActionKey(Consts.TSET1)
	public void test1() {
		render("test1.html");
	}

}
