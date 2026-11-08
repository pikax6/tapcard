package com.zxy.tapcard.model.acct;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("card_face")
public class CardFace {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String cardNo;
    private Long acctId;
    private Integer state;          // 1 正常 / 2 挂失 / 3 作废
    private LocalDateTime issueAt;
    private LocalDateTime loseAt;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
