package com.ytdinfo.inndoo.modules.core.serviceimpl.mybatis;

import cn.hutool.core.collection.CollectionUtil;
import com.ytdinfo.inndoo.base.mybatis.BaseServiceImpl;
import com.ytdinfo.inndoo.modules.core.dao.mapper.WhiteListRecordMapper;
import com.ytdinfo.inndoo.modules.core.entity.WhiteListRecord;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IWhiteListRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 白名单数据
 * @author Jxy
 */
@Slf4j
@Service
public class IWhiteListRecordServiceImpl extends BaseServiceImpl<WhiteListRecordMapper, WhiteListRecord> implements IWhiteListRecordService {


    @Autowired
    private WhiteListRecordMapper whiteListRecordMapper;


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
                return whiteListRecordMapper.batchDeleteByIdentifierAndListId(map);
            } else {
                int deleteTotal = 0;
                int times = length / num;
                for (int i = 0; i < times; i++) {
                    List<String> temp = list.subList(i * num, (i + 1) * num);
                    Map<String,Object> map = new HashMap<>();
                    map.put("list",temp);
                    map.put("listId",listId);
                    if(CollectionUtil.isNotEmpty(temp)){
                        deleteTotal += whiteListRecordMapper.batchDeleteByIdentifierAndListId(map);
                    }

                }
                List<String> temp1 = list.subList(times * num, length);
                Map<String,Object> map = new HashMap<>();
                map.put("list",temp1);
                map.put("listId",listId);
                if(CollectionUtil.isNotEmpty(temp1)){
                    deleteTotal += whiteListRecordMapper.batchDeleteByIdentifierAndListId(map);
                }
                return deleteTotal;
            }
        }else {
            return 0;
        }
    }

    @Override
    public long countByListId(String id) {
        return whiteListRecordMapper.countByListId(id);
    }

    @Override
    public Integer aesDataSwitchPassword(Map<String,Object> map)
    {
        return whiteListRecordMapper.aesDataSwitchPassword(map);
    }
}
