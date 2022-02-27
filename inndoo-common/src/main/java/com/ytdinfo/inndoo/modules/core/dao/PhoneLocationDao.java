package com.ytdinfo.inndoo.modules.core.dao;

import com.ytdinfo.inndoo.base.BaseDao;
import com.ytdinfo.inndoo.modules.core.entity.PhoneLocation;
import com.ytdinfo.inndoo.modules.core.entity.Staff;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

/**
 * 手机号码归属地数据处理层
 * @author Timmy
 */
public interface PhoneLocationDao extends BaseDao<PhoneLocation,String> {

    PhoneLocation findByPhone(String aesPhone);
}