package com.zxy.tapcard.biz.notify;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class PayBillSignal {

    private final List<PayBillWatcher> watchers;

    public PayBillSignal(List<PayBillWatcher> watchers) { this.watchers = watchers; }

    public void fire(PayBillEvent e) {
        for (PayBillWatcher w : watchers) {
            try {
                w.onPaid(e);
            } catch (Exception ex) {
                // 一个观察者崩了，不能影响其他观察者，更不能影响主流程
                log.warn("观察者 {} 处理失败 billNo={}", w.name(), e.billNo(), ex);
            }
        }
    }
}
