package com.ytdinfo.inndoo.modules.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ytdinfo.inndoo.base.BaseWechatEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;

@Data
@Entity
@Table(name = "t_decrypt_account")
@TableName("t_decrypt_account")
@ApiModel(value = "解密账户关键信息")
@SQLDelete(sql = "update t_decrypt_account set is_deleted=1 where id=?")
@Where(clause = "is_deleted=0")
public class DecryptAccount  extends BaseWechatEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "小核心账户")
    @Column(length = 32, nullable = false)
    private String coreAccountId;

    @ApiModelProperty(value = "解密账户字段")
    @Column(length = 32, nullable = false)
    private String accountType;

    @ApiModelProperty(value = "解密数据")
    @Column(length = 64, nullable = false)
    private String decryptValue;
}
