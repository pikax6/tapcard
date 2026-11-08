package com.zxy.tapcard.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zxy.tapcard.dao.stat.DevDayRow;
import com.zxy.tapcard.model.settle.SettleBill;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface SettleBillDao extends BaseMapper<SettleBill> {

    /** 按机具按日汇总系统侧：笔数 + 金额 + 减免 */
    @Select("select count(*) as cnt, ifnull(sum(amt),0) as amt, ifnull(sum(rebate_amt),0) as rebate_amt " +
            "from pay_bill " +
            "where state = 0 and kind = 2 and dev_no = #{devNo} " +
            "  and created_at >= #{from} and created_at < #{to}")
    DevDayRow sumByDev(@Param("devNo") String devNo,
                       @Param("from") LocalDateTime from,
                       @Param("to") LocalDateTime to);

    /** 对账大屏：某个日期的全部结算单 */
    @Select("select * from settle_bill where settle_date = #{day} order by dev_no")
    List<SettleBill> byDay(@Param("day") LocalDate day);
}
