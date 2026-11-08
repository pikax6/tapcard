package com.zxy.tapcard.biz.pay.rebate;

import com.zxy.tapcard.common.Amt;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PointCutCalc implements RebateCalc {

    /** 演示环境：每张卡固定 1000 积分（本项目未建积分账户表） */
    private static final long FIXED_POINTS = 1000;

    @Override
    public boolean mine(int rebateKind) { return rebateKind == 5; }

    @Override
    public BigDecimal calc(RebateBox box) {
        long perYuan = box.longOf("pointPerYuan");           // 100 积分抵 1 元
        if (perYuan <= 0) return BigDecimal.ZERO;

        // 1000 / 100 = 10，单位是「元」不是「分」，所以不能用 Amt.toYuan()
        BigDecimal cut = BigDecimal.valueOf(FIXED_POINTS / perYuan);
        return Amt.min(cut, box.getAmt());                   // 封顶到原价
    }
}
