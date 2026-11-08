package com.zxy.tapcard.biz.pay.maker;

import com.zxy.tapcard.biz.pay.draft.PayBillDraft;
import com.zxy.tapcard.common.Amt;
import com.zxy.tapcard.model.pay.PayBill;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RechargeMaker implements PayBillMaker {

    @Override
    public boolean mine(int billKind) { return billKind == 1; }

    @Override
    public PayBill make(PayBillDraft d) {
        PayBill b = baseOf(d);
        b.setAmt(d.amt());
        b.setRebateAmt(BigDecimal.ZERO);
        b.setPayAmt(Amt.fix2(d.amt()));
        b.setState(1);                  // 充值不参与机具对账，直接终态
        b.setRemark("充值");
        return b;
    }
}
