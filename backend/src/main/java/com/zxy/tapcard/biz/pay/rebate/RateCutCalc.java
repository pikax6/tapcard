package com.zxy.tapcard.biz.pay.rebate;

import com.zxy.tapcard.common.Amt;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RateCutCalc implements RebateCalc {

    @Override
    public boolean mine(int rebateKind) { return rebateKind == 2; }

    @Override
    public BigDecimal calc(RebateBox box) {
        BigDecimal amt  = box.getAmt();
        BigDecimal rate = box.rateOf("rate");              // 0.90

        BigDecimal pay = Amt.fix2(amt.multiply(rate));     // 8.00 × 0.9 = 7.20
        BigDecimal cut = Amt.fix2(amt.subtract(pay));      // 8.00 − 7.20 = 0.80
        return Amt.min(cut, amt);                          // 封顶
    }
}
