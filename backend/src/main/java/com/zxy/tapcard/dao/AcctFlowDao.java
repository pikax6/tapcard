package com.zxy.tapcard.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zxy.tapcard.model.acct.AcctFlow;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

public interface AcctFlowDao extends BaseMapper<AcctFlow> {

    /** 学生的流水列表（带时间范围），索引 idx_flow_acct(acct_id, created_at) 正好命中 */
    @Select("select * from acct_flow " +
            "where acct_id = #{acctId} and created_at >= #{from} and created_at < #{to} " +
            "order by created_at desc")
    List<AcctFlow> pageByAcct(@Param("acctId") Long acctId,
                              @Param("from") LocalDateTime from,
                              @Param("to") LocalDateTime to);
}
