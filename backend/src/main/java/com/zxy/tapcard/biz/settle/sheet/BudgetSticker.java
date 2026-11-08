package com.zxy.tapcard.biz.settle.sheet;

import com.zxy.tapcard.common.Amt;

import java.math.BigDecimal;

/** 具体装饰：补贴预算池。算剩余额度、判断是否超支，同样不碰金额 */
public class BudgetSticker extends SettleSheet {

    private final BigDecimal rebateAmt;
    private final BigDecimal budget;

    public BudgetSticker(SettleSheet inner, BigDecimal rebateAmt, BigDecimal budget) {
        super(inner);
        this.rebateAmt = Amt.fix2(rebateAmt);
        this.budget = Amt.fix2(budget);
    }

    public BigDecimal left() { return Amt.fix2(budget.subtract(rebateAmt)); }

    public boolean over() { return Amt.lt(left(), BigDecimal.ZERO); }

    @Override
    public BigDecimal total() { return innerTotal(); }

    @Override
    public int cnt() { return innerCnt(); }

    @Override
    public String desc() {
        return innerDesc() + " | 预算剩余 " + left() + (over() ? "（超支）" : "");
    }
}
