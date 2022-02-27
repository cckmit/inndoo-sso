package com.ytdinfo.inndoo.modules.activiti.serviceimpl;

import com.ytdinfo.inndoo.modules.activiti.dao.ActCategoryDao;
import com.ytdinfo.inndoo.modules.activiti.entity.ActCategory;
import com.ytdinfo.inndoo.modules.activiti.service.ActCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 流程分类接口实现
 * @author Exrick
 */
@Slf4j
@Service
public class ActCategoryServiceImpl implements ActCategoryService {

    @Autowired
    private ActCategoryDao actCategoryDao;

    @Override
    public ActCategoryDao getRepository() {
        return actCategoryDao;
    }

    @Override
    public List<ActCategory> findByParentIdOrderBySortOrder(String parentId) {

        return actCategoryDao.findByParentIdOrderBySortOrder(parentId);
    }

    @Override
    public List<ActCategory> findByTitleLikeOrderBySortOrder(String title) {

        return actCategoryDao.findByTitleLikeOrderBySortOrder(title);
    }
}