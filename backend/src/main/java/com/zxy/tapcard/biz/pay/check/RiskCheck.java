package com.zxy.tapcard.biz.pay.check;

import com.zxy.tapcard.biz.pay.draft.PayBillDraft;
import com.zxy.tapcard.common.BizErr;
import com.zxy.tapcard.common.ErrCode;
import com.zxy.tapcard.dao.PayBillDao;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Order(50)
public class RiskCheck extends PayCheck {

    private static final int MAX_PER_MIN = 3;
    private final PayBillDao payBillDao;

    public RiskCheck(PayBillDao payBillDao) { this.payBillDao = payBillDao; }

    @Override
    protected void verify(PayBillDraft d) {
        if (d.kind() != 2) return;

        int c = payBillDao.countRecent(d.acct().getId(), LocalDateTime.now().minusMinutes(1));
        if (c >= MAX_PER_MIN) {
            throw BizErr.of(ErrCode.RISK_HIT, "1 分钟内已刷 " + c + " 笔，请稍后再试");
        }
    }
}
