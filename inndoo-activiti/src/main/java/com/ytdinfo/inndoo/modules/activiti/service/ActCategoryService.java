package com.ytdinfo.inndoo.modules.activiti.service;

import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.modules.activiti.entity.ActCategory;

import java.util.List;

/**
 * 流程分类接口
 * @author Exrick
 */
public interface ActCategoryService extends BaseService<ActCategory,String> {

    /**
     * 通过父id获取
     * @param parentId
     * @return
     */
    List<ActCategory> findByParentIdOrderBySortOrder(String parentId);

    /**
     * 通过名称模糊搜索
     * @param title
     * @return
     */
    List<ActCategory> findByTitleLikeOrderBySortOrder(String title);
}