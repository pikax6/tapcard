package com.zxy.tapcard.web.param;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RechargeParam {
    private String cardNo;
    private BigDecimal amtYuan;
    private String reqSn;       // 可为空，服务端会补一个
}
