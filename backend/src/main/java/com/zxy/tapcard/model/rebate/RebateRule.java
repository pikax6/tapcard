package com.zxy.tapcard.model.rebate;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rebate_rule")
public class RebateRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ruleName;
    private Integer kind;           // 1 不打折 / 2 折扣率 / 3 立减 / 4 每日补贴 / 5 积分
    private String params;          // JSON 列，用 String 接
    private Integer prio;
    private Integer devKind;        // null = 不限
    private Long stallId;           // null = 不限
    private LocalDateTime startAt;
    private LocalDateTime endAt;    // null = 长期
    private Integer enabled;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
