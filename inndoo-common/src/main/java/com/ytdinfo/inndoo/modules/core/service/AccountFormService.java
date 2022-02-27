package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.modules.core.entity.AccountForm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 会员注册页面主信息接口
 * @author Timmy
 */
public interface AccountFormService extends BaseService<AccountForm,String> {

    /**
    * 多条件分页获取
    * @param accountForm
    * @param searchVo
    * @param pageable
    * @return
    */
    Page<AccountForm> findByCondition(AccountForm accountForm, SearchVo searchVo, Pageable pageable);

    /**
     * 设置actStatus的返回值
     * @param AccountForm
     * @return
     */
    AccountForm setActStatus(AccountForm AccountForm);

    /**
     * 修改状态
     * @param entity
     * @return
     */
    AccountForm updateStatus(AccountForm entity);

    /**
     * 保存身份识别表单
     * @param entity
     * @return
     */
    AccountForm saveIdentifierForm(AccountForm entity);

    List<AccountForm> findByAppid(String appid);

    /**
     * 获取身份识别表单页
     * @param appid
     * @param isIdentifierForm
     * @return
     */
    AccountForm findByAppidAndIsIdentifierForm(String appid,Boolean isIdentifierForm);

    /**
     * 设置为默认表单
     * @param accountForm
     * @return
     */
    AccountForm setDefault(AccountForm accountForm);

    /**
     * 多条件查询
     * @param map
     * @return
     */
    List<AccountForm> findByMap(Map<String,Object> map);

    AccountForm queryByName (String name,String appid );

    long countByName(String name);
}