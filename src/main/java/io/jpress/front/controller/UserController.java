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
package io.jpress.front.controller;

import com.jfinal.aop.Before;
import com.jfinal.aop.Clear;
import com.jfinal.core.ActionKey;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.IAtom;
import com.jfinal.plugin.activerecord.Record;

import io.jpress.Consts;
import io.jpress.core.BaseFrontController;
import io.jpress.interceptor.UCodeInterceptor;
import io.jpress.interceptor.UserInterceptor;
import io.jpress.message.Actions;
import io.jpress.message.MessageKit;
import io.jpress.model.*;
import io.jpress.model.query.*;
import io.jpress.router.RouterMapping;
import io.jpress.ui.freemarker.tag.ExtractPageTag;
import io.jpress.ui.freemarker.tag.ShoppingCartPageTag;
import io.jpress.ui.freemarker.tag.TransactionPageTag;
import io.jpress.ui.freemarker.tag.UserAddressPageTag;
import io.jpress.ui.freemarker.tag.UserAmountPageTag;
import io.jpress.utils.CookieUtils;
import io.jpress.utils.EncryptUtils;
import io.jpress.utils.StringUtils;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import io.jpress.wechat.WechatUserInterceptor;

@RouterMapping(url = Consts.ROUTER_USER)
//@Before(UserInterceptor.class)
@Before(WechatUserInterceptor.class)
public class UserController extends BaseFrontController {

	private void gotoUrl(){
		String gotoUrl=getPara("goto");
		if (StringUtils.isNotEmpty(gotoUrl)) {
			gotoUrl = StringUtils.urlDecode(gotoUrl);
			gotoUrl = StringUtils.urlRedirect(gotoUrl);
		}
		setAttr("goto", gotoUrl);
	}
	
	//登录
	//@Clear(UserInterceptor.class)
	@Clear(WechatUserInterceptor.class)
	@ActionKey(Consts.ROUTER_USER_LOGIN) // 固定登录的url
	public void login(){
		gotoUrl();
		render("user_login.html");
	}

	@Clear(UserInterceptor.class)
	public void doLogin() {
		keepPara();

		String username = getPara("username");
		String password = getPara("password");
		setAttr("username", username);
		setAttr("password", password);

		if (StringUtils.isBlank(username)) {
			setAttr("error_msg", "用户名不能为空");
			login();
			return;
		}
		
		if (StringUtils.isBlank(password)) {
			setAttr("error_msg", "密码不能为空");
			login();
			return;
		}
		
		if (!validateCaptcha("_login_captcha")) { // 验证码没验证成功！
			setAttr("error_msg", "验证码错误");
			login();
			return;
		}

		/*long errorTimes = CookieUtils.getLong(this, "_login_errors", 0);
		if (errorTimes >= 3) {
			if (!validateCaptcha("_login_captcha")) { // 验证码没验证成功！
				if (isAjaxRequest()) {
					renderAjaxResultForError("没有该用户");
				} else {
					redirect(Consts.ROUTER_USER_LOGIN);
				}
				return;
			}
		}*/

		User user = UserQuery.me().findUserByUsernameAndFlag(username, User.FLAG_FRONT);
		if (null == user) {
			if (isAjaxRequest()) {
				renderAjaxResultForError("没有该用户");
			} else {
				setAttr("error_msg", "没有该用户");
				login();
				return;
			}
			/*CookieUtils.put(this, "_login_errors", errorTimes + 1);
			return;*/
		}

		if (EncryptUtils.verlifyUser(user.getPassword(), user.getSalt(), password)) {
			MessageKit.sendMessage(Actions.USER_LOGINED, user);
			CookieUtils.put(this, Consts.COOKIE_LOGINED_USER, user.getId());
			if (this.isAjaxRequest()) {
				renderAjaxResultForSuccess("登录成功");
			} else {
				String gotoUrl = getPara("goto");
				if (StringUtils.isNotEmpty(gotoUrl)) {
					gotoUrl = StringUtils.urlDecode(gotoUrl);
					gotoUrl = StringUtils.urlRedirect(gotoUrl);
					redirect(gotoUrl);
				} else {
					redirect(Consts.ROUTER_USER_CENTER);
				}
			}
		} else {
			if (isAjaxRequest()) {
				renderAjaxResultForError("密码错误");
			} else {
				setAttr("error_msg", "密码错误");
				login();
				return;
			}
			//CookieUtils.put(this, "_login_errors", errorTimes + 1);
		}
	}

