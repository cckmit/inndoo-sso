package com.ytdinfo.inndoo.modules.base.service;

import com.ytdinfo.inndoo.base.BaseService;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.modules.base.entity.File;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.io.*;
/**
 * 文件管理接口
 * @author Exrick
 */
public interface FileService extends BaseService<File,String> {

    /**
     * 多条件获取列表
     * @param file
     * @param searchVo
     * @param pageable
     * @return
     */
    Page<File> findByCondition(File file, SearchVo searchVo, Pageable pageable);

    Result<Object> upload(java.io.File file,String contentType);

    String uploadFileInputStream(InputStream inputStream , String fKey, String contentType, int size);
}