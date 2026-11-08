package com.zxy.tapcard.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zxy.tapcard.model.notify.NotifyLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface NotifyLogDao extends BaseMapper<NotifyLog> {
    // 观察者只往这张表 insert，BaseMapper 够了

    @Select("select * from notify_log where bill_no = #{billNo} order by id")
    List<NotifyLog> byBillNo(@Param("billNo") String billNo);
}
