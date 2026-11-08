package com.zxy.tapcard.biz.notify;

import com.zxy.tapcard.dao.NotifyLogDao;
import com.zxy.tapcard.model.notify.EvtKind;
import com.zxy.tapcard.model.notify.NotifyLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SettleWatcher implements PayBillWatcher {

    private final NotifyLogDao notifyLogDao;

    public SettleWatcher(NotifyLogDao notifyLogDao) { this.notifyLogDao = notifyLogDao; }

    @Override
    public String name() { return "SettleWatcher"; }

    @Override
    public void onPaid(PayBillEvent e) {
        // 只有消费单进日结对账：充值不走机具、冲正是对原单的修正，都不参与对账
        if (e.kind() != 2) {
            log.debug("非消费单，跳过对账登记 billNo={} kind={}", e.billNo(), e.kind());
            return;
        }
        NotifyLog row = new NotifyLog();
        row.setEvtType(EvtKind.BILL_DONE.code());
        row.setBillNo(e.billNo());
        row.setWatcher(name());
        row.setOk(1);
        notifyLogDao.insert(row);
        log.info("账单 {} 已登记待对账，实付 {}", e.billNo(), e.payAmt());
    }
}

