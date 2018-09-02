package io.jpress.quartz;

import java.util.ArrayList;
import java.util.List;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.jfinal.log.Log;
import com.jfinal.weixin.sdk.api.ApiResult;
import com.jfinal.weixin.sdk.api.TemplateData;
import com.jfinal.weixin.sdk.api.TemplateMsgApi;

import io.jpress.model.query.OptionQuery;

public class WeekStatisticJob implements Job {

	private static final Log log = Log.getLog(WeekStatisticJob.class);

	private static final String WECHAT_WEEK_STATISTIC_TEMPMSG_ID = OptionQuery.me().findValue("wechat_week_statistic_tempMsg_id");

	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {

		final List<TemplateData> tempMsgList = new ArrayList<TemplateData>();
		tempMsgList.add(TemplateData.New().setTouser("o-2OIxGnn9ul-XyN-rg8WrzWsv_0") // 消息接收者
				.setTemplate_id(WECHAT_WEEK_STATISTIC_TEMPMSG_ID) // 模板id
				// 模板参数
				.add("first", "本周销售统计如下:\n", "#999")
				.add("keyword1", "营业总额:"+12+"人", "#999")
				.add("keyword2", "营业收入:"+12+"人", "#999")
				.add("keyword3", "销售数量:"+12+"人", "#999")
				.add("keyword4", "销售次数:"+12+"人", "#999")
				.add("remark", "祝您周末愉快!", "#999"));

		// 推送模板消息
		for (TemplateData templateData : tempMsgList) {
			ApiResult result = TemplateMsgApi.send(templateData.build());

			log.info("用户(" + templateData.getTouser() + ")奖金到账消息[" + templateData.build() + "]第一次推送结果："
					+ result.toString());
		}

	}

}
