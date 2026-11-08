package com.zxy.tapcard.model.settle;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("settle_bill")
public class SettleBill {

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDate settleDate;   // DATE 列用 LocalDate，不是 LocalDateTime
    private String devNo;
    private Integer cnt;
    private Integer devCnt;
    private BigDecimal amt;
    private BigDecimal rebateAmt;
    private Integer state;          // 0 待核对 / 1 已平账 / 2 有差异

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
