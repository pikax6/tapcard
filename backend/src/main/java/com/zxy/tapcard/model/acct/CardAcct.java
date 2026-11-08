package com.zxy.tapcard.model.acct;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("card_acct")
public class CardAcct {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String stuNo;
    private String stuName;
    private String dept;
    private BigDecimal bal;
    private Integer state;//1正常/2冻结/3注销

    @Version
    private Integer ver;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
