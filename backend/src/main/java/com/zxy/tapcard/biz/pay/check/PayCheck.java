package com.zxy.tapcard.biz.pay.check;

import com.zxy.tapcard.biz.pay.draft.PayBillDraft;

public abstract class PayCheck {

    protected PayCheck next;

    /** 链式装配：a.next(b).next(c) */
    public PayCheck next(PayCheck n) { this.next = n; return n; }

    /** 模板方法：final，不许子类改流程 */
    public final void run(PayBillDraft draft) {
        verify(draft);
        if (next != null) next.run(draft);
    }

    /** 子类只实现这个 */
    protected abstract void verify(PayBillDraft draft);
}
