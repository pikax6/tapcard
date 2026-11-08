package com.zxy.tapcard.model.rebate;

import com.zxy.tapcard.model.pay.BillKind;

public enum RebateKind {

    NO_CUT(1),
    RATE_CUT(2),
    MINUS_CUT(3),
    DAILY_SUBSIDY(4),
    POINT_CUT(5);

    private final int code;

    RebateKind(int code) { this.code = code; }

    public int code() { return code; }

    public static RebateKind of(int code) {
        for (RebateKind v : values()) {
            if (v.code == code) return v;
        }
        throw new IllegalArgumentException("未知的减免类型: " + code);
    }
}
