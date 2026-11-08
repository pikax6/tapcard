package com.zxy.tapcard.dao.stat;

import lombok.Data;

import java.math.BigDecimal;

/** 按档口/日期汇总的查询结果行 */
@Data
public class AcctStatRow {
    private Long stallId;
    private String stallName;
    private Integer cnt;
    private BigDecimal amt;
    private BigDecimal rebateAmt;
    private BigDecimal payAmt;
}
