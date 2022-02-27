package com.ytdinfo.inndoo.consumer;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ytdinfo.inndoo.common.datasource.DynamicDataSourceContextHolder;
import com.ytdinfo.inndoo.common.exception.InndooException;
import com.ytdinfo.inndoo.common.rabbit.MQMessage;
import com.ytdinfo.inndoo.common.rabbit.consumer.BaseThreadPoolConsumer;
import com.ytdinfo.inndoo.common.utils.AESUtil;
import com.ytdinfo.inndoo.modules.core.entity.PhoneLocation;
import com.ytdinfo.inndoo.modules.core.service.AccountService;
import com.ytdinfo.inndoo.modules.core.service.PhoneLocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * @author timmy
 * @date 2019/10/17
 */
@Service
@Scope("prototype")
public class PhoneLocationConsumer extends BaseThreadPoolConsumer {

    @Autowired
    private PhoneLocationService phoneLocationService;

    @Override
    public void onMessage(MQMessage mqMessage) {
        String tenantId = PhoneLocationConsumer.super.getTenantId();
        DynamicDataSourceContextHolder.setDataSourceType(tenantId);
        PhoneLocation pl = JSONUtil.toBean((JSONObject) mqMessage.getContent(), PhoneLocation.class);
        String phone = pl.getPhone();
        String aesPhone = AESUtil.encrypt(phone);
        PhoneLocation phoneLocation = phoneLocationService.getByPhone(aesPhone);
        if (phoneLocation == null) {
            phoneLocation = new PhoneLocation();
            PhoneLocation phoneLocationFromApi = null;
            try {
                phoneLocationFromApi = phoneLocationService.getPhoneLocationFromApi(phone);
            } catch (Exception e) {
                e.printStackTrace();
                throw new InndooException(e.getMessage());
            }
            CopyOptions copyOptions = CopyOptions.create().ignoreNullValue();
            BeanUtil.copyProperties(phoneLocationFromApi, phoneLocation, copyOptions);
            phoneLocationService.save(phoneLocation);
        }
    }

}