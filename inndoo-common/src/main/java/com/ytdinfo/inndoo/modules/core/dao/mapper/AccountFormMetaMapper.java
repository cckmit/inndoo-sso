package com.ytdinfo.inndoo.modules.core.dao.mapper;


import com.ytdinfo.inndoo.base.mybatis.BaseInndooMapper;
import com.ytdinfo.inndoo.modules.core.entity.AccountFormMeta;
import org.springframework.data.repository.query.Param;

import java.util.List;
public interface AccountFormMetaMapper extends BaseInndooMapper<AccountFormMeta> {
    int deleteByAccountFormId(@Param("accountFormId") String accountFormId);

    List<AccountFormMeta> selectAccountFormMetasByAccountFormId(@Param("accountFormId") String accountFormId);
}
