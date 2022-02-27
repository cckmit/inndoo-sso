package com.ytdinfo.inndoo.common.exception;

import lombok.Data;

/**
 * @author Exrickx
 */
@Data
public class InndooException extends RuntimeException {

    private String msg;

    public InndooException(String msg){
        super(msg);
        this.msg = msg;
    }
}
