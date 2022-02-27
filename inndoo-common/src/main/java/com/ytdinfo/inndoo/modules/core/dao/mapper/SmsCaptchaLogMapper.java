package com.ytdinfo.inndoo.modules.core.dao.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ytdinfo.inndoo.base.mybatis.BaseInndooMapper;
import com.ytdinfo.inndoo.common.vo.SmsCaptchaLogVo;
import com.ytdinfo.inndoo.common.vo.SmsSendLogSearchVo;
import com.ytdinfo.inndoo.modules.core.entity.SmsCaptchaLog;
import org.apache.ibatis.annotations.Param;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 短信发送记录数据处理层
 *
 * @author haiqing
 */
public interface SmsCaptchaLogMapper extends BaseInndooMapper<SmsCaptchaLog> {

    IPage<SmsCaptchaLogVo> listForHelper(Page page, @Param("searchVo") SmsSendLogSearchVo searchVo);
}