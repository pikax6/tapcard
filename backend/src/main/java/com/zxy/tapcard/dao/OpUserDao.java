package com.zxy.tapcard.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zxy.tapcard.model.auth.OpUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface OpUserDao extends BaseMapper<OpUser> {

    /** 登录：按工号/学号命中 uk_emp_no */
    @Select("select * from op_user where emp_no = #{empNo}")
    OpUser byEmpNo(@Param("empNo") String empNo);
}