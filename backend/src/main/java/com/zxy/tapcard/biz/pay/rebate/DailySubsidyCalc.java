package com.zxy.tapcard.biz.pay.rebate;

import com.zxy.tapcard.common.Amt;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DailySubsidyCalc implements RebateCalc {

    @Override
    public boolean mine(int rebateKind) { return rebateKind == 4; }

    @Override
    public BigDecimal calc(RebateBox box) {
        BigDecimal cap  = Amt.toYuan(box.longOf("capFen"));   // 每日上限 3.00 元
        BigDecimal left = Amt.fix2(cap.subtract(box.getUsedToday()));

        if (left.signum() <= 0) return BigDecimal.ZERO;         // 今天已经补满

        BigDecimal want = Amt.fix2(box.getAmt().multiply(box.rateOf("rate")));  // 想补一半
        return Amt.min(Amt.min(want, left), box.getAmt());      // 三重封顶
    }
}
