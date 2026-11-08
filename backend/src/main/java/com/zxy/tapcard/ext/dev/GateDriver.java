package com.zxy.tapcard.ext.dev;

import com.zxy.tapcard.common.Amt;
import com.zxy.tapcard.common.BizErr;
import com.zxy.tapcard.common.ErrCode;
import com.zxy.tapcard.model.dev.DevChargeCmd;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** 门禁：走汇通 SDK，字段顺序 IC_NO|AMT_YUAN|TS|SEQ，金额单位元 */
@Component
public class GateDriver implements DevDriver {

    @Override
    public boolean mine(int devKind) { return devKind == 3; }

    @Override
    public DevChargeCmd adapt(String devNo, String rawMsg) {
        String[] p = HuiTongSdk.readRecord(rawMsg);
        if (p.length < 4) {
            throw BizErr.of(ErrCode.PARAM_MISS, "门禁报文段数不足: " + rawMsg);
        }
        BigDecimal yuan;
        try {
            yuan = Amt.fix2(new BigDecimal(p[1].trim()));
        } catch (NumberFormatException e) {
            throw BizErr.of(ErrCode.PARAM_MISS, "门禁金额不是合法小数: " + p[1]);
        }
        return new DevChargeCmd(p[0].trim(), devNo, yuan, p[2].trim(), p[3].trim(), null);
    }
}
