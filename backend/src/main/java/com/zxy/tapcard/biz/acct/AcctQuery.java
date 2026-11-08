package com.zxy.tapcard.biz.acct;

import com.zxy.tapcard.model.acct.AcctFlow;

import java.time.LocalDateTime;
import java.util.List;

/** 共同接口：页面只认它，不认实现类 */
public interface AcctQuery {

    /** 单个卡户详情 */
    AcctVo mine(Long acctId);

    /** 卡户册（按学号/姓名模糊），学生只能看到自己那一条 */
    List<AcctVo> list(String keyword);

    /** 某卡户在某段时间的流水 */
    List<AcctFlow> flows(Long acctId, LocalDateTime from, LocalDateTime to);
}
