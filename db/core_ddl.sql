
SET NAMES utf8;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `ACT_EVT_LOG`
-- ----------------------------
CREATE TABLE `ACT_EVT_LOG` (
  `LOG_NR_` bigint(20) NOT NULL AUTO_INCREMENT,
  `TYPE_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `TIME_STAMP_` timestamp(3) NOT NULL,
  `USER_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `DATA_` longblob,
  `LOCK_OWNER_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `LOCK_TIME_` timestamp(3) NULL DEFAULT NULL,
  `IS_PROCESSED_` tinyint(4) DEFAULT '0',
  PRIMARY KEY (`LOG_NR_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
--  Table structure for `ACT_GE_BYTEARRAY`
-- ----------------------------
CREATE TABLE `ACT_GE_BYTEARRAY` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int(11) DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `BYTES_` longblob,
  `GENERATED_` tinyint(4) DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_FK_BYTEARR_DEPL` (`DEPLOYMENT_ID_`),
  CONSTRAINT `ACT_GE_BYTEARRAY_ibfk_1` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `ACT_RE_DEPLOYMENT` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
--  Table structure for `ACT_GE_PROPERTY`
-- ----------------------------
CREATE TABLE `ACT_GE_PROPERTY` (
  `NAME_` varchar(64) COLLATE utf8_bin NOT NULL,
  `VALUE_` varchar(300) COLLATE utf8_bin DEFAULT NULL,
  `REV_` int(11) DEFAULT NULL,
  PRIMARY KEY (`NAME_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
--  Table structure for `ACT_HI_ACTINST`
-- ----------------------------
CREATE TABLE `ACT_HI_ACTINST` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `ACT_ID_` varchar(255) COLLATE utf8_bin NOT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `CALL_PROC_INST_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `ACT_NAME_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `ACT_TYPE_` varchar(255) COLLATE utf8_bin NOT NULL,
  `ASSIGNEE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `START_TIME_` datetime(3) NOT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `DURATION_` bigint(20) DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_ACT_INST_START` (`START_TIME_`),
  KEY `ACT_IDX_HI_ACT_INST_END` (`END_TIME_`),
  KEY `ACT_IDX_HI_ACT_INST_PROCINST` (`PROC_INST_ID_`,`ACT_ID_`),
  KEY `ACT_IDX_HI_ACT_INST_EXEC` (`EXECUTION_ID_`,`ACT_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
--  Table structure for `ACT_HI_ATTACHMENT`
-- ----------------------------
CREATE TABLE `ACT_HI_ATTACHMENT` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int(11) DEFAULT NULL,
  `USER_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `URL_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `CONTENT_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `TIME_` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
--  Table structure for `ACT_HI_COMMENT`
-- ----------------------------
CREATE TABLE `ACT_HI_COMMENT` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `TIME_` datetime(3) NOT NULL,
  `USER_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `ACTION_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `MESSAGE_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `FULL_MSG_` longblob,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
--  Table structure for `ACT_HI_DETAIL`
-- ----------------------------
CREATE TABLE `ACT_HI_DETAIL` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `TYPE_` varchar(255) COLLATE utf8_bin NOT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `ACT_INST_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8_bin NOT NULL,
  `VAR_TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `REV_` int(11) DEFAULT NULL,
  `TIME_` datetime(3) NOT NULL,
  `BYTEARRAY_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `DOUBLE_` double DEFAULT NULL,
  `LONG_` bigint(20) DEFAULT NULL,
  `TEXT_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `TEXT2_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_DETAIL_PROC_INST` (`PROC_INST_ID_`),
  KEY `ACT_IDX_HI_DETAIL_ACT_INST` (`ACT_INST_ID_`),
  KEY `ACT_IDX_HI_DETAIL_TIME` (`TIME_`),
  KEY `ACT_IDX_HI_DETAIL_NAME` (`NAME_`),
  KEY `ACT_IDX_HI_DETAIL_TASK_ID` (`TASK_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
--  Table structure for `ACT_HI_IDENTITYLINK`
-- ----------------------------
CREATE TABLE `ACT_HI_IDENTITYLINK` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `GROUP_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `USER_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_IDENT_LNK_USER` (`USER_ID_`),
  KEY `ACT_IDX_HI_IDENT_LNK_TASK` (`TASK_ID_`),
  KEY `ACT_IDX_HI_IDENT_LNK_PROCINST` (`PROC_INST_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
--  Table structure for `ACT_HI_PROCINST`
-- ----------------------------
CREATE TABLE `ACT_HI_PROCINST` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `BUSINESS_KEY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `START_TIME_` datetime(3) NOT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `DURATION_` bigint(20) DEFAULT NULL,
  `START_USER_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `START_ACT_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `END_ACT_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SUPER_PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `DELETE_REASON_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8_bin DEFAULT '',
  `NAME_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `PROC_INST_ID_` (`PROC_INST_ID_`),
  KEY `ACT_IDX_HI_PRO_INST_END` (`END_TIME_`),
  KEY `ACT_IDX_HI_PRO_I_BUSKEY` (`BUSINESS_KEY_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
--  Table structure for `ACT_HI_TASKINST`
-- ----------------------------
CREATE TABLE `ACT_HI_TASKINST` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `TASK_DEF_KEY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `PARENT_TASK_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `OWNER_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `ASSIGNEE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `START_TIME_` datetime(3) NOT NULL,
  `CLAIM_TIME_` datetime(3) DEFAULT NULL,
  `END_TIME_` datetime(3) DEFAULT NULL,
  `DURATION_` bigint(20) DEFAULT NULL,
  `DELETE_REASON_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `PRIORITY_` int(11) DEFAULT NULL,
  `DUE_DATE_` datetime(3) DEFAULT NULL,
  `FORM_KEY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_TASK_INST_PROCINST` (`PROC_INST_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
--  Table structure for `ACT_HI_VARINST`
-- ----------------------------
CREATE TABLE `ACT_HI_VARINST` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8_bin NOT NULL,
  `VAR_TYPE_` varchar(100) COLLATE utf8_bin DEFAULT NULL,
  `REV_` int(11) DEFAULT NULL,
  `BYTEARRAY_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `DOUBLE_` double DEFAULT NULL,
  `LONG_` bigint(20) DEFAULT NULL,
  `TEXT_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `TEXT2_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `CREATE_TIME_` datetime(3) DEFAULT NULL,
  `LAST_UPDATED_TIME_` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_HI_PROCVAR_PROC_INST` (`PROC_INST_ID_`),
  KEY `ACT_IDX_HI_PROCVAR_NAME_TYPE` (`NAME_`,`VAR_TYPE_`),
  KEY `ACT_IDX_HI_PROCVAR_TASK_ID` (`TASK_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
--  Table structure for `ACT_ID_GROUP`
-- ----------------------------
CREATE TABLE `ACT_ID_GROUP` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int(11) DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
--  Table structure for `ACT_ID_INFO`
-- ----------------------------
CREATE TABLE `ACT_ID_INFO` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int(11) DEFAULT NULL,
  `USER_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `TYPE_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `VALUE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `PASSWORD_` longblob,
  `PARENT_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
--  Table structure for `ACT_ID_MEMBERSHIP`
-- ----------------------------
CREATE TABLE `ACT_ID_MEMBERSHIP` (
  `USER_ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `GROUP_ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  PRIMARY KEY (`USER_ID_`,`GROUP_ID_`),
  KEY `ACT_FK_MEMB_GROUP` (`GROUP_ID_`),
  CONSTRAINT `ACT_ID_MEMBERSHIP_ibfk_1` FOREIGN KEY (`GROUP_ID_`) REFERENCES `ACT_ID_GROUP` (`ID_`),
  CONSTRAINT `ACT_ID_MEMBERSHIP_ibfk_2` FOREIGN KEY (`USER_ID_`) REFERENCES `ACT_ID_USER` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
--  Table structure for `ACT_ID_USER`
-- ----------------------------
CREATE TABLE `ACT_ID_USER` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int(11) DEFAULT NULL,
  `FIRST_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `LAST_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `EMAIL_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `PWD_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `PICTURE_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
--  Table structure for `ACT_PROCDEF_INFO`
-- ----------------------------
CREATE TABLE `ACT_PROCDEF_INFO` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int(11) DEFAULT NULL,
  `INFO_JSON_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_UNIQ_INFO_PROCDEF` (`PROC_DEF_ID_`),
  KEY `ACT_IDX_INFO_PROCDEF` (`PROC_DEF_ID_`),
  KEY `ACT_FK_INFO_JSON_BA` (`INFO_JSON_ID_`),
  CONSTRAINT `ACT_PROCDEF_INFO_ibfk_1` FOREIGN KEY (`INFO_JSON_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_PROCDEF_INFO_ibfk_2` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `ACT_RE_PROCDEF` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
--  Table structure for `ACT_RE_DEPLOYMENT`
-- ----------------------------
CREATE TABLE `ACT_RE_DEPLOYMENT` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `NAME_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8_bin DEFAULT '',
  `DEPLOY_TIME_` timestamp(3) NULL DEFAULT NULL,
  PRIMARY KEY (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
--  Table structure for `ACT_RE_MODEL`
-- ----------------------------
CREATE TABLE `ACT_RE_MODEL` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int(11) DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LAST_UPDATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `VERSION_` int(11) DEFAULT NULL,
  `META_INFO_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `DEPLOYMENT_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `EDITOR_SOURCE_VALUE_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `EDITOR_SOURCE_EXTRA_VALUE_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_FK_MODEL_SOURCE` (`EDITOR_SOURCE_VALUE_ID_`),
  KEY `ACT_FK_MODEL_SOURCE_EXTRA` (`EDITOR_SOURCE_EXTRA_VALUE_ID_`),
  KEY `ACT_FK_MODEL_DEPLOYMENT` (`DEPLOYMENT_ID_`),
  CONSTRAINT `ACT_RE_MODEL_ibfk_1` FOREIGN KEY (`DEPLOYMENT_ID_`) REFERENCES `ACT_RE_DEPLOYMENT` (`ID_`),
  CONSTRAINT `ACT_RE_MODEL_ibfk_2` FOREIGN KEY (`EDITOR_SOURCE_VALUE_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_RE_MODEL_ibfk_3` FOREIGN KEY (`EDITOR_SOURCE_EXTRA_VALUE_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
--  Table structure for `ACT_RE_PROCDEF`
-- ----------------------------
CREATE TABLE `ACT_RE_PROCDEF` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int(11) DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `KEY_` varchar(255) COLLATE utf8_bin NOT NULL,
  `VERSION_` int(11) NOT NULL,
  `DEPLOYMENT_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `RESOURCE_NAME_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `DGRM_RESOURCE_NAME_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `HAS_START_FORM_KEY_` tinyint(4) DEFAULT NULL,
  `HAS_GRAPHICAL_NOTATION_` tinyint(4) DEFAULT NULL,
  `SUSPENSION_STATE_` int(11) DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  UNIQUE KEY `ACT_UNIQ_PROCDEF` (`KEY_`,`VERSION_`,`TENANT_ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
--  Table structure for `ACT_RU_EVENT_SUBSCR`
-- ----------------------------
CREATE TABLE `ACT_RU_EVENT_SUBSCR` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int(11) DEFAULT NULL,
  `EVENT_TYPE_` varchar(255) COLLATE utf8_bin NOT NULL,
  `EVENT_NAME_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `ACTIVITY_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `CONFIGURATION_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `CREATED_` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `PROC_DEF_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_EVENT_SUBSCR_CONFIG_` (`CONFIGURATION_`),
  KEY `ACT_FK_EVENT_EXEC` (`EXECUTION_ID_`),
  CONSTRAINT `ACT_RU_EVENT_SUBSCR_ibfk_1` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
--  Table structure for `ACT_RU_EXECUTION`
-- ----------------------------
CREATE TABLE `ACT_RU_EXECUTION` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int(11) DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `BUSINESS_KEY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `PARENT_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `SUPER_EXEC_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `ACT_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `IS_ACTIVE_` tinyint(4) DEFAULT NULL,
  `IS_CONCURRENT_` tinyint(4) DEFAULT NULL,
  `IS_SCOPE_` tinyint(4) DEFAULT NULL,
  `IS_EVENT_SCOPE_` tinyint(4) DEFAULT NULL,
  `SUSPENSION_STATE_` int(11) DEFAULT NULL,
  `CACHED_ENT_STATE_` int(11) DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8_bin DEFAULT '',
  `NAME_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `LOCK_TIME_` timestamp(3) NULL DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_EXEC_BUSKEY` (`BUSINESS_KEY_`),
  KEY `ACT_FK_EXE_PROCINST` (`PROC_INST_ID_`),
  KEY `ACT_FK_EXE_PARENT` (`PARENT_ID_`),
  KEY `ACT_FK_EXE_SUPER` (`SUPER_EXEC_`),
  KEY `ACT_FK_EXE_PROCDEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_RU_EXECUTION_ibfk_1` FOREIGN KEY (`PARENT_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`),
  CONSTRAINT `ACT_RU_EXECUTION_ibfk_2` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `ACT_RE_PROCDEF` (`ID_`),
  CONSTRAINT `ACT_RU_EXECUTION_ibfk_3` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `ACT_RU_EXECUTION_ibfk_4` FOREIGN KEY (`SUPER_EXEC_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
--  Table structure for `ACT_RU_IDENTITYLINK`
-- ----------------------------
CREATE TABLE `ACT_RU_IDENTITYLINK` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int(11) DEFAULT NULL,
  `GROUP_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `USER_ID_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_IDENT_LNK_USER` (`USER_ID_`),
  KEY `ACT_IDX_IDENT_LNK_GROUP` (`GROUP_ID_`),
  KEY `ACT_IDX_ATHRZ_PROCEDEF` (`PROC_DEF_ID_`),
  KEY `ACT_FK_TSKASS_TASK` (`TASK_ID_`),
  KEY `ACT_FK_IDL_PROCINST` (`PROC_INST_ID_`),
  CONSTRAINT `ACT_RU_IDENTITYLINK_ibfk_1` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `ACT_RE_PROCDEF` (`ID_`),
  CONSTRAINT `ACT_RU_IDENTITYLINK_ibfk_2` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`),
  CONSTRAINT `ACT_RU_IDENTITYLINK_ibfk_3` FOREIGN KEY (`TASK_ID_`) REFERENCES `ACT_RU_TASK` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
--  Table structure for `ACT_RU_JOB`
-- ----------------------------
CREATE TABLE `ACT_RU_JOB` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int(11) DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8_bin NOT NULL,
  `LOCK_EXP_TIME_` timestamp(3) NULL DEFAULT NULL,
  `LOCK_OWNER_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `EXCLUSIVE_` tinyint(1) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PROCESS_INSTANCE_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `RETRIES_` int(11) DEFAULT NULL,
  `EXCEPTION_STACK_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `EXCEPTION_MSG_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `DUEDATE_` timestamp(3) NULL DEFAULT NULL,
  `REPEAT_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `HANDLER_TYPE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `HANDLER_CFG_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8_bin DEFAULT '',
  PRIMARY KEY (`ID_`),
  KEY `ACT_FK_JOB_EXCEPTION` (`EXCEPTION_STACK_ID_`),
  CONSTRAINT `ACT_RU_JOB_ibfk_1` FOREIGN KEY (`EXCEPTION_STACK_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
--  Table structure for `ACT_RU_TASK`
-- ----------------------------
CREATE TABLE `ACT_RU_TASK` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int(11) DEFAULT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PROC_DEF_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `NAME_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `PARENT_TASK_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `DESCRIPTION_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `TASK_DEF_KEY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `OWNER_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `ASSIGNEE_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `DELEGATION_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PRIORITY_` int(11) DEFAULT NULL,
  `CREATE_TIME_` timestamp(3) NULL DEFAULT NULL,
  `DUE_DATE_` datetime(3) DEFAULT NULL,
  `CATEGORY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `SUSPENSION_STATE_` int(11) DEFAULT NULL,
  `TENANT_ID_` varchar(255) COLLATE utf8_bin DEFAULT '',
  `FORM_KEY_` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_TASK_CREATE` (`CREATE_TIME_`),
  KEY `ACT_FK_TASK_EXE` (`EXECUTION_ID_`),
  KEY `ACT_FK_TASK_PROCINST` (`PROC_INST_ID_`),
  KEY `ACT_FK_TASK_PROCDEF` (`PROC_DEF_ID_`),
  CONSTRAINT `ACT_RU_TASK_ibfk_1` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`),
  CONSTRAINT `ACT_RU_TASK_ibfk_2` FOREIGN KEY (`PROC_DEF_ID_`) REFERENCES `ACT_RE_PROCDEF` (`ID_`),
  CONSTRAINT `ACT_RU_TASK_ibfk_3` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
--  Table structure for `ACT_RU_VARIABLE`
-- ----------------------------
CREATE TABLE `ACT_RU_VARIABLE` (
  `ID_` varchar(64) COLLATE utf8_bin NOT NULL,
  `REV_` int(11) DEFAULT NULL,
  `TYPE_` varchar(255) COLLATE utf8_bin NOT NULL,
  `NAME_` varchar(255) COLLATE utf8_bin NOT NULL,
  `EXECUTION_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `PROC_INST_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `TASK_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `BYTEARRAY_ID_` varchar(64) COLLATE utf8_bin DEFAULT NULL,
  `DOUBLE_` double DEFAULT NULL,
  `LONG_` bigint(20) DEFAULT NULL,
  `TEXT_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  `TEXT2_` varchar(4000) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`ID_`),
  KEY `ACT_IDX_VARIABLE_TASK_ID` (`TASK_ID_`),
  KEY `ACT_FK_VAR_EXE` (`EXECUTION_ID_`),
  KEY `ACT_FK_VAR_PROCINST` (`PROC_INST_ID_`),
  KEY `ACT_FK_VAR_BYTEARRAY` (`BYTEARRAY_ID_`),
  CONSTRAINT `ACT_RU_VARIABLE_ibfk_1` FOREIGN KEY (`BYTEARRAY_ID_`) REFERENCES `ACT_GE_BYTEARRAY` (`ID_`),
  CONSTRAINT `ACT_RU_VARIABLE_ibfk_2` FOREIGN KEY (`EXECUTION_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`),
  CONSTRAINT `ACT_RU_VARIABLE_ibfk_3` FOREIGN KEY (`PROC_INST_ID_`) REFERENCES `ACT_RU_EXECUTION` (`ID_`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
--  Table structure for `t_account`
-- ----------------------------
CREATE TABLE `t_account` (
  `id` varchar(19) NOT NULL,
  `create_by` varchar(19) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `is_deleted` bit(1) NOT NULL,
  `update_by` varchar(19) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `address` varchar(200) DEFAULT NULL,
  `bankcard_no` varchar(64) NOT NULL,
  `birthday` varchar(40) NOT NULL,
  `customer_no` varchar(64) NOT NULL,
  `email` varchar(200) DEFAULT NULL,
  `idcard_no` varchar(64) NOT NULL,
  `idcard_photo` varchar(400) DEFAULT NULL,
  `identifier` varchar(64) NOT NULL,
  `license_plate_no` varchar(32) NOT NULL,
  `name` varchar(64) NOT NULL,
  `phone` varchar(32) NOT NULL,
  `sex` varchar(45) DEFAULT NULL,
  `appid` varchar(19) NOT NULL,
  `is_staff` int(11) NOT NULL,
  `staff_no` varchar(40) NOT NULL,
  `dept_no` varchar(40) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_0` (`name`) USING BTREE,
  KEY `idx_1` (`identifier`) USING BTREE,
  KEY `idx_2` (`staff_no`) USING BTREE,
  KEY `idx_3` (`phone`) USING BTREE,
  KEY `idx_4` (`is_deleted`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
--  Table structure for `t_account_export`
-- ----------------------------
CREATE TABLE `t_account_export` (
  `id` varchar(19) NOT NULL,
  `create_by` varchar(19) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `is_deleted` bit(1) NOT NULL,
  `update_by` varchar(19) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `platform_code` varchar(255) NOT NULL,
  `record_count` int(11) NOT NULL,
  `appid` varchar(19) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
--  Table structure for `t_account_export_record`
-- ----------------------------
CREATE TABLE `t_account_export_record` (
  `id` varchar(19) NOT NULL,
  `create_by` varchar(19) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `is_deleted` bit(1) NOT NULL,
  `update_by` varchar(19) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `identifier` varchar(32) NOT NULL,
  `appid` varchar(19) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_0` (`identifier`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
--  Table structure for `t_account_form`
-- ----------------------------
CREATE TABLE `t_account_form` (
  `id` varchar(19) NOT NULL,
  `create_by` varchar(19) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `is_deleted` bit(1) NOT NULL,
  `update_by` varchar(19) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `appid` varchar(19) NOT NULL,
  `end_date` date NOT NULL,
  `end_time` varchar(10) NOT NULL,
  `name` varchar(50) NOT NULL,
  `remark` varchar(2000) NOT NULL,
  `start_date` date NOT NULL,
  `start_time` varchar(10) NOT NULL,
  `status` int(11) NOT NULL,
  `title` varchar(50) NOT NULL,
  `agreement` text NOT NULL,
  `enable_agreement` bit(1) NOT NULL,
  `enable_captcha` bit(1) NOT NULL,
  `form_type` int(11) NOT NULL,
  `platform_limit` varchar(100) NOT NULL,
  `redirect_url` varchar(300) NOT NULL,
  `is_identifier_form` bit(1) NOT NULL,
  `is_default` bit(1) NOT NULL,
  `check_staff` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_0` (`name`) USING BTREE,
  KEY `idx_1` (`form_type`) USING BTREE,
  KEY `idx_2` (`title`) USING BTREE,
  KEY `idx_3` (`is_deleted`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
--  Table structure for `t_account_form_field`
-- ----------------------------
CREATE TABLE `t_account_form_field` (
  `id` varchar(19) NOT NULL,
  `create_by` varchar(19) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `is_deleted` bit(1) NOT NULL,
  `update_by` varchar(19) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `account_id` varchar(19) NOT NULL,
  `field_data` text NOT NULL,
  `field_short_data` varchar(600) NOT NULL,
  `form_id` varchar(19) NOT NULL,
  `meta_title` varchar(20) NOT NULL,
  `meta_type` varchar(20) NOT NULL,
  `meta_id` varchar(19) NOT NULL,
  `appid` varchar(19) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_0` (`account_id`) USING BTREE,
  KEY `idx_1` (`form_id`) USING BTREE,
  KEY `idx_2` (`meta_title`) USING BTREE,
  KEY `idx_3` (`appid`) USING BTREE,
  KEY `idx_4` (`is_deleted`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
--  Table structure for `t_account_form_meta`
-- ----------------------------
CREATE TABLE `t_account_form_meta` (
  `id` varchar(19) NOT NULL,
  `create_by` varchar(19) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `is_deleted` bit(1) NOT NULL,
  `update_by` varchar(19) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `appid` varchar(19) NOT NULL,
  `account_form_id` varchar(19) NOT NULL,
  `is_identifier` bit(1) NOT NULL,
  `is_required` bit(1) NOT NULL,
  `is_standard` bit(1) NOT NULL,
  `max_length` int(11) NOT NULL,
  `meta_data` text NOT NULL,
  `meta_desc` varchar(500) NOT NULL,
  `meta_type` varchar(20) NOT NULL,
  `min_length` int(11) NOT NULL,
  `place_holder` varchar(200) NOT NULL,
  `required_notice` varchar(200) NOT NULL,
  `sort_order` int(11) NOT NULL,
  `title` varchar(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_0` (`is_deleted`) USING BTREE,
  KEY `idx_1` (`appid`) USING BTREE,
  KEY `idx_2` (`account_form_id`) USING BTREE,
  KEY `idx_3` (`title`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
--  Table structure for `t_account_form_resource`
-- ----------------------------
CREATE TABLE `t_account_form_resource` (
  `id` varchar(19) NOT NULL,
  `create_by` varchar(19) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `is_deleted` bit(1) NOT NULL,
  `update_by` varchar(19) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `form_id` varchar(19) NOT NULL,
  `resource_data` text NOT NULL,
  `appid` varchar(19) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_0` (`form_id`) USING BTREE,
  KEY `idx_1` (`appid`) USING BTREE,
  KEY `idx_2` (`is_deleted`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
--  Table structure for `t_achieve_list`
-- ----------------------------
CREATE TABLE `t_achieve_list` (
  `id` varchar(19) NOT NULL,
  `create_by` varchar(19) NOT NULL,
  `create_time` datetime NOT NULL,
  `is_deleted` bit(1) NOT NULL,
  `update_by` varchar(19) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `appid` varchar(19) NOT NULL,
  `expire_date` datetime NOT NULL,
  `form_id` varchar(19) NOT NULL,
  `list_type` int(11) NOT NULL,
  `name` varchar(20) NOT NULL,
  `remark` varchar(2000) NOT NULL,
  `validate_fields` varchar(200) NOT NULL,
  `link_id` varchar(19) NOT NULL,
  `link_type` tinyint(4) NOT NULL,
  `is_times` tinyint(4) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_0` (`appid`) USING BTREE,
  KEY `idx_1` (`form_id`) USING BTREE,
  KEY `idx_2` (`name`) USING BTREE,
  KEY `idx_3` (`is_deleted`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
--  Table structure for `t_achieve_list_extend_record`
-- ----------------------------
CREATE TABLE `t_achieve_list_extend_record` (
  `id` varchar(19) NOT NULL,
  `create_by` varchar(19) NOT NULL,
  `create_time` datetime NOT NULL,
  `is_deleted` bit(1) NOT NULL,
  `update_by` varchar(19) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `form_meta_id` varchar(19) NOT NULL,
  `list_id` varchar(19) NOT NULL,
  `meta_code` varchar(10) NOT NULL,
  `meta_title` varchar(10) NOT NULL,
  `record` varchar(50) NOT NULL,
  `record_id` varchar(19) NOT NULL,
  `appid` varchar(19) NOT NULL,
  `identifier` varchar(32) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_u_1` (`list_id`,`identifier`,`form_meta_id`),
  KEY `idx_0` (`identifier`) USING BTREE,
  KEY `idx_1` (`list_id`) USING BTREE,
  KEY `idx_2` (`form_meta_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
--  Table structure for `t_achieve_list_record`
-- ----------------------------
CREATE TABLE `t_achieve_list_record` (
  `id` varchar(19) NOT NULL,
  `create_by` varchar(19) NOT NULL,
  `create_time` datetime NOT NULL,
  `is_deleted` bit(1) NOT NULL,
  `update_by` varchar(19) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `identifier` varchar(32) NOT NULL,
  `list_id` varchar(19) NOT NULL,
  `status` int(11) NOT NULL,
  `appid` varchar(19) NOT NULL,
  `times` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_u_1` (`is_deleted`,`identifier`,`list_id`),
  KEY `idx_0` (`is_deleted`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
--  Table structure for `t_act_account`
-- ----------------------------
CREATE TABLE `t_act_account` (
  `id` varchar(19) NOT NULL,
  `create_by` varchar(19) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `is_deleted` bit(1) NOT NULL,
  `update_by` varchar(19) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `act_account_id` varchar(19) NOT NULL,
  `core_account_id` varchar(19) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_0` (`is_deleted`),
  KEY `idx_1` (`act_account_id`),
  KEY `idx_2` (`core_account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
--  Table structure for `t_act_business`
-- ----------------------------
CREATE TABLE `t_act_business` (
  `id` varchar(255) NOT NULL,
  `create_by` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(255) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `proc_def_id` varchar(255) DEFAULT NULL,
  `proc_inst_id` varchar(255) DEFAULT NULL,
  `result` int(11) DEFAULT NULL,
  `status` int(11) DEFAULT NULL,
  `table_id` varchar(255) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `user_id` varchar(255) DEFAULT NULL,
  `apply_time` datetime DEFAULT NULL,
  `is_history` bit(1) DEFAULT NULL,
  `is_deleted` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `t_act_category`
-- ----------------------------
CREATE TABLE `t_act_category` (
  `id` varchar(255) NOT NULL,
  `create_by` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(255) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `is_parent` bit(1) DEFAULT NULL,
  `parent_id` varchar(255) DEFAULT NULL,
  `sort_order` decimal(10,2) DEFAULT NULL,
  `status` int(11) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `type` int(11) DEFAULT NULL,
  `is_deleted` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `t_act_model`
-- ----------------------------
CREATE TABLE `t_act_model` (
  `id` varchar(255) NOT NULL,
  `create_by` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(255) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `model_key` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `version` int(11) DEFAULT NULL,
  `is_deleted` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `t_act_node`
-- ----------------------------
CREATE TABLE `t_act_node` (
  `id` varchar(255) NOT NULL,
  `create_by` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(255) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `node_id` varchar(255) DEFAULT NULL,
  `type` varchar(255) DEFAULT NULL,
  `relate_id` varchar(255) DEFAULT NULL,
  `is_deleted` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `t_act_process`
-- ----------------------------
CREATE TABLE `t_act_process` (
  `id` varchar(255) NOT NULL,
  `create_by` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(255) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `category_id` varchar(255) DEFAULT NULL,
  `deployment_id` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `diagram_name` varchar(255) DEFAULT NULL,
  `latest` bit(1) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `process_key` varchar(255) DEFAULT NULL,
  `status` int(11) DEFAULT NULL,
  `version` int(11) DEFAULT NULL,
  `xml_name` varchar(255) DEFAULT NULL,
  `business_table` varchar(255) DEFAULT NULL,
  `route_name` varchar(255) DEFAULT NULL,
  `is_deleted` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `t_api_account`
-- ----------------------------
CREATE TABLE `t_api_account` (
  `id` varchar(19) NOT NULL,
  `create_by` varchar(19) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `is_deleted` bit(1) NOT NULL,
  `update_by` varchar(19) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `account_name` varchar(20) NOT NULL,
  `app_secret` varchar(32) NOT NULL,
  `appid` varchar(19) NOT NULL,
  `appkey` varchar(32) NOT NULL,
  `component_appid` varchar(19) NOT NULL,
  `ip_white_list` text NOT NULL,
  `status` tinyint(4) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_0` (`is_deleted`),
  KEY `idx_1` (`appkey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
--  Table structure for `t_department`
-- ----------------------------
CREATE TABLE `t_department` (
  `id` varchar(255) NOT NULL,
  `create_by` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(255) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `parent_id` varchar(255) DEFAULT NULL,
  `sort_order` decimal(10,2) DEFAULT NULL,
  `status` int(11) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `is_parent` bit(1) DEFAULT NULL,
  `is_deleted` bit(1) DEFAULT NULL,
  `dept_code` varchar(20) NOT NULL,
  `appid` varchar(19) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `t_department_header`
-- ----------------------------
CREATE TABLE `t_department_header` (
  `id` varchar(255) NOT NULL,
  `create_by` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(255) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `department_id` varchar(255) DEFAULT NULL,
  `type` int(11) DEFAULT NULL,
  `user_id` varchar(255) DEFAULT NULL,
  `is_deleted` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `t_dict`
-- ----------------------------
CREATE TABLE `t_dict` (
  `id` varchar(255) NOT NULL,
  `create_by` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(255) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `type` varchar(255) DEFAULT NULL,
  `sort_order` decimal(10,2) DEFAULT NULL,
  `is_deleted` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `t_dict_data`
-- ----------------------------
CREATE TABLE `t_dict_data` (
  `id` varchar(255) NOT NULL,
  `create_by` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(255) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `dict_id` varchar(255) DEFAULT NULL,
  `sort_order` decimal(10,2) DEFAULT NULL,
  `status` int(11) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `value` varchar(255) DEFAULT NULL,
  `is_deleted` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `t_exception_log`
-- ----------------------------
CREATE TABLE `t_exception_log` (
  `id` varchar(19) NOT NULL,
  `create_by` varchar(19) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `is_deleted` bit(1) NOT NULL,
  `update_by` varchar(19) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `exception` longtext NOT NULL,
  `msg_body` longtext NOT NULL,
  `url` text NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
--  Table structure for `t_file`
-- ----------------------------
CREATE TABLE `t_file` (
  `id` varchar(255) NOT NULL,
  `create_by` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(255) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `size` bigint(20) DEFAULT NULL,
  `type` varchar(255) DEFAULT NULL,
  `url` varchar(255) DEFAULT NULL,
  `f_key` varchar(255) DEFAULT NULL,
  `location` int(11) DEFAULT NULL,
  `is_deleted` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `t_github`
-- ----------------------------
CREATE TABLE `t_github` (
  `id` varchar(255) NOT NULL,
  `create_by` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(255) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `avatar` varchar(255) DEFAULT NULL,
  `is_related` bit(1) DEFAULT NULL,
  `open_id` varchar(255) DEFAULT NULL,
  `relate_username` varchar(255) DEFAULT NULL,
  `username` varchar(255) DEFAULT NULL,
  `is_deleted` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `t_group`
-- ----------------------------
CREATE TABLE `t_group` (
  `id` varchar(19) NOT NULL,
  `create_by` varchar(19) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `is_deleted` bit(1) NOT NULL,
  `update_by` varchar(19) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `appid` varchar(19) NOT NULL,
  `name` varchar(20) NOT NULL,
  `remark` varchar(2000) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
--  Table structure for `t_group_user`
-- ----------------------------
CREATE TABLE `t_group_user` (
  `id` varchar(19) NOT NULL,
  `create_by` varchar(19) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `is_deleted` bit(1) NOT NULL,
  `update_by` varchar(19) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `appid` varchar(19) NOT NULL,
  `account_id` varchar(19) NOT NULL,
  `group_id` varchar(19) NOT NULL,
  `is_staff` bit(1) NOT NULL,
  `phone` varchar(32) NOT NULL,
  `staff_no` varchar(20) NOT NULL,
  `status` tinyint(4) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
--  Table structure for `t_leave`
-- ----------------------------
CREATE TABLE `t_leave` (
  `id` varchar(255) NOT NULL,
  `create_by` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(255) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `end_date` datetime DEFAULT NULL,
  `start_date` datetime DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `duration` int(11) DEFAULT NULL,
  `type` varchar(255) DEFAULT NULL,
  `is_deleted` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `t_limit_list`
-- ----------------------------
CREATE TABLE `t_limit_list` (
  `id` varchar(19) NOT NULL,
  `create_by` varchar(19) NOT NULL,
  `create_time` datetime NOT NULL,
  `is_deleted` bit(1) NOT NULL,
  `update_by` varchar(19) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `list_type` int(11) NOT NULL,
  `name` varchar(20) NOT NULL,
  `remark` varchar(2000) NOT NULL,
  `validate_fields` varchar(200) NOT NULL,
  `form_id` varchar(19) NOT NULL,
  `expire_date` datetime DEFAULT NULL,
  `appid` varchar(19) NOT NULL,
  `link_id` varchar(19) NOT NULL,
  `link_type` tinyint(4) NOT NULL,
  `is_times` tinyint(4) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_0` (`is_deleted`) USING BTREE,
  KEY `idx_1` (`appid`) USING BTREE,
  KEY `idx_2` (`name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
--  Table structure for `t_limit_list_extend_record`
-- ----------------------------
CREATE TABLE `t_limit_list_extend_record` (
  `id` varchar(19) NOT NULL,
  `create_by` varchar(19) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `is_deleted` bit(1) NOT NULL,
  `update_by` varchar(19) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `form_meta_id` varchar(19) NOT NULL,
  `list_id` varchar(19) NOT NULL,
  `meta_code` varchar(10) NOT NULL,
  `meta_title` varchar(10) NOT NULL,
  `record` varchar(50) NOT NULL,
  `record_id` varchar(19) NOT NULL,
  `appid` varchar(19) NOT NULL,
  `identifier` varchar(32) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_u_1` (`list_id`,`identifier`,`form_meta_id`),
  KEY `idx_0` (`is_deleted`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
--  Table structure for `t_limit_list_record`
-- ----------------------------
CREATE TABLE `t_limit_list_record` (
  `id` varchar(19) NOT NULL,
  `create_by` varchar(19) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `is_deleted` bit(1) NOT NULL,
  `update_by` varchar(19) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `identifier` varchar(32) NOT NULL,
  `list_id` varchar(19) NOT NULL,
  `status` int(11) NOT NULL,
  `appid` varchar(19) NOT NULL,
  `times` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_u_0` (`is_deleted`,`identifier`,`list_id`),
  KEY `idx_0` (`is_deleted`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
--  Table structure for `t_log`
-- ----------------------------
CREATE TABLE `t_log` (
  `id` varchar(255) NOT NULL,
  `create_by` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(255) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `cost_time` int(11) DEFAULT NULL,
  `ip` varchar(255) DEFAULT NULL,
  `ip_info` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `request_param` longtext,
  `request_type` varchar(255) DEFAULT NULL,
  `request_url` varchar(255) DEFAULT NULL,
  `username` varchar(255) DEFAULT NULL,
  `log_type` int(11) DEFAULT NULL,
  `is_deleted` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `t_message`
-- ----------------------------
CREATE TABLE `t_message` (
  `id` varchar(255) NOT NULL,
  `create_by` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(255) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `create_send` bit(1) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `type` varchar(255) DEFAULT NULL,
  `content` longtext,
  `is_deleted` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `t_message_send`
-- ----------------------------
CREATE TABLE `t_message_send` (
  `id` varchar(255) NOT NULL,
  `create_by` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(255) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `message_id` varchar(255) DEFAULT NULL,
  `status` int(11) DEFAULT NULL,
  `user_id` varchar(255) DEFAULT NULL,
  `is_deleted` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `t_mq_exception`
-- ----------------------------
CREATE TABLE `t_mq_exception` (
  `id` varchar(19) NOT NULL,
  `create_by` varchar(19) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `is_deleted` bit(1) NOT NULL,
  `update_by` varchar(19) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `appid` varchar(19) NOT NULL,
  `exception` longtext NOT NULL,
  `msg_body` longtext NOT NULL,
  `queue_name` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_0` (`is_deleted`) USING BTREE,
  KEY `idx_1` (`appid`) USING BTREE,
  KEY `idx_2` (`queue_name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
--  Table structure for `t_permission`
-- ----------------------------
CREATE TABLE `t_permission` (
  `id` varchar(255) NOT NULL,
  `create_by` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(255) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `parent_id` varchar(255) DEFAULT NULL,
  `type` int(11) DEFAULT NULL,
  `sort_order` decimal(10,2) DEFAULT NULL,
  `component` varchar(255) DEFAULT NULL,
  `path` varchar(255) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `icon` varchar(255) DEFAULT NULL,
  `level` int(11) DEFAULT NULL,
  `button_type` varchar(255) DEFAULT NULL,
  `status` int(11) DEFAULT NULL,
  `url` varchar(255) DEFAULT NULL,
  `is_deleted` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_0` (`url`) USING BTREE,
  KEY `idx_1` (`name`) USING BTREE,
  KEY `idx_2` (`title`) USING BTREE,
  KEY `idx_3` (`path`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `t_phone_location`
-- ----------------------------
CREATE TABLE `t_phone_location` (
  `id` varchar(19) NOT NULL,
  `create_by` varchar(19) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `is_deleted` bit(1) NOT NULL,
  `update_by` varchar(19) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `city` varchar(25) NOT NULL,
  `company` varchar(25) NOT NULL,
  `phone` varchar(32) NOT NULL,
  `province` varchar(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_u_0` (`phone`) USING BTREE,
  KEY `idx_0` (`is_deleted`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
--  Table structure for `t_qq`
-- ----------------------------
CREATE TABLE `t_qq` (
  `id` varchar(255) NOT NULL,
  `create_by` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(255) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `avatar` varchar(255) DEFAULT NULL,
  `is_related` bit(1) DEFAULT NULL,
  `open_id` varchar(255) DEFAULT NULL,
  `relate_username` varchar(255) DEFAULT NULL,
  `username` varchar(255) DEFAULT NULL,
  `is_deleted` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `t_role`
-- ----------------------------
CREATE TABLE `t_role` (
  `id` varchar(255) NOT NULL,
  `create_by` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(255) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `default_role` bit(1) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `data_type` int(11) DEFAULT NULL,
  `is_deleted` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `t_role_department`
-- ----------------------------
CREATE TABLE `t_role_department` (
  `id` varchar(255) NOT NULL,
  `create_by` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(255) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `department_id` varchar(255) DEFAULT NULL,
  `role_id` varchar(255) DEFAULT NULL,
  `is_deleted` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `t_role_permission`
-- ----------------------------
CREATE TABLE `t_role_permission` (
  `id` varchar(255) NOT NULL,
  `create_by` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(255) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `permission_id` varchar(255) DEFAULT NULL,
  `role_id` varchar(255) DEFAULT NULL,
  `is_deleted` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_0` (`role_id`) USING BTREE,
  KEY `idx_1` (`permission_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `t_settings`
-- ----------------------------
CREATE TABLE `t_settings` (
  `id` varchar(19) NOT NULL,
  `create_by` varchar(19) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `is_deleted` bit(1) NOT NULL,
  `update_by` varchar(19) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `key_name` varchar(50) NOT NULL,
  `value` varchar(2000) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_0` (`key_name`) USING BTREE,
  KEY `idx_1` (`is_deleted`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
--  Table structure for `t_staff`
-- ----------------------------
CREATE TABLE `t_staff` (
  `id` varchar(19) NOT NULL,
  `create_by` varchar(19) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `is_deleted` bit(1) NOT NULL,
  `update_by` varchar(19) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `account_id` varchar(19) NOT NULL,
  `dept_no` varchar(20) NOT NULL,
  `name` varchar(10) NOT NULL,
  `phone` varchar(32) NOT NULL,
  `staff_no` varchar(20) NOT NULL,
  `status` int(11) NOT NULL,
  `appid` varchar(19) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_0` (`staff_no`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
--  Table structure for `t_user`
-- ----------------------------
CREATE TABLE `t_user` (
  `id` varchar(255) NOT NULL,
  `create_by` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(255) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `avatar` varchar(1000) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `mobile` varchar(255) DEFAULT NULL,
  `nick_name` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `sex` varchar(255) DEFAULT NULL,
  `status` int(11) DEFAULT NULL,
  `type` int(11) DEFAULT NULL,
  `username` varchar(255) DEFAULT NULL,
  `department_id` varchar(255) DEFAULT NULL,
  `street` varchar(255) DEFAULT NULL,
  `pass_strength` varchar(2) DEFAULT NULL,
  `is_deleted` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_0` (`username`) USING BTREE,
  KEY `idx_1` (`mobile`) USING BTREE,
  KEY `idx_2` (`mobile`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `t_user_role`
-- ----------------------------
CREATE TABLE `t_user_role` (
  `id` varchar(255) NOT NULL,
  `create_by` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(255) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `role_id` varchar(255) DEFAULT NULL,
  `user_id` varchar(255) DEFAULT NULL,
  `is_deleted` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_0` (`role_id`) USING BTREE,
  KEY `idx_1` (`user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `t_weibo`
-- ----------------------------
CREATE TABLE `t_weibo` (
  `id` varchar(255) NOT NULL,
  `create_by` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(255) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `avatar` varchar(255) DEFAULT NULL,
  `is_related` bit(1) DEFAULT NULL,
  `open_id` varchar(255) DEFAULT NULL,
  `relate_username` varchar(255) DEFAULT NULL,
  `username` varchar(255) DEFAULT NULL,
  `is_deleted` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `t_white_list`
-- ----------------------------
CREATE TABLE `t_white_list` (
  `id` varchar(19) NOT NULL,
  `create_by` varchar(19) NOT NULL,
  `create_time` datetime NOT NULL,
  `is_deleted` bit(1) NOT NULL,
  `update_by` varchar(19) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `list_type` int(11) NOT NULL,
  `name` varchar(20) NOT NULL,
  `remark` varchar(1000) NOT NULL,
  `validate_fields` varchar(2000) NOT NULL,
  `expire_date` datetime DEFAULT NULL,
  `form_id` varchar(19) NOT NULL,
  `appid` varchar(19) NOT NULL,
  `link_id` varchar(19) NOT NULL,
  `link_type` tinyint(4) NOT NULL,
  `is_times` tinyint(4) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_u_0` (`name`),
  KEY `idx_0` (`is_deleted`) USING BTREE,
  KEY `idx_1` (`appid`) USING BTREE,
  KEY `idx_2` (`form_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
--  Table structure for `t_white_list_extend_record`
-- ----------------------------
CREATE TABLE `t_white_list_extend_record` (
  `id` varchar(19) NOT NULL,
  `create_by` varchar(19) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `is_deleted` bit(1) NOT NULL,
  `update_by` varchar(19) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `form_meta_id` varchar(19) NOT NULL,
  `list_id` varchar(19) NOT NULL,
  `meta_code` varchar(10) NOT NULL,
  `meta_title` varchar(10) NOT NULL,
  `record` varchar(50) NOT NULL,
  `record_id` varchar(19) NOT NULL,
  `appid` varchar(19) NOT NULL,
  `identifier` varchar(32) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_u_0` (`list_id`,`identifier`,`form_meta_id`) USING BTREE,
  KEY `idx_0` (`list_id`,`identifier`,`meta_code`) USING BTREE,
  KEY `idx_1` (`is_deleted`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
--  Table structure for `t_white_list_record`
-- ----------------------------
CREATE TABLE `t_white_list_record` (
  `id` varchar(19) NOT NULL,
  `create_by` varchar(19) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `is_deleted` bit(1) NOT NULL,
  `update_by` varchar(19) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `identifier` varchar(32) NOT NULL,
  `list_id` varchar(19) NOT NULL,
  `status` int(11) NOT NULL,
  `appid` varchar(19) NOT NULL,
  `times` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_u_0` (`is_deleted`,`identifier`,`list_id`),
  KEY `idx_0` (`list_id`,`identifier`) USING BTREE,
  KEY `idx_1` (`is_deleted`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;
ALTER TABLE `t_account` CHANGE COLUMN `dept_no` `dept_no` varchar(100) CHARACTER SET utf8mb4 NOT NULL after `staff_no`;
SET FOREIGN_KEY_CHECKS = 1;


SET FOREIGN_KEY_CHECKS = 0;
ALTER TABLE `t_account_form_field` ADD FULLTEXT `idx_5` (`field_data`) comment '';
ALTER TABLE `t_achieve_list_extend_record` ADD INDEX `idx_3` USING BTREE (`record_id`) comment '';
ALTER TABLE `t_achieve_list_record` ADD COLUMN `push_act` bit(1) NOT NULL after `times`;
SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;
ALTER TABLE `t_achieve_list_extend_record` CHANGE COLUMN `record` `record` varchar(500) CHARACTER SET utf8mb4 NOT NULL after `meta_title`;
ALTER TABLE `t_limit_list_extend_record` CHANGE COLUMN `record` `record` varchar(500) CHARACTER SET utf8mb4 NOT NULL after `meta_title`;
ALTER TABLE `t_white_list_extend_record` CHANGE COLUMN `record` `record` varchar(500) CHARACTER SET utf8mb4 NOT NULL after `meta_title`;
SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;
ALTER TABLE `t_achieve_list` CHANGE COLUMN `expire_date` `expire_date` datetime DEFAULT NULL after `appid`;
SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;
CREATE TABLE `t_customer_information` (
	`id` varchar(19) NOT NULL,
	`create_by` varchar(19) DEFAULT NULL,
	`create_time` datetime DEFAULT NULL,
	`is_deleted` bit(1) NOT NULL,
	`update_by` varchar(19) DEFAULT NULL,
	`update_time` datetime DEFAULT NULL,
	`appid` varchar(19) NOT NULL,
	`address` varchar(200) NOT NULL,
	`bank_branch_name` varchar(100) NOT NULL,
	`bank_branch_no` varchar(20) NOT NULL,
	`birthday` varchar(40) NOT NULL,
	`card_no` varchar(64) NOT NULL,
	`customer_group_coding` varchar(30) NOT NULL,
	`customer_no` varchar(32) NOT NULL,
	`email` varchar(200) NOT NULL,
	`idcard_no` varchar(64) NOT NULL,
	`institutional_code` varchar(20) NOT NULL,
	`institutional_name` varchar(100) NOT NULL,
	`name` varchar(32) NOT NULL,
	`phone` varchar(32) NOT NULL,
	`bankcard_no` varchar(64) NOT NULL,
	`identifier` varchar(64) NOT NULL,
	PRIMARY KEY (`id`)) ENGINE=`InnoDB` COMMENT='' CHECKSUM=0 DELAY_KEY_WRITE=0;
CREATE TABLE `t_customer_information_extend` (
	`id` varchar(19) NOT NULL,
	`create_by` varchar(19) DEFAULT NULL,
	`create_time` datetime DEFAULT NULL,
	`is_deleted` bit(1) NOT NULL,
	`update_by` varchar(19) DEFAULT NULL,
	`update_time` datetime DEFAULT NULL,
	`appid` varchar(19) NOT NULL,
	`customer_information_id` varchar(19) NOT NULL,
	`title` varchar(40) NOT NULL,
	`value` varchar(200) NOT NULL,
	PRIMARY KEY (`id`)) ENGINE=`InnoDB` COMMENT='' CHECKSUM=0 DELAY_KEY_WRITE=0;
ALTER TABLE `t_exception_log` CHANGE COLUMN `exception` `exception` longtext CHARACTER SET utf8mb4 NOT NULL after `update_time`;
ALTER TABLE `t_exception_log` CHANGE COLUMN `msg_body` `msg_body` longtext CHARACTER SET utf8mb4 NOT NULL after `exception`;
ALTER TABLE `t_mq_exception` CHANGE COLUMN `exception` `exception` longtext CHARACTER SET utf8mb4 NOT NULL after `appid`;
ALTER TABLE `t_mq_exception` CHANGE COLUMN `msg_body` `msg_body` longtext CHARACTER SET utf8mb4 NOT NULL after `exception`;
SET FOREIGN_KEY_CHECKS = 1;