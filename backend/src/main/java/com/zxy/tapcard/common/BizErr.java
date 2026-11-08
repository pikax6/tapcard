package com.zxy.tapcard.common;

public class BizErr extends RuntimeException {

    private final ErrCode ec;
    private final String  detail;   // 只写日志，不回前端

    public BizErr(ErrCode ec) { this(ec, null); }

    public BizErr(ErrCode ec, String detail) {
        super(ec.msg());
        this.ec = ec;
        this.detail = detail;
    }

    public ErrCode ec()     { return ec; }
    public String  detail() { return detail; }

    public static BizErr of(ErrCode ec) { return new BizErr(ec); }
    public static BizErr of(ErrCode ec, String detail) { return new BizErr(ec, detail); }
}
