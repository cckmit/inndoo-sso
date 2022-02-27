package com.ytdinfo.inndoo.modules.core.service.mybatis;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ytdinfo.inndoo.modules.core.entity.Account;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

public interface IAccountService extends IService<Account> {
    List<Account> findByMap(Map<String,Object> map);

    Integer aesDataSwitchPassword(Map<String,Object> map);

    void deleteById( String id );

    /**
     * 统计指定日期的数量
     * @param startTime 起始时间(包含) yyyy-MM-dd HH:mm:ss
     * @param endTime 截至时间(包含) yyyy-MM-dd HH:mm:ss
     * @return 指定区间的数量
     */
    Integer countByCreateTime(String startTime, String endTime);
}
