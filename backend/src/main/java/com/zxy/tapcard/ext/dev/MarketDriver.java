package com.zxy.tapcard.ext.dev;

import com.zxy.tapcard.common.Amt;
import com.zxy.tapcard.common.BizErr;
import com.zxy.tapcard.common.ErrCode;
import com.zxy.tapcard.model.dev.DevChargeCmd;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/** 超市收银：走鑫业 SDK，字段名是 card_id / total_fee / trade_time / out_trade_no */
@Component
public class MarketDriver implements DevDriver {

    @Override
    public boolean mine(int devKind) { return devKind == 2; }

    @Override
    public DevChargeCmd adapt(String devNo, String rawMsg) {
        Map<String, String> m = XinYeSdk.readBill(rawMsg);

        String cardNo = m.get("card_id");
        String fee    = m.get("total_fee");
        if (cardNo == null || cardNo.isBlank() || fee == null || fee.isBlank()) {
            throw BizErr.of(ErrCode.PARAM_MISS, "超市报文缺 card_id 或 total_fee: " + rawMsg);
        }

        long fen;
        try {
            fen = Long.parseLong(fee.trim());
        } catch (NumberFormatException e) {
            throw BizErr.of(ErrCode.PARAM_MISS, "total_fee 不是整数分: " + fee);
        }

        BigDecimal yuan = Amt.toYuan(fen);
        String ts    = m.getOrDefault("trade_time", "");
        String nonce = m.getOrDefault("out_trade_no", "");
        String pwd   = m.get("pay_pwd");
        return new DevChargeCmd(cardNo.trim(), devNo, yuan, ts, nonce, pwd);
    }
}