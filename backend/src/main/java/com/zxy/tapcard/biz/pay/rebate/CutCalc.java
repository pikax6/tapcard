package com.zxy.tapcard.biz.pay.rebate;

import com.zxy.tapcard.common.Amt;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CutCalc implements RebateCalc {

    @Override
    public boolean mine(int rebateKind) { return rebateKind == 3; }

    @Override
    public BigDecimal calc(RebateBox box) {
        BigDecimal cut = Amt.toYuan(box.longOf("cutFen"));   // 100 分 = 1.00 元
        return Amt.min(cut, box.getAmt());                   // 封顶
    }
}
