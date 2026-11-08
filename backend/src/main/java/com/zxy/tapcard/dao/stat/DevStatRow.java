package com.zxy.tapcard.dao.stat;

import lombok.Data;

import java.math.BigDecimal;

/** 按机具汇总的统计行 */
@Data
public class DevStatRow {
    private String devNo;
    private String devName;
    private Integer cnt;
    private BigDecimal amt;
    private BigDecimal rebateAmt;
    private BigDecimal payAmt;
}
