package com.zxy.tapcard.web.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SettleVo {

    private Long id;
    private LocalDate settleDate;
    private String devNo;
    private Integer cnt;
    private Integer devCnt;
    private Integer diff;           // devCnt - cnt，不落库，查询时算
    private BigDecimal amt;
    private BigDecimal rebateAmt;
    private BigDecimal payAmt;

    private Integer state;
    private String stateName;
    private String desc;            // 装饰链算出来的描述，直接给大屏
}
