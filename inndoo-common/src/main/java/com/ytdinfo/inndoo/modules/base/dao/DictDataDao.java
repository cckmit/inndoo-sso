package com.ytdinfo.inndoo.modules.base.dao;

import com.ytdinfo.inndoo.base.BaseDao;
import com.ytdinfo.inndoo.modules.base.entity.DictData;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 字典数据数据处理层
 * @author Exrick
 */
public interface DictDataDao extends BaseDao<DictData,String> {


    /**
     * 通过dictId和状态获取
     * @param dictId
     * @param status
     * @return
     */
    List<DictData> findByDictIdAndStatusOrderBySortOrder(String dictId, Integer status);

    /**
     * 通过dictId删除
     * @param dictId
     */
    @Transactional(rollbackFor = Exception.class)
    void deleteByDictId(String dictId);

    DictData findByDictIdAndTitle(String dictId, String appid);

    List<DictData> findByValueAndDictId(String value,String dictId);

    DictData findFirstByTitle(String title);
}