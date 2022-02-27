package com.ytdinfo.inndoo.modules.core.dao;

import com.ytdinfo.inndoo.base.BaseDao;
import com.ytdinfo.inndoo.modules.core.entity.AccountForm;

import java.util.List;

/**
 * 会员注册页面主信息数据处理层
 * @author Timmy
 */
public interface AccountFormDao extends BaseDao<AccountForm,String> {

    List<AccountForm> findByAppid(String appid);

    List<AccountForm> findByAppidAndIsIdentifierForm(String appid,Boolean isIdentifierForm);

    Integer countByAppidAndFormTypeAndIsDefaultAndIsIdentifierForm(String appid,Integer formType,Boolean isDefault,Boolean isIdentifierForm);

    long countByName(String name);
}