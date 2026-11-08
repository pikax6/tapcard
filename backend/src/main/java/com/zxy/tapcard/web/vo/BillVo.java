package com.zxy.tapcard.web.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BillVo {

    private Long id;
    private String billNo;
    private Integer kind;
    private String kindName;
    private Long acctId;
    private String stuName;
    private String devNo;
    private Long stallId;
    private BigDecimal amt;
    private BigDecimal rebateAmt;
    private BigDecimal payAmt;
    private Integer state;
    private String stateName;
    private String reqSn;
    private Long oriBillId;
    private String remark;
    private LocalDateTime createdAt;

    public static String kindName(Integer k) {
        if (k == null) return "";
        return switch (k) {
            case 1 -> "充值";
            case 2 -> "消费";
            case 3 -> "冲正";
            default -> "未知";
        };
    }

    public static String stateName(Integer s) {
        if (s == null) return "";
        return switch (s) {
            case 0 -> "待结算";
            case 1 -> "已结算";
            case 2 -> "已冲正";
            default -> "未知";
        };
    }
}
