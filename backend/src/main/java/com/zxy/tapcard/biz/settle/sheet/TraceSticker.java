package com.zxy.tapcard.biz.settle.sheet;

import java.math.BigDecimal;

/** 具体装饰：操作留痕。不改变任何数字，只往描述后面追加一段 */
public class TraceSticker extends SettleSheet {

    private final String note;

    public TraceSticker(SettleSheet inner, String note) {
        super(inner);
        this.note = note;
    }

    @Override
    public BigDecimal total() { return innerTotal(); }

    @Override
    public int cnt() { return innerCnt(); }

    @Override
    public String desc() { return innerDesc() + " | " + note; }
}
