package com.zxy.tapcard.web.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 统计行：key 是分组标识（档口 id / 机具号 / 日期字符串） */
@Data
public class StatVo {

    private String key;
    private String name;
    private Integer cnt;
    private BigDecimal amt;
    private BigDecimal rebateAmt;
    private BigDecimal payAmt;
}
