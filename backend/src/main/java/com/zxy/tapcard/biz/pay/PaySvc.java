package com.zxy.tapcard.biz.pay;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zxy.tapcard.biz.notify.PayBillEvent;
import com.zxy.tapcard.biz.notify.PayBillSignal;
import com.zxy.tapcard.biz.pay.check.PayCheckLine;
import com.zxy.tapcard.biz.pay.draft.PayBillDraft;
import com.zxy.tapcard.biz.pay.maker.BillMakerRegistry;
import com.zxy.tapcard.biz.pay.rebate.RebateBox;
import com.zxy.tapcard.biz.pay.rebate.RebateCalc;
import com.zxy.tapcard.common.Amt;
import com.zxy.tapcard.common.BillNoGen;
import com.zxy.tapcard.common.BizErr;
import com.zxy.tapcard.common.ErrCode;
import com.zxy.tapcard.dao.AcctFlowDao;
import com.zxy.tapcard.dao.CardAcctDao;
import com.zxy.tapcard.dao.CardFaceDao;
import com.zxy.tapcard.dao.PayBillDao;
import com.zxy.tapcard.dao.PosDevDao;
import com.zxy.tapcard.dao.RebateRuleDao;
import com.zxy.tapcard.dao.StallDao;
import com.zxy.tapcard.ext.dev.DevDriver;
import com.zxy.tapcard.model.acct.AcctFlow;
import com.zxy.tapcard.model.acct.CardAcct;
import com.zxy.tapcard.model.acct.CardFace;
import com.zxy.tapcard.model.dev.DevChargeCmd;
import com.zxy.tapcard.model.dev.PosDev;
import com.zxy.tapcard.model.dev.Stall;
import com.zxy.tapcard.model.pay.PayBill;
import com.zxy.tapcard.model.rebate.RebateRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 扣款主流程。整条链只在这里编排，各环节自己不知道自己被串起来。
 *
 * 依赖方向固定：ext（报文） → draft（攒材料） → check（校验并扣款）
 *              → rebate（算减免） → maker（成单） → notify（事后反应）
 */
@Slf4j
@Service
public class PaySvc {

    private final CardFaceDao    cardFaceDao;
    private final CardAcctDao    cardAcctDao;
    private final PosDevDao      posDevDao;
    private final StallDao       stallDao;
    private final PayBillDao     payBillDao;
    private final AcctFlowDao    acctFlowDao;
    private final RebateRuleDao  rebateRuleDao;

    private final List<DevDriver> drivers;
    private final List<RebateCalc> calcs;

    private final PayCheckLine     chain;
    private final BillMakerRegistry registry;
    private final PayBillSignal    signal;

    public PaySvc(CardFaceDao cardFaceDao, CardAcctDao cardAcctDao, PosDevDao posDevDao,
                  StallDao stallDao, PayBillDao payBillDao, AcctFlowDao acctFlowDao,
                  RebateRuleDao rebateRuleDao, List<DevDriver> drivers,
                  List<RebateCalc> calcs, PayCheckLine chain,
                  BillMakerRegistry registry, PayBillSignal signal) {
        this.cardFaceDao = cardFaceDao;
        this.cardAcctDao = cardAcctDao;
        this.posDevDao = posDevDao;
        this.stallDao = stallDao;
        this.payBillDao = payBillDao;
        this.acctFlowDao = acctFlowDao;
        this.rebateRuleDao = rebateRuleDao;
        this.drivers = drivers;
        this.calcs = calcs;
        this.chain = chain;
        this.registry = registry;
        this.signal = signal;
    }

