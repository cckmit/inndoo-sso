package com.ytdinfo.inndoo.modules.core.dao.mapper;

import com.ytdinfo.inndoo.base.mybatis.BaseInndooMapper;
import com.ytdinfo.inndoo.modules.core.entity.Account;
import com.ytdinfo.inndoo.modules.core.entity.AccountFormField;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;

/**
 * 会员注册扩展表单内容数据处理层
 * @author zhulin
 */
public interface AccountFormFieldMapper extends BaseInndooMapper<AccountFormField> {

    List<AccountFormField> findWithFormByAccount(Account account);

    Integer aesDataSwitchPassword(Map<String,Object> map);

    List<String> findAccountIdsByFieldData(@Param("fieldData") String fieldData);
}