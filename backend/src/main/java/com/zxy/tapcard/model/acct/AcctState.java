package com.zxy.tapcard.model.acct;

public enum AcctState {

    NORMAL(1),
    FROZEN(2),
    CLOSED(3);

    private final int code;

    AcctState(int code) { this.code = code; }

    public int code() { return code; }

    public static AcctState of(int code) {
        for (AcctState v : values()) {
            if (v.code == code) return v;
        }
        throw new IllegalArgumentException("未知的卡户状态: " + code);
    }
}
