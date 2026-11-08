package com.zxy.tapcard.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zxy.tapcard.model.acct.CardAcct;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

public interface CardAcctDao extends BaseMapper<CardAcct> {

    /** 扣款：条件更新，影响行数 0 = 余额不足或版本冲突 */
    @Update("update card_acct set bal = bal - #{amt}, ver = ver + 1 " +
            "where id = #{id} and ver = #{ver} and bal >= #{amt}")
    int deduct(@Param("id") Long id,
               @Param("amt") BigDecimal amt,
               @Param("ver") Integer ver);

    /** 加钱：充值 / 冲正回退。同样要版本匹配 */
    @Update("update card_acct set bal = bal + #{amt}, ver = ver + 1 " +
            "where id = #{id} and ver = #{ver}")
    int raise(@Param("id") Long id,
              @Param("amt") BigDecimal amt,
              @Param("ver") Integer ver);

    @Select("select count(*) from card_acct where stu_no = #{stuNo}")
    int countByStuNo(@Param("stuNo") String stuNo);
}