	//退出
	//@Before(UCodeInterceptor.class)
	@Before(UCodeInterceptor.class)
	public void logout() {
		CookieUtils.remove(this, Consts.COOKIE_LOGINED_USER);
		redirect("/");
	}
	
	//注册
	@Clear(UserInterceptor.class)
	public void register(){
		gotoUrl();
		render("user_register.html");
	}

	@Clear(UserInterceptor.class)
	public void doRegister() {
		keepPara();

		String username = getPara("username");
		String password = getPara("password");
		String confirm_password = getPara("confirm_password");
		setAttr("username", username);
		setAttr("password", password);
		setAttr("confirm_password", confirm_password);

		if (StringUtils.isBlank(username)) {
			setAttr("error_msg", "用户名不能为空");
			register();
			return;
		}

		if (StringUtils.isBlank(password)) {
			setAttr("error_msg", "密码不能为空");
			register();
			return;
		}

		if (StringUtils.isNotEmpty(confirm_password)) {
			if (!confirm_password.equals(password)) {
				setAttr("error_msg", "密码不相等");
				register();
				return;
			}
		}
		
		if (!validateCaptcha("_register_captcha")) { // 验证码没验证成功！
			setAttr("error_msg", "验证码错误");
			register();
			return;
		}

		if (UserQuery.me().findUserByUsernameAndFlag(username, User.FLAG_FRONT) != null) {
			setAttr("error_msg", "该用户已经存在");
			register();
			return;
		}

		User user = new User();
		user.setUsername(username);
		user.setNickname(username);
		user.setFlag(User.FLAG_FRONT);

		String salt = EncryptUtils.salt();
		password = EncryptUtils.encryptPassword(password, salt);
		user.setPassword(password);
		user.setSalt(salt);
		user.setCreated(new Date());

		if (user.save()) {
			CookieUtils.put(this, Consts.COOKIE_LOGINED_USER, user.getId());

			if (isAjaxRequest()) {
				renderAjaxResultForSuccess();
			} else {
				String gotoUrl = getPara("goto");
				if (StringUtils.isNotEmpty(gotoUrl)) {
					gotoUrl = StringUtils.urlDecode(gotoUrl);
					gotoUrl = StringUtils.urlRedirect(gotoUrl);
					redirect(gotoUrl);
				} else {
					redirect(Consts.ROUTER_USER_CENTER);
				}
			}
		} else {
			renderAjaxResultForError();
		}
	}

	//修改密码
	@Before(UCodeInterceptor.class)
	public void doModifyPassword(){
		String newPassword=getPara("newPassword");
		if(StringUtils.isBlank(newPassword)){
			renderAjaxResultForError("新密码不能为空");
			return;
		}
		User user=getLoginedUser();
		String password=EncryptUtils.encryptPassword(newPassword, user.getSalt());
		user.setPassword(password);
		if(user.update()){
			renderAjaxResultForSuccess();
		}else{
			renderAjaxResultForError("操作失败");
		}
	}

	//用户中心
	public void center() {
		User loginedUser = getLoginedUser();
		if(loginedUser!=null) {
		    User u = UserQuery.DAO.findById(loginedUser.getId());
			setAttr("accountMoney", u.getAmount());
			setAttr("extractedMoney", "10");//TODO
			setAttr("teamNum", u.getTeamNum());
			setAttr("teamBuyAmount", u.getTeamBuyAmount());
			
			setAttr("unpayed", TransactionQuery.me().findcount(getLoginedUser().getId(), Transaction.STATUS_1));
			setAttr("unreceived", TransactionQuery.me().findcount(getLoginedUser().getId(), Transaction.STATUS_3));
			setAttr("uncomment", TransactionQuery.me().findcount(getLoginedUser().getId(), Transaction.STATUS_4));
		}
		render("user_center.html");
	}

