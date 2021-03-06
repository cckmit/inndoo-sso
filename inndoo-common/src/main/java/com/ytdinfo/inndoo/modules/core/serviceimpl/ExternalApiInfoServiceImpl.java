package com.ytdinfo.inndoo.modules.core.serviceimpl;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.http.HtmlUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.script.ScriptUtil;
import com.ytdinfo.inndoo.common.constant.CommonConstant;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.enums.RedisCacheKeyConsts;
import com.ytdinfo.inndoo.common.enums.SourceType;
import com.ytdinfo.inndoo.common.utils.*;
import com.ytdinfo.inndoo.common.vo.*;
import com.ytdinfo.inndoo.config.redis.CacheExpire;
import com.ytdinfo.inndoo.modules.core.dao.ExternalApiInfoDao;
import com.ytdinfo.inndoo.modules.core.entity.Account;
import com.ytdinfo.inndoo.modules.core.entity.ActAccount;
import com.ytdinfo.inndoo.modules.core.entity.ExternalApiInfo;
import com.ytdinfo.inndoo.modules.core.handler.customapi.BaseCustomAPIHandler;
import com.ytdinfo.inndoo.modules.core.service.AccountService;
import com.ytdinfo.inndoo.modules.core.service.ActAccountService;
import com.ytdinfo.inndoo.modules.core.service.ExternalApiInfoService;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.*;
import javax.script.SimpleBindings;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * ???????????????????????????????????????
 *
 * @author yaochangning
 */
@Slf4j
@Service
@CacheConfig(cacheNames = "ExternalApiInfo")
public class ExternalApiInfoServiceImpl implements ExternalApiInfoService {

    @Autowired
    private ExternalApiInfoDao externalApiInfoDao;
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ActAccountService actAccountService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private LiaoNingUrlUtil liaoNingUrlUtil;

    @Autowired
    private ExternalApiInfoService dmzService;

    @Autowired
    protected ActivityApiUtil activityApiUtil;

    @Autowired
    private RedisTemplate<String,String> stringRedisTemplate;

    @Override
    public ExternalApiInfoDao getRepository() {
        return externalApiInfoDao;
    }

    /**
     * ??????ID??????
     *
     * @param id
     * @return
     */
    @Cacheable(key = "#id")
    @CacheExpire(expire = CommonConstant.SECOND_1DAY)
    @Override
    public ExternalApiInfo get(String id) {
        Optional<ExternalApiInfo> entity = getRepository().findById(id);
        if (entity.isPresent()) {
            return entity.get();
        }
        return null;
    }

    /**
     * ??????
     *
     * @param entity
     * @return
     */
    @CachePut(key = "#entity.id")
    @CacheExpire(expire = CommonConstant.SECOND_1DAY)
    @Override
    public ExternalApiInfo save(ExternalApiInfo entity) {
        if (StrUtil.isNotEmpty(entity.getApiSandboxScript())) {
            entity.setApiSandboxScript(HtmlUtil.unescape(entity.getApiSandboxScript()));
        }
        if (StrUtil.isNotEmpty(entity.getAchieveSandboxScript())) {
            entity.setAchieveSandboxScript(HtmlUtil.unescape(entity.getAchieveSandboxScript()));
        }
        return getRepository().save(entity);
    }

    /**
     * ??????
     *
     * @param entity
     * @return
     */
    @CachePut(key = "#entity.id")
    @CacheExpire(expire = CommonConstant.SECOND_1DAY)
    @Override
    public ExternalApiInfo update(ExternalApiInfo entity) {
        if (StrUtil.isNotEmpty(entity.getApiSandboxScript())) {
            entity.setApiSandboxScript(HtmlUtil.unescape(entity.getApiSandboxScript()));
        }
        if (StrUtil.isNotEmpty(entity.getAchieveSandboxScript())) {
            entity.setAchieveSandboxScript(HtmlUtil.unescape(entity.getAchieveSandboxScript()));
        }
        return getRepository().saveAndFlush(entity);
    }

    /**
     * ??????
     *
     * @param entity
     */
    @CacheEvict(key = "#entity.id")
    @Override
    public void delete(ExternalApiInfo entity) {
        getRepository().delete(entity);
    }

