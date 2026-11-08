package com.zxy.tapcard.biz.pay.rebate;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class NoCutCalc implements RebateCalc {

    @Override
    public boolean mine(int rebateKind) { return rebateKind == 1; }

    @Override
    public BigDecimal calc(RebateBox box) { return BigDecimal.ZERO; }
}
