package com.ytdinfo.inndoo.common.utils;

import cn.hutool.core.util.ReflectUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class YReflectUtil {

    public static <T1, T2> void copyFields(T1 source, T2 target) {
        Field[] sourceFields = ReflectUtil.getFields(source.getClass());
        Field[] targetFields = ReflectUtil.getFields(target.getClass());
        for (Field targetField : targetFields) {
            for (Field sourceField : sourceFields) {
                if(!Modifier.isFinal(targetField.getModifiers())) {
                    if (targetField.getName().equals(sourceField.getName()) && targetField.getType().isAssignableFrom(sourceField.getType())) {
                        ReflectUtil.setFieldValue(target, targetField, ReflectUtil.getFieldValue(source, sourceField));
                    }
                }

            }
        }
    }

    public static <T1, T2> T2 copyFields(T1 source, Class<T2> targetClass) throws IllegalAccessException, InstantiationException {
        Field[] sourceFields = ReflectUtil.getFields(source.getClass());
        Field[] targetFields = ReflectUtil.getFields(targetClass);
        T2 target = targetClass.newInstance();
        for (Field targetField : targetFields) {
            for (Field sourceField : sourceFields) {
                if(!Modifier.isFinal(targetField.getModifiers())) {
                    if (targetField.getName().equals(sourceField.getName()) && targetField.getType().isAssignableFrom(sourceField.getType())) {
                        ReflectUtil.setFieldValue(target, targetField, ReflectUtil.getFieldValue(source, sourceField));
                    }
                }

            }
        }
        return target;
    }

}
