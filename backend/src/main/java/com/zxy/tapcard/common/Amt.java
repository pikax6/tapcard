package com.zxy.tapcard.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Amt {

    private Amt() {}

    /** 收口到两位小数，四舍五入。所有金额落库前都要过它 */
    public static BigDecimal fix2(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v.setScale(2, RoundingMode.HALF_UP);
    }

    /** null 安全 */
    public static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** 元 → 分（用于 JSON 参数，整数分不经过 double） */
    public static long toFen(BigDecimal yuan) {
        return nz(yuan).movePointRight(2).longValueExact();
    }

    /** 分 → 元 */
    public static BigDecimal toYuan(long fen) {
        return BigDecimal.valueOf(fen).movePointLeft(2);
    }

    /** 安全比较：一律走 compareTo */
    public static boolean lt(BigDecimal a, BigDecimal b) { return nz(a).compareTo(nz(b)) < 0; }
    public static boolean ge(BigDecimal a, BigDecimal b) { return nz(a).compareTo(nz(b)) >= 0; }

    /** 取小，用于「减免不能超过原价」 */
    public static BigDecimal min(BigDecimal a, BigDecimal b) {
        return lt(a, b) ? nz(a) : nz(b);
    }
}
