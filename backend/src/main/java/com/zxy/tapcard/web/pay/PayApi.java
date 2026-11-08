package com.zxy.tapcard.web.pay;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zxy.tapcard.auth.WhoIsIt;
import com.zxy.tapcard.biz.pay.PaySvc;
import com.zxy.tapcard.common.BizErr;
import com.zxy.tapcard.common.ErrCode;
import com.zxy.tapcard.common.R;
import com.zxy.tapcard.dao.CardAcctDao;
import com.zxy.tapcard.dao.PayBillDao;
import com.zxy.tapcard.model.acct.CardAcct;
import com.zxy.tapcard.model.pay.PayBill;
import com.zxy.tapcard.web.param.ConsumeParam;
import com.zxy.tapcard.web.param.PageParam;
import com.zxy.tapcard.web.param.RechargeParam;
import com.zxy.tapcard.web.param.ReverseParam;
import com.zxy.tapcard.web.vo.BillVo;
import com.zxy.tapcard.web.vo.PageVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pay")
public class PayApi {

    private final PaySvc paySvc;             // 可能为 null：PaySvc 还没写时先让其余接口可用
    private final PayBillDao payBillDao;
    private final CardAcctDao cardAcctDao;

    public PayApi(PaySvc paySvc, PayBillDao payBillDao, CardAcctDao cardAcctDao) {
        this.paySvc = paySvc;
        this.payBillDao = payBillDao;
        this.cardAcctDao = cardAcctDao;
    }

    @PostMapping("/consume")
    public R<BillVo> consume(@RequestBody ConsumeParam p) {
        if (p == null || p.getDevNo() == null || p.getRaw() == null) {
            throw BizErr.of(ErrCode.PARAM_MISS, "机具号与报文不能为空");
        }
        WhoIsIt.CurUser u = WhoIsIt.must();
        if (u.role() == 1) throw BizErr.of(ErrCode.NO_RIGHT, "学生账号不能发起扣款");
        // 商户只能操作绑定给自己的那台机具，不能拿别人的机具号刷
        if (u.role() == 2 && (u.devNo() == null || !u.devNo().equals(p.getDevNo()))) {
            throw BizErr.of(ErrCode.NO_RIGHT, "该机具不属于当前商户: " + p.getDevNo());
        }
        return R.ok(toVoWithName(paySvc.consume(p.getDevNo(), p.getRaw())));
    }

    @PostMapping("/recharge")
    public R<BillVo> recharge(@RequestBody RechargeParam p) {
        if (p == null || p.getCardNo() == null || p.getAmtYuan() == null) {
            throw BizErr.of(ErrCode.PARAM_MISS, "卡号与金额不能为空");
        }
        mustCashier();
        return R.ok(toVoWithName(paySvc.recharge(p.getCardNo(), p.getAmtYuan(), p.getReqSn())));
    }

    @PostMapping("/reverse")
    public R<BillVo> reverse(@RequestBody ReverseParam p) {
        if (p == null || p.getBillId() == null) {
            throw BizErr.of(ErrCode.PARAM_MISS, "原单号不能为空");
        }
        WhoIsIt.CurUser u = WhoIsIt.must();
        if (u.role() != 3 && u.role() != 9) {
            throw BizErr.of(ErrCode.NO_RIGHT, "只有财务或管理员可以冲正");
        }
        return R.ok(toVoWithName(paySvc.reverse(p.getBillId(), p.getReason())));
    }

    /** 账单簿。学生只看自己名下，商户只看自己机具，财务与管理员看全部 */
    @GetMapping("/bills")
    public R<PageVo<BillVo>> bills(@ModelAttribute PageParam p) {
        WhoIsIt.CurUser u = WhoIsIt.must();

        // count 与 list 必须用同一套条件，否则"共 N 条"和翻出来的页数对不上
        long total = payBillDao.selectCount(scoped(p, u));

        LambdaQueryWrapper<PayBill> w = scoped(p, u);
        w.orderByDesc(PayBill::getId);
        w.last("limit " + p.limit() + " offset " + p.offset());

        List<PayBill> rows = payBillDao.selectList(w);
        Map<Long, String> names = namesOf(rows);
        List<BillVo> out = new ArrayList<>();
        for (PayBill b : rows) {
            BillVo v = toVo(b);
            v.setStuName(names.getOrDefault(b.getAcctId(), ""));
            out.add(v);
        }
        return R.ok(new PageVo<>(out, total, p.pageNo(), p.limit()));
    }

