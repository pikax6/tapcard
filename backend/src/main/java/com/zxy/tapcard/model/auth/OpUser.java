package com.zxy.tapcard.model.auth;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("op_user")
public class OpUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String empNo;
    private String empName;
    private String pwdHash;
    private Integer role;           // 1 学生 / 2 商户 / 3 财务 / 9 管理员
    private Long acctId;
    private String devNo;
    private Long stallId;
    private Integer state;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
