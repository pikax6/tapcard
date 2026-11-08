package com.zxy.tapcard.model.dev;


public enum DevKind {

    CANTEEN_POS(1),
    MARKET(2),
    GATE(3),
    WATER(4);

    private final int code;

    DevKind(int code) { this.code = code; }

    public int code() { return code; }

    public static DevKind of(int code) {
        for (DevKind v : values()) {
            if (v.code == code) return v;
        }
        throw new IllegalArgumentException("未知的类型: " + code);
    }
}