    /**
     * 把"谁能看哪些单"和所有筛选条件收在一处。
     * count 与 list 共用这个方法，避免两个 wrapper 各写一遍、日后改条件只改一处漏一处。
     */
    private LambdaQueryWrapper<PayBill> scoped(PageParam p, WhoIsIt.CurUser u) {
        LambdaQueryWrapper<PayBill> w = new LambdaQueryWrapper<>();

        if (u.role() == 1) {
            if (u.acctId() == null) throw BizErr.of(ErrCode.NO_ROLE, "学生账号未绑定卡户");
            w.eq(PayBill::getAcctId, u.acctId());
        } else if (u.role() == 2) {
            // 商户只看得见自己那台机具的账，不能用收银账号翻全校流水
            if (u.devNo() == null) throw BizErr.of(ErrCode.NO_ROLE, "商户账号未绑定机具");
            w.eq(PayBill::getDevNo, u.devNo());
        }

        if (p.getKeyword() != null && !p.getKeyword().isBlank()) {
            String k = p.getKeyword().trim();
            w.and(q -> q.like(PayBill::getBillNo, k).or().like(PayBill::getDevNo, k));
        }
        if (p.getKind() != null) w.eq(PayBill::getKind, p.getKind());
        if (p.getState() != null) w.eq(PayBill::getState, p.getState());
        if (p.getFrom() != null) w.ge(PayBill::getCreatedAt, p.getFrom().atStartOfDay());
        if (p.getTo() != null) w.lt(PayBill::getCreatedAt, p.getTo().plusDays(1).atStartOfDay());
        return w;
    }

    /** 一次把用到的卡户名查出来，避免逐行查库 */
    private Map<Long, String> namesOf(List<PayBill> rows) {
        Set<Long> ids = rows.stream().map(PayBill::getAcctId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();

        Map<Long, String> m = new HashMap<>();
        for (CardAcct a : cardAcctDao.selectBatchIds(ids)) {
            m.put(a.getId(), a.getStuName());
        }
        return m;
    }

    private BillVo toVo(PayBill b) {
        BillVo v = new BillVo();
        v.setId(b.getId());
        v.setBillNo(b.getBillNo());
        v.setKind(b.getKind());
        v.setKindName(BillVo.kindName(b.getKind()));
        v.setAcctId(b.getAcctId());
        v.setDevNo(b.getDevNo());
        v.setStallId(b.getStallId());
        v.setAmt(b.getAmt());
        v.setRebateAmt(b.getRebateAmt());
        v.setPayAmt(b.getPayAmt());
        v.setState(b.getState());
        v.setStateName(BillVo.stateName(b.getState()));
        v.setReqSn(b.getReqSn());
        v.setOriBillId(b.getOriBillId());
        v.setRemark(b.getRemark());
        v.setCreatedAt(b.getCreatedAt());
        return v;
    }

    /**
     * 单张账单转视图，额外补上卡户姓名。
     * 刷卡/充值/冲正只返回一张单，多一次主键查询可以忽略；
     * 列表页不走这里，它用 namesOf 批量查，避免 N+1。
     */
    private BillVo toVoWithName(PayBill b) {
        BillVo v = toVo(b);
        if (b.getAcctId() != null) {
            CardAcct a = cardAcctDao.selectById(b.getAcctId());
            if (a != null) v.setStuName(a.getStuName());
        }
        return v;
    }

    /** 刷卡与充值：商户、财务、管理员都能做，学生不行 */
    private void mustCashier() {
        WhoIsIt.CurUser u = WhoIsIt.must();
        if (u.role() == 1) throw BizErr.of(ErrCode.NO_RIGHT, "学生账号不能发起扣款");
    }
}
