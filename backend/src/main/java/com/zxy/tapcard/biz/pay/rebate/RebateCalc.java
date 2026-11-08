package com.zxy.tapcard.biz.pay.rebate;

import java.math.BigDecimal;

public interface RebateCalc {

    /** 这种规则归我算吗 —— 统一动词 mine */
    boolean mine(int rebateKind);

    /** 算减免额（元），返回值不会超过原价 */
    BigDecimal calc(RebateBox box);
}
