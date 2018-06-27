-- 商户平台支付到个人配置
insert into `jpress_mall`.`jp_option` ( `option_key`, `option_value`) values ( 'wechat_pay_mchid', '1337083401');
insert into `jpress_mall`.`jp_option` ( `option_key`, `option_value`) values ( 'wechat_pay_spbill_create_ip', 'm.yuweiguoye.com');
-- 腾讯短信配置
insert into `jpress_mall`.`jp_option` ( `option_key`, `option_value`) values ( 'tencent_sms_APPID', '1400070735');
insert into `jpress_mall`.`jp_option` ( `option_key`, `option_value`) values ( 'tencent_sms_APPKEY', '38b1e95302833109287dc76a4006363f');


-- 用户提现申请
DROP TABLE IF EXISTS `jp_extract`;
CREATE TABLE `jp_extract` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) unsigned DEFAULT NULL COMMENT '用户id',
  `realname` varchar(255) DEFAULT NULL COMMENT '真实姓名',
  `telephone` varchar(512) DEFAULT NULL COMMENT '用户手机号',
  `extract_money` decimal(10,2) DEFAULT '0.00' COMMENT '提现金额(元)',
  `status` varchar(32) DEFAULT '0' COMMENT '申请状态(0：待审核,1：审核通过,2：审核不通过，3：支付完成)',
  `remark` varchar(256) DEFAULT NULL COMMENT '用户备注',
  `payed_time` datetime DEFAULT NULL COMMENT '支付完成时间',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `payed_money` decimal(10,0) DEFAULT NULL COMMENT '支付金额',
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='提现申请表';

-- 提现支付明细
DROP TABLE IF EXISTS `jp_extract_pay`;
CREATE TABLE `jp_extract_pay` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `extract_id` bigint(20) unsigned DEFAULT NULL COMMENT '提现申请id',
  `extract_money` decimal(10,2) DEFAULT '0.00' COMMENT '提现金额(元)',
  `pay_money` decimal(10,2) DEFAULT '0.00' COMMENT '申请状态(0：待审核,1：审核通过,2：审核不通过，3：支付完成)',
  `remark` varchar(256) DEFAULT NULL COMMENT '用户备注',
  `payed_time` datetime DEFAULT NULL COMMENT '支付完成时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=44 DEFAULT CHARSET=utf8mb4 COMMENT='提现支付明细';

-- 手机验证码
DROP TABLE IF EXISTS `jp_verify_code`;
CREATE TABLE `jp_verify_code` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `telephone` varchar(512) DEFAULT NULL COMMENT '手机号',
  `status` varchar(32) DEFAULT '0' COMMENT '状态(0：待审核,1：审核通过)',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `code` varchar(255) DEFAULT NULL COMMENT '验证码',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='手机验证码';