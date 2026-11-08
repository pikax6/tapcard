package com.zxy.tapcard.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zxy.tapcard.model.dev.Stall;

public interface StallDao extends BaseMapper<Stall> {
    // 全是简单条件查询，wrapper 足够，接口留空
}
