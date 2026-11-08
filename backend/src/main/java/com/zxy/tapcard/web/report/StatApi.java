package com.zxy.tapcard.web.report;

import com.zxy.tapcard.auth.WhoIsIt;
import com.zxy.tapcard.common.BizErr;
import com.zxy.tapcard.common.ErrCode;
import com.zxy.tapcard.common.R;
import com.zxy.tapcard.dao.StatDao;
import com.zxy.tapcard.dao.stat.AcctStatRow;
import com.zxy.tapcard.dao.stat.DayStatRow;
import com.zxy.tapcard.dao.stat.DevStatRow;
import com.zxy.tapcard.web.vo.StatVo;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/stat")
public class StatApi {

    private final StatDao statDao;

    public StatApi(StatDao statDao) { this.statDao = statDao; }

    @GetMapping("/stall")
    public R<List<StatVo>> byStall(@RequestParam(required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day) {
        mustFinance();
        LocalDate d = day == null ? LocalDate.now() : day;

        List<StatVo> out = new ArrayList<>();
        for (AcctStatRow r : statDao.byStall(d.atStartOfDay(), d.plusDays(1).atStartOfDay())) {
            StatVo v = new StatVo();
            v.setKey(String.valueOf(r.getStallId()));
            v.setName(r.getStallName());
            v.setCnt(r.getCnt());
            v.setAmt(r.getAmt());
            v.setRebateAmt(r.getRebateAmt());
            v.setPayAmt(r.getPayAmt());
            out.add(v);
        }
        return R.ok(out);
    }

    @GetMapping("/dev")
    public R<List<StatVo>> byDev(@RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day) {
        mustFinance();
        LocalDate d = day == null ? LocalDate.now() : day;

        List<StatVo> out = new ArrayList<>();
        for (DevStatRow r : statDao.byDev(d.atStartOfDay(), d.plusDays(1).atStartOfDay())) {
            StatVo v = new StatVo();
            v.setKey(r.getDevNo());
            v.setName(r.getDevName());
            v.setCnt(r.getCnt());
            v.setAmt(r.getAmt());
            v.setRebateAmt(r.getRebateAmt());
            v.setPayAmt(r.getPayAmt());
            out.add(v);
        }
        return R.ok(out);
    }

    @GetMapping("/day")
    public R<List<StatVo>> byDay(@RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                 @RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        mustFinance();
        LocalDate f = from == null ? LocalDate.now().minusDays(6) : from;
        LocalDate t = to == null ? LocalDate.now() : to;

        List<StatVo> out = new ArrayList<>();
        for (DayStatRow r : statDao.byDay(f.atStartOfDay(), t.plusDays(1).atStartOfDay())) {
            StatVo v = new StatVo();
            v.setKey(String.valueOf(r.getDay()));
            v.setName(String.valueOf(r.getDay()));
            v.setCnt(r.getCnt());
            v.setAmt(r.getAmt());
            v.setRebateAmt(r.getRebateAmt());
            v.setPayAmt(r.getPayAmt());
            out.add(v);
        }
        return R.ok(out);
    }

    private void mustFinance() {
        WhoIsIt.CurUser u = WhoIsIt.must();
        if (u.role() != 3 && u.role() != 9) {
            throw BizErr.of(ErrCode.NO_RIGHT, "只有财务或管理员可以看统计");
        }
    }
}
