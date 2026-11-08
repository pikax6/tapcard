package com.zxy.tapcard.model.settle;

import com.zxy.tapcard.model.pay.BillKind;

public enum SettleState{

    WAIT_CHECK(0),
    BALANCED(1),
    DIFF(2);

    private final int code;

    SettleState(int code) { this.code = code; }

    public int code() { return code; }

    public static SettleState of(int code) {
        for (SettleState v : values()) {
            if (v.code == code) return v;
        }
        throw new IllegalArgumentException("未知的结算状态: " + code);
    }
}
