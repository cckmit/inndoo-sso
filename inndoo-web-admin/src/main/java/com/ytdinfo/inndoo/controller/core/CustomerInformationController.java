package com.ytdinfo.inndoo.controller.core;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.metadata.Sheet;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.ytdinfo.inndoo.base.BaseController;
import com.ytdinfo.inndoo.common.annotation.SystemLog;
import com.ytdinfo.inndoo.common.constant.ExcelConstant;
import com.ytdinfo.inndoo.common.context.UserContext;
import com.ytdinfo.inndoo.common.utils.*;
import com.ytdinfo.inndoo.common.vo.ExcelRow;
import com.ytdinfo.inndoo.common.vo.PageVo;
import com.ytdinfo.inndoo.common.vo.Result;
import com.ytdinfo.inndoo.common.vo.SearchVo;
import com.ytdinfo.inndoo.modules.base.entity.Dict;
import com.ytdinfo.inndoo.modules.base.entity.DictData;
import com.ytdinfo.inndoo.modules.base.service.DictDataService;
import com.ytdinfo.inndoo.modules.base.service.DictService;
import com.ytdinfo.inndoo.modules.core.entity.*;
import com.ytdinfo.inndoo.modules.core.service.AccountFormService;
import com.ytdinfo.inndoo.modules.core.service.CustomerInformationService;
import com.ytdinfo.inndoo.modules.core.service.mybatis.ICustomerInformationExtendService;
import com.ytdinfo.inndoo.modules.core.service.mybatis.ICustomerInformationService;
import com.ytdinfo.inndoo.utils.PrivacyUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.StringUtil;
import org.aspectj.apache.bcel.classfile.Module;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author yaochangning
 */
@Slf4j
@RestController
@Api(description = "客户信息表管理接口")
@RequestMapping("/customerinformation")
public class CustomerInformationController extends BaseController<CustomerInformation, String> {

    @Autowired
    private CustomerInformationService customerInformationService;

    @Autowired
    private AccountFormService accountFormService;
    @Autowired
    private DictService dictService;
    @Autowired
    private DictDataService dictDataService;
    @Autowired
    private ICustomerInformationService iCustomerInformationService;
    @Autowired
    private ICustomerInformationExtendService iCustomerInformationExtendService;
    @Override
    public CustomerInformationService getService() {
        return customerInformationService;
    }


    @RequestMapping(value = "/listByCondition", method = RequestMethod.GET)
    @ApiOperation(value = "多条件分页获取")
    @SystemLog(description = "多条件分页获取")
    public Result<Page<CustomerInformation>> listByCondition(@ModelAttribute CustomerInformation customerInformation,
                                                            @ModelAttribute SearchVo searchVo,
                                                            @ModelAttribute PageVo pageVo){

        Page<CustomerInformation> page = customerInformationService.findByCondition(customerInformation, searchVo, PageUtil.initPage(pageVo));
        for(CustomerInformation item:page.getContent()) {
            item.setName(PrivacyUtil.nameEncrypt(item.getName()));
            item.setPhone(PrivacyUtil.phoneEncrypt(item.getPhone()));
            item.setIdcardNo(PrivacyUtil.formatToMask(item.getIdcardNo()));
            item.setBankBranchName(PrivacyUtil.formatToMask(item.getBankBranchName()));
            item.setEmail(PrivacyUtil.formatToMask(item.getEmail()));
            item.setInstitutionalName(PrivacyUtil.formatToMask(item.getInstitutionalName()));
            item.setAddress(PrivacyUtil.formatToMask(item.getAddress()));
            item.setCustomerNo(PrivacyUtil.formatToMask(item.getCustomerNo()));
        }
        return new ResultUtil<Page<CustomerInformation>>().setData(page);
    }

    @Override
    @RequestMapping(value = "/batch_delete/{ids}", method = RequestMethod.DELETE)
    @ResponseBody
    @ApiOperation(value = "通过id批量删除")
    public Result<Object> batchDeleteByIds(@PathVariable String[] ids){
        customerInformationService.delete(ids);
        return new ResultUtil<Object>().setSuccessMsg("批量删除数据成功");
    }


