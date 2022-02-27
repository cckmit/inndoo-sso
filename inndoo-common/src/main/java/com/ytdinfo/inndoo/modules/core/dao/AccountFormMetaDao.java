package com.ytdinfo.inndoo.modules.core.dao;

import com.ytdinfo.inndoo.base.BaseDao;
import com.ytdinfo.inndoo.modules.core.entity.AccountFormMeta;

import java.util.List;

/**
 * 动态表单控件配置信息数据处理层
 * @author Timmy
 */
public interface AccountFormMetaDao extends BaseDao<AccountFormMeta,String> {

    List<AccountFormMeta> findByAccountFormIdAndIsIdentifierTrue(String accountFormId);
    List<AccountFormMeta> findByAccountFormIdAndIsRequiredTrue(String accountFormId);
    List<AccountFormMeta> findByAccountFormIdAndIsRequiredIsTrue(String accountFormId);



    List<AccountFormMeta> findByAccountFormIdAndMetaType(String accountFormId,String metaType);


}