    /**
     * ??????Id??????
     *
     * @param id
     */
    @CacheEvict(key = "#id")
    @Override
    public void delete(String id) {
        getRepository().deleteById(id);
    }

    /**
     * ?????????????????????
     *
     * @param entities
     * @return
     */
    @Override
    public Iterable<ExternalApiInfo> saveOrUpdateAll(Iterable<ExternalApiInfo> entities) {
        List<ExternalApiInfo> list = getRepository().saveAll(entities);
        List<String> redisKeys = new ArrayList<>();
        for (ExternalApiInfo entity : entities) {
            redisKeys.add("ExternalApiInfo::" + entity.getId());
        }
        redisTemplate.delete(redisKeys);
        return list;
    }

    /**
     * ??????Id????????????
     *
     * @param ids
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(String[] ids) {
        ExternalApiInfoDao repository = getRepository();
        ArrayList<String> list = new ArrayList<>();
        Collections.addAll(list, ids);
        List<ExternalApiInfo> list4Delete = repository.findAllById(list);
        repository.deleteInBatch(list4Delete);
        List<String> redisKeys = new ArrayList<>();
        for (String id : ids) {
            redisKeys.add("ExternalApiInfo::" + id);
        }
        redisTemplate.delete(redisKeys);
    }

    /**
     * ????????????
     *
     * @param entities
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Iterable<ExternalApiInfo> entities) {
        getRepository().deleteAll(entities);
        List<String> redisKeys = new ArrayList<>();
        for (ExternalApiInfo entity : entities) {
            redisKeys.add("ExternalApiInfo::" + entity.getId());
        }
        redisTemplate.delete(redisKeys);
    }

    @Override
    public Page<ExternalApiInfo> findByCondition(ExternalApiInfo externalApiInfo, SearchVo searchVo, Pageable pageable) {

        return externalApiInfoDao.findAll(new Specification<ExternalApiInfo>() {
            @Nullable
            @Override
            public Predicate toPredicate(Root<ExternalApiInfo> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {

                // TODO ??????????????????????????????????????? ??????????????????????????????
                Path<Date> createTimeField = root.get("createTime");
                Path<String> nameField = root.get("name");
                Path<Integer> sourceField = root.get("source");
                List<Predicate> list = new ArrayList<Predicate>();

                //????????????
                if (StrUtil.isNotBlank(searchVo.getStartDate()) && StrUtil.isNotBlank(searchVo.getEndDate())) {
                    Date start = DateUtil.parse(searchVo.getStartDate());
                    Date end = DateUtil.parse(searchVo.getEndDate());
                    list.add(cb.between(createTimeField, start, DateUtil.endOfDay(end)));
                }
                if (StrUtil.isNotBlank(externalApiInfo.getName())) {
                    list.add(cb.like(nameField, "%" + externalApiInfo.getName() + "%"));
                }
                if (null != externalApiInfo.getSource()) {
                    list.add(cb.equal(sourceField, externalApiInfo.getSource()));
                }
                Predicate[] arr = new Predicate[list.size()];
                if (list.size() > 0) {
                    cq.where(list.toArray(arr));
                }
                return null;
            }
        }, pageable);
    }

    @Override
    public Result<Object> execute(String id, String accountId, String ext, boolean isObj) {
        ExternalApiInfo apiInfo = dmzService.get(id);
        if (apiInfo == null) {
            return new ResultUtil<>().setErrorMsg(String.format("externalApiInfo[%s]?????????", id));
        }
        if(SourceType.ZHE_JIANG_BANK_CCB.getValue().equals(apiInfo.getSource())){
            ExternalAPIResultVo vo = checkZJCCBWhite(apiInfo,accountId);
            if(vo.isSuccess()){
                return new ResultUtil<>().setData("true");
            }
            return new ResultUtil<>().setErrorMsg(vo.getMsg());
        }
        if(SourceType.HU_NAN_BANK_CCB.getValue().equals(apiInfo.getSource())){
            ExternalAPIResultVo vo = checkHNCCBWhite(apiInfo,accountId);
            if(vo.isSuccess()){
                return new ResultUtil<>().setData("true");
            }
            return new ResultUtil<>().setErrorMsg(vo.getMsg());
        }


        ExternalAPIResultVo vo = getRes(id, accountId, ext);
        if (!vo.isSuccess()) {
            return new ResultUtil<>().setErrorMsg(vo.getMsg());
        }
        ExternalApiInfo externalApiInfo = vo.getExternalApiInfo();
        String apiSandboxScript = externalApiInfo.getApiSandboxScript();
        try {
            String content = vo.getMsg();
            LiaoNingResult result = JSONUtil.toBean(content, LiaoNingResult.class);
            if (Boolean.TRUE.equals(result.getSuccess())) {
                List<Map<String, Object>> data = result.getData();
                if (data == null || data.size() == 0) {
                    return new ResultUtil<>().setErrorMsg("api??????????????????");
                } else {
                    if (StrUtil.isNotEmpty(externalApiInfo.getOutMsgBody())) {
                        // ????????????????????????????????????
                        Map defineMap = JSONUtil.toBean(externalApiInfo.getOutMsgBody(), Map.class);
                        String beanName = defineMap.get("beanName").toString();
                        BaseCustomAPIHandler customAPIHandler = SpringContextUtil.getBean(beanName, BaseCustomAPIHandler.class);
                        ResultCustomVo process = customAPIHandler.process(accountId, defineMap, vo.getActResult(), data.get(0));
                        if (isObj) {
                            return new ResultUtil<>().setData(process);
                        } else {
                            return new ResultUtil<>().setData(process.isSuccess());
                        }
                    }
                    SimpleBindings simpleBindings = new SimpleBindings();
                    simpleBindings.put("data", data);
                    simpleBindings.put("actResult", vo.getActResult());
                    Object obj = ScriptUtil.eval(apiSandboxScript, simpleBindings);
                    if (isObj) {
                        String jsonStr = JSONUtil.toJsonStr(obj);
                        Map returnObj = JSONUtil.toBean(jsonStr, Map.class);
                        return new ResultUtil<>().setData(returnObj);
                    } else {
                        return new ResultUtil<>().setData(obj);
                    }
                }
            } else {
                return new ResultUtil<>().setErrorMsg("api????????????");
            }
        } catch (Exception e) {
            return new ResultUtil<>().setErrorMsg(ExceptionUtil.stacktraceToString(e));
        }
    }

    /**
     * ???????????????????????????.
     * <p><br>
     * @param apiInfo
     * @param accountId
     * @return com.ytdinfo.inndoo.common.vo.ExternalAPIResultVo
     * @author zhulin
     * @date 2020/10/12 9:02 AM
     **/
    public ExternalAPIResultVo  checkHNCCBWhite(ExternalApiInfo apiInfo, String accountId){
        ExternalAPIResultVo vo = new ExternalAPIResultVo();
        vo.setSuccess(false);
        if(apiInfo == null){
            vo.setMsg("externalApiInfo?????????");
            return vo;
        }
        ActAccount actAccount = actAccountService.findByActAccountId(accountId);
        if(actAccount == null){
            vo.setMsg("?????????????????????");
            return vo;
        }
        String coreAccountId = actAccount.getCoreAccountId();
        Account account = accountService.get(coreAccountId);
        if(account == null ){
            vo.setMsg("????????????????????????");
            return vo;
        }
        if(StrUtil.isBlank(account.getPhone())){
            vo.setMsg("??????????????????????????????");
            return vo;
        }
        if(StrUtil.isBlank(account.getIdcardNo())){
            vo.setMsg("????????????????????????????????????");
            return vo;
        }
        if(StrUtil.length(account.getIdcardNo()) < 6){
            vo.setMsg("????????????????????????????????????");
            return vo;
        }
        String inputBody =  apiInfo.getInputDefinitionBody();
        Map<String,String> map = new HashMap<>();
        try{
            map =  JSONUtil.toBean(inputBody,Map.class);
        }catch (Exception e){
            vo.setMsg("externalApiInfo["+apiInfo.getId()+"]?????????????????????????????????");
            return vo;
        }
        if(!map.containsKey("org") || !map.containsKey("num")){
            vo.setMsg("externalApiInfo["+apiInfo.getId()+"]?????????????????????????????????");
            return vo;
        }
        String phone =  account.getPhone();
        String certNo = StrUtil.subSufByLength(account.getIdcardNo(),6) ;
        Map<String,Object> paramMap = new HashMap<>();
        paramMap.put("phone",phone);
        paramMap.put("certNo",certNo);
        String rediskey = RedisCacheKeyConsts.HN_CCB_INT_RESPONSE + "thenew:"+phone+":"+certNo;
        String redisBody = stringRedisTemplate.opsForValue().get(rediskey);
        String responseBody = "";
        if(redisBody == null){
            String params = HttpUtil.toParams(paramMap);
            String url = apiInfo.getUrl() + "?" + params;
            responseBody = HttpRequestUtil.get(url);
            if(StrUtil.isNotBlank(responseBody) && responseBody.contains("HEAD")){
                stringRedisTemplate.opsForValue().set(rediskey,responseBody);
                stringRedisTemplate.expire(rediskey,5,TimeUnit.MINUTES);
            }
        }else{
            responseBody =redisBody;
        }
        Document  document = null;
        try {
//          responseBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ROOT><HEAD><TranCode>1002</TranCode><TranDate>20201016</TranDate><TranTime>115344</TranTime><RetCode>00</RetCode><RetMsg>ok</RetMsg></HEAD><BODY><SjSignOrg>??????</SjSignOrg><SjSignDate>2012-09-01</SjSignDate><SjNum>1000000</SjNum><BusDate>2020-09-13</BusDate></BODY></ROOT>";
            document = DocumentHelper.parseText(responseBody);
        } catch (DocumentException e) {
            return vo;
        }
        // ???????????????
        Element root = document.getRootElement();
        // ??????Head??????
        Element HEAD = root.element("HEAD");
        List<Element> headChild = HEAD!=null?HEAD.elements():new ArrayList<>();
        Map<String,String> headMap = new HashMap<>();
        for(Element element :headChild){
            headMap.put(element.getName(),element.getText());
        }
        if(!headMap.containsKey("RetCode") || !StrUtil.equals("00",headMap.get("RetCode"))){
            return vo;
        }
        // ??????body??????
        Element BODY = root.element("BODY");
        List<Element> bodyChild = BODY!=null?BODY.elements():new ArrayList<>();
        Map<String,String> bodyMap = new HashMap<>();
        for(Element element :bodyChild){
            bodyMap.put(element.getName(),element.getText());
        }
        if (bodyMap.get("SjSignOrg") == null) {
            return vo;
        }
        String SjSignOrg = bodyMap.get("SjSignOrg");
        String SjNum = bodyMap.get("SjNum");
        boolean orgFlag = false;
        boolean numFlag = false;
        String org = map.get("org");
        String numStr = map.get("num");
        // ??????????????? ?????????????????????
        if(StrUtil.isBlank(org)){
            if(StrUtil.equals(SjSignOrg,"??????")){
                return vo;
            }else{
                orgFlag = true;
            }
        }else{
            if(StrUtil.equals(SjSignOrg,org)){
                orgFlag = true;
            }else{
                return vo;
            }
        }

        // ????????????????????? ???????????????????????????
        if(StrUtil.isBlank(numStr)){
            numFlag = true;
        }else{
            // numStr  :??????????????????????????????
            // 1000000 :????????????4???
            // 1000001 :??????1???
            // 1000001 :??????1???
            // 1000002 :??????2???
            // 1000004 :??????0???
            if(StrUtil.equals(SjNum,numStr)){
                numFlag = true;
            }
        }
        vo.setSuccess(orgFlag && numFlag);
        return vo;
    }


