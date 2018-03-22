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
package io.jpress.wechat;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.IAtom;
import com.jfinal.plugin.ehcache.CacheKit;
import com.jfinal.weixin.sdk.api.ApiConfig;
import com.jfinal.weixin.sdk.api.ApiResult;
import com.jfinal.weixin.sdk.jfinal.MsgController;
import com.jfinal.weixin.sdk.msg.in.InImageMsg;
import com.jfinal.weixin.sdk.msg.in.InLinkMsg;
import com.jfinal.weixin.sdk.msg.in.InLocationMsg;
import com.jfinal.weixin.sdk.msg.in.InMsg;
import com.jfinal.weixin.sdk.msg.in.InNotDefinedMsg;
import com.jfinal.weixin.sdk.msg.in.InShortVideoMsg;
import com.jfinal.weixin.sdk.msg.in.InTextMsg;
import com.jfinal.weixin.sdk.msg.in.InVideoMsg;
import com.jfinal.weixin.sdk.msg.in.InVoiceMsg;
import com.jfinal.weixin.sdk.msg.in.event.InCustomEvent;
import com.jfinal.weixin.sdk.msg.in.event.InFollowEvent;
import com.jfinal.weixin.sdk.msg.in.event.InLocationEvent;
import com.jfinal.weixin.sdk.msg.in.event.InMassEvent;
import com.jfinal.weixin.sdk.msg.in.event.InMenuEvent;
import com.jfinal.weixin.sdk.msg.in.event.InMerChantOrderEvent;
import com.jfinal.weixin.sdk.msg.in.event.InNotDefinedEvent;
import com.jfinal.weixin.sdk.msg.in.event.InPoiCheckNotifyEvent;
import com.jfinal.weixin.sdk.msg.in.event.InQrCodeEvent;
import com.jfinal.weixin.sdk.msg.in.event.InShakearoundUserShakeEvent;
import com.jfinal.weixin.sdk.msg.in.event.InSubmitMemberCardEvent;
import com.jfinal.weixin.sdk.msg.in.event.InTemplateMsgEvent;
import com.jfinal.weixin.sdk.msg.in.event.InUpdateMemberCardEvent;
import com.jfinal.weixin.sdk.msg.in.event.InUserPayFromCardEvent;
import com.jfinal.weixin.sdk.msg.in.event.InUserViewCardEvent;
import com.jfinal.weixin.sdk.msg.in.event.InVerifyFailEvent;
import com.jfinal.weixin.sdk.msg.in.event.InVerifySuccessEvent;
import com.jfinal.weixin.sdk.msg.in.event.InWifiEvent;
import com.jfinal.weixin.sdk.msg.in.speech_recognition.InSpeechRecognitionResults;
import com.jfinal.weixin.sdk.msg.out.News;
import com.jfinal.weixin.sdk.msg.out.OutCustomMsg;
import com.jfinal.weixin.sdk.msg.out.OutMsg;
import com.jfinal.weixin.sdk.msg.out.OutNewsMsg;
import com.jfinal.weixin.sdk.msg.out.OutTextMsg;

import io.jpress.Consts;
import io.jpress.core.JSession;
import io.jpress.model.Content;
import io.jpress.model.User;
import io.jpress.model.query.ContentQuery;
import io.jpress.model.query.OptionQuery;
import io.jpress.model.query.UserQuery;
import io.jpress.router.RouterMapping;
import io.jpress.template.TemplateManager;
import io.jpress.template.TplModule;
import io.jpress.utils.CookieUtils;
import io.jpress.utils.StringUtils;

@RouterMapping(url = "/wechat")
public class WechatMessageController extends MsgController {

	private static final String UID = "uid=";

    @Override
	public ApiConfig getApiConfig() {
		return WechatApi.getApiConfig();
	}
    