    @RequestMapping(value = "/exportTemplate")
    @ApiOperation(value = "导出客户信息表管理模板")
    @SystemLog(description = "导出客户信息表管理模板")
    public void exportTemplate( HttpServletResponse response)  throws IOException,NoSuchFieldException {
        List<List<String>> rows = CollUtil.newArrayList();
        List<String> list = new ArrayList();
        list.add("手机号码");
        list.add("客户号");
        list.add("姓名");
        list.add("身份证号码");
        list.add("银行卡号");
        list.add("出生日期");
        list.add("邮箱");
        list.add("地址");
        list.add("分行编码");
        list.add("分行名称");
        list.add("机构编号");
        list.add("机构名称");
        list.add("企业名称");
        list.add("客群编码");
        //获取客户信息拓展字段
        List<DictData> dictDatas = customerInformationService.findDictData();
        if(CollectionUtil.isNotEmpty(dictDatas)){
            for(DictData dictData: dictDatas){
                list.add(dictData.getTitle());
            }
        }
        AccountForm AccountForm = accountFormService.findByAppidAndIsIdentifierForm(UserContext.getAppid(), true);
        //获取初始的用户标识的注册页控件列表
        List<AccountFormMeta> IsIdentifierFormMetas = AccountForm.getAccountFormMetas();
        if (null != IsIdentifierFormMetas && IsIdentifierFormMetas.size() > 0) {
            CustomerInformation customerInformation = new CustomerInformation();
            for(AccountFormMeta accountFormMeta: IsIdentifierFormMetas){
                 boolean flag = isExistFieldName(accountFormMeta.getMetaType(),customerInformation);
                 if(! flag) {
                     boolean flag1 = isExistByDictData(accountFormMeta.getTitle());
                     if(!flag1){
                         list.add(accountFormMeta.getTitle());
                     }
                 }
            }
        }
        List<String> row1 = CollUtil.newArrayList(list);
        rows.add(row1);
        List<String> row2 = getRow2();
        rows.add(row2);
        String downloadDate = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String excelFileName  = "客户信息导入模板"+downloadDate+".xlsx";
        OutputStream out = null;
        ExcelWriter writer = null;
        try {
            out = response.getOutputStream();
            response.setContentType("multipart/form-data");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-disposition", "attachment;filename=" + URLUtil.encode(excelFileName, StringUtil.UTF8));
            writer = new ExcelWriter(out, ExcelTypeEnum.XLSX, true);
            Sheet sheet =new Sheet(1,0);
            sheet.setAutoWidth(Boolean.TRUE);
            writer.write(rows,sheet);
            rows.clear();;
        } catch (IOException e) {
            response.reset();
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");
            response.getWriter().println(JSONUtil.toJsonStr(  new ResultUtil<Object>().setErrorMsg("下载模板失败")));
        }finally {
            out.flush();
            writer.finish();
            out.close();
        }

    }

