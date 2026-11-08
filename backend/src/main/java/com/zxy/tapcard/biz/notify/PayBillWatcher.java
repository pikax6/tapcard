package com.zxy.tapcard.biz.notify;

public interface PayBillWatcher {

    /** 账单完成后被调用。实现里自己吞异常，不许往外抛 */
    void onPaid(PayBillEvent e);

    /** 观察者名字，写进 notify_log.watcher 用 */
    String name();
}