	//添加商品到购物车
	public void doAddShoppingCart(){
		BigInteger contentId=getParaToBigInteger("content.id");
		BigInteger specValueId=getParaToBigInteger("specValue.id");
		Integer quantity=getParaToInt("quantity");

		if(contentId==null){
			renderAjaxResultForError("商品id不能为空");
			return;
		}
		if(specValueId==null){
			renderAjaxResultForError("请选择规格");
			return;
		}
		if(quantity==null){
			renderAjaxResultForError("请选择数量");
			return;
		}

		Content content=ContentQuery.me().findById(contentId);
		if(content==null){
			renderAjaxResultForError("商品不存在");
			return;
		}
		if(!"normal".equals(content.getStatus())){
			renderAjaxResultForError("商品已经下架");
			return;
		}

		ContentSpecItem contentSpecItem=ContentSpecItemQuery.me().findByContentIdAndSpecValueId(contentId, specValueId);
		if(contentSpecItem==null){
			renderAjaxResultForError("货存不足");
			return;
		}

		ShoppingCart shoppingCart=ShoppingCartQuery.me().find(getLoginedUser().getId(), contentId, specValueId);
		if(shoppingCart!=null){
			shoppingCart.setQuantity(shoppingCart.getQuantity()+quantity);
			if(shoppingCart.update()){
				renderAjaxResultForSuccess();
			}else{
				renderAjaxResultForError("操作失败");
			}
		}else{
			shoppingCart=new ShoppingCart();
			shoppingCart.setContentId(contentId);
			shoppingCart.setSpecValueId(specValueId);
			shoppingCart.setUserId(getLoginedUser().getId());
			shoppingCart.setQuantity(quantity);
			shoppingCart.setCreated(new Date());
			if(shoppingCart.save()){
				renderAjaxResultForSuccess();
			}else{
				renderAjaxResultForError("操作失败");
			}
		}
	}

	//购物车列表
	public void shoppingCart(){
		int pageNumber=getParaToInt("pageNumber", 1);
		BigInteger userId=getLoginedUser().getId();
		setAttr(ShoppingCartPageTag.TAG_NAME, new ShoppingCartPageTag(getRequest(), pageNumber, null, userId, null));
		setAttr(Consts.ATTR_GLOBAL_WEB_TITLE, "购物车");
		render("user_shopping_cart.html");
	}

	//修改购物车商品数量
	public void doChangeShoppingCartQuantity(){
		BigInteger id=getParaToBigInteger("id");
		Integer quantity=getParaToInt("quantity");
		if(id==null){
			renderAjaxResultForError("购物车id不能为空");
			return;
		}
		if(quantity==null){
			renderAjaxResultForError("购物车数量不能为空");
			return;
		}
		ShoppingCart shoppingCart=ShoppingCartQuery.me().findById(id);
		if(shoppingCart!=null){
			shoppingCart.setQuantity(quantity);
			if(shoppingCart.update()){
				renderAjaxResultForSuccess();
			}else{
				renderAjaxResultForError("操作失败");
			}
		}else{
			renderAjaxResultForError("购物车不存在");
		}
	}

	//清空购物车
	@Before(UCodeInterceptor.class)
	public void doClearShoppingCart(){
		ShoppingCartQuery.me().deleteByUserId(getLoginedUser().getId());
		renderAjaxResultForSuccess();
	}

	//从购物车删除商品
	@Before(UCodeInterceptor.class)
	public void doDeleteShoppingCart(){
		String ids=getPara("ids");
		if(StringUtils.isBlank(ids)){
			renderAjaxResultForError("购物车id为空");
			return;
		}
		ShoppingCartQuery.me().deleteByIds(ids);
		renderAjaxResultForSuccess();
	}

	//用户收货地址列表
	public void userAddress(){
		int pageNumber=getParaToInt("pageNumber", 1);
		BigInteger userId=getLoginedUser().getId();
		setAttr(UserAddressPageTag.TAG_NAME, new UserAddressPageTag(getRequest(), pageNumber, userId, null));
		setAttr(Consts.ATTR_GLOBAL_WEB_TITLE, "收货地址");
		render("user_address.html");
	}

