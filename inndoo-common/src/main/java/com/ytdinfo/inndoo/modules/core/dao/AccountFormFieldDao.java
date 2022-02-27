package com.ytdinfo.inndoo.modules.core.dao;

import com.ytdinfo.inndoo.base.BaseDao;
import com.ytdinfo.inndoo.modules.core.entity.AccountFormField;

import java.util.List;

/**
 * 会员注册扩展表单内容数据处理层
 * @author Timmy
 */
public interface AccountFormFieldDao extends BaseDao<AccountFormField,String> {

    List<AccountFormField> findByFormIdAndAccountId(String formId, String accountId);

    List<AccountFormField> findByAccountId(String accountId);

    List<AccountFormField> findByAccountIdIn(List<String> accountIds);

    List<AccountFormField> findByFieldDataAndMetaTitleAndAppid(String fieldData,String metaTitle,String appid);
}