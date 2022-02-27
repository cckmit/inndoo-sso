package com.ytdinfo.inndoo.modules.core.service;

import com.ytdinfo.inndoo.common.vo.NameListValidateResultVo;

/**
 * 接口名单
 *
 * @author zhuzheng
 */
public interface DictDataApiListService {

    NameListValidateResultVo verify(String dictDataId, String recordIdentifier, String openId);

}