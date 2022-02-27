package com.ytdinfo.inndoo.modules.core.service.mybatis;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ytdinfo.inndoo.common.vo.SmsCaptchaLogVo;
import com.ytdinfo.inndoo.common.vo.SmsSendLogSearchVo;
import com.ytdinfo.inndoo.modules.core.entity.SmsCaptchaLog;

import java.util.List;

/**
 * 短信发送记录接口
 * @author haiqing
 */
public interface ISmsCaptchaLogService extends IService<SmsCaptchaLog> {

    IPage<SmsCaptchaLogVo> listForHelper(SmsSendLogSearchVo smsSendLogSearchVo);
}