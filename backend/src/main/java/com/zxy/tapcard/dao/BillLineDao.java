package com.zxy.tapcard.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zxy.tapcard.model.pay.BillLine;

public interface BillLineDao extends BaseMapper<BillLine> {
    // 只按 billId 查，用条件构造器即可，不用写 SQL：
    //   List<BillLine> lines = billLineDao.selectList(
    //       new LambdaQueryWrapper<BillLine>().eq(BillLine::getBillId, billId));
}


