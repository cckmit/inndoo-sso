package com.ytdinfo.inndoo.controller;

import com.ytdinfo.inndoo.common.annotation.APIModifier;
import com.ytdinfo.inndoo.common.enums.APIModifierType;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.modules.core.entity.ExternalApiInfo;
import com.ytdinfo.inndoo.modules.core.service.ExternalApiInfoService;
import com.ytdinfo.inndoo.vo.NameListVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@RestController
@Api(description = "外部api")
@RequestMapping("/externalapi")
@APIModifier(APIModifierType.PRIVATE)
public class ExternalApiController {


    @Autowired
    private ExternalApiInfoService externalApiInfoService;

    @RequestMapping(value = "/query/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "查询接口")
    public Result<Object> query(@PathVariable String id, @RequestParam String accountId, String ext, HttpServletRequest request) {
        return externalApiInfoService.execute(id, accountId, ext, true);
    }

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ApiOperation(value = "查询接口")
    public Result<List<NameListVo>> list(HttpServletRequest request) {
        List<ExternalApiInfo> list = externalApiInfoService.findAll();
        List<NameListVo> listVos = new ArrayList<>();
        list.forEach(entity -> {
            NameListVo vo = new NameListVo();
            vo.setName(entity.getName());
            vo.setId(entity.getId());
            listVos.add(vo);
        });
        Collator collator = Collator.getInstance(Locale.CHINESE);
        listVos.sort((o1, o2) -> collator.compare(o1.getName(), o2.getName()));
        return new ResultUtil<List<NameListVo>>().setData(listVos);
    }
}
