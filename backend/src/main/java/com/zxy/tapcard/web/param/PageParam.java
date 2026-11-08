package com.zxy.tapcard.web.param;

import lombok.Data;

import java.time.LocalDate;

/** 列表查询的公共入参。分页用 limit/offset 而不是插件，够用且少一层依赖 */
@Data
public class PageParam {

    private Integer page;
    private Integer size;
    private String keyword;
    private LocalDate from;
    private LocalDate to;

    /** 账单类型（1 充值 / 2 消费 / 3 冲正），null 表示不筛 */
    private Integer kind;

    /** 账单状态（0 待结算 / 1 已结算 / 2 已冲正），null 表示不筛 */
    private Integer state;

    public int pageNo() {
        return page == null || page <= 1 ? 1 : page;
    }

    public int limit() {
        int s = size == null ? 20 : size;
        if (s <= 0) return 20;
        return Math.min(s, 200);        // 上限 200，防止前端一次拉全表
    }

    public int offset() {
        return (pageNo() - 1) * limit();
    }
}
