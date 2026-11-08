package com.zxy.tapcard.biz.settle.sheet;

import java.math.BigDecimal;

/**
 * 抽象构件，同时兼作装饰基类。
 * 之所以不拆成「Component + Decorator」两个类：本项目只有一条装饰链，
 * 多一个空白抽象类只会让类图变长，讲不出新东西。
 */
public abstract class SettleSheet {

    /** 被装饰者；基础构件为 null */
    protected final SettleSheet inner;

    protected SettleSheet() { this.inner = null; }

    protected SettleSheet(SettleSheet inner) { this.inner = inner; }

    public abstract BigDecimal total();

    public abstract int cnt();

    public abstract String desc();

    protected BigDecimal innerTotal() { return inner == null ? BigDecimal.ZERO : inner.total(); }

    protected int innerCnt() { return inner == null ? 0 : inner.cnt(); }

    protected String innerDesc() { return inner == null ? "" : inner.desc(); }
}
