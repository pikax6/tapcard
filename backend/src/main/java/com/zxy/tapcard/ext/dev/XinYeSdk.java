package com.zxy.tapcard.ext.dev;

import java.util.HashMap;
import java.util.Map;

/**
 * 模拟「鑫业收银」第三方 SDK。
 * 方法签名与线上版本保持一致，作为被适配者：本工程无权修改它。
 */
public final class XinYeSdk {

    private XinYeSdk() {}

    /** SDK 自己的解析入口：返回字段名固定的键值对，字段名不可改 */
    public static Map<String, String> readBill(String payload) {
        Map<String, String> m = new HashMap<>();
        String[] kvs = payload.split("&");
        for (String kv : kvs) {
            int i = kv.indexOf('=');
            if (i > 0) m.put(kv.substring(0, i).trim(), kv.substring(i + 1).trim());
        }
        return m;
    }
}
