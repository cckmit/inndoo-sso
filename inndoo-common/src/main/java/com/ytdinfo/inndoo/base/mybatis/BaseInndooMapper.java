package com.ytdinfo.inndoo.base.mybatis;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * Created by timmy on 2019/10/18.
 */
public interface BaseInndooMapper<T> extends BaseMapper<T> {
    /**
     * 插入一条记录，如果主键、唯一索引冲突则忽略
     *
     * @param entity 实体对象
     */
    int insertIgnore(T entity);

    int insertOnDuplicateUpdate(T entity);
}