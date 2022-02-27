package com.ytdinfo.inndoo.consumer;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ytdinfo.inndoo.common.dto.SimulationStaffRegistrationDto;
import com.ytdinfo.inndoo.common.lock.Callback;
import com.ytdinfo.inndoo.common.lock.RedisDistributedLockTemplate;
import com.ytdinfo.inndoo.common.rabbit.MQMessage;
import com.ytdinfo.inndoo.common.rabbit.QueueEnum;
import com.ytdinfo.inndoo.common.rabbit.RabbitUtil;
import com.ytdinfo.inndoo.common.rabbit.consumer.BaseConsumer;
import com.ytdinfo.inndoo.modules.core.service.ActAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * 账户导入处理
 */
@Service
@Scope("prototype")
public class SimulationStaffRegistrationConsumer extends BaseConsumer {
    @Autowired
    private RabbitUtil rabbitUtil;

    @Autowired
    private RedisDistributedLockTemplate lockTemplate;

    @Autowired
    private ActAccountService actAccountService;

    @Override
    public void onMessage(MQMessage mqMessage) {
        SimulationStaffRegistrationDto dto = JSONUtil.toBean((JSONObject) mqMessage.getContent(), SimulationStaffRegistrationDto.class);
        String lockKey = "SimulationStaffRegistrationConsumer:"+ dto.getPhone();
        lockTemplate.execute(lockKey, 3000, new Callback() {
            @Override
            public Object onGetLock() throws InterruptedException {
                actAccountService.simulationStaffRegistration(dto);
                return "SUCCESS";
            }

            @Override
            public Object onTimeout() throws InterruptedException {
                rabbitUtil.sendToExchange(QueueEnum.QUEUE_SIMULATION_STAFF_REGISTRATION.getExchange(), "", mqMessage);
                return "SUCCESS";
            }
        });
    }
}
