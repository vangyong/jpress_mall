ALTER TABLE `jp_user` ADD `openid` varchar(100) DEFAULT NULL COMMENT '第三方用户id';
ALTER TABLE `jp_user` ADD `pid` bigint(20) unsigned NOT NULL COMMENT '父用户id';