	@Before(WechatApiConfigInterceptor.class)
	public void callback() {
		String gotoUrl = getPara("goto");
		String code = getPara("code");

		String appId = OptionQuery.me().findValue("wechat_appid");
		String appSecret = OptionQuery.me().findValue("wechat_appsecret");

		if (StringUtils.areNotBlank(appId, appSecret)) {
			ApiResult result = WechatApi.getOpenId(appId, appSecret, code);
			if (result != null && StringUtils.isNotBlank(result.getStr("openid"))) {
			    final String openid = result.getStr("openid");
			    ApiResult userInfo = WechatApi.getUserInfo(openid); // 获取已关注的用户信息
			    final Map<String, Object> userInfoMap = new HashMap<>();
			    copyUserInfoToMap(userInfo,userInfoMap);
			    if (userInfo.getInt("subscribe") == 0) { //表示用户未关注公众号
			        final String accessToken = result.getStr("access_token");
			        ApiResult userInfoUnsubscribe = WechatApi.getUserInfo(openid,accessToken);// 获取未关注的用户信息
			        copyUserInfoToMap(userInfoUnsubscribe,userInfoMap);
			    }
			    if (!userInfoMap.isEmpty()) {
			        final BigInteger pid = getPidByUrl(gotoUrl);//解析地址中的pid
			        
			        final Controller ctr = this;
                    boolean res = Db.tx(new IAtom() {
                        @Override
                        public boolean run() throws SQLException {
                          //获取用户后保存到数据库,jiangjb,20180110
                            User currUser = UserQuery.me().findByOpenId(openid);
                            User pUser = null;//父节点-C
                            BigInteger currUid = null;
                            if (currUser != null) {//
                                currUid = currUser.getId();
                                pUser = UserQuery.me().findById(currUser.getPid());
                            } else {
                                currUser = new User();
                            }
                            
                            boolean setPid = false;
                            if (currUid == null || currUser.getPid().compareTo(BigInteger.valueOf(0)) == 0) { //只有新增用户或者pid为0的用户才修改其pid的值
                                pUser = UserQuery.me().findById(pid);
                                if (pUser == null ) {
                                    currUser.setPid(BigInteger.valueOf(0)); //系统找不到父亲的用户数据则记录为-1
                                } else {
                                    currUser.setPid(pid); //设置当前用户的父id
                                    setPid = true;
                                }
                            }
                            
                            //更新用户的信息
                            currUser.setUsername((String)userInfoMap.get("nickname"));
                            currUser.setNickname((String)userInfoMap.get("nickname"));
                            Integer sex = (Integer)userInfoMap.get("sex") == null ? 0 : (Integer)userInfoMap.get("sex");
                            currUser.setGender(sex == 1 ? "男" : sex == 2 ? "女" : "未知");
                            currUser.setAvatar((String)userInfoMap.get("headimgurl"));
                            currUser.setFlag(User.FLAG_FRONT);
                            currUser.setCreateSource(User.SOURCE_WECHAT);
                            currUser.setOpenid(openid);
                            currUser.setCreated(new Date());
                            if (!currUser.saveOrUpdate()) {
                                return false;
                            }
 
                            //更新了用户的pid时，各个父级的团队人数加1
                            if (setPid) {
                                //跟新父亲（C）的团队人数
                                if (pUser != null) {
                                    if (Db.update("update jp_user set child_num = child_num + 1 where id = ?", pUser.getId()) <= 0) {
                                        return false;
                                    }
                                    if (Db.update("update jp_user set team_num = team_num + 1 where id = ?", pUser.getId()) <= 0) {
                                        return false;
                                    }
                                } else {
                                    return true;
                                }
                                
                                //更新父亲的父亲（B）的团队人数
                                User ppUser = UserQuery.me().findById(pUser.getPid());
                                if (ppUser != null) {
                                    if (Db.update("update jp_user set child_num = child_num + 1 where id = ?", ppUser.getId()) <= 0) {
                                        return false;
                                    }
                                    if (Db.update("update jp_user set team_num = team_num + 1 where id = ?", ppUser.getId()) <= 0) {
                                        return false;
                                    }
                                } else {
                                    return true;
                                }
                                
                                //更新父亲的父亲的父亲（A）的团队人数
                                User pppUser = UserQuery.me().findById(ppUser.getPid());
                                if (pppUser != null) {
                                    if (Db.update("update jp_user set child_num = child_num + 1 where id = ?", pppUser.getId()) <= 0) {
                                        return false;
                                    }
                                    if (Db.update("update jp_user set team_num = team_num + 1 where id = ?", pppUser.getId()) <= 0) {
                                        return false;
                                    }
                                }
                            }

                            CookieUtils.put(ctr, Consts.COOKIE_LOGINED_USER, currUser.getId());//缓存当前用户id
                            return true;
                        }
                    });
                    
                    if (res) { //订单更新失败
                        this.setSessionAttr(Consts.SESSION_WECHAT_USER, result.getJson());
                        CookieUtils.put(this, "test_key", "test_value");
                    }
                    
			    }
			    
		        
			}
		}

		redirect(gotoUrl);
	}
	
