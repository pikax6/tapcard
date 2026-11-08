package com.zxy.tapcard.biz.pay.maker;

import com.zxy.tapcard.biz.pay.draft.PayBillDraft;
import com.zxy.tapcard.model.pay.PayBill;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ReverseMaker implements PayBillMaker {

    @Override
    public boolean mine(int billKind) { return billKind == 3; }

    @Override
    public PayBill make(PayBillDraft d) {
        PayBill b = baseOf(d);
        // 冲正单是负向单：金额、实付都要取负，否则 pay_amt = amt - rebate_amt 这条恒等式会断
        b.setAmt(d.amt().negate());
        b.setRebateAmt(d.rebateAmt().negate());
        b.setPayAmt(d.payAmt().negate());
        b.setState(0);
        b.setOriBillId(d.oriBillId());
        b.setRemark("冲正原单 #" + d.oriBillId());
        return b;
    }
}
