package com.zxy.tapcard.biz.pay.maker;

import com.zxy.tapcard.biz.pay.draft.PayBillDraft;
import com.zxy.tapcard.model.pay.PayBill;
import org.springframework.stereotype.Component;

@Component
public class ConsumeMaker implements PayBillMaker {

    @Override
    public boolean mine(int billKind) { return billKind == 2; }

    @Override
    public PayBill make(PayBillDraft d) {
        PayBill b = baseOf(d);
        b.setAmt(d.amt());
        b.setRebateAmt(d.rebateAmt());
        b.setPayAmt(d.payAmt());
        b.setState(0);                  // 待结算，等日结批次处理
        return b;
    }
}
