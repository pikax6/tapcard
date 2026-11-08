package com.zxy.tapcard.biz.notify;

import com.zxy.tapcard.dao.NotifyLogDao;
import com.zxy.tapcard.model.notify.EvtKind;
import com.zxy.tapcard.model.notify.NotifyLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SmsWatcher implements PayBillWatcher {

    private final NotifyLogDao notifyLogDao;

    public SmsWatcher(NotifyLogDao notifyLogDao) { this.notifyLogDao = notifyLogDao; }

    @Override
    public String name() { return "SmsWatcher"; }

    @Override
    public void onPaid(PayBillEvent e) {
        String text = compose(e);
        // 短信网关用控制台模拟：真发短信要接运营商通道，作业范围内不实现
        log.info("【模拟短信】{}", text);

        NotifyLog row = new NotifyLog();
        row.setEvtType(EvtKind.BILL_DONE.code());
        row.setBillNo(e.billNo());
        row.setWatcher(name());
        row.setOk(1);
        notifyLogDao.insert(row);
    }

    /** 按账单类型出不同文案，这是它的变化点 */
    private String compose(PayBillEvent e) {
        String amt = e.payAmt().abs().toPlainString();
        return switch (e.kind()) {
            case 1 -> "【校园卡】充值 " + amt + " 元已到账。";
            case 2 -> "【校园卡】消费 " + amt + " 元，感谢使用。";
            case 3 -> "【校园卡】冲正 " + amt + " 元已退回。";
            default -> "【校园卡】账单 " + e.billNo() + " 已处理。";
        };
    }
}
