package com.zxy.tapcard.common;

public record R<T>(int code, String msg, T data) {

    public static <T> R<T> ok(T data)  { return new R<>(0, "ok", data); }
    public static R<Void>  ok()        { return new R<>(0, "ok", null); }

    public static <T> R<T> fail(ErrCode ec) {
        return new R<>(ec.code(), ec.msg(), null);
    }
    public static <T> R<T> fail(ErrCode ec, String msg) {
        return new R<>(ec.code(), msg, null);
    }
}