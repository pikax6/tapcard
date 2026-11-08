package com.zxy.tapcard.common;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class BillNoGen {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final Map<String, AtomicInteger> seq = new ConcurrentHashMap<>();

    private BillNoGen() {}

    public static BillNoGen me() { return Holder.IT; }

    private static final class Holder {
        private static final BillNoGen IT = new BillNoGen();
    }

    /**
     * @param devNo 4 位机具号，充值单传 null
     * @return 18 位单号，如 20261104D001000007
     */
    public String next(String devNo) {
        String dev = pad4(devNo);
        String day = LocalDate.now().format(DAY);
        int n = seq.computeIfAbsent(day + "-" + dev, k -> new AtomicInteger(0))
                .incrementAndGet();
        return day + dev + String.format("%06d", n);
    }

    private String pad4(String devNo) {
        if (devNo == null || devNo.isBlank()) {
            return "0000";                       // 充值单没有机具
        }
        String d = devNo.trim();
        if (d.length() > 4) {
            // 宁可报错也不要静默截断：不同机具号截断后可能撞成同一个，单号就重复了
            throw BizErr.of(ErrCode.PARAM_MISS, "机具号超长: " + d);
        }
        if (d.length() == 4) {
            return d;
        }
        return "0".repeat(4 - d.length()) + d;
    }
}
