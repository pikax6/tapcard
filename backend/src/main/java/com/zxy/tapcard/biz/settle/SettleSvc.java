package com.zxy.tapcard.biz.settle;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zxy.tapcard.biz.settle.sheet.BaseSheet;
import com.zxy.tapcard.biz.settle.sheet.BudgetSticker;
import com.zxy.tapcard.biz.settle.sheet.PointSticker;
import com.zxy.tapcard.biz.settle.sheet.SettleSheet;
import com.zxy.tapcard.biz.settle.sheet.TraceSticker;
import com.zxy.tapcard.common.Amt;
import com.zxy.tapcard.dao.PayBillDao;
import com.zxy.tapcard.dao.SettleBillDao;
import com.zxy.tapcard.model.pay.PayBill;
import com.zxy.tapcard.model.settle.SettleBill;
import com.zxy.tapcard.model.settle.SettleState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SettleSvc {

    private final PayBillDao payBillDao;
    private final SettleBillDao settleBillDao;

    public SettleSvc(PayBillDao payBillDao, SettleBillDao settleBillDao) {
        this.payBillDao = payBillDao;
        this.settleBillDao = settleBillDao;
    }

    /**
     * 生成某天的结算批次。
     *
     * @param reported 机具上报笔数；传 null 表示"按系统数平账"。
     *                 真实系统这一步是读机具上传的日结文件，作业里由调用方模拟。
     */
    @Transactional(rollbackFor = Exception.class)
    public List<SettleBill> generate(LocalDate day, Map<String, Integer> reported) {
        LocalDateTime from = day.atStartOfDay();
        LocalDateTime to = day.plusDays(1).atStartOfDay();

        List<PayBill> bills = payBillDao.pickUnsettled(from, to);
        Map<String, List<PayBill>> byDev = new LinkedHashMap<>();
        for (PayBill b : bills) {
            if (b.getDevNo() == null || b.getDevNo().isBlank()) continue;
            byDev.computeIfAbsent(b.getDevNo(), k -> new ArrayList<>()).add(b);
        }
        if (byDev.isEmpty()) {
            log.info("{} 没有待结算的消费单", day);
            return List.of();
        }

        List<SettleBill> out = new ArrayList<>();
        for (Map.Entry<String, List<PayBill>> e : byDev.entrySet()) {
            String devNo = e.getKey();
            List<PayBill> list = e.getValue();

            BigDecimal amt = BigDecimal.ZERO;
            BigDecimal rebate = BigDecimal.ZERO;
            for (PayBill b : list) {
                amt = amt.add(Amt.nz(b.getAmt()));
                rebate = rebate.add(Amt.nz(b.getRebateAmt()));
            }
            amt = Amt.fix2(amt);
            rebate = Amt.fix2(rebate);

            int cnt = list.size();
            int devCnt = (reported == null || !reported.containsKey(devNo)) ? cnt : reported.get(devNo);
            boolean diff = devCnt != cnt;

            SettleBill sb = new SettleBill();
            sb.setSettleDate(day);
            sb.setDevNo(devNo);
            sb.setCnt(cnt);
            sb.setDevCnt(devCnt);
            sb.setAmt(amt);
            sb.setRebateAmt(rebate);
            sb.setState(diff ? SettleState.DIFF.code() : SettleState.BALANCED.code());

            // 唯一键是 (settle_date, dev_no)：同一天重复跑只更新，不新增
            SettleBill old = settleBillDao.selectOne(new LambdaQueryWrapper<SettleBill>()
                    .eq(SettleBill::getSettleDate, day)
                    .eq(SettleBill::getDevNo, devNo));
            if (old == null) {
                settleBillDao.insert(sb);
            } else {
                sb.setId(old.getId());
                settleBillDao.updateById(sb);
            }

            // 平账才把账单推进到"已结算"；有差异的留在待结算，等人处理
            if (!diff) {
                for (PayBill b : list) payBillDao.moveState(b.getId(), 0, 1);
            } else {
                log.warn("{} 机具 {} 存在差异：系统 {} 笔 / 机具 {} 笔", day, devNo, cnt, devCnt);
            }

            out.add(sb);
        }
        return out;
    }

    public List<SettleBill> list(LocalDate day) {
        return settleBillDao.byDay(day);
    }

    /** 大屏用：把一张结算单套成装饰链，调用方只认 SettleSheet，不知道有几层 */
    public SettleSheet sheetOf(SettleBill sb, int perBill, BigDecimal budget, String operator) {
        SettleSheet s = new BaseSheet(Amt.nz(BigDecimal.valueOf(sb.getCnt())).intValue(),
                sb.getAmt(), sb.getRebateAmt());
        s = new PointSticker(s, perBill);
        s = new BudgetSticker(s, sb.getRebateAmt(), budget);
        s = new TraceSticker(s, operator + " 于 " + LocalTime.now().withNano(0) + " 查看");
        return s;
    }
}
