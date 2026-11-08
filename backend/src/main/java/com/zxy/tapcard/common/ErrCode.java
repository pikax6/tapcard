package com.zxy.tapcard.common;

public enum ErrCode {
    // 1xxx 参数与请求
    PARAM_MISS   (1001, "缺少必要参数"),
    AMT_ILLEGAL  (1002, "金额不合法"),
    NOT_FOUND    (1003, "接口不存在"),

    // 2xxx 卡与账户
    FACE_NOT_FOUND(2001, "卡号不存在"),
    FACE_DEAD    (2002, "该卡已作废"),
    FACE_LOST    (2003, "该卡已挂失"),
    ACCT_FROZEN  (2004, "账户已冻结"),
    ACCT_CLOSED  (2005, "账户已注销"),

    // 3xxx 交易
    BAL_LACK     (3001, "余额不足"),
    QUOTA_OVER   (3002, "超出单笔限额"),
    REQ_REPEAT   (3003, "重复请求"),
    BILL_STATE_BAD(3004, "账单当前状态不允许此操作"),
    REBATE_OVER  (3005, "减免金额超过原价"),

    // 4xxx 权限
    PWD_BAD      (4004, "账号或密码错误"),
    NO_RIGHT     (4001, "没有权限访问该数据"),
    NO_ROLE      (4002, "账号角色不正确"),
    TOKEN_BAD    (4003, "登录已失效，请重新登录"),

    // 5xxx 设备与规则
    DEV_OFFLINE  (5001, "机具离线"),
    DEV_STOPPED  (5002, "机具已停用"),
    RULE_NONE    (5003, "未配置可用的减免规则"),

    SYS_ERR      (9999, "系统繁忙，请稍后再试"),
    RISK_HIT(3006, "触发风控规则，交易被拦截");

    private final int code;
    private final String msg;

    ErrCode(int code, String msg) { this.code = code; this.msg = msg; }

    public int code()   { return code; }
    public String msg() { return msg; }
}
