package com.zxy.tapcard.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zxy.tapcard.model.rebate.RebateRule;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface RebateRuleDao extends BaseMapper<RebateRule> {

    @Select("select * from rebate_rule " +
            "where enabled = 1 " +
            "  and start_at <= now() " +
            "  and (end_at is null or end_at > now()) " +
            "  and (dev_kind is null or dev_kind = #{devKind,jdbcType=TINYINT}) " +
            "  and (stall_id is null or stall_id = #{stallId,jdbcType=BIGINT}) " +
            "order by prio asc, id asc limit 1")
    RebateRule pick(@Param("devKind") Integer devKind,
                    @Param("stallId") Long stallId);
}