	private void copyUserInfoToMap(ApiResult userInfo, Map<String, Object> userInfoMap) {
	    userInfoMap.put("subscribe", userInfo.getInt("subscribe"));
	    userInfoMap.put("openid", userInfo.getStr("openid"));
	    userInfoMap.put("nickname", userInfo.getStr("nickname"));
	    userInfoMap.put("sex", userInfo.getInt("sex"));
	    userInfoMap.put("language", userInfo.getStr("language"));
	    userInfoMap.put("city", userInfo.getStr("city"));
	    userInfoMap.put("province", userInfo.getStr("province"));
	    userInfoMap.put("country", userInfo.getStr("country"));
	    userInfoMap.put("headimgurl", userInfo.getStr("headimgurl"));
        userInfoMap.put("unionid", userInfo.getStr("unionid"));
        userInfoMap.put("remark", userInfo.getStr("remark"));
    }

    private BigInteger getPidByUrl(String gotoUrl){
	    String pidStr = "";
        BigInteger pid = new BigInteger("0");
        
        String [] urlArr = gotoUrl.split("\\?");
        String uidUrl = urlArr[0];
        if (urlArr.length > 1) {
            uidUrl = urlArr[1] + "&";
        }
        
        String[] uidUrlArr = uidUrl.substring(uidUrl.lastIndexOf(UID) > 0 ? uidUrl.lastIndexOf(UID) : 0).split("&");
        for (String url : uidUrlArr) {
            if (url.contains(UID)) {
                
                if(url.contains("&")) {
                    pidStr = url.substring(url.lastIndexOf(UID) + 4,url.indexOf("&"));
                }else {
                    pidStr = url.substring(url.lastIndexOf(UID)+4);
                }
                try {
                    pid = StringUtils.isNotBlank(pidStr) ? new BigInteger(pidStr) : new BigInteger("0");
                    break;
                } catch (Exception e) {
                }
            }
        }
        return pid;
	}
	
	@Override
	public Controller setSessionAttr(String key, Object value) {
		new JSession(this).setAttribute(key, value);
		return this;
	}

	// 处理接收到的文本消息
	protected void processInTextMsg(InTextMsg inTextMsg) {
		String text = inTextMsg.getContent();
		processTextReplay(inTextMsg, text);
	}

	// 处理接收到点击菜单事件
	protected void processInMenuEvent(InMenuEvent inMenuEvent) {
		if (InMenuEvent.EVENT_INMENU_CLICK.equals(inMenuEvent.getEvent())) {
			String text = inMenuEvent.getEventKey();
			processTextReplay(inMenuEvent, text);
		} else {
			renderNull();
		}
	}

	// 处理接收到的图片消息
	protected void processInImageMsg(InImageMsg inImageMsg) {
		processDefaultReplay("wechat_processInImageMsg", inImageMsg);
	}

