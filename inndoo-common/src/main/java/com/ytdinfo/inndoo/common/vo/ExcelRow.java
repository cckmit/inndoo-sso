package com.ytdinfo.inndoo.common.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.metadata.BaseRowModel;

import java.io.Serializable;

/**
 * @ClassName 动态excel通用实体类
 * @Description: TODO
 * @Author: zl
 * @DATE 2019/11/1
 * @TIME 12:21
 * @Version 1.0
 */
public class ExcelRow  extends BaseRowModel implements Serializable {


    @ExcelProperty(index = 0)
    private String colum0;

    @ExcelProperty(index = 1)
    private String colum1;
    
    @ExcelProperty(index = 2)
    private String colum2;
    
    @ExcelProperty(index = 3)
    private String colum3;
    
    @ExcelProperty(index = 4)
    private String colum4;

    @ExcelProperty(index = 5)
    private String colum5;

    @ExcelProperty(index = 6)
    private String colum6;

    @ExcelProperty(index = 7)
    private String colum7;

    @ExcelProperty(index = 8)
    private String colum8;

    @ExcelProperty(index = 9)
    private String colum9;
    @ExcelProperty(index = 10)
    private String colum10;
    @ExcelProperty(index = 11)
    private String colum11;
    @ExcelProperty(index = 12)
    private String colum12;
    @ExcelProperty(index = 13)
    private String colum13;
    @ExcelProperty(index = 14)
    private String colum14;
    @ExcelProperty(index = 15)
    private String colum15;
    @ExcelProperty(index = 16)
    private String colum16;
    @ExcelProperty(index = 17)
    private String colum17;
    @ExcelProperty(index = 18)
    private String colum18;
    @ExcelProperty(index = 19)
    private String colum19;

    @ExcelProperty(index = 20)
    private String colum20;
    @ExcelProperty(index = 21)
    private String colum21;
    @ExcelProperty(index = 22)
    private String colum22;
    @ExcelProperty(index = 23)
    private String colum23;

    public String getColum0() {
        return colum0;
    }

    public void setColum0(String colum0) {
        this.colum0 = colum0;
    }

    public String getColum1() {
        return colum1;
    }

    public void setColum1(String colum1) {
        this.colum1 = colum1;
    }

    public String getColum2() {
        return colum2;
    }

    public void setColum2(String colum2) {
        this.colum2 = colum2;
    }

    public String getColum3() {
        return colum3;
    }

    public void setColum3(String colum3) {
        this.colum3 = colum3;
    }

    public String getColum4() {
        return colum4;
    }

    public void setColum4(String colum4) {
        this.colum4 = colum4;
    }

    public String getColum5() {
        return colum5;
    }

    public void setColum5(String colum5) {
        this.colum5 = colum5;
    }

    public String getColum6() {
        return colum6;
    }

    public void setColum6(String colum6) {
        this.colum6 = colum6;
    }

    public String getColum7() {
        return colum7;
    }

    public void setColum7(String colum7) {
        this.colum7 = colum7;
    }

    public String getColum8() {
        return colum8;
    }

    public void setColum8(String colum8) {
        this.colum8 = colum8;
    }

    public String getColum9() {
        return colum9;
    }

    public void setColum9(String colum9) {
        this.colum9 = colum9;
    }

    public String getColum10() {
        return colum10;
    }

    public void setColum10(String colum10) {
        this.colum10 = colum10;
    }

    public String getColum11() {
        return colum11;
    }

    public void setColum11(String colum11) {
        this.colum11 = colum11;
    }

    public String getColum12() {
        return colum12;
    }

    public void setColum12(String colum12) {
        this.colum12 = colum12;
    }

    public String getColum13() {
        return colum13;
    }

    public void setColum13(String colum13) {
        this.colum13 = colum13;
    }

    public String getColum14() {
        return colum14;
    }

    public void setColum14(String colum14) {
        this.colum14 = colum14;
    }

    public String getColum15() {
        return colum15;
    }

    public void setColum15(String colum15) {
        this.colum15 = colum15;
    }

    public String getColum16() {
        return colum16;
    }

    public void setColum16(String colum16) {
        this.colum16 = colum16;
    }

    public String getColum17() {
        return colum17;
    }

    public void setColum17(String colum17) {
        this.colum17 = colum17;
    }

    public String getColum18() {
        return colum18;
    }

    public void setColum18(String colum18) {
        this.colum18 = colum18;
    }

    public String getColum19() {
        return colum19;
    }

    public void setColum19(String colum19) {
        this.colum19 = colum19;
    }

    public String getColum20() {
        return colum20;
    }

    public void setColum20(String colum20) {
        this.colum20 = colum20;
    }

    public String getColum21() {
        return colum21;
    }

    public void setColum21(String colum21) {
        this.colum21 = colum21;
    }

    public String getColum22() {
        return colum22;
    }

    public void setColum22(String colum22) {
        this.colum22 = colum22;
    }

    public String getColum23() {
        return colum23;
    }

    public void setColum23(String colum23) {
        this.colum23 = colum23;
    }

    public String[] getColums(){
        String[] strings = new String[24];
        strings[0] =this.colum0!=null?this.colum0:"";
        strings[1] =this.colum1!=null?this.colum1:"";
        strings[2] =this.colum2!=null?this.colum2:"";
        strings[3] =this.colum3!=null?this.colum3:"";
        strings[4] =this.colum4!=null?this.colum4:"";
        strings[5] =this.colum5!=null?this.colum5:"";
        strings[6] =this.colum6!=null?this.colum6:"";
        strings[7] =this.colum7!=null?this.colum7:"";
        strings[8] =this.colum8!=null?this.colum8:"";
        strings[9] =this.colum9!=null?this.colum9:"";
        strings[10] =this.colum10!=null?this.colum10:"";
        strings[11] =this.colum11!=null?this.colum11:"";
        strings[12] =this.colum12!=null?this.colum12:"";
        strings[13] =this.colum13!=null?this.colum13:"";
        strings[14] =this.colum14!=null?this.colum14:"";
        strings[15] =this.colum15!=null?this.colum15:"";
        strings[16] =this.colum16!=null?this.colum16:"";
        strings[17] =this.colum17!=null?this.colum17:"";
        strings[18] =this.colum18!=null?this.colum18:"";
        strings[19] =this.colum19!=null?this.colum19:"";
        strings[20] =this.colum20!=null?this.colum20:"";
        strings[21] =this.colum21!=null?this.colum21:"";
        strings[22] =this.colum22!=null?this.colum22:"";
        strings[23] =this.colum23!=null?this.colum23:"";
        return  strings;
    }

}