    // ── 消费：机具报文 → 一张待结算的消费单 ──────────────────────────
    @Transactional(rollbackFor = Exception.class)
    public PayBill consume(String devNo, String rawMsg) {
        PosDev dev = posDevDao.byDevNo(devNo);
        if (dev == null) throw BizErr.of(ErrCode.PARAM_MISS, "机具不存在: " + devNo);
        if (dev.getState() != null && dev.getState() == 2) {
            throw BizErr.of(ErrCode.DEV_STOPPED, "机具 " + devNo);
        }

        // ① 适配器：厂商报文 → 统一指令
        DevChargeCmd cmd = driverOf(dev.getKind()).adapt(devNo, rawMsg);
        if (Amt.lt(cmd.amtYuan(), BigDecimal.ZERO) || cmd.amtYuan().signum() == 0) {
            throw BizErr.of(ErrCode.AMT_ILLEGAL, "金额必须大于 0: " + cmd.amtYuan());
        }

        // ② 幂等：同一台机具的同一条流水号只认第一笔
        PayBill old = payBillDao.byReqSn(cmd.nonce());
        if (old != null) {
            log.info("重复请求，直接返回原单 reqSn={} billNo={}", cmd.nonce(), old.getBillNo());
            return old;
        }

        CardFace face = cardFaceDao.byCardNo(cmd.cardNo());
        if (face == null) throw BizErr.of(ErrCode.FACE_NOT_FOUND, "卡号 " + cmd.cardNo());
        CardAcct acct = cardAcctDao.selectById(face.getAcctId());
        if (acct == null) throw BizErr.of(ErrCode.FACE_NOT_FOUND, "卡面无对应卡户");

        Stall stall = dev.getStallId() == null ? null : stallDao.selectById(dev.getStallId());

        // ③ 建造者：把材料攒进草稿
        PayBillDraft draft = new PayBillDraft()
                .withFace(face).withAcct(acct).withDev(dev).withStall(stall)
                .withCmd(cmd).withKind(2).withAmt(cmd.amtYuan())
                .withBillNo(BillNoGen.me().next(devNo));

        // ④ 策略：命中哪条规则由规则表决定，算法本身在 RebateCalc 里
        draft.withRebate(cutOf(draft));

        draft.seal();

        // ⑤ 责任链：挂失/密码/限额/余额/风控 全在这条线上，最后一步真扣钱
        chain.run(draft);

        // ⑥ 工厂：按账单类型交给对应 Maker
        return save(draft, null);
    }

    // ── 充值：不经过机具，单号机具位补 0000 ───────────────────────────
    @Transactional(rollbackFor = Exception.class)
    public PayBill recharge(String cardNo, BigDecimal amtYuan, String reqSn) {
        if (Amt.lt(amtYuan, BigDecimal.ZERO) || amtYuan.signum() == 0) {
            throw BizErr.of(ErrCode.AMT_ILLEGAL, "充值金额必须大于 0");
        }
        String sn = (reqSn == null || reqSn.isBlank()) ? "R" + UUID.randomUUID() : reqSn;

        PayBill old = payBillDao.byReqSn(sn);
        if (old != null) return old;

        CardFace face = cardFaceDao.byCardNo(cardNo);
        if (face == null) throw BizErr.of(ErrCode.FACE_NOT_FOUND, "卡号 " + cardNo);
        CardAcct acct = cardAcctDao.selectById(face.getAcctId());
        if (acct == null) throw BizErr.of(ErrCode.FACE_NOT_FOUND, "卡面无对应卡户");

        BigDecimal amt = Amt.fix2(amtYuan);
        DevChargeCmd cmd = new DevChargeCmd(cardNo, null, amt, "", sn, null);

        PayBillDraft draft = new PayBillDraft()
                .withFace(face).withAcct(acct).withCmd(cmd)
                .withKind(1).withAmt(amt).withRebate(BigDecimal.ZERO)
                .withBillNo(BillNoGen.me().next(null));

        draft.seal();
        chain.run(draft);

        return save(draft, "充值");
    }

    // ── 冲正：不删不改原单，另开一张负向单 ───────────────────────────
    @Transactional(rollbackFor = Exception.class)
    public PayBill reverse(Long oriBillId, String reason) {
        PayBill ori = payBillDao.selectById(oriBillId);
        if (ori == null) throw BizErr.of(ErrCode.NOT_FOUND, "原单不存在: " + oriBillId);
        if (ori.getKind() != 2) throw BizErr.of(ErrCode.BILL_STATE_BAD, "只有消费单能冲正");
        if (ori.getState() == 2) throw BizErr.of(ErrCode.BILL_STATE_BAD, "该单已冲正过");

        Long already = payBillDao.selectCount(
                new LambdaQueryWrapper<PayBill>().eq(PayBill::getOriBillId, oriBillId));
        if (already != null && already > 0) {
            throw BizErr.of(ErrCode.BILL_STATE_BAD, "已存在冲正单，不能重复冲正");
        }

        CardAcct acct = cardAcctDao.selectById(ori.getAcctId());
        if (acct == null) throw BizErr.of(ErrCode.FACE_NOT_FOUND, "原单卡户不存在");

        PosDev dev = ori.getDevNo() == null ? null : posDevDao.byDevNo(ori.getDevNo());
        Stall stall = ori.getStallId() == null ? null : stallDao.selectById(ori.getStallId());

        DevChargeCmd cmd = new DevChargeCmd(null, ori.getDevNo(), ori.getAmt(), "", 
                "V" + UUID.randomUUID(), null);

        PayBillDraft draft = new PayBillDraft()
                .withAcct(acct).withDev(dev).withStall(stall).withCmd(cmd)
                .withKind(3).withAmt(ori.getAmt()).withRebate(Amt.fix2(ori.getAmt().subtract(ori.getPayAmt())))
                .withBillNo(BillNoGen.me().next(ori.getDevNo()))
                .withOriBill(ori.getId())
                .withRemark(reason);

        draft.seal();
        chain.run(draft);

        PayBill rev = save(draft, reason == null ? "冲正" : reason);

        // 原单标记为已冲正：条件更新，防并发重复
        int moved = payBillDao.moveState(ori.getId(), ori.getState(), 2);
        if (moved == 0) {
            throw BizErr.of(ErrCode.BILL_STATE_BAD, "原单状态已变化，冲正失败");
        }
        return rev;
    }

