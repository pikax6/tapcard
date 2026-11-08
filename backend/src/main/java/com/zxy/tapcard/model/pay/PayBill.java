package com.zxy.tapcard.model.pay;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("pay_bill")
public class PayBill {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String billNo;
    private Integer kind;           // 1 充值 / 2 消费 / 3 冲正
    private Long acctId;
    private String devNo;
    private Long stallId;

    private BigDecimal amt;         // 原价，冲正单为负
    private BigDecimal rebateAmt;
    private BigDecimal payAmt;      // = amt - rebateAmt

    private Integer state;          // 0 待结算 / 1 已结算 / 2 已冲正
    private String reqSn;           // 幂等键
    private Long oriBillId;         // 冲正指向原单
    private String remark;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
