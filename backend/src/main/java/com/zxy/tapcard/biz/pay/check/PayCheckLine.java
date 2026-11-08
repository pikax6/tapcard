package com.zxy.tapcard.biz.pay.check;

import com.zxy.tapcard.biz.pay.draft.PayBillDraft;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PayCheckLine {

    private final PayCheck head;

    public PayCheckLine(List<PayCheck> checks) {
        // 按 @Order 排序后再串起来
        List<PayCheck> sorted = new ArrayList<>(checks);
        AnnotationAwareOrderComparator.sort(sorted);

        for (int i = 0; i < sorted.size() - 1; i++) {
            sorted.get(i).next(sorted.get(i + 1));
        }
        this.head = sorted.isEmpty() ? null : sorted.get(0);
    }

    public void run(PayBillDraft draft) {
        if (head != null) head.run(draft);
    }
}
