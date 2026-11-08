package com.zxy.tapcard.biz.acct;

import com.zxy.tapcard.auth.WhoIsIt;
import com.zxy.tapcard.common.BizErr;
import com.zxy.tapcard.common.ErrCode;
import com.zxy.tapcard.model.acct.AcctFlow;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 代理：真正做越权拦截的地方。页面注入的是 AcctQuery 接口，
 * @Primary 让它成为默认实现，业务代码不会绕过它。
 */
@Primary
@Service
public class AcctGuard implements AcctQuery {

    private final AcctQuerySvc real;

    public AcctGuard(AcctQuerySvc real) { this.real = real; }

    @Override
    public AcctVo mine(Long acctId) {
        check(acctId);
        return real.mine(acctId);
    }

    @Override
    public List<AcctVo> list(String keyword) {
        WhoIsIt.CurUser u = WhoIsIt.must();

        // 学生只可能看到自己那一条：无视关键字，直接按自己的 acctId 查
        if (u.role() == 1) {
            if (u.acctId() == null) throw BizErr.of(ErrCode.NO_ROLE, "学生账号未绑定卡户");
            return List.of(real.mine(u.acctId()));
        }
        // 商户不参与卡户册，避免用收银账号翻别人的账
        if (u.role() == 2) throw BizErr.of(ErrCode.NO_RIGHT, "商户账号无权查看卡户册");

        return real.list(keyword);
    }

    @Override
    public List<AcctFlow> flows(Long acctId, LocalDateTime from, LocalDateTime to) {
        check(acctId);
        return real.flows(acctId, from, to);
    }

    /** 归属校验：学生只能碰自己的 acctId，财务和管理员放行，商户一律挡 */
    private void check(Long acctId) {
        WhoIsIt.CurUser u = WhoIsIt.must();

        if (u.role() == 3 || u.role() == 9) return;              // 财务 / 管理员
        if (u.role() == 1) {
            if (u.acctId() == null || !u.acctId().equals(acctId)) {
                throw BizErr.of(ErrCode.NO_RIGHT, "只能查看自己的卡户");
            }
            return;
        }
        throw BizErr.of(ErrCode.NO_RIGHT, "该角色无权访问卡户数据");
    }
}
