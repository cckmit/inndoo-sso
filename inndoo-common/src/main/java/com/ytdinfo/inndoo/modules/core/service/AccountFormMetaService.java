package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.modules.core.entity.AccountFormMeta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.ytdinfo.inndoo.common.vo.SearchVo;

import java.util.List;

/**
 * 动态表单控件配置信息接口
 * @author Timmy
 */
public interface AccountFormMetaService extends BaseService<AccountFormMeta,String> {

    /**
    * 多条件分页获取
    * @param formMetaData
    * @param searchVo
    * @param pageable
    * @return
    */
    Page<AccountFormMeta> findByCondition(AccountFormMeta formMetaData, SearchVo searchVo, Pageable pageable);

    void deleteByAccountFormId(String accountFormId);

    List<AccountFormMeta> findListByAccountFormId(String accountFormId);

    List<AccountFormMeta> findByAccountFormIdAndMetaType(String accountFormId,String metaType);

    List<AccountFormMeta> findFormMetaListByIds(List<String> ids);

    List<AccountFormMeta> findByNameList(String listId, List<String> ids);
}