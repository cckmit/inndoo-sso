package com.ytdinfo.inndoo.base.mybatis;

import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Collection;
public interface BaseIService<T> extends IService<T> {
    boolean saveBatchWithIgnore(Collection<T> entityList, int batchSize);
    boolean saveBatchOnDuplicateUpdate(Collection<T> entityList, int batchSize);
}