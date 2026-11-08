package com.zxy.tapcard.ext.dev;

import com.zxy.tapcard.common.Amt;
import com.zxy.tapcard.common.BizErr;
import com.zxy.tapcard.common.ErrCode;
import com.zxy.tapcard.model.dev.DevChargeCmd;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** 食堂 POS：自家格式，纯文本逗号分隔 —— 卡号,金额分,时间戳,流水号[,密码] */
@Component
public class CanteenPosDriver implements DevDriver {

    @Override
    public boolean mine(int devKind) { return devKind == 1; }

    @Override
    public DevChargeCmd adapt(String devNo, String rawMsg) {
        String[] p = rawMsg.split(",");
        if (p.length < 4) {
            throw BizErr.of(ErrCode.PARAM_MISS, "食堂POS报文字段不足: " + rawMsg);
        }
        long fen = parseFen(p[1], rawMsg);
        BigDecimal yuan = Amt.toYuan(fen);
        String pwd = p.length >= 5 ? p[4].trim() : null;
        return new DevChargeCmd(p[0].trim(), devNo, yuan, p[2].trim(), p[3].trim(), pwd);
    }

    private long parseFen(String s, String raw) {
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            throw BizErr.of(ErrCode.PARAM_MISS, "金额不是整数分: " + raw);
        }
    }
}
