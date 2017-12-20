/**
 * 
 */
package io.jpress.wechat.plugin;

import com.jfinal.log.Log;
import com.jfinal.plugin.IPlugin;
import io.jpress.model.query.OptionQuery;
import io.jpress.utils.StringUtils;
import io.jpress.wechat.utils.AuthJsApiUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**  
* @Description: TODO(微信获取JsapiTicket监听器)
* @author heguoliang  
* @date 2016年12月8日 上午10:22:01
*/
public class JsapiTicketPlugin implements IPlugin{
	
	private static Log log=Log.getLog(JsapiTicketPlugin.class);

	@Override
	public boolean start() {
		autoRegister();
		return true;
	}

	@Override
	public boolean stop() {
		return true;
	}
	
	private void autoRegister() {
		Boolean bool = OptionQuery.me().findValueAsBool("wechat_jsSign_enable");
		if(bool != null && bool){
			final String appId= OptionQuery.me().findValue("wechat_appid");
			final String appSecret= OptionQuery.me().findValue("wechat_appsecret");
			if(StringUtils.isNotBlank(appId) && StringUtils.isNotBlank(appSecret)){
				Runnable runnable=new Runnable() {		
					@Override
					public void run() {
						log.warn("JsapiTicketPlugin initAndSetTicket() on time...");
						AuthJsApiUtils.initAndSetTicket(appId, appSecret);
					}
				};
				ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();  
		        service.scheduleAtFixedRate(runnable, 1, 7000, TimeUnit.SECONDS);
			}			
		}		
	}

}
