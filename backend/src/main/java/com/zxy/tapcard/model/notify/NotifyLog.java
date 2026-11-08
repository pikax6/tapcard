package com.zxy.tapcard.model.notify;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notify_log")
public class NotifyLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer evtType;        // 1 账单完成 / 2 结算完成
    private String billNo;
    private String watcher;
    private Integer ok;             // 1 成功 / 0 失败
    private String errMsg;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
}
