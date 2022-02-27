package com.ytdinfo.inndoo.schedule;

import com.ytdinfo.inndoo.common.datasource.DynamicDataSourceContextHolder;
import com.ytdinfo.inndoo.common.utils.MatrixApiUtil;
import com.ytdinfo.inndoo.common.vo.Tenant;
import com.ytdinfo.inndoo.modules.core.entity.ApiRequestLog;
import com.ytdinfo.inndoo.modules.core.service.ApiRequestLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zhuzheng
 */
@Service
public class ApiRequestLogInsertSchedule {


    @Autowired
    private RedisTemplate<String, ApiRequestLog> redisTemplate;

    @Autowired
    private ApiRequestLogService apiRequestLogService;

    @Autowired
    private MatrixApiUtil matrixApiUtil;

    private static final String CACHE_KEY = "batch:ApiRequestLogDelayInsert";

    /**
     * 每隔180000毫秒执行一次，必须是上次调度成功后180000毫秒；
     */
    @Scheduled(fixedDelay = 5000)
    public void apiRequestLogInsert() {
        List<Tenant> tenantList = matrixApiUtil.getTenantList();
        Long batchSize = 100L;
        for (Tenant tenant : tenantList) {
            String cacheKey = tenant.getId() + ":" + CACHE_KEY;
            Long logSize = redisTemplate.opsForSet().size(cacheKey);
            DynamicDataSourceContextHolder.setDataSourceType(tenant.getId());
            while (logSize > 0){
                if(batchSize > logSize){
                    batchSize = logSize;
                }
                List<ApiRequestLog> logs = new ArrayList<>();
                logs.addAll(redisTemplate.opsForSet().pop(cacheKey, batchSize));
                logSize = logSize - batchSize;
                apiRequestLogService.saveBatch(logs, 50);
            }
        }
    }

}
