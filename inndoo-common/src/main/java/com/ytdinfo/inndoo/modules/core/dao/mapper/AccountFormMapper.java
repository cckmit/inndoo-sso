package com.ytdinfo.inndoo.modules.core.dao.mapper;

import com.ytdinfo.inndoo.base.mybatis.BaseInndooMapper;
import com.ytdinfo.inndoo.modules.core.entity.AccountForm;

import java.util.List;
import java.util.Map;

/**
 * @Author yaochangning
 */
public interface AccountFormMapper  extends BaseInndooMapper<AccountForm> {
    Integer updateStatus(Map<String,Object> map);

    List<AccountForm> findByMap(Map<String, Object> map);

    Integer selectCountByMap(Map<String, Object> map);
}
