package com.ytdinfo.inndoo.modules.core.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ytdinfo.inndoo.base.mybatis.BaseInndooMapper;
import com.ytdinfo.inndoo.modules.core.entity.Account;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface AccountMapper extends BaseInndooMapper<Account> {

    List<Account> findByMap(Map<String,Object> map);

    Integer aesDataSwitchPassword(Map<String,Object> map);

    Integer deleteById(String id);

    /**
     * 统计指定日期的数量
     * @param appid 微信公众号
     * @param startTime 起始时间(包含) yyyy-MM-dd HH:mm:ss
     * @param endTime 截至时间(包含) yyyy-MM-dd HH:mm:ss
     * @return 指定区间的数量
     */
    Integer countByCreateTime(@Param(value = "appid") String appid, @Param(value = "startTime") String startTime, @Param(value = "endTime") String endTime);
}
