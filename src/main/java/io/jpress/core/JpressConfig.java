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
package io.jpress.core;

import com.alibaba.druid.filter.stat.StatFilter;
import com.alibaba.druid.wall.WallFilter;
import com.jfinal.config.*;
import com.jfinal.core.Controller;
import com.jfinal.core.JFinal;
import com.jfinal.kit.PathKit;
import com.jfinal.kit.Prop;
import com.jfinal.kit.PropKit;
import com.jfinal.kit.StrKit;
import com.jfinal.log.Log;
import com.jfinal.plugin.activerecord.ActiveRecordPlugin;
import com.jfinal.plugin.activerecord.IDataSourceProvider;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.druid.DruidPlugin;
import com.jfinal.plugin.ehcache.EhCachePlugin;
import com.jfinal.plugin.redis.RedisPlugin;
import com.jfinal.render.ViewType;
import com.jfinal.template.Engine;

import cn.dreampie.quartz.QuartzPlugin;
import io.jpress.Consts;
import io.jpress.cache.JCachePlugin;
import io.jpress.core.cache.ActionCacheHandler;
import io.jpress.core.interceptor.HookInterceptor;
import io.jpress.core.interceptor.JI18nInterceptor;
import io.jpress.core.render.JpressRenderFactory;
import io.jpress.interceptor.AdminInterceptor;
import io.jpress.interceptor.GlobalInterceptor;
import io.jpress.message.plugin.MessagePlugin;
import io.jpress.model.core.JModelMapping;
import io.jpress.model.core.Table;
import io.jpress.plugin.search.SearcherPlugin;
import io.jpress.router.RouterMapping;
import io.jpress.utils.ClassUtils;
import io.jpress.utils.StringUtils;
import io.jpress.wechat.plugin.JsapiTicketPlugin;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import net.sf.ehcache.config.DiskStoreConfiguration;

import java.io.File;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;
import java.util.List;

public abstract class JpressConfig extends JFinalConfig {

	static Log log = Log.getLog(JpressConfig.class);

	@Override
	public void configEngine(Engine engine) {

	}

	public void configConstant(Constants constants) {

		log.info("JPress is starting ...");

		PropKit.use("jpress.properties");

		constants.setDevMode(PropKit.getBoolean("dev_mode", false));
		constants.setViewType(ViewType.FREE_MARKER);
		constants.setI18nDefaultBaseName("language");
		constants.setBaseUploadPath("attachment");
		constants.setEncoding(Consts.CHARTSET_UTF8);
		constants.setMaxPostSize(1024 * 1024 * 200);
		constants.setRenderFactory(new JpressRenderFactory());

		// constants.setTokenCache(new JTokenCache());
	}

	@SuppressWarnings("unchecked")
	public void configRoute(Routes routes) {
		List<Class<Controller>> controllerClassList = ClassUtils.scanSubClass(Controller.class);
		if (controllerClassList != null) {
			for (Class<?> clazz : controllerClassList) {
				RouterMapping urlMapping = clazz.getAnnotation(RouterMapping.class);
				if (null != urlMapping && StringUtils.isNotBlank(urlMapping.url())) {
					if (StrKit.notBlank(urlMapping.viewPath())) {
						routes.add(urlMapping.url(), (Class<? extends Controller>) clazz, urlMapping.viewPath());
					} else {
						routes.add(urlMapping.url(), (Class<? extends Controller>) clazz);
					}
				}
			}
		}
	}

	public void configPlugin(Plugins plugins) {
		plugins.add(createEhCachePlugin());

		if (Jpress.isInstalled()) {
            //redis_session缓存
//            RedisPlugin rp = new RedisPlugin("redis1", "10.121.30.62", 20881, "111111");
            RedisPlugin rp = new RedisPlugin("redis1", "127.0.0.1", 6379, "yeyeredisyeye");
            plugins.add(rp);
            
			JCachePlugin leCachePlugin = new JCachePlugin();
			plugins.add(leCachePlugin);

			DruidPlugin druidPlugin = createDruidPlugin();
			plugins.add(druidPlugin);

			ActiveRecordPlugin activeRecordPlugin = createRecordPlugin(druidPlugin);
			activeRecordPlugin.setCache(leCachePlugin.getCache());
			activeRecordPlugin.setShowSql(JFinal.me().getConstants().getDevMode());

			plugins.add(activeRecordPlugin);

			plugins.add(new SearcherPlugin());

			plugins.add(new MessagePlugin());

			/*微信js检验*/
			plugins.add(new JsapiTicketPlugin());
			
	        QuartzPlugin quartz = new QuartzPlugin();
	        quartz.setJobs("quartzJob.properties");
	        plugins.add(quartz);
		}
	}

