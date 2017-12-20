package io.jpress.front.controller;

import com.jfinal.upload.UploadFile;

import io.jpress.core.BaseFrontController;
import io.jpress.router.RouterMapping;

@RouterMapping(url = "/test")
public class TestController extends BaseFrontController{
	
	public void index() {
		if(isMultipartRequest()) {
			UploadFile file = getFile();
			file.getFile().getAbsolutePath();
		}
		renderText("test upload file");
		
	}

}
