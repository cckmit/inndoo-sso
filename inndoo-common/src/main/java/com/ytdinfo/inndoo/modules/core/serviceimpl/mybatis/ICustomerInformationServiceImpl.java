package com.ytdinfo.inndoo.modules.core.serviceimpl.mybatis;

import com.ytdinfo.inndoo.base.mybatis.BaseServiceImpl;
import com.ytdinfo.inndoo.modules.core.dao.mapper.CustomerInformationMapper;
import com.ytdinfo.inndoo.modules.core.entity.CustomerInformation;
import com.ytdinfo.inndoo.modules.core.service.mybatis.ICustomerInformationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class ICustomerInformationServiceImpl extends BaseServiceImpl<CustomerInformationMapper, CustomerInformation> implements ICustomerInformationService {
    @Autowired
    private CustomerInformationMapper customerInformationMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteBatchByIdentifers(List<String> list, int num) {
        if (list != null && list.size() > 0) {
            num = (num > 0) ? num : 10000;
            int length = list.size();
            if (length <= num) {
                return customerInformationMapper.batchDeleteByIdentifier(list);
            } else {
                int deleteTotal = 0;
                int times = length / num;
                for (int i = 0; i < times; i++) {
                    List<String> temp = list.subList(i * num, (i + 1) * num);
                    deleteTotal += customerInformationMapper.batchDeleteByIdentifier(temp);
                }
                List<String> temp1 = list.subList(times * num, length);
                deleteTotal += customerInformationMapper.batchDeleteByIdentifier(temp1);
                return deleteTotal;
            }
        }else {
            return 0;
        }
    }
}
