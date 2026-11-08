package com.zxy.tapcard.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zxy.tapcard.model.dev.PosDev;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface PosDevDao extends BaseMapper<PosDev> {

    @Select("select * from pos_dev where dev_no = #{devNo}")
    PosDev byDevNo(@Param("devNo") String devNo);

    /** 心跳：机具每隔一段时间刷一次 */
    @Update("update pos_dev set last_beat_at = now() where dev_no = #{devNo}")
    int beat(@Param("devNo") String devNo);

    /** 在线判断：不存布尔列，实时算 */
    @Select("select count(*) from pos_dev " +
            "where dev_no = #{devNo} and last_beat_at >= date_sub(now(), interval #{sec} second)")
    int onlineCnt(@Param("devNo") String devNo, @Param("sec") int sec);
}
