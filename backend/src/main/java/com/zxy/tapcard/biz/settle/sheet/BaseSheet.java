package com.zxy.tapcard.biz.settle.sheet;

import com.zxy.tapcard.common.Amt;

import java.math.BigDecimal;

/** 具体构件：一天的结算数字，后面所有装饰都叠在它上面 */
public class BaseSheet extends SettleSheet {

    private final int cnt;
    private final BigDecimal amt;
    private final BigDecimal rebateAmt;

    public BaseSheet(int cnt, BigDecimal amt, BigDecimal rebateAmt) {
        this.cnt = cnt;
        this.amt = Amt.fix2(amt);
        this.rebateAmt = Amt.fix2(rebateAmt);
    }

    @Override
    public BigDecimal total() { return Amt.fix2(amt.subtract(rebateAmt)); }

    @Override
    public int cnt() { return cnt; }

    @Override
    public String desc() {
        return "系统 " + cnt + " 笔 / 原价 " + amt + " / 减免 " + rebateAmt + " / 实付 " + total();
    }
}
