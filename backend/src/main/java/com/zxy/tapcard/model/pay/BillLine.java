package com.zxy.tapcard.model.pay;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("bill_line")
public class BillLine {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long billId;
    private String itemName;
    private Integer qty;
    private BigDecimal amt;
    private BigDecimal rebateAmt;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
}