    @ResponseBody
    @RequestMapping(value = "/importData")
    @ApiOperation(value = "导入客户信息")
    @SystemLog(description = "导入客户信息")
    public Result<Object> importExcel(@RequestParam(value = "file", required = true) MultipartFile file,
                                      HttpServletRequest request, HttpSession session) throws SQLException ,NoSuchFieldException{
        boolean checkpass= FileHeaderUtil.isConsistentType(file);
        if(!checkpass) {
            return new ResultUtil<Object>().setErrorMsg("文件头与文件类型不一致，请检查文件");
        }
        /*1.读Excel, 检查是否存在
          2.数据处理（判定这个名单是否含高级校验），如果含高级校验组装扩展表的数据
          2.批量插入
         */
        if(file == null){
            return new ResultUtil<Object>().setErrorMsg("未能读取到文件");
        }
        String fileName=file.getOriginalFilename();
        if(StringUtils.isBlank(fileName) ){
            return new ResultUtil<Object>().setErrorMsg("请上传xlsx，xls文件");
        }
        String suffix = StringUtils.substring(fileName,StringUtils.lastIndexOf(fileName,"."));
        if(!StringUtils.equalsAnyIgnoreCase(suffix,".xlsx") &&  !StringUtils.equalsAnyIgnoreCase(suffix,".xls") ){
            return new ResultUtil<Object>().setErrorMsg("请上传xlsx，xls文件");
        }
        long start = System.currentTimeMillis();
        System.out.println(start);

        // 导入文件地址
        String filePath = ReadExcelUtil.FilePath(file, request);
        // excel读取方法
        CustomerInformationController.ExcelListener excelListener = new CustomerInformationController.ExcelListener();
        try {
            InputStream fileStream = new BufferedInputStream(new FileInputStream(filePath));
            EasyExcelFactory.readBySax(fileStream, new Sheet(1, 0, ExcelRow.class), excelListener);
        } catch (FileNotFoundException e) {
            return new ResultUtil<Object>().setErrorMsg("文件读取异常！");
        }
        List<ExcelRow> excelRows = excelListener.getDatas();
        long redtime = (System.currentTimeMillis()-start)/1000;
        long preTime = 0;
        long writeTime1 = 0;
        long writeTime2 = 0;
        long writeTime3 = 0;
        long writeTime4 = 0;

        if (excelRows != null && excelRows.size() > 0) {
            // 校验excel头部
            ExcelRow excelHead = excelRows.get(0);
            if (excelHead == null) {
                return new ResultUtil<Object>().setErrorMsg("没有头列,导入模板错误！");
            }
            if (!"手机号码".equals(excelHead.getColum0())) {
                return new ResultUtil<Object>().setErrorMsg("第1列不是手机号码,导入模板错误！");
            }
            if (!"客户号".equals(excelHead.getColum1())) {
                return new ResultUtil<Object>().setErrorMsg("第2列不是客户号,导入模板错误！");
            }
            if (!"姓名".equals(excelHead.getColum2())) {
                return new ResultUtil<Object>().setErrorMsg("第3列不是姓名,导入模板错误！");
            }
            if (!"身份证号码".equals(excelHead.getColum3())) {
                return new ResultUtil<Object>().setErrorMsg("第4列不是身份证号码,导入模板错误！");
            }
            if (!"银行卡号".equals(excelHead.getColum4())) {
                return new ResultUtil<Object>().setErrorMsg("第5列不是银行卡号,导入模板错误！");
            }
            if (!"出生日期".equals(excelHead.getColum5())) {
                return new ResultUtil<Object>().setErrorMsg("第6列不是出生日期,导入模板错误！");
            }
            if (!"邮箱".equals(excelHead.getColum6())) {
                return new ResultUtil<Object>().setErrorMsg("第6列不是邮箱,导入模板错误！");
            }
            if (!"地址".equals(excelHead.getColum7())) {
                return new ResultUtil<Object>().setErrorMsg("第8列不是地址,导入模板错误！");
            }
            if (!"分行编码".equals(excelHead.getColum8())) {
                return new ResultUtil<Object>().setErrorMsg("第9列不是分行编码,导入模板错误！");
            }
            if (!"分行名称".equals(excelHead.getColum9())) {
                return new ResultUtil<Object>().setErrorMsg("第10列不是分行名称,导入模板错误！");
            }
            if (!"机构编号".equals(excelHead.getColum10())) {
                return new ResultUtil<Object>().setErrorMsg("第11列不是机构编号,导入模板错误！");
            }
            if (!"机构名称".equals(excelHead.getColum11())) {
                return new ResultUtil<Object>().setErrorMsg("第12列不是机构名称,导入模板错误！");
            }
            if (!"企业名称".equals(excelHead.getColum12())) {
                return new ResultUtil<Object>().setErrorMsg("第13列不是企业名称,导入模板错误！");
            }
            if (!"客群编码".equals(excelHead.getColum13())) {
                return new ResultUtil<Object>().setErrorMsg("第14列不是客群编码,导入模板错误！");
            }
            if (!"年龄".equals(excelHead.getColum14())) {
                return new ResultUtil<Object>().setErrorMsg("第15列不是年龄,导入模板错误！");
            }
            if (!"客户护照号".equals(excelHead.getColum15())) {
                return new ResultUtil<Object>().setErrorMsg("第16列不是客户护照号,导入模板错误！");
            }
            String[] heand = excelHead.getColums();
            excelRows.remove(0);
            // 批量处理
            List<CustomerInformation> customerInformations = new ArrayList<>();
            List<String> deleteIdentifers = new ArrayList<>();
            Date newDate = new Date();
            for (ExcelRow temp : excelRows) {
                CustomerInformation customerInformation = new CustomerInformation();
                customerInformation.setId(String.valueOf(SnowFlakeUtil.getFlowIdInstance().nextId()) );
                if(StrUtil.isNotBlank(temp.getColum0())){
                    customerInformation.setPhone(temp.getColum0().trim());
                }else {
                    customerInformation.setPhone("");
                }
                if(StrUtil.isNotBlank(temp.getColum1())){
                    customerInformation.setCustomerNo(temp.getColum1().trim());
                }else {
                    customerInformation.setCustomerNo("");
                }
                if(StrUtil.isNotBlank(temp.getColum2())){
                    customerInformation.setName(temp.getColum2().trim());
                }else {
                    customerInformation.setName("");
                }
                if(StrUtil.isNotBlank(temp.getColum3())){
                    customerInformation.setIdcardNo(temp.getColum3().trim());
                }else {
                    customerInformation.setIdcardNo("");
                }
                if(StrUtil.isNotBlank(temp.getColum4())){
                    customerInformation.setBankcardNo(temp.getColum4().trim());
                }else {
                    customerInformation.setBankcardNo("");
                }
                if(StrUtil.isNotBlank(temp.getColum5())){
                    customerInformation.setBirthday(temp.getColum5().trim());
                }else {
                    customerInformation.setBirthday("");
                }
                if(StrUtil.isNotBlank(temp.getColum6())){
                    customerInformation.setEmail(temp.getColum6().trim());
                }else {
                    customerInformation.setEmail("");
                }
                if(StrUtil.isNotBlank(temp.getColum7())){
                    customerInformation.setAddress(temp.getColum7().trim());
                }else {
                    customerInformation.setAddress("");
                }
                if(StrUtil.isNotBlank(temp.getColum8())){
                    customerInformation.setBankBranchNo(temp.getColum8().trim());
                }else {
                    customerInformation.setBankBranchNo("");
                }
                if(StrUtil.isNotBlank(temp.getColum9())){
                    customerInformation.setBankBranchName(temp.getColum9().trim());
                }else {
                    customerInformation.setBankBranchName("");
                }
                if(StrUtil.isNotBlank(temp.getColum10())){
                    customerInformation.setInstitutionalCode(temp.getColum10().trim());
                }else {
                    customerInformation.setInstitutionalCode("");
                }
                if(StrUtil.isNotBlank(temp.getColum11())){
                    customerInformation.setInstitutionalName(temp.getColum11().trim());
                }else {
                    customerInformation.setInstitutionalName("");
                }
                if(StrUtil.isNotBlank(temp.getColum12())){
                    customerInformation.setCompanyName(temp.getColum12().trim());
                }else {
                    customerInformation.setCompanyName("");
                }
                if(StrUtil.isNotBlank(temp.getColum13())){
                    customerInformation.setCustomerGroupCoding(temp.getColum13().trim());
                }else {
                    customerInformation.setCustomerGroupCoding("");
                }
                //获取拓展字段的值

                //获取类的属性值数量
                Integer fieldSize = fieldSize(customerInformation) - 3;
                if(heand.length > fieldSize-1 ) {
                    List<CustomerInformationExtend> customerInformationExtends = new ArrayList<>();
                    for(Integer index = fieldSize;index < heand.length;index ++ ){
                        Object object = ReflectUtil.getFieldValue(temp, "colum"+index);
                        if(null != object && StrUtil.isNotBlank(object.toString().trim())){
                            CustomerInformationExtend customerInformationExtend = new CustomerInformationExtend();
                            String handName = heand[index];
                            customerInformationExtend.setTitle(handName);
                            customerInformationExtend.setValue(object.toString());
                            customerInformationExtend.setCustomerInformationId(customerInformation.getId());
                            customerInformationExtend.setCreateTime(newDate);
                            customerInformationExtend.setUpdateTime(newDate);
                            customerInformationExtends.add(customerInformationExtend);
                        }

                    }
                    customerInformation.setCustomerInformationExtends(customerInformationExtends);
                }
                customerInformation.setCreateTime(new Date());
                customerInformation.setUpdateTime(new Date());
                Result<String> identifierResult = customerInformationService.getIdentifier(customerInformation);
                if (identifierResult.isSuccess()) {
                    String identifier = identifierResult.getResult();
                    customerInformation.setIdentifier(identifier);
                    //如果有重复的identifier数据删除之前的数据保留最新的数据
                    if(deleteIdentifers.contains(identifier)){
                        customerInformations.removeIf(it -> it.getIdentifier().equals(identifier));
                    }
                    deleteIdentifers.add(identifier);
                } else {
                    return new ResultUtil<Object>().setErrorMsg(identifierResult.getMessage());
                }
                customerInformations.add(customerInformation);
            }
            preTime = (System.currentTimeMillis()-start)/1000 - redtime;
            if(deleteIdentifers.size()>0){
                iCustomerInformationService.deleteBatchByIdentifers(deleteIdentifers,100000);
            }
            writeTime1 = (System.currentTimeMillis()-start)/1000 - preTime;
            iCustomerInformationService.saveBatchWithIgnore(customerInformations, 10000);
            writeTime2 = (System.currentTimeMillis()-start)/1000 - writeTime1;

            //批量保存客户信息拓展数据
            if(CollectionUtil.isNotEmpty(customerInformations) ){
                List<CustomerInformationExtend> customerInformationExtends = new ArrayList<>();
                for(CustomerInformation customerInformation: customerInformations) {
                    List<CustomerInformationExtend> cExtends = customerInformation.getCustomerInformationExtends();
                    customerInformationExtends.addAll(cExtends);
                }
                if (CollectionUtil.isNotEmpty(customerInformationExtends)) {
                    iCustomerInformationExtendService.saveBatchWithIgnore(customerInformationExtends,10000);
                }
            }

        }


        System.out.println("总耗时"+(System.currentTimeMillis()-start)/1000);
        return new ResultUtil<Object>().setSuccessMsg("导入成功");

    }

