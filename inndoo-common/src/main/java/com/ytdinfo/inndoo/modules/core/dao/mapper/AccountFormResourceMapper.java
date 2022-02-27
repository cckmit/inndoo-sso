package com.ytdinfo.inndoo.modules.core.dao.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ytdinfo.inndoo.base.mybatis.BaseInndooMapper;
import com.ytdinfo.inndoo.modules.core.entity.AccountFormResource;
import org.springframework.data.repository.query.Param;

import java.util.List;
public interface AccountFormResourceMapper extends BaseInndooMapper<AccountFormResource> {

    int deleteByAccountFormId(@Param("accountFormId") String accountFormId);

    List<AccountFormResource> selectAccountFormResourcesByAccountFormId(@Param("accountFormId") String accountFormId);
}
