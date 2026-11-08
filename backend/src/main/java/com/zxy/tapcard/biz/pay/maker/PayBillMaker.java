package com.zxy.tapcard.biz.pay.maker;

import com.zxy.tapcard.biz.pay.draft.PayBillDraft;
import com.zxy.tapcard.model.pay.PayBill;

public interface PayBillMaker {

    boolean mine(int billKind);

    PayBill make(PayBillDraft draft);

    /** 三个 Maker 共用的字段搬运，子类不用重复写 */
    default PayBill baseOf(PayBillDraft d) {
        PayBill b = new PayBill();
        b.setBillNo(d.billNo());
        b.setKind(d.kind());
        b.setAcctId(d.acct().getId());
        b.setDevNo(d.dev() == null ? null : d.dev().getDevNo());
        b.setStallId(d.stallId());
        b.setReqSn(d.cmd().nonce());
        return b;
    }
}
