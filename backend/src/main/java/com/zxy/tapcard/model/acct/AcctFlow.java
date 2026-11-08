package com.zxy.tapcard.model.acct;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("acct_flow")
public class AcctFlow {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long acctId;
    private Long billId;
    private Integer kind;           // 1 充值 / 2 消费 / 3 冲正
    private BigDecimal chgAmt;      // 允许负数
    private BigDecimal balBefore;
    private BigDecimal balAfter;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
}