	public EhCachePlugin createEhCachePlugin() {
		String ehcacheDiskStorePath = PathKit.getRootClassPath();
		File pathFile = new File(ehcacheDiskStorePath, ".ehcache");

		Configuration cfg = ConfigurationFactory.parseConfiguration();
		cfg.addDiskStore(new DiskStoreConfiguration().path(pathFile.getAbsolutePath()));
		return new EhCachePlugin(cfg);
	}

	public DruidPlugin createDruidPlugin() {

		Prop dbProp = PropKit.use("db.properties");
		String db_host = dbProp.get("db_host").trim();

		String db_host_port = dbProp.get("db_host_port");
		db_host_port = StringUtils.isNotBlank(db_host_port) ? db_host_port.trim() : "3306";

		String db_name = dbProp.get("db_name").trim();
		String db_user = dbProp.get("db_user").trim();
		String db_password = dbProp.get("db_password").trim();

		String jdbc_url = "jdbc:mysql://" + db_host + ":" + db_host_port + "/" + db_name + "?" + "useUnicode=true&"
				/*+ "characterEncoding=utf8&"*/ + "zeroDateTimeBehavior=convertToNull";

		DruidPlugin druidPlugin = new DruidPlugin(jdbc_url, db_user, db_password);
		int db_initialSize = dbProp.getInt("db_initialSize");
		int db_minIdle = dbProp.getInt("db_minIdle");
        int db_maxActive = dbProp.getInt("db_maxActive");
		druidPlugin.set(db_initialSize, db_minIdle, db_maxActive);  
		druidPlugin.addFilter(new StatFilter());
		
		// 3.防注入插件
	    WallFilter wall = new WallFilter();
	    wall.setDbType("mysql");
	    druidPlugin.addFilter(wall);

		return druidPlugin;
	}
	

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public ActiveRecordPlugin createRecordPlugin(IDataSourceProvider dsp) {
		ActiveRecordPlugin arPlugin = new ActiveRecordPlugin(dsp);
		List<Class<Model>> modelClassList = ClassUtils.scanSubClass(Model.class);
		if (modelClassList != null) {
			String tablePrefix = PropKit.use("db.properties").get("db_tablePrefix");
			tablePrefix = (StrKit.isBlank(tablePrefix)) ? "" : (tablePrefix.trim());
			for (Class<?> clazz : modelClassList) {
				Table tb = clazz.getAnnotation(Table.class);
				if (tb == null)
					continue;
				String tname = tablePrefix + tb.tableName();
				if (StringUtils.isNotBlank(tb.primaryKey())) {
					arPlugin.addMapping(tname, tb.primaryKey(), (Class<? extends Model<?>>) clazz);
				} else {
					arPlugin.addMapping(tname, (Class<? extends Model<?>>) clazz);
				}

				JModelMapping.me().mapping(clazz.getSimpleName().toLowerCase(), tname);
			}
		}
		return arPlugin;
	}

	public void configInterceptor(Interceptors interceptors) {
		interceptors.add(new JI18nInterceptor());
		interceptors.add(new GlobalInterceptor());
		interceptors.add(new AdminInterceptor());
		interceptors.add(new HookInterceptor());
	}

	public void configHandler(Handlers handlers) {
		handlers.add(new ActionCacheHandler());
		handlers.add(new JHandler());
		handlers.add(new ActionCacheHandler());
		MyDruidStatViewHandler druidViewHandler = new MyDruidStatViewHandler();
		handlers.add(druidViewHandler);
		//xss过滤
		handlers.add(new XssHandler("admin"));
	}

	@Override
	public void afterJFinalStart() {
		if (Jpress.isInstalled()) {
			Jpress.loadFinished();
		}

		Jpress.renderImmediately();
		onJPressStarted();

		log.info("JPress is started!");
	}

	@Override
	public void beforeJFinalStop() {
		Enumeration<Driver> drivers = DriverManager.getDrivers();
		if (drivers != null) {
			while (drivers.hasMoreElements()) {
				try {
					Driver driver = drivers.nextElement();
					DriverManager.deregisterDriver(driver);
				} catch (Exception e) {
					log.error("deregisterDriver error in beforeJFinalStop() method.", e);
				}
			}
		}
	}

	public void onJPressStarted() {
	};

}
