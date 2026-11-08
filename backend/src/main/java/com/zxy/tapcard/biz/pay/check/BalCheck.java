package com.zxy.tapcard.biz.pay.check;

import com.zxy.tapcard.biz.pay.draft.PayBillDraft;
import com.zxy.tapcard.common.Amt;
import com.zxy.tapcard.common.BizErr;
import com.zxy.tapcard.common.ErrCode;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Order(40)
public class BalCheck extends PayCheck {

    @Override
    protected void verify(PayBillDraft d) {
        if (d.kind() == 1) return;                       // 充值不消耗余额

        BigDecimal bal = d.acct().getBal();
        if (Amt.lt(bal, d.payAmt())) {
            throw BizErr.of(ErrCode.BAL_LACK, "余额 " + bal + " 元，本笔需 " + d.payAmt() + " 元");
        }
    }
}