    public List<String> getRow2(){
        List<List<String>> rows = CollUtil.newArrayList();
        List<String> list = new ArrayList();
        list.add("188XXXXX");
        list.add("12XX23");
        list.add("XXXX");
        list.add("XXXXXXXXX");
        list.add("XXXXX");
        list.add("yyyy-mm-dd");
        list.add("XXX@XXX");
        list.add("XXXX");
        list.add("123XXX23");
        list.add("XXX");
        list.add("XXX");
        list.add("XXX");
        list.add("XXX");

        //获取客户信息拓展字段
        List<DictData> dictDatas = customerInformationService.findDictData();
        if(CollectionUtil.isNotEmpty(dictDatas)){
            for(DictData dictData: dictDatas){
                list.add("XXXX");
            }
        }
        List<String> row2 = CollUtil.newArrayList(list);
        return row2;
    }

    /**
     * 判断你一个类是否存在某个属性（字段）
     * @param fieldName 字段
     * @param obj   类对象
     * @return true:存在，false:不存在, null:参数不合法
     */
    public Boolean isExistFieldName(String fieldName, Object obj) throws NoSuchFieldException {
        if (obj == null || StrUtil.isEmpty(fieldName)) {
            return null;
        }
        //获取这个类的所有属性
        Field[] fields = obj.getClass().getDeclaredFields();
        boolean flag = false;
        //循环遍历所有的fields
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].getName().equals(fieldName)) {
                flag = true;
                break;
            }
        }
        return flag;
    }

    /**
     * 判断你一个类属性数量
     * @param obj   类对象
     * @return true:存在，false:不存在, null:参数不合法
     */
    public Integer fieldSize( Object obj) throws NoSuchFieldException {
        //获取这个类的所有属性
        Field[] fields = obj.getClass().getDeclaredFields();
        return fields.length;
    }

    /**
     * 判断是否在字典里
     * @param fieldName
     * @return
     */
    public Boolean isExistByDictData(String fieldName)  {
        Boolean flag = false;
        //获取客户信息拓展字段
        List<DictData> dictDatas = customerInformationService.findDictData();
        if(CollectionUtil.isNotEmpty(dictDatas)){
            for(DictData dictData: dictDatas){
                if(dictData.getTitle().equals(fieldName)){
                    flag = true;
                    return flag;
                }
            }
        }
        return flag;
    }

    // 私有化导入监听器
    public class ExcelListener extends AnalysisEventListener<ExcelRow> {
        private List<ExcelRow> datas = new ArrayList<>();
        @Override
        public void invoke(ExcelRow excelRow, AnalysisContext analysisContext) {
            if (excelRow != null) {
                datas.add(excelRow);
            }
        }
        @Override
        public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        }
        public List<ExcelRow> getDatas() {
            return this.datas;
        }
    }


    @RequestMapping(value = "/query/{id}", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(value = "通过id获取")
    @Override
    public Result<CustomerInformation> query(@PathVariable String id){
        CustomerInformation item = getService().get(id);
        item.setTmname(PrivacyUtil.nameEncrypt(item.getName()));
        item.setTmphone(PrivacyUtil.phoneEncrypt(item.getPhone()));
        item.setTmidcardNo(PrivacyUtil.formatToMask(item.getIdcardNo()));
        item.setTmemail(PrivacyUtil.formatToMask(item.getEmail()));
        item.setTmbankBranchName(PrivacyUtil.formatToMask(item.getBankBranchName()));
        item.setTminstitutionalName(PrivacyUtil.formatToMask(item.getInstitutionalName()));
        item.setTmaddress(PrivacyUtil.formatToMask(item.getAddress()));
        item.setTmcustomerNo(PrivacyUtil.formatToMask(item.getCustomerNo()));
        item.setTmbankcardNo(PrivacyUtil.formatToMask(item.getBankcardNo()));
        return new ResultUtil<CustomerInformation>().setData(item);
    }
}