    public ExternalAPIResultVo checkZJCCBWhite(ExternalApiInfo apiInfo, String accountId){
        ExternalAPIResultVo vo = new ExternalAPIResultVo();
        vo.setSuccess(false);
        if(apiInfo == null){
            vo.setMsg("externalApiInfo?????????");
            return vo;
        }
        String inputBody =  apiInfo.getInputDefinitionBody();
        Map<String,String> map = new HashMap<>();
        try{
            map =  JSONUtil.toBean(inputBody,Map.class);

        }catch (Exception e){
            vo.setMsg("externalApiInfo["+apiInfo.getId()+"]?????????????????????????????????");
            return vo;
        }

        // {"ip":"15.34.1.40","port":"33139","tranCode":"1001","charset":"GBK","ItemCode","9999999"}
        if( !map.containsKey("ip") ||
            !map.containsKey("port")||
            !map.containsKey("tranCode")||
            !map.containsKey("ItemCode")||
            !map.containsKey("charset") ){
            vo.setMsg("externalApiInfo["+apiInfo.getId()+"]????????????????????????????????????");
            return vo;
        }
        // ???????????????????????????
        boolean isNew = false;
        if(StrUtil.equals(map.get("type"),"2")){
            isNew = true;
        }
        Map<String,String> headbody = new HashMap<>();
        if(isNew){
            headbody.put("url",map.get("url"));
            headbody.put("trCode",map.get("trCode"));
        }else{
            headbody.put("ip",map.get("ip"));
            headbody.put("port",map.get("port"));
            headbody.put("tranCode",map.get("tranCode"));
            headbody.put("charset",map.get("charset"));
        }
        ActAccount actAccount = actAccountService.findByActAccountId(accountId);
        if(actAccount == null){
            vo.setMsg("?????????????????????");
            return vo;
        }
        String coreAccountId = actAccount.getCoreAccountId();
        Account account = accountService.get(coreAccountId);
        if(account == null ){
            vo.setMsg("????????????????????????");
            return vo;
        }
        if(StrUtil.isBlank(account.getPhone())){
            vo.setMsg("??????????????????????????????");
            return vo;
        }

        String phone =  account.getPhone();
//        phone =  "15057593566";
        // ????????????
        String itemCode = map.get("ItemCode");
        String prefix = "core:";
        String tenantId = UserContext.getTenantId();
        if (StrUtil.isNotEmpty(tenantId)) {
            prefix += tenantId + ":";
        }
        String cacheKey = prefix + RedisCacheKeyConsts.ZJ_CCB_WHITE_LIST_RECORD + "new:" + itemCode;
        Object whitelistRecord = redisTemplate.opsForHash().get(cacheKey, phone);
        if(whitelistRecord != null){
            vo.setSuccess(true);
            return vo;
        }

        Map<String,Object> paramMap = new HashMap();
        // ?????? ??????ip??????????????????????????????1001?????????????????????
        paramMap.put("headbody",JSONUtil.toJsonStr(headbody));
        Map<String,String> requestbodyMap =new HashMap<>();
        // ?????????????????? ?????????
        requestbodyMap.put("Mobile",phone);
        requestbodyMap.put("ItemCode",map.get("ItemCode"));
        paramMap.put("requestbody",JSONUtil.toJsonStr(requestbodyMap));
        String responseBody = HttpUtil.post(apiInfo.getUrl(),paramMap);
        if(StrUtil.isNotEmpty(responseBody)){
            Map<String,Object>  responseMap = JSONUtil.toBean(responseBody, Map.class);
            if(responseMap.containsKey("success")){
                Object success =  responseMap.get("success");
                if(success != null){
                    if((Boolean) success){
                        responseBody = responseMap.get("message").toString();
                    }
                }

            }
        }
        String newRespone = StringUtils.substring(responseBody,8);
        Document document = null;
        try {
            document = DocumentHelper.parseText(newRespone);
        } catch (DocumentException e) {
            return vo;
        }
        // ???????????????
        Element root = document.getRootElement();
        Element HEAD = root.element("HEAD");
        List<Element> headChild = HEAD!=null?HEAD.elements():new ArrayList<>();
        boolean flag =false;
        for(Element element :headChild){
            if(StringUtils.equals(element.getName(),"RetCode")){
                flag = StringUtils.equals(element.getText(),"00");
                if(flag){
                    redisTemplate.opsForHash().put(cacheKey, phone, new Date() );
                }
                break;
            }
        }
        vo.setSuccess(flag);
        return vo;
    }


