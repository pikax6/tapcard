package com.zxy.tapcard.biz.settle.sheet;

import java.math.BigDecimal;

/** 具体装饰：按笔数折算积分。只改描述，不改金额 */
public class PointSticker extends SettleSheet {

    private final int perBill;

    public PointSticker(SettleSheet inner, int perBill) {
        super(inner);
        this.perBill = perBill;
    }

    public int points() { return innerCnt() * perBill; }

    @Override
    public BigDecimal total() { return innerTotal(); }

    @Override
    public int cnt() { return innerCnt(); }

    @Override
    public String desc() { return innerDesc() + " | 积分 +" + points(); }
}
