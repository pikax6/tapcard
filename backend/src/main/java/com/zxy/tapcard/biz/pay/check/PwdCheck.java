package com.zxy.tapcard.biz.pay.check;

import com.zxy.tapcard.biz.pay.draft.PayBillDraft;
import com.zxy.tapcard.common.Amt;
import com.zxy.tapcard.common.BizErr;
import com.zxy.tapcard.common.ErrCode;
import com.zxy.tapcard.config.SysParamBag;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class PwdCheck extends PayCheck {

    @Override
    protected void verify(PayBillDraft d) {
        if (d.kind() != 2) return;                       // 只有消费要密码

        int devKind = d.dev() == null ? 0 : d.dev().getKind();
        if (devKind == 3 || devKind == 4) return;        // 门禁、水控免密

        if (Amt.toFen(d.payAmt()) <= SysParamBag.me().noPwdFen()) return;   // 小额免密

        String pwd = d.cmd() == null ? null : d.cmd().pwd();
        if (pwd == null || !pwd.matches("\\d{6}")) {
            throw BizErr.of(ErrCode.PARAM_MISS, "该笔需输入 6 位密码");
        }
    }
}