	//清空收货地址
	@Before(UCodeInterceptor.class)
	public void doClearUserAddress(){
		UserAddressQuery.me().deleteByUserId(getLoginedUser().getId());
		renderAjaxResultForSuccess();
	}

	//删除收货地址
	@Before(UCodeInterceptor.class)
	public void doDeleteUserAddress(){
		String ids=getPara("ids");
		if(StringUtils.isBlank(ids)){
			renderAjaxResultForError("收货地址id为空");
			return;
		}
		UserAddressQuery.me().deleteByIds(ids);
		renderAjaxResultForSuccess();
	}

	@Before(UCodeInterceptor.class)
	public void doSaveUserAddress(){
		UserAddress userAddress=getModel(UserAddress.class);
		if(StringUtils.isBlank(userAddress.getAddress())){
			renderAjaxResultForError("收货地址不能为空");
			return;
		}
		if(StringUtils.isBlank(userAddress.getName())){
			renderAjaxResultForError("收货人姓名不能为空");
			return;
		}
		if(StringUtils.isBlank(userAddress.getMobile())){
			renderAjaxResultForError("收货人手机号码不能为空");
			return;
		}
		if(userAddress.getUserId()==null){
			userAddress.setUserId(getLoginedUser().getId());
		}
		if(userAddress.getCreated()==null){
			userAddress.setCreated(new Date());
		}
		if(userAddress.saveOrUpdate()){
//			renderAjaxResultForSuccess();
			renderAjaxResult("success", 0, userAddress);
		}else{
			renderAjaxResultForError("操作失败");
		}
	}

	public void doGetUserAddress(){
		BigInteger id=getParaToBigInteger("id");
		if(id==null){
			renderAjaxResultForError("收货地址id不能为空");
			return;
		}
		UserAddress userAddress=UserAddressQuery.me().findById(id);
		if(userAddress!=null){
			renderAjaxResult("操作成功", 0, userAddress);
		}else{
			renderAjaxResultForError("收货地址不存在");
		}
	}

	//购物车-用户结算
	public void userShoppingCartSettlement(){
		gotoUrl();
		String ids=getPara("ids");
		if(StringUtils.isBlank(ids)){
			renderAjaxResultForError("购物车id不能为空");
			return;
		}
		int pageNumber=getParaToInt("pageNumber", 1);
		User currUser = getLoginedUser();
        BigInteger userId=currUser.getId();
		Object object=ShoppingCartQuery.me().getTotalFeeAndQuantity(ids, userId);
		setAttr("object", object);
		setAttr(UserAddressPageTag.TAG_NAME, new UserAddressPageTag(getRequest(), pageNumber, userId, null));
		setAttr(ShoppingCartPageTag.TAG_NAME, new ShoppingCartPageTag(getRequest(), pageNumber, ids, userId, null));
		
		//查询当前用户的优惠券信息
        List<Coupon> couponList = CouponQuery.me().findListByUserId(1, 100, userId);
        setAttr("couponList", couponList);
        setAttr("amount", currUser.getAmount());
        
        setAttr(Consts.ATTR_GLOBAL_WEB_TITLE, "订单确认");
		render("user_settlement.html");
	}

	public void checkUserSettlement(){
		BigInteger contentId=getParaToBigInteger("content.id");
		BigInteger specValueId=getParaToBigInteger("specValue.id");
		Integer quantity=getParaToInt("quantity");
		if(contentId==null){
			renderAjaxResultForError("商品id不能为空");
			return;
		}
		if(specValueId==null){
			renderAjaxResultForError("请选择规格");
			return;
		}
		if(quantity==null){
			renderAjaxResultForError("请选择数量");
			return;
		}
		renderAjaxResultForSuccess();
	}

