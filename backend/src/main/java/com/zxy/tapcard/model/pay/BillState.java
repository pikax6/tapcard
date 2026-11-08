package com.zxy.tapcard.model.pay;

public enum BillState {

    WAIT_SETTLE(0),
    SETTLED(1),
    REVERSED(2);

    private final int code;

    BillState(int code) { this.code = code; }

    public int code() { return code; }

    public static BillState of(int code) {
        for (BillState v : values()) {
            if (v.code == code) return v;
        }
        throw new IllegalArgumentException("未知的账单状态: " + code);
    }
}