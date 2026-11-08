package com.zxy.tapcard.model.dev;

import com.zxy.tapcard.model.pay.BillKind;

public enum StallKind {

    CANTEEN(1),
    MARKET(2),
    OTHER(3);

    private final int code;

    StallKind(int code) { this.code = code; }

    public int code() { return code; }

    public static StallKind of(int code) {
        for (StallKind v : values()) {
            if (v.code == code) return v;
        }
        throw new IllegalArgumentException("未知的档口类型: " + code);
    }
}
