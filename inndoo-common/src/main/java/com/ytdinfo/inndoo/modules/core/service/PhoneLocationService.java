package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.modules.core.entity.PhoneLocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.ytdinfo.inndoo.common.vo.SearchVo;

import java.util.List;

/**
 * 手机号码归属地接口
 * @author Timmy
 */
public interface PhoneLocationService extends BaseService<PhoneLocation,String> {

    /**
    * 多条件分页获取
    * @param phoneLocation
    * @param searchVo
    * @param pageable
    * @return
    */
    Page<PhoneLocation> findByCondition(PhoneLocation phoneLocation, SearchVo searchVo, Pageable pageable);

    /**
     * 根据密文phone查询
     * @param phone
     * @return
     */
    PhoneLocation getByPhone(String phone);

    /**
     * 保存
     * @param entity
     * @return
     */
    PhoneLocation saveByPhone(PhoneLocation entity);

    /**
     * 更新
     * @param entity
     * @return
     */
    PhoneLocation updateByPhone(PhoneLocation entity);

    /**
     * 删除
     * @param entity
     */
    void deleteByPhone(PhoneLocation entity);

    /**
     * 从api平台获取手机归属信息
     * @param phone
     * @return
     */
    PhoneLocation getPhoneLocationFromApi(String phone) throws Exception;
}