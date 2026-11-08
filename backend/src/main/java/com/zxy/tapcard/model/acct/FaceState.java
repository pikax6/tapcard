package com.zxy.tapcard.model.acct;


import com.zxy.tapcard.model.pay.BillKind;

public enum FaceState{

    NORMAL(1),
    LOST(2),
    DEAD(3);

    private final int code;

    FaceState(int code) { this.code = code; }

    public int code() { return code; }

    public static FaceState of(int code) {
        for (FaceState v : values()) {
            if (v.code == code) return v;
        }
        throw new IllegalArgumentException("未知的卡面状态: " + code);
    }
}