    /** 刷卡即心跳：报文能到就证明设备在线，这里只负责把时间戳刷新 */
    public void beat(String devNo) { posDevDao.beat(devNo); }

    // ── 下面三块是三类单共用的收尾动作 ───────────────────────────────

    private PayBill save(PayBillDraft d, String remark) {
        PayBill bill;
        try {
            bill = registry.pick(d.kind()).make(d);
            if (remark != null) bill.setRemark(remark);
            payBillDao.insert(bill);
        } catch (DuplicateKeyException e) {
            // 唯一键是幂等的最后一道闸：并发下两笔同时通过前置检查时由它兜底
            PayBill old = payBillDao.byReqSn(d.cmd().nonce());
            if (old != null) return old;
            throw BizErr.of(ErrCode.REQ_REPEAT);
        }

        writeFlow(d, bill);
        if (d.dev() != null) posDevDao.beat(d.dev().getDevNo());

        signal.fire(new PayBillEvent(bill.getId(), bill.getBillNo(), bill.getAcctId(),
                bill.getDevNo(), bill.getStallId(), bill.getPayAmt(), bill.getKind()));
        return bill;
    }

    /** 流水是账本的唯一凭据：金额、变动前后余额三者和账单必须自洽 */
    private void writeFlow(PayBillDraft d, PayBill bill) {
        BigDecimal signed = d.kind() == 2 ? d.payAmt().negate() : d.payAmt().abs();
        BigDecimal after  = d.acct().getBal();
        BigDecimal before = Amt.fix2(after.subtract(signed));

        AcctFlow flow = new AcctFlow();
        flow.setAcctId(bill.getAcctId());
        flow.setBillId(bill.getId());
        flow.setKind(bill.getKind());
        flow.setChgAmt(signed);
        flow.setBalBefore(before);
        flow.setBalAfter(after);
        acctFlowDao.insert(flow);
    }

    /** 命中规则就交给对应策略算，没命中就是零 —— 不打折是缺省而不是异常 */
    private BigDecimal cutOf(PayBillDraft d) {
        RebateRule rule = rebateRuleDao.pick(d.dev().getKind(), d.stallId());
        if (rule == null) return BigDecimal.ZERO;

        RebateBox box = new RebateBox();
        box.setRule(rule);
        box.setAmt(d.amt());
        box.setDevKind(d.dev().getKind());
        box.setAcctId(d.acct().getId());
        box.setUsedToday(BigDecimal.ZERO);

        // 每日补贴要知道今天已经补掉多少，其它规则不需要这次查询
        if (rule.getKind() == 4) {
            LocalDate today = LocalDate.now();
            box.setUsedToday(Amt.nz(payBillDao.sumRebateBetween(
                    d.acct().getId(), today.atStartOfDay(), today.plusDays(1).atStartOfDay())));
        }

        for (RebateCalc c : calcs) {
            if (c.mine(rule.getKind())) {
                BigDecimal cut = Amt.fix2(c.calc(box));
                return Amt.min(cut, d.amt());
            }
        }
        throw BizErr.of(ErrCode.RULE_NONE, "规则类型 " + rule.getKind() + " 没有对应算法");
    }

    private DevDriver driverOf(Integer devKind) {
        int k = devKind == null ? -1 : devKind;
        for (DevDriver d : drivers) {
            if (d.mine(k)) return d;
        }
        throw BizErr.of(ErrCode.PARAM_MISS, "机具类型 " + devKind + " 没有对应驱动");
    }
}
