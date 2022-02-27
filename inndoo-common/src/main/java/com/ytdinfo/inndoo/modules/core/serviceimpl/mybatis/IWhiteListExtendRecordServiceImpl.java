package com.ytdinfo.inndoo.modules.core.serviceimpl.mybatis;

import cn.hutool.core.collection.CollectionUtil;
import com.ytdinfo.inndoo.base.mybatis.BaseServiceImpl;
import com.ytdinfo.inndoo.modules.core.dao.mapper.WhiteListExtendRecordMapper;
import com.ytdinfo.inndoo.modules.core.entity.WhiteListExtendRecord;
import com.ytdinfo.inndoo.modules.core.service.mybatis.IWhiteListExtendRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 白名单数据
 * @author Jxy
 */
@Slf4j
@Service
public class IWhiteListExtendRecordServiceImpl extends BaseServiceImpl<WhiteListExtendRecordMapper, WhiteListExtendRecord> implements IWhiteListExtendRecordService {

    @Autowired
    private WhiteListExtendRecordMapper whiteListExtendRecordMapper;


    @Override
    public int deleteBatchByIdentifersAndListId(List<String> list,String listId, int num) {
        if (list != null && list.size() > 0) {
            num = (num > 0) ? num : 3000;
            int length = list.size();
            if (length <= num) {
                Map<String,Object> map = new HashMap<>();
                map.put("list",list);
                map.put("listId",listId);
                return whiteListExtendRecordMapper.batchDeleteByIdentifiersAndListId(map);
            } else {
                int deleteTotal = 0;
                int times = length / num;
                for (int i = 0; i < times; i++) {
                    List<String> temp = list.subList(i * num, (i + 1) * num);
                    Map<String,Object> map = new HashMap<>();
                    map.put("list",temp);
                    map.put("listId",listId);
                    deleteTotal += whiteListExtendRecordMapper.batchDeleteByIdentifiersAndListId(map);
                }
                List<String> temp1 = list.subList(times * num, length);
                if(CollectionUtil.isNotEmpty(temp1)){
                    Map<String,Object> map = new HashMap<>();
                    map.put("list",temp1);
                    map.put("listId",listId);
                    deleteTotal += whiteListExtendRecordMapper.batchDeleteByIdentifiersAndListId(map);
                }

                return deleteTotal;
            }
        }else {
            return 0;
        }
    }

    @Override
    public List<Map<String, Object>> findTransformDate(Map<String, Object> findMap) {
        return whiteListExtendRecordMapper.findTransformDate(findMap);
    }
}
