package com.zxy.tapcard.biz.notify;

import com.zxy.tapcard.common.Amt;
import com.zxy.tapcard.dao.NotifyLogDao;
import com.zxy.tapcard.model.notify.EvtKind;
import com.zxy.tapcard.model.notify.NotifyLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class StatWatcher implements PayBillWatcher {

    private final NotifyLogDao notifyLogDao;

    /** 当日累计，只作大屏缓存用；真值永远以 pay_bill 聚合为准 */
    private final AtomicInteger todayCnt = new AtomicInteger();
    private final AtomicLong    todayFen = new AtomicLong();

    public StatWatcher(NotifyLogDao notifyLogDao) { this.notifyLogDao = notifyLogDao; }

    @Override
    public String name() { return "StatWatcher"; }

    @Override
    public void onPaid(PayBillEvent e) {
        todayCnt.incrementAndGet();
        todayFen.addAndGet(Amt.toFen(e.payAmt()));

        NotifyLog row = new NotifyLog();
        row.setEvtType(EvtKind.BILL_DONE.code());
        row.setBillNo(e.billNo());
        row.setWatcher(name());
        row.setOk(1);
        notifyLogDao.insert(row);

        log.info("当日累计 笔数={} 金额={}", todayCnt.get(), Amt.toYuan(todayFen.get()));
    }

    public int todayCnt() { return todayCnt.get(); }

    public BigDecimal todayAmt() { return Amt.toYuan(todayFen.get()); }
}


