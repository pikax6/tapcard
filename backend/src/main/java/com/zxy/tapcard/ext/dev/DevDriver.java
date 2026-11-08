package com.zxy.tapcard.ext.dev;

import com.zxy.tapcard.model.dev.DevChargeCmd;

/** 目标接口：所有厂商报文都要被转成这一种指令 */
public interface DevDriver {

    /** 这台驱动管哪类机具 */
    boolean mine(int devKind);

    /**
     * 原始报文 → 统一指令。
     * devNo 不在厂商报文里，由请求头/设备注册带上来，所以单独传参。
     * 报文不合法一律抛 BizErr，不要返回 null。
     */
    DevChargeCmd adapt(String devNo, String rawMsg);
}