	//立即购买-用户结算
	public void userSettlement(){
		gotoUrl();
		BigInteger contentId=getParaToBigInteger("content.id");
		BigInteger specValueId=getParaToBigInteger("specValue.id");
		Integer quantity=getParaToInt("quantity");
		int pageNumber=getParaToInt("pageNumber", 1);
		User currUser = getLoginedUser();
		BigInteger userId=currUser.getId();
		setAttr(UserAddressPageTag.TAG_NAME, new UserAddressPageTag(getRequest(), pageNumber, userId, null));
		Content content=ContentQuery.me().find(contentId, specValueId);
		setAttr("content", content);
		setAttr("quantity", quantity);
		
		//查询当前用户的优惠券信息
		List<Coupon> couponList = CouponQuery.me().findListByUserId(1, 100, userId);
		setAttr("couponList", couponList);
		setAttr("amount", currUser.getAmount());
		
		setAttr(Consts.ATTR_GLOBAL_WEB_TITLE, "订单确认");
		render("user_settlement.html");
	}

	//用户待支付列表
	public void userUnpayed(){
		//gotoUrl();
		int pageNumber=getParaToInt("pageNumber", 1);
		BigInteger userId=getLoginedUser().getId();
		setAttr("title","待支付订单");
		setAttr(Consts.ATTR_GLOBAL_WEB_TITLE, "待支付订单");
		setAttr(TransactionPageTag.TAG_NAME, new TransactionPageTag(getRequest(), pageNumber, Transaction.STATUS_1, userId, null));
		render("user_transaction.html");
	}

	//用户待收货列表
	public void userUnreceived(){
		//gotoUrl();
		int pageNumber=getParaToInt("pageNumber", 1);
		BigInteger userId=getLoginedUser().getId();
		setAttr("title","待收货订单");
        setAttr(Consts.ATTR_GLOBAL_WEB_TITLE, "待收货订单");
		setAttr(TransactionPageTag.TAG_NAME, new TransactionPageTag(getRequest(), pageNumber, Transaction.STATUS_3, userId, null));
		render("user_transaction.html");
	}

	//用户待评价列表
	public void userUncomment(){
		//gotoUrl();
		int pageNumber=getParaToInt("pageNumber", 1);
		BigInteger userId=getLoginedUser().getId();
		setAttr("title","待评价订单");
        setAttr(Consts.ATTR_GLOBAL_WEB_TITLE, "待评价订单");
		setAttr(TransactionPageTag.TAG_NAME, new TransactionPageTag(getRequest(), pageNumber, Transaction.STATUS_4, userId, null));
		render("user_transaction.html");
	}

	//用户所有订单列表
	public void userTransaction(){
		//gotoUrl();
		int pageNumber=getParaToInt("pageNumber", 1);
		BigInteger userId=getLoginedUser().getId();
		setAttr("title","全部订单");
        setAttr(Consts.ATTR_GLOBAL_WEB_TITLE, "全部订单");
		setAttr(TransactionPageTag.TAG_NAME, new TransactionPageTag(getRequest(), pageNumber, null, userId, null));
		render("user_transaction.html");
	}

	//用户订单详情
	public void userTransactionItem(){
		//gotoUrl();
		BigInteger id=getParaToBigInteger("id");
		Transaction transaction=TransactionQuery.me().findById(id);
		List<TransactionItem> transactionItemList=TransactionItemQuery.me().findList(transaction.getId());
		setAttr("transaction",transaction);
		setAttr("transactionItemList",transactionItemList);
		setAttr(Consts.ATTR_GLOBAL_WEB_TITLE, "订单详情");
		render("user_transaction_item.html");
	}

	
	//用户基本信息设置
	public void userSetting(){
		BigInteger userId=getLoginedUser().getId();
		setAttr("user", UserQuery.me().findById(userId));
		setAttr(Consts.ATTR_GLOBAL_WEB_TITLE, "个人信息设置");
		render("user_setting.html");
	}
	
