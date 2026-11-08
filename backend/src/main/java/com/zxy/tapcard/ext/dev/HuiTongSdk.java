package com.zxy.tapcard.ext.dev;

/**
 * 模拟「汇通」门禁 SDK。
 * 现场设备按 GBK 发报文，框架层已解成字符串；字段顺序固定，SDK 不做校验。
 */
public final class HuiTongSdk {

    private HuiTongSdk() {}

    /** 按 '|' 切分并原样返回，长度不足或多余都由调用方自己判断 */
    public static String[] readRecord(String payload) {
        return payload.split("\\|", -1);
    }
}