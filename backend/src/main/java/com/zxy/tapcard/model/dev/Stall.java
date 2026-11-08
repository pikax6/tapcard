package com.zxy.tapcard.model.dev;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("stall")
public class Stall {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String stallName;
    private Integer stallType;      // 1 食堂 / 2 超市 / 3 其他
    private String contact;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