	//保存用户信息
	public void doSaveUserSetting() {
		final User user = getModel(User.class);
		if (StringUtils.isBlank(user.getRealname())) {
			renderAjaxResultForError("姓名不能为空。");
			return;
		}
		if (StringUtils.isBlank(user.getMobile())) {
			renderAjaxResultForError("手机号码不能为空。");
			return;
		}
		
		
	
		boolean saved = Db.tx(new IAtom() {
			@Override
			public boolean run() throws SQLException {

				if (!user.saveOrUpdate()) {
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
	}
	
	
	//账户余额
	public void accountDetail(){
        /*BigInteger userId=getLoginedUser().getId();
		List<Record> accountDetailList = UserQuery.me().findAmountList(getPageNumber(), getPageSize(), userId);
        if (accountDetailList != null && accountDetailList.size() == 0) {
            setAttr("accountDetailList", accountDetailList);
        }*/
	    int pageNumber=getParaToInt("pageNumber", 1);
        BigInteger userId=getLoginedUser().getId();
	    setAttr(UserAmountPageTag.TAG_NAME, new UserAmountPageTag(getRequest(), pageNumber, userId, null));
	    setAttr(Consts.ATTR_GLOBAL_WEB_TITLE, "余额明细");
		render("account_detail.html");
	}
	
	
	//钱包提现(如果用户真实姓名和手机号未完善，跳转到用户基本设置)
	public void accountExtract(){
		int pageNumber=getParaToInt("pageNumber", 1);
		BigInteger userId=getLoginedUser().getId();
		User user = UserQuery.me().findById(userId);
		if(user!=null && user.getRealname()!=null) {
			setAttr(ExtractPageTag.TAG_NAME, new ExtractPageTag(getRequest(), pageNumber, userId, null));
			setAttr(Consts.ATTR_GLOBAL_WEB_TITLE, "钱包提现");
			render("account_extract.html");
		}else {
			setAttr("user", user);
			setAttr("msg", "请先完善基本信息");
			setAttr(Consts.ATTR_GLOBAL_WEB_TITLE, "基本信息完善");
			redirect("/user/userSetting");
		}
	}
	
	//新增钱包提现
	public void addAccountExtract(){
		BigInteger userId=getLoginedUser().getId();
		User user = UserQuery.DAO.findById(userId);
		if(user!=null){
			renderAjaxResult("操作成功", 0, user);
		}else{
			renderAjaxResultForError("用户信息获取失败");
		}
	}
	
	public void doSaveAccountExtract(){
		Extract extract=getModel(Extract.class);
		if(StringUtils.isBlank(extract.getRealName())){
			renderAjaxResultForError("真实姓名不能为空");
			return;
		}
		if(StringUtils.isBlank(extract.getTelephone())){
			renderAjaxResultForError("手机号码不能为空");
			return;
		}
		if(extract.getExtractMoney()==null){
			renderAjaxResultForError("提现金额不能为空");
			return;
		}
		boolean flag = extract.saveOrUpdate();
		if(flag){
			renderAjaxResultForSuccess();
		}else{
			renderAjaxResultForError("操作失败");
		}
	}
	
	//删除钱包提现
	public void doDeleteAccountExtract(){
		String ids=getPara("ids");
		if(StringUtils.isBlank(ids)){
			renderAjaxResultForError("钱包提现id为空");
			return;
		}
		ExtractQuery.me().deleteByIds(ids);
		renderAjaxResultForSuccess();
	}
	
	public void doGetAccountExtract(){
		BigInteger id=getParaToBigInteger("id");
		if(id==null){
			renderAjaxResultForError("提现id不能为空");
			return;
		}
		Extract accountExtract=ExtractQuery.me().findById(id);
		if(accountExtract!=null){
			renderAjaxResult("操作成功", 0, accountExtract);
		}else{
			renderAjaxResultForError("提现不存在");
		}
	}
	
	
	//报名成为吃货达人
	public void userBeing(){
		BigInteger userId=getLoginedUser().getId();
		setAttr("user", UserQuery.me().findById(userId));
		setAttr(Consts.ATTR_GLOBAL_WEB_TITLE, "达人协议");
		render("user_being.html");
	}
	
	//用户成为吃货达人
	public void doSaveUserBeing() {
		BigInteger userId=getLoginedUser().getId();
		final User user = UserQuery.me().findById(userId);
		user.setAgentsLevel(user.AGENTS_LEVEL_1);
		boolean saved = Db.tx(new IAtom() {
			@Override
			public boolean run() throws SQLException {
				if (!user.saveOrUpdate()) {
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
	}

}
