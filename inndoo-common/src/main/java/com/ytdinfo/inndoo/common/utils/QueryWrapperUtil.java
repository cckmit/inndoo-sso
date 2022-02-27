package com.ytdinfo.inndoo.common.utils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ytdinfo.inndoo.base.BaseEntity;

public class QueryWrapperUtil {
    public static <E extends BaseEntity> QueryWrapper<E> getQueryWrapper() {
        QueryWrapper<E> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_deleted", false);
        return queryWrapper;
    }
}
