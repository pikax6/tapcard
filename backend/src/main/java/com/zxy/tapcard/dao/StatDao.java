package com.zxy.tapcard.dao;

import com.zxy.tapcard.dao.stat.AcctStatRow;
import com.zxy.tapcard.dao.stat.DayStatRow;
import com.zxy.tapcard.dao.stat.DevStatRow;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/** 只做聚合查询，没有对应实体，所以不继承 BaseMapper */
public interface StatDao {

    @Select("select s.id as stall_id, s.stall_name, count(*) as cnt, " +
            "       ifnull(sum(b.amt),0) as amt, ifnull(sum(b.rebate_amt),0) as rebate_amt, " +
            "       ifnull(sum(b.pay_amt),0) as pay_amt " +
            "from pay_bill b join stall s on s.id = b.stall_id " +
            "where b.kind = 2 and b.state <> 2 " +
            "  and b.created_at >= #{from} and b.created_at < #{to} " +
            "group by s.id, s.stall_name order by pay_amt desc")
    List<AcctStatRow> byStall(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Select("select b.dev_no, d.dev_name, count(*) as cnt, " +
            "       ifnull(sum(b.amt),0) as amt, ifnull(sum(b.rebate_amt),0) as rebate_amt, " +
            "       ifnull(sum(b.pay_amt),0) as pay_amt " +
            "from pay_bill b join pos_dev d on d.dev_no = b.dev_no " +
            "where b.kind = 2 and b.state <> 2 " +
            "  and b.created_at >= #{from} and b.created_at < #{to} " +
            "group by b.dev_no, d.dev_name order by pay_amt desc")
    List<DevStatRow> byDev(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Select("select date(b.created_at) as day, count(*) as cnt, " +
            "       ifnull(sum(b.amt),0) as amt, ifnull(sum(b.rebate_amt),0) as rebate_amt, " +
            "       ifnull(sum(b.pay_amt),0) as pay_amt " +
            "from pay_bill b " +
            "where b.kind = 2 and b.state <> 2 " +
            "  and b.created_at >= #{from} and b.created_at < #{to} " +
            "group by date(b.created_at) order by day")
    List<DayStatRow> byDay(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
