package com.zxy.tapcard.ext.dev;

import com.zxy.tapcard.common.Amt;
import com.zxy.tapcard.common.BizErr;
import com.zxy.tapcard.common.ErrCode;
import com.zxy.tapcard.model.dev.DevChargeCmd;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** 水控：自家格式，分号分隔 —— 卡号;金额元;时间戳;流水号。注意金额单位是元 */
@Component
public class WaterDriver implements DevDriver {

    @Override
    public boolean mine(int devKind) { return devKind == 4; }

    @Override
    public DevChargeCmd adapt(String devNo, String rawMsg) {
        String[] p = rawMsg.split(";");
        if (p.length < 4) {
            throw BizErr.of(ErrCode.PARAM_MISS, "水控报文字段不足: " + rawMsg);
        }
        BigDecimal yuan;
        try {
            yuan = Amt.fix2(new BigDecimal(p[1].trim()));
        } catch (NumberFormatException e) {
            throw BizErr.of(ErrCode.PARAM_MISS, "水控金额不是合法小数: " + p[1]);
        }
        return new DevChargeCmd(p[0].trim(), devNo, yuan, p[2].trim(), p[3].trim(), null);
    }
}
