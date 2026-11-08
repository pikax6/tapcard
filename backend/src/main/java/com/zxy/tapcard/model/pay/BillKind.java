package com.zxy.tapcard.model.pay;

public enum BillKind {

    RECHARGE(1),
    CONSUME(2),
    REVERSE(3);

    private final int code;

    BillKind(int code) { this.code = code; }

    public int code() { return code; }

    public static BillKind of(int code) {
        for (BillKind v : values()) {
            if (v.code == code) return v;
        }
        throw new IllegalArgumentException("未知的账单类型: " + code);
    }
}
