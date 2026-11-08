package com.zxy.tapcard.biz.pay.check;

import com.zxy.tapcard.biz.pay.draft.PayBillDraft;
import com.zxy.tapcard.common.Amt;
import com.zxy.tapcard.common.BizErr;
import com.zxy.tapcard.common.ErrCode;
import com.zxy.tapcard.dao.CardAcctDao;
import com.zxy.tapcard.model.acct.CardAcct;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Order(60)
public class DeductCheck extends PayCheck {

    private final CardAcctDao cardAcctDao;

    public DeductCheck(CardAcctDao cardAcctDao) { this.cardAcctDao = cardAcctDao; }

    @Override
    protected void verify(PayBillDraft d) {
        CardAcct a = d.acct();
        int rows;

        if (d.kind() == 1 || d.kind() == 3) {
            // 充值、冲正：加钱
            rows = cardAcctDao.raise(a.getId(), d.payAmt().abs(), a.getVer());
        } else {
            // 消费：扣钱
            rows = cardAcctDao.deduct(a.getId(), d.payAmt(), a.getVer());
        }

        if (rows == 0) {
            // 影响行数 0 有两种可能，重查一次才能说清是哪一种
            CardAcct now = cardAcctDao.selectById(a.getId());
            if (now == null) throw BizErr.of(ErrCode.FACE_NOT_FOUND, "卡户不存在");
            if (d.kind() == 2 && Amt.lt(now.getBal(), d.payAmt())) {
                throw BizErr.of(ErrCode.BAL_LACK,
                        "余额 " + now.getBal() + " 元，本笔需 " + d.payAmt() + " 元");
            }
            throw BizErr.of(ErrCode.SYS_ERR, "扣款版本冲突，请重试");
        }

        // 把新余额和新版本写回内存，供流水与后续节点使用
        BigDecimal after = d.kind() == 2
                ? a.getBal().subtract(d.payAmt())
                : a.getBal().add(d.payAmt().abs());
        a.setBal(Amt.fix2(after));
        a.setVer(a.getVer() + 1);
    }
}
