package com.ytdinfo.inndoo.modules.core.serviceimpl.mybatis;

import cn.hutool.core.collection.CollectionUtil;
import com.ytdinfo.inndoo.base.mybatis.BaseServiceImpl;
import com.ytdinfo.inndoo.modules.core.dao.mapper.LimitListRecordMapper;
import com.ytdinfo.inndoo.modules.core.entity.LimitListRecord;
import com.ytdinfo.inndoo.modules.core.service.mybatis.ILimitListRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 受限名单数据
 * @author Jxy
 */
@Slf4j
@Service
public class ILimitListRecordServiceImpl extends BaseServiceImpl<LimitListRecordMapper, LimitListRecord> implements ILimitListRecordService {

    @Autowired
    private LimitListRecordMapper limitListRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteBatchByIdentifersAndListId(List<String> list, int num,String listId) {
        if (list != null && list.size() > 0) {
            num = (num > 0) ? num : 3000;
            int length = list.size();
            if (length <= num) {
                Map<String,Object> map = new HashMap<>();
                map.put("list",list);
                map.put("listId",listId);
                return limitListRecordMapper.batchDeleteByIdentifierAndListId(map);
            } else {
                int deleteTotal = 0;
                int times = length / num;
                for (int i = 0; i < times; i++) {
                    List<String> temp = list.subList(i * num, (i + 1) * num);
                    Map<String,Object> map = new HashMap<>();
                    map.put("list",temp);
                    map.put("listId",listId);
                    if(CollectionUtil.isNotEmpty(temp)){
                        deleteTotal += limitListRecordMapper.batchDeleteByIdentifierAndListId(map);
                    }
                }
                List<String> temp1 = list.subList(times * num, length);
                Map<String,Object> map = new HashMap<>();
                map.put("list",temp1);
                map.put("listId",listId);
                if(CollectionUtil.isNotEmpty(temp1)){
                    deleteTotal += limitListRecordMapper.batchDeleteByIdentifierAndListId(map);
                }
                return deleteTotal;
            }
        }else {
            return 0;
        }
    }

    @Override
    public long countByListId(String id) {
        return limitListRecordMapper.countByListId(id);
    }

    @Override
    public Integer aesDataSwitchPassword(Map<String,Object> map)
    {
        return limitListRecordMapper.aesDataSwitchPassword(map);
    }
}