    public ExternalAPIResultVo getRes(String id, String accountId, String ext) {
        ExternalAPIResultVo vo = new ExternalAPIResultVo();
        vo.setSuccess(false);
        ActAccount actAccount = actAccountService.findByActAccountId(accountId);
        if (null == actAccount) {
            vo.setMsg("????????????????????????????????????");
            return vo;
        }
        Map params = new HashMap<>();
        if (StrUtil.isNotEmpty(ext)) {
            ext = HexUtil.decodeHexStr(ext);
            params = JSONUtil.toBean(ext, Map.class);
        }
        // ????????????????????????????????????
        ExternalApiInfo externalApiInfo = dmzService.get(id);
        if (externalApiInfo == null) {
            vo.setMsg(String.format("externalApiInfo[%s]?????????", id));
            return vo;
        }
        vo.setActResult(new HashMap());
        String achieveSandboxScript = externalApiInfo.getAchieveSandboxScript();
        if (StrUtil.isNotEmpty(achieveSandboxScript)) {
            // ??????????????????
            RequestDefineVo requestDefineVo = JSONUtil.toBean(achieveSandboxScript, RequestDefineVo.class);
            Map<String, String> apiReqMap = requestDefineVo.getParams();
            apiReqMap.put("accountId", accountId);
            apiReqMap.put("wxappid", externalApiInfo.getAppid());
            Map actCustomAPIData = activityApiUtil.getActCustomAPIData(requestDefineVo.getUrl(), apiReqMap);
            if (actCustomAPIData == null && actCustomAPIData.size() == 0) {
                vo.setMsg(String.format("[%s][%s]????????????", id, requestDefineVo.getUrl()));
                return vo;
            } else {
                vo.setActResult(actCustomAPIData);
                params.putAll(actCustomAPIData);
            }
        }
        if (StrUtil.isNotEmpty(externalApiInfo.getOutMsgBody())) {
            // ????????????????????????????????????
            Map defineMap = JSONUtil.toBean(externalApiInfo.getOutMsgBody(), Map.class);
            String beanName = defineMap.get("beanName").toString();
            BaseCustomAPIHandler customAPIHandler = SpringContextUtil.getBean(beanName, BaseCustomAPIHandler.class);
            Map<String, Object> params1 = customAPIHandler.getParams(accountId);
            if (params1 != null && params1.size() > 0) {
                params.putAll(params1);
            }
        }
        vo.setExternalApiInfo(externalApiInfo);
        String url = externalApiInfo.getUrl();
        String dataMapper = externalApiInfo.getInputDefinitionBody();
        String coreAccountId = actAccount.getCoreAccountId();
        Map<String, String> reqParams = new HashMap<>();
        if (StrUtil.isNotEmpty(dataMapper)) {
            if (dataMapper.contains("\"phone\"")) {
                Account account = accountService.get(coreAccountId);
                if (null == account) {
                    vo.setMsg("????????????????????????");
                    return vo;
                }
                // ?????????????????????????????????map??????
                String phone = account.getPhone();
                params.put("phone", phone);
            }
            if (dataMapper.contains("\"coreId\"") && StrUtil.isNotEmpty(coreAccountId)) {
                params.put("coreId", coreAccountId);
            }
            if (dataMapper.contains("\"accountId\"")) {
                params.put("accountId", accountId);
            }
            Map<String, String> dataMap = JSONUtil.toBean(dataMapper, Map.class);
            for (Map.Entry<String, String> entry : dataMap.entrySet()) {
                if (params.containsKey(entry.getKey())) {
                    reqParams.put(entry.getValue(), params.get(entry.getKey()).toString());
                }
            }
        }
        try {
            String content;
            if (Boolean.TRUE.equals(externalApiInfo.getIsDebugger())) {
                content = externalApiInfo.getMock();
            } else {
                content = liaoNingUrlUtil.getContent(url, reqParams, true);
            }
            vo.setMsg(content);
            vo.setSuccess(true);
            return vo;
        } catch (Exception e) {
            vo.setMsg(ExceptionUtil.stacktraceToString(e));
            return vo;
        }
    }

    @Override
    public boolean vertify(String id, String accountId, String ext) {
        Result<Object> execute = execute(id, accountId, ext, false);
        //log.error(JSONUtil.toJsonStr(execute));
        if (execute.isSuccess()) {
            if ("true".equals(execute.getResult().toString())) {
                return true;
            }
        }
        return false;
    }
}