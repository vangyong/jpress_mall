-- 增加退款单表（退款粒度只能以订单为单位）
DROP TABLE IF EXISTS `jpress_mall`.`jp_refund`;
CREATE TABLE `jp_refund` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `refund_no` varchar(128) DEFAULT NULL COMMENT '退款单编号',
  `order_no` varchar(128) DEFAULT NULL COMMENT '本地订单号',
  `trade_no` varchar(128) DEFAULT NULL COMMENT '微信/支付宝订单号',
  `trade_refund_no` varchar(128) DEFAULT NULL COMMENT '微信/支付宝退款单号',
  `status` varchar(10) NOT NULL COMMENT '状态：退款申请成功|退款中|退款成功|退款失败',
  `amount` decimal(10,2) unsigned DEFAULT '0.00' COMMENT '退款金额',
  `desc` varchar(256) DEFAULT NULL COMMENT '退款原因',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modified_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `refund_order_status` (`refund_no`,`order_no`,`status`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COMMENT='退款单（以订单为单位,且一个订单最多只允许一个退款单）';

-- 奖金流水表 奖金类型增加：6 订单退款
ALTER TABLE `jpress_mall`.`jp_bonus` modify `bonus_type` int(2) unsigned DEFAULT '0' COMMENT '奖金分类（1 个人提成-直接推广;2 个人提成-间接推广;3 团队提成;4 团队管理费;5 订单消费;6 订单退款）';
