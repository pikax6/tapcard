package com.zxy.tapcard.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zxy.tapcard.model.pay.PayBill;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PayBillDao extends BaseMapper<PayBill> {

    /** 幂等回查：撞了 uk_req_sn 之后靠它拿原单 */
    @Select("select * from pay_bill where req_sn = #{reqSn}")
    PayBill byReqSn(@Param("reqSn") String reqSn);

    /** 状态流转：只能从旧状态改到新状态，防重复操作 */
    @Update("update pay_bill set state = #{to} where id = #{id} and state = #{from}")
    int moveState(@Param("id") Long id,
                  @Param("from") Integer from,
                  @Param("to") Integer to);

    /** 结算扫描：按日取待结算的消费单。命中 idx_bill_settle(state, created_at) */
    @Select("select * from pay_bill " +
            "where state = 0 and kind = 2 and created_at >= #{from} and created_at < #{to}")
    List<PayBill> pickUnsettled(@Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to);
    /** 风控：该卡最近 N 分钟刷了几笔消费。命中 idx_bill_acct(acct_id, created_at) */
    @Select("select count(*) from pay_bill " +
            "where acct_id = #{acctId} and kind = 2 and created_at >= #{since}")
    int countRecent(@Param("acctId") Long acctId, @Param("since") LocalDateTime since);

    /** 今日已减免合计，每日补贴算剩余额度用 */
    @Select("select ifnull(sum(rebate_amt), 0) from pay_bill " +
            "where acct_id = #{acctId} and kind = 2 and state <> 2 " +
            "  and created_at >= #{from} and created_at < #{to}")
    BigDecimal sumRebateBetween(@Param("acctId") Long acctId,
                                @Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to);
}
