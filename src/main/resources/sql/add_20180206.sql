ALTER TABLE `jp_user` change column `childNum` `child_num` int(11) unsigned DEFAULT '0' COMMENT '直推人数（直接子节点数）';
ALTER TABLE `jp_user` change column `teamNum` `team_num` int(11) unsigned DEFAULT '0' COMMENT '团队人数总计（直接子节点数+间接子节点数，目前层级3级）';
ALTER TABLE `jp_user` change column `teamBuyAmount` `team_buy_amount` decimal(12,2) unsigned DEFAULT '0.00' COMMENT '团队消费金额总计';

ALTER TABLE `jp_transaction` ADD `coupon_fee` decimal(12,2) unsigned DEFAULT '0.00' COMMENT '使用优惠券支付金额';
ALTER TABLE `jp_transaction` ADD `amount_fee` decimal(12,2) unsigned DEFAULT '0.00' COMMENT '使用账户余额支付金额';
ALTER TABLE `jp_transaction` ADD `cash_fee` decimal(12,2) unsigned DEFAULT '0.00' COMMENT '使用现金支付金额';

DROP TABLE IF EXISTS `jp_coupon`;
CREATE TABLE `jp_coupon` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` varchar(128) DEFAULT NULL COMMENT '优惠券码（唯一标识码）',
  `name` varchar(128) DEFAULT NULL COMMENT '优惠券名称',
  `last_date` date DEFAULT NULL COMMENT '优惠券使用截止日期',
  `type` int(2) unsigned DEFAULT '0' COMMENT '优惠券分类（1 通用券）',
  `invalid` int(2) unsigned DEFAULT '0' COMMENT '是否失效：1 是 0 否',
  `total_num` int(11) unsigned DEFAULT '0' COMMENT '总数量',
  `free_num` int(11) unsigned DEFAULT '0' COMMENT '剩余数量',
  `amount` decimal(10,2) unsigned DEFAULT '0.00' COMMENT '优惠券金额',
  `desc` text COMMENT '描述（使用说明）',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modified_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COMMENT='优惠券';


DROP TABLE IF EXISTS `jp_coupon_used`;
CREATE TABLE `jp_coupon_used` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `coupon_id` bigint(20) unsigned NOT NULL COMMENT '优惠券表ID',
  `user_id` BIGINT(20) UNSIGNED NOT NULL COMMENT '领取优惠券的用户ID',
  `used` int(2) unsigned DEFAULT '0' COMMENT '是否使用：1 是 0 否',
  `transaction_id` bigint(20) unsigned DEFAULT NULL COMMENT '订单id',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modified_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `transaction_id` (`transaction_id`),
  UNIQUE KEY `coupon_user` (`coupon_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券使用记录';