	// 处理接收到的语音消息
	protected void processInVoiceMsg(InVoiceMsg inVoiceMsg) {
		processDefaultReplay("wechat_processInVoiceMsg", inVoiceMsg);
	}

	// 处理接收到的视频消息
	protected void processInVideoMsg(InVideoMsg inVideoMsg) {
		processDefaultReplay("wechat_processInVideoMsg", inVideoMsg);
	}

	// 处理接收到的视频消息
	protected void processInShortVideoMsg(InShortVideoMsg inShortVideoMsg) {
		// 同：processInVideoMsg
		processDefaultReplay("wechat_processInVideoMsg", inShortVideoMsg);
	}

	// 处理接收到的地址位置消息
	protected void processInLocationMsg(InLocationMsg inLocationMsg) {
		processDefaultReplay("wechat_processInLocationMsg", inLocationMsg);
	}

	// 处理接收到的链接消息
	protected void processInLinkMsg(InLinkMsg inLinkMsg) {
		processDefaultReplay("wechat_processInLinkMsg", inLinkMsg);
	}

	// 处理接收到的多客服管理事件
	protected void processInCustomEvent(InCustomEvent inCustomEvent) {

		// 关闭多客服
		if (InCustomEvent.EVENT_INCUSTOM_KF_CLOSE_SESSION.equals(inCustomEvent.getEvent())) {

		}

		processDefaultReplay("wechat_processInCustomEvent", inCustomEvent);
	}

	// 处理接收到的关注/取消关注事件
	protected void processInFollowEvent(InFollowEvent inFollowEvent) {

		// 用户关注公众号了
		if (InFollowEvent.EVENT_INFOLLOW_SUBSCRIBE.equals(inFollowEvent.getEvent())) {
			processDefaultReplay("wechat_processInFollowEvent", inFollowEvent);
		}

		// 如果为取消关注事件，将无法接收到传回的信息
		if (InFollowEvent.EVENT_INFOLLOW_UNSUBSCRIBE.equals(inFollowEvent.getEvent())) {
			// 取消关注，无法发送消息给用户了，可以做一些系统处理。
		}

	}

	// 处理接收到的扫描带参数二维码事件
	protected void processInQrCodeEvent(InQrCodeEvent inQrCodeEvent) {
		processDefaultReplay("wechat_processInQrCodeEvent", inQrCodeEvent);
	}

	// 处理接收到的上报地理位置事件
	protected void processInLocationEvent(InLocationEvent inLocationEvent) {
		processDefaultReplay("wechat_processInLocationEvent", inLocationEvent);
	}

	// 处理接收到的群发任务结束时通知事件
	protected void processInMassEvent(InMassEvent inMassEvent) {
		processDefaultReplay("wechat_processInMassEvent", inMassEvent);
	}

	// 处理接收到的语音识别结果
	protected void processInSpeechRecognitionResults(InSpeechRecognitionResults inSpeechRecognitionResults) {
		processDefaultReplay("wechat_processInSpeechRecognitionResults", inSpeechRecognitionResults);
	}

	// 处理接收到的模板消息是否送达成功通知事件
	protected void processInTemplateMsgEvent(InTemplateMsgEvent inTemplateMsgEvent) {
		processDefaultReplay("wechat_processInTemplateMsgEvent", inTemplateMsgEvent);
	}

	// 处理微信摇一摇事件
	protected void processInShakearoundUserShakeEvent(InShakearoundUserShakeEvent inShakearoundUserShakeEvent) {
		processDefaultReplay("wechat_processInShakearoundUserShakeEvent", inShakearoundUserShakeEvent);
	}

	// 资质认证成功 || 名称认证成功 || 年审通知 || 认证过期失效通知
	protected void processInVerifySuccessEvent(InVerifySuccessEvent inVerifySuccessEvent) {
		processDefaultReplay("wechat_processInVerifySuccessEvent", inVerifySuccessEvent);
	}

	// 资质认证失败 || 名称认证失败
	protected void processInVerifyFailEvent(InVerifyFailEvent inVerifyFailEvent) {
		processDefaultReplay("wechat_processInVerifyFailEvent", inVerifyFailEvent);
	}

	// 门店在审核事件消息
	protected void processInPoiCheckNotifyEvent(InPoiCheckNotifyEvent inPoiCheckNotifyEvent) {
		processDefaultReplay("wechat_processInPoiCheckNotifyEvent", inPoiCheckNotifyEvent);
	}

	private void processTextReplay(InMsg message, String userInput) {

		// 多客服的相关处理
		if (dkfProcess(message, userInput)) {
			return;
		}

		// 自动回复
		Content content = ContentQuery.me().findFirstByModuleAndTitle(Consts.MODULE_WECHAT_REPLY, userInput);
		if (content != null && StringUtils.isNotBlank(content.getText())) {
			textOrSeniorRender(message, content.getText());
			return;
		}

		// 搜索相关
		if (searchProcess(message, userInput)) {
			return;
		}

		// 没有匹配
		processDefaultReplay("wechat_search_no_matching", message);
	}

	/**
	 * 搜索相关处理
	 * 
	 * @param message
	 * @param userInput
	 * @return
	 */
	private boolean searchProcess(InMsg message, String userInput) {
		List<TplModule> modules = TemplateManager.me().currentTemplateModules();
		if (StringUtils.isBlank(userInput) || modules == null || modules.size() == 0) {
			return false;
		}

		TplModule searchModule = null;
		TplModule nonePrefixModule = null;
		for (TplModule module : modules) {
			// 是否启用搜索
			Boolean bool = OptionQuery.me().findValueAsBool(String.format("wechat_search_%s_enable", module.getName()));
			if (bool == null || bool == false) {
				continue;
			}

			// 搜索关键字 前缀
			String prefix = OptionQuery.me().findValue(String.format("wechat_search_%s_prefix", module.getName()));
			if (StringUtils.isBlank(prefix) && nonePrefixModule == null) {
				nonePrefixModule = module;
				continue;
			}

			if (StringUtils.isNotBlank(prefix) && userInput.startsWith(prefix)) {
				searchModule = module;
				userInput = userInput.substring(prefix.length());
				break;
			}
		}

		if (searchModule == null) {
			searchModule = nonePrefixModule;
		}

		if (searchModule == null || StringUtils.isBlank(userInput)) {
			return false;
		}

		// 搜索结果数量
		Integer count = OptionQuery.me()
				.findValueAsInteger(String.format("wechat_search_%s_count", searchModule.getName()));
		if (count == null || count <= 0 || count > 10) {
			count = 10;
		}

		List<Content> contents = ContentQuery.me().searchByModuleAndTitle(searchModule.getName(), userInput, count);
		if (contents == null || contents.isEmpty()) {
			// 搜索不到内容时
			processDefaultReplay("wechat_search_none_content", message);
			return true;
		}

		String domain = OptionQuery.me().findValue("web_domain");
		if (StringUtils.isBlank(domain)) {
			OutTextMsg otm = new OutTextMsg(message);
			otm.setContent("您还没有配置您的域名，请先在后台的【设置】>【常规】里配置您的网站域名！");
			render(otm);
			return true;
		}

		OutNewsMsg out = new OutNewsMsg(message);
		for (Content content : contents) {
			News news = new News();
			news.setTitle(content.getTitle());
			news.setDescription(content.getSummary());
			news.setPicUrl(domain + content.getImage());
			news.setUrl(domain + content.getUrl());
			out.addNews(news);
		}
		render(out);
		return true;

	}

