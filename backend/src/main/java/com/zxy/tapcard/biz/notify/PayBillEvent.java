package com.zxy.tapcard.biz.notify;

import java.math.BigDecimal;

public record PayBillEvent(
        Long   billId,
        String billNo,
        Long   acctId,
        String devNo,
        Long   stallId,
        BigDecimal payAmt,
        int    kind
) {}
