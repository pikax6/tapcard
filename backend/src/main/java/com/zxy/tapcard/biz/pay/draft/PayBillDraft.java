package com.zxy.tapcard.biz.pay.draft;

import com.zxy.tapcard.common.Amt;
import com.zxy.tapcard.common.BizErr;
import com.zxy.tapcard.common.ErrCode;
import com.zxy.tapcard.model.acct.CardAcct;
import com.zxy.tapcard.model.acct.CardFace;
import com.zxy.tapcard.model.dev.DevChargeCmd;
import com.zxy.tapcard.model.dev.PosDev;
import com.zxy.tapcard.model.dev.Stall;
import com.zxy.tapcard.model.pay.BillLine;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PayBillDraft {
    // ── 上下文：校验要用的材料 ──────────────────────────
    private CardFace face;
    private CardAcct acct;
    private PosDev dev;
    private Stall stall;
    private DevChargeCmd cmd;

    // ── 正在攒的账单字段 ────────────────────────────────
    private int        kind;
    private String     billNo;
    private BigDecimal amt      = BigDecimal.ZERO;   // 原价
    private BigDecimal rebateAmt = BigDecimal.ZERO;  // 减免
    private BigDecimal payAmt    = BigDecimal.ZERO;  // 实付
    private Integer    devKind;
    private Long       stallId;
    private Long       oriBillId;
    private String     remark;
    private List<BillLine> lines = new ArrayList<>();

    // ── 链式填单：每个方法返回 this ─────────────────────
    public PayBillDraft withFace(CardFace f)  { this.face = f; return this; }
    public PayBillDraft withAcct(CardAcct a)  { this.acct = a; return this; }
    public PayBillDraft withDev(PosDev d)     { this.dev = d; return this; }
    public PayBillDraft withStall(Stall s)    { this.stall = s;
        this.stallId=(s==null ? null:s.getId());
        return this; }
    public PayBillDraft withBillNo(String no)       { this.billNo = no; return this; }
    public PayBillDraft withOriBill(Long oriBillId) { this.oriBillId = oriBillId; return this; }
    public PayBillDraft withRemark(String remark)   { this.remark = remark; return this; }

    public String billNo()    { return billNo; }
    public Long   oriBillId() { return oriBillId; }
    public Long   stallId()   { return stallId; }
    public Stall  stall()     { return stall; }
    public PayBillDraft withCmd(DevChargeCmd c){ this.cmd = c; return this; }
    public PayBillDraft withKind(int k)       { this.kind = k; return this; }
    public PayBillDraft withAmt(BigDecimal a) { this.amt = Amt.fix2(a); return this; }
    public PayBillDraft withRebate(BigDecimal r){ this.rebateAmt = Amt.fix2(r); return this; }
    public PayBillDraft withLine(String item, int qty, BigDecimal a, BigDecimal r) {
        BillLine l = new BillLine();
        l.setItemName(item);
        l.setQty(qty);
        l.setAmt(Amt.fix2(a));
        l.setRebateAmt(Amt.fix2(r));
        lines.add(l);
        return this;
    }

    public List<BillLine> lines() { return lines; }

    // ── 收口：先补齐派生字段，再自检 ────────────────────
    public void seal() {
        if (kind == 0 || acct == null || amt.signum() <= 0) {
            throw BizErr.of(ErrCode.PARAM_MISS, "草稿不完整");
        }
        this.payAmt = Amt.fix2(amt.subtract(rebateAmt));
        if (payAmt.signum() < 0) throw BizErr.of(ErrCode.REBATE_OVER);
    }

    // ── 只读访问器，给链上的节点用 ──────────────────────
    public CardAcct acct()  { return acct; }
    public CardFace face()  { return face; }
    public PosDev   dev()   { return dev; }
    public BigDecimal amt() { return amt; }
    public BigDecimal payAmt() { return payAmt; }
    public void addRebate(BigDecimal r) { this.rebateAmt = Amt.fix2(this.rebateAmt.add(r)); }

    public int kind() { return kind; }
    public DevChargeCmd cmd() { return cmd; }
    public BigDecimal rebateAmt() { return rebateAmt; }
}
