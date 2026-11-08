package com.zxy.tapcard.dao.stat;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 按日期汇总的统计行 */
@Data
public class DayStatRow {
    private LocalDate day;
    private Integer cnt;
    private BigDecimal amt;
    private BigDecimal rebateAmt;
    private BigDecimal payAmt;
}
