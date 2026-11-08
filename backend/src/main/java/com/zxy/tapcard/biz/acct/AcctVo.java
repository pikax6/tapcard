package com.zxy.tapcard.biz.acct;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 卡户视图：给页面用，不是数据库实体。放在 biz 而不是 model，避免实体类被展示字段污染 */
@Data
public class AcctVo {

    private Long id;
    private String stuNo;
    private String stuName;
    private String dept;
    private BigDecimal bal;
    private Integer state;
    private LocalDateTime createdAt;

    /**
     * 名下所有卡，含已作废的。
     * 带上 state 而不是只给卡号：列表页要按卡面状态标色，挂失/解挂后要能立刻看出变化。
     */
    private List<CardVo> cards = new ArrayList<>();

    /** 卡面精简视图，只给页面看，不暴露 createdAt 这类用不上的字段 */
    public record CardVo(String cardNo, Integer state) {}
}
