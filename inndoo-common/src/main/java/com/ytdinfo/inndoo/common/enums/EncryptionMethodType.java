package com.ytdinfo.inndoo.common.enums;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhuzheng
 */
public enum EncryptionMethodType {

    /**
     * 加密方式
     */
    MD5(0, "MD5", false),

    AES(1, "AES", true),

    SHA256(2, "SHA256", false),

    SM3(3,"SM3",false);

    private Byte value;

    private String description;

    private boolean passwordRequired;

    EncryptionMethodType(Integer value, String description, boolean passwordRequired) {
        this.value = value.byteValue();
        this.description = description;
        this.passwordRequired = passwordRequired;
    }

    public Byte getValue(){
        return this.value;
    }

    public boolean getPasswordRequired(){
        return this.passwordRequired;
    }

    public static List<Map<String, Object>> getEncryptionMethods() {
        List<Map<String, Object>> list = new ArrayList<>();
        EncryptionMethodType[] types = EncryptionMethodType.values();
        for (EncryptionMethodType type : types) {
            Map<String, Object> map = new HashMap<>();
            map.put("value", type.value);
            map.put("description", type.description);
            map.put("passwordRequired", type.passwordRequired);
            list.add(map);
        }
        return list;
    }

    public static EncryptionMethodType getByValue(Byte value) {
        EncryptionMethodType[] types = EncryptionMethodType.values();
        for (EncryptionMethodType methodType : types) {
            if (methodType.value.equals(value)){
                return methodType;
            }
        }
        return null;
    }

}
