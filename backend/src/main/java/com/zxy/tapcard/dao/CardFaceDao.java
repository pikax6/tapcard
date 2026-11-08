package com.zxy.tapcard.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zxy.tapcard.model.acct.CardFace;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface CardFaceDao extends BaseMapper<CardFace> {

    /** 刷卡第一跳：按卡号找卡面，拿到 acctId */
    @Select("select * from card_face where card_no = #{cardNo}")
    CardFace byCardNo(@Param("cardNo") String cardNo);

    /** 补卡：作废旧卡 */
    @Update("update card_face set state = 3, lose_at = now() " +
            "where acct_id = #{acctId} and state = 1")
    int killByAcct(@Param("acctId") Long acctId);
}
