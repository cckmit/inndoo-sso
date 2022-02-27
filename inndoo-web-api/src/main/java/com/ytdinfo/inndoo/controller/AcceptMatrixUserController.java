package com.ytdinfo.inndoo.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.ytdinfo.inndoo.common.annotation.APIModifier;
import com.ytdinfo.inndoo.common.enums.APIModifierType;
import com.ytdinfo.inndoo.common.utils.ResultUtil;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.modules.base.dao.UserDao;
import com.ytdinfo.inndoo.modules.base.entity.User;
import com.ytdinfo.inndoo.modules.base.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author matao
 */
@Slf4j
@RestController
@Api(value = "接收管理平台用户信息")
@RequestMapping("/matrixUser")
@APIModifier(value = APIModifierType.PRIVATE)
public class AcceptMatrixUserController {
    @Autowired
    private UserService userService;
    @Autowired
    private UserDao userDao;

    @RequestMapping(value = "/acceptMatrixUser", method = RequestMethod.POST)
    @ApiOperation(value = "接收授权过来的租户用户并保存起来")
    public Result<String> acceptSsoUser(@RequestParam Map map){
        log.info("map:"+JSONUtil.toJsonStr(map));
        if(map != null){
            if(map.containsKey("action") && map.get("action") != null){
                String action = map.get("action").toString();
                User user = JSONUtil.toBean(map.get("user").toString(), User.class);
                if("add".equals(action)){
                    insertUser(user);
                }else if("update".equals(action)){
                    if(StrUtil.isNotEmpty(user.getOldMobile())){
                        updateUserMobile(user);
                    }else{
                        insertUser(user);
                    }
                }else if("delete".equals(action)){
                    if(user != null){
                        String mobile = user.getMobile();
                        if(StrUtil.isNotEmpty(mobile)){
                            List<User> userList = userDao.findByMobile(user.getMobile());
                            if(userList != null && userList.size() == 1){
                                user.setId(userList.get(0).getId());
                                user.setIsDeleted(true);
                                userService.update(user);
                            }
                        }
                    }
                }
            }
        }
        return new ResultUtil<String>().setData("");
    }

    private void insertUser(User user){
        if(user != null && StrUtil.isNotEmpty(user.getMobile())){
            List<User> userList = userDao.findByMobile(user.getMobile());
            if(userList == null || userList.size() == 0){
                userService.save(user);
            }else if(userList.size() == 1){
                user.setId(userList.get(0).getId());
                userService.update(user);
            }
        }
    }

    private void updateUserMobile(User user) {
        if (user != null && StrUtil.isNotEmpty(user.getMobile())) {
            List<User> userList = userDao.findByMobile(user.getOldMobile());
            if (userList != null && userList.size() == 1) {
                user.setId(userList.get(0).getId());
                userService.update(user);
            }
        }
    }

    @RequestMapping(value = "/acceptMatrixUserPw", method = RequestMethod.POST)
    @ApiOperation(value = "接收管理平台修改密码的用户更新一下密码")
    public Result<String> acceptActUserPw(@RequestParam Map map){
        log.info("map:"+JSONUtil.toJsonStr(map));
        if(map != null){
            if(map.containsKey("user") && map.get("user") != null
                    && map.containsKey("action") && map.get("action") != null){
                String action = map.get("action").toString();
                User user = JSONUtil.toBean(map.get("user").toString(),User.class);
                if("updatePassword".equals(action)){
                    updateUserPw(user);
                }
            }
        }
        return new ResultUtil<String>().setData("");
    }

    private void updateUserPw(User user){
        if(user != null && StrUtil.isNotEmpty(user.getMobile())){
            List<User> userList = userDao.findByMobile(user.getMobile());
            if(userList.size() == 1){
                User matrixUser = userList.get(0);
                matrixUser.setPassword(user.getPassword());
                matrixUser.setUpdatePasswordTime(user.getUpdatePasswordTime());
                userService.update(matrixUser);
            }
        }
    }

}
