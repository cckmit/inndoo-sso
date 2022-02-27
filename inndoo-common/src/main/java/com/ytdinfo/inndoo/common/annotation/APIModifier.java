package com.ytdinfo.inndoo.common.annotation;

import com.ytdinfo.inndoo.common.enums.APIModifierType;

import java.lang.annotation.*;

/**
 * API Token校验注解
 * @author Timmy
 */
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface APIModifier {

        /**
         *
         * @return
         */
        APIModifierType value();
}
