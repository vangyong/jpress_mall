ALTER TABLE `jp_user` ADD `childNum` int(11) unsigned DEFAULT '0' COMMENT '直推人数（直接子节点数）';
ALTER TABLE `jp_user` ADD `teamNum` int(11) unsigned DEFAULT '0' COMMENT '团队人数总计（直接子节点数+间接子节点数，目前层级3级）';
ALTER TABLE `jp_user` ADD `teamBuyAmount` decimal(12,2) unsigned DEFAULT '0.00' COMMENT '团队消费金额总计';

DROP TABLE IF EXISTS `jp_bonus`;

CREATE TABLE `jp_bonus` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `transaction_id` bigint(20) unsigned NOT NULL COMMENT '产生奖金的订单id',
  `user_id` BIGINT(20) UNSIGNED NOT NULL COMMENT '获得奖金的用户ID',
  `amount` decimal(10,2) unsigned DEFAULT '0.00' COMMENT '金额（奖金）',
  `bonus_cycle` int(2) unsigned DEFAULT '0' COMMENT '奖金计算周期类型（1 按订单结算也就是实时结算;2 按月结算）',
  `bonus_type` int(2) unsigned DEFAULT '0' COMMENT '奖金分类（1 个人提成-直接推广;2 个人提成-间接推广;3 团队提成;4 团队管理费）',
  `bonus_time` datetime DEFAULT NULL COMMENT '奖金产生时间',
  PRIMARY KEY (`id`),
  KEY `transaction_id` (`transaction_id`),
  KEY `user_id` (`user_id`),
  KEY `bonus_cycle` (`bonus_cycle`),
  KEY `bonus_type` (`bonus_type`),
  KEY `bonus_time` (`bonus_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户奖金表，保存用户获得的奖金信息。';
