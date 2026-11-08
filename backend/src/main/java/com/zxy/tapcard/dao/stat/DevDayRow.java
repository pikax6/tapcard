package com.zxy.tapcard.dao.stat;


import lombok.Data;

import java.math.BigDecimal;

/** 按机具汇总的查询结果行，不对应任何表 */
@Data
public class DevDayRow {
    private Integer cnt;            // 笔数       ← 对应 SQL 的 count(*) as cnt
    private BigDecimal amt;         // 金额合计   ← sum(amt) as amt
    private BigDecimal rebateAmt;   // 减免合计   ← sum(rebate_amt) as rebate_amt
}
