package com.ytdinfo.inndoo.common.enums;

import lombok.Getter;

/**
 * @author timmy
 * @date 2019/11/20
 */
@Getter
public enum SourceType {
    /**
     * 辽宁中行
     */
    LIAO_NING_BANK_CHINA(1, "辽宁中行私有化接口"),

    ZHE_JIANG_BANK_CCB(2, "浙江建行白名单接口"),

    HU_NAN_BANK_CCB(3, "湖南建行白名单接口");

    /**
     * 活动名称
     */
    private String displayName;

    /**
     * 定义活动枚举值，用于数据库存储
     */
    private Integer value;


    SourceType(int value, String displayName) {
        this.value =  value;
        this.displayName = displayName;

    }

    public static SourceType valueOf(Integer value) {
        SourceType[] values = SourceType.values();
        for (SourceType item : values) {
            if (item.getValue().equals(value)) {
                return item;
            }
        }
        return null;
    }


}