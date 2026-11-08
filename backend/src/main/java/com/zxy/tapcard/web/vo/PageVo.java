package com.zxy.tapcard.web.vo;

import lombok.Data;

import java.util.List;

/**
 * 分页返回体。
 * 只做列表的接口返回它，前端才能渲染出"共 N 条 / 第几页"，
 * 单条查询和写操作仍然直接返回业务对象。
 */
@Data
public class PageVo<T> {

    private List<T> rows;
    private long total;
    private int page;
    private int size;

    public PageVo() {
    }

    public PageVo(List<T> rows, long total, int page, int size) {
        this.rows = rows;
        this.total = total;
        this.page = page;
        this.size = size;
    }
}
