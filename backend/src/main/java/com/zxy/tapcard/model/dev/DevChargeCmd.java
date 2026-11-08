package com.zxy.tapcard.model.dev;

import java.math.BigDecimal;

/** 统一刷卡指令：厂商报文经适配器转换后的中间产物，不落库 */
public record DevChargeCmd(
        String cardNo,          // 卡号
        String devNo,           // 机具号
        BigDecimal amtYuan,     // 金额（元）
        String ts,              // 机具时间戳
        String nonce,
        String pwd// 机具侧流水号，映射到 reqSn
) {}
