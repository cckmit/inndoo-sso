package com.ytdinfo.inndoo.common.enums;

import lombok.Getter;

/**
 * @author timmy
 * @date 2020/5/6
 */
@Getter
public enum ThreadPoolType {
    handleAccount(5, 35, 100, 1000);

    private Integer core;
    private Integer max;
    private Integer queueSize;
    private Integer activeTime;

    ThreadPoolType(int core, int max, int queueSize, int activeTime) {
        this.core = core;
        this.max = max;
        this.queueSize = queueSize;
        this.activeTime = activeTime;
    }
}