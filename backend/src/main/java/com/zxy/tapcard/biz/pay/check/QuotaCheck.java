package com.zxy.tapcard.biz.pay.check;

import com.zxy.tapcard.biz.pay.draft.PayBillDraft;
import com.zxy.tapcard.common.Amt;
import com.zxy.tapcard.common.BizErr;
import com.zxy.tapcard.common.ErrCode;
import com.zxy.tapcard.config.SysParamBag;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Order(30)
public class QuotaCheck extends PayCheck {

    @Override
    protected void verify(PayBillDraft d) {
        if (d.kind() != 2) return;                       // 充值不受单笔限额约束

        int devKind = d.dev() == null ? 0 : d.dev().getKind();
        if (devKind == 3 || devKind == 4) return;        // 门禁水控金额本来就小

        BigDecimal cap = Amt.toYuan(SysParamBag.me().singleQuotaFen());
        if (Amt.lt(cap, d.payAmt())) {
            throw BizErr.of(ErrCode.QUOTA_OVER,
                    "单笔上限 " + cap + " 元，本笔 " + d.payAmt() + " 元");
        }
    }
}
