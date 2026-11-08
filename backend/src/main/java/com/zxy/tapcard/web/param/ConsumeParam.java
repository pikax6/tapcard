package com.zxy.tapcard.web.param;

import lombok.Data;

/**
 * 消费入参：前端扮演机具，直接发厂商报文原文。
 * 报文格式由机具类型决定，格式转换在 ext 层做，接口不做任何格式假设。
 */
@Data
public class ConsumeParam {
    private String devNo;
    private String raw;
}
