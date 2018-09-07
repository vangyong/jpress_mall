package io.jpress.quartz;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.jfinal.log.Log;
import com.jfinal.weixin.sdk.api.ApiConfig;
import com.jfinal.weixin.sdk.api.ApiConfigKit;
import com.jfinal.weixin.sdk.api.ApiResult;
import com.jfinal.weixin.sdk.api.TemplateData;
import com.jfinal.weixin.sdk.api.TemplateMsgApi;

import io.jpress.model.query.OptionQuery;
import io.jpress.model.query.TransactionQuery;
import io.jpress.model.query.UserQuery;
import io.jpress.wechat.WechatApi;

public class DayStatisticJob implements Job {

	private static final Log log = Log.getLog(DayStatisticJob.class);

	private static final String WECHAT_DAY_STATISTIC_TEMPMSG_ID = OptionQuery.me().findValue("wechat_day_statistic_tempMsg_id");

	private static final String managerOpenId = OptionQuery.me().findValue("managerOpenId");
	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		ApiConfig ac = WechatApi.getApiConfig();
		ApiConfigKit.setThreadLocalApiConfig(ac);
		
		long totalUser = UserQuery.me().findCount("front");
		long todayUser = UserQuery.me().findCount("front",new Date());
		long todayOrder = TransactionQuery.me().findCount(new Integer("2"),new Date());
		long totalOrder = TransactionQuery.me().findCount(new Integer("2"));
		BigDecimal dodayMoney = TransactionQuery.me().findAmountOfDay(new Date());

		final List<TemplateData> tempMsgList = new ArrayList<TemplateData>();
		
		if(managerOpenId!=null) {
			String[] openIds = managerOpenId.split(",");
			if(openIds!=null&&openIds.length>0) {
				for(String openId:openIds) {
					tempMsgList.add(TemplateData.New().setTouser(openId) // 消息接收者
							.setTemplate_id(WECHAT_DAY_STATISTIC_TEMPMSG_ID) // 模板id
							// 模板参数
							.add("first", "当日销售统计如下:\n", "#999")
							.add("keyword1", "新增关注量:"+todayUser+"人", "#999")
							.add("keyword2", "总关注量:"+totalUser+"人", "#999")
							.add("keyword3", "新增订单量:"+todayOrder+"人", "#999")
							.add("keyword4", "总订单量:"+totalOrder+"单", "#999")
							.add("keyword5", "新增订单量额:"+dodayMoney+"元", "#999")
							.add("remark", "祝您健康开心每一天!", "#999"));
				}
			}
		}
		// 推送模板消息
		for (TemplateData templateData : tempMsgList) {
			ApiResult result = TemplateMsgApi.send(templateData.build());
			log.info("模版ID："+WECHAT_DAY_STATISTIC_TEMPMSG_ID);
			log.info("用户(" + templateData.getTouser() + ")结算销售统计消息[" + templateData.build() + "]推送结果："
					+ result.toString());
		}

	}

}