	/**
	 * 多客服相关处理
	 * 
	 * @param message
	 * @param userInput
	 * @return
	 */
	private boolean dkfProcess(InMsg message, String userInput) {
		String dkf_quit_key = OptionQuery.me().findValue("wechat_dkf_quit_key");
		if (StringUtils.isNotBlank(dkf_quit_key) && dkf_quit_key.equals(userInput)) {
			CacheKit.remove("wechat_dkf", message.getFromUserName());

			String quit_message = OptionQuery.me().findValue("wechat_dkf_quit_message");
			OutTextMsg otm = new OutTextMsg(message);
			otm.setContent(quit_message);
			render(otm);

			return true;
		}

		Boolean isInDkf = CacheKit.get("wechat_dkf", message.getFromUserName());
		if (isInDkf != null && isInDkf == true) {

			// 重新更新ehcache存储的开始时间，5分钟后失效。
			{
				CacheKit.remove("wechat_dkf", message.getFromUserName());
				CacheKit.put("wechat_dkf", message.getFromUserName(), true);
			}

			OutCustomMsg outCustomMsg = new OutCustomMsg(message);
			render(outCustomMsg);
			return true;
		}

		String dkf_enter_key = OptionQuery.me().findValue("wechat_dkf_enter_key");
		if (StringUtils.isNotBlank(dkf_enter_key) && dkf_enter_key.equals(userInput)) {
			// ehcache的过期时间为5分钟，如果用户5分钟未咨询，自动失效。
			CacheKit.put("wechat_dkf", message.getFromUserName(), true);

			// 进入多客服
			String quit_message = OptionQuery.me().findValue("wechat_dkf_enter_message");
			OutTextMsg otm = new OutTextMsg(message);
			otm.setContent(quit_message);
			render(otm);

			return true;
		}

		return false;
	}

	private void processDefaultReplay(String optionKey, InMsg message) {

		String replyContent = OptionQuery.me().findValue(optionKey);

		if (StringUtils.isBlank(replyContent)) {
			renderNull();
			return;
		}

		textOrSeniorRender(message, replyContent);
	}

	private void textOrSeniorRender(InMsg message, String replyContent) {
		if (isSeniorReplay(replyContent)) {
			OutMsg outMsg = ProcesserManager.me().invoke(replyContent, message);
			if (outMsg != null) {
				render(outMsg);
			} else {
				renderConfigErrorMessage(message, replyContent);
			}
		} else {
			OutTextMsg outTextMsg = new OutTextMsg(message);
			outTextMsg.setContent(replyContent);
			render(outTextMsg);
		}
	}

	private void renderConfigErrorMessage(InMsg message, String replyContent) {
		OutTextMsg outTextMsg = new OutTextMsg(message);
		outTextMsg.setContent("配置错误，没有高级回复 " + replyContent + ",请联系网站管理员。");
		render(outTextMsg);
	}

	private static final boolean isSeniorReplay(String string) {
		return string != null && string.startsWith("[") && string.contains("]");
	}

	@Override
	protected void processInMerChantOrderEvent(InMerChantOrderEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void processInSubmitMemberCardEvent(InSubmitMemberCardEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void processInUpdateMemberCardEvent(InUpdateMemberCardEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void processInUserPayFromCardEvent(InUserPayFromCardEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void processInUserViewCardEvent(InUserViewCardEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void processInWifiEvent(InWifiEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void processIsNotDefinedEvent(InNotDefinedEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void processIsNotDefinedMsg(InNotDefinedMsg arg0) {
		// TODO Auto-generated method stub
		
	}
    
    public static void main(String[] args) {
        String url ="/?uid=11?from=singlemsg";
        String [] urlArr = url.split("\\?");
        String uidUrl = urlArr[0];
        if (urlArr.length > 1) {
            uidUrl = urlArr[1] + "&";;
        }
        System.out.println(uidUrl);
        String pidStr = uidUrl.substring(uidUrl.indexOf(UID) + 4,uidUrl.indexOf("&"));
        System.out.println(pidStr);
    }
    
}
