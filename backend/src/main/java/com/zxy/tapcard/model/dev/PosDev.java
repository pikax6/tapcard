package com.zxy.tapcard.model.dev;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pos_dev")
public class PosDev {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String devNo;
    private String devName;
    private Integer kind;           // 1 食堂POS / 2 超市 / 3 门禁 / 4 水控
    private Long stallId;           // 门禁水控为 null
    private Integer state;          // 1 可用 / 2 停用
    private LocalDateTime lastBeatAt;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
