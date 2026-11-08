package com.zxy.tapcard.biz.pay.rebate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxy.tapcard.common.BizErr;
import com.zxy.tapcard.common.ErrCode;
import com.zxy.tapcard.model.rebate.RebateRule;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RebateBox {

    private static final ObjectMapper M = new ObjectMapper();

    private RebateRule rule;          // 命中的规则（含 params JSON）
    private BigDecimal amt;           // 本单原价
    private BigDecimal usedToday;     // 今日已减累计（每日补贴要算上限）
    private int        devKind;
    private Long       acctId;

    /** 读整数（分），读不到返回 0 */
    public long longOf(String key) {
        try {
            JsonNode n = M.readTree(rule.getParams());
            return n.path(key).asLong(0L);
        } catch (Exception e) {
            throw BizErr.of(ErrCode.RULE_NONE, "params 解析失败: " + rule.getParams());
        }
    }

    /** 读小数（折扣率等），走文本解析，避开 double 精度 */
    public BigDecimal rateOf(String key) {
        try {
            JsonNode n = M.readTree(rule.getParams());
            return new BigDecimal(n.path(key).asText("0"));
        } catch (Exception e) {
            throw BizErr.of(ErrCode.RULE_NONE, "params 解析失败: " + rule.getParams());
        }
    }
}
