-- 新增快递单号字段，原有express字段为快递单信息字段，如快递公司名称
ALTER TABLE `jpress_mall`.`jp_transaction` add `express_no` varchar(128) DEFAULT NULL COMMENT '快递单号';
