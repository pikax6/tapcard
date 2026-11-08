package com.zxy.tapcard.model.notify;

import com.zxy.tapcard.model.pay.BillKind;

public enum EvtKind {

    BILL_DONE(1),
    SETTLE_DONE(2);

    private final int code;

    EvtKind(int code) { this.code = code; }

    public int code() { return code; }

    public static EvtKind of(int code) {
        for (EvtKind v : values()) {
            if (v.code == code) return v;
        }
        throw new IllegalArgumentException("未知的类型: " + code);
    }
}
