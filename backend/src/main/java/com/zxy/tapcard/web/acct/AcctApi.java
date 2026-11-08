package com.zxy.tapcard.web.acct;

import com.zxy.tapcard.auth.WhoIsIt;
import com.zxy.tapcard.biz.acct.AcctOpSvc;
import com.zxy.tapcard.biz.acct.AcctQuery;
import com.zxy.tapcard.biz.acct.AcctVo;
import com.zxy.tapcard.common.BizErr;
import com.zxy.tapcard.common.ErrCode;
import com.zxy.tapcard.common.R;
import com.zxy.tapcard.model.acct.AcctFlow;
import com.zxy.tapcard.web.param.AcctOpenParam;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/acct")
public class AcctApi {

    private final AcctQuery acctQuery;       // 注入接口，拿到的就是代理
    private final AcctOpSvc acctOpSvc;

    public AcctApi(AcctQuery acctQuery, AcctOpSvc acctOpSvc) {
        this.acctQuery = acctQuery;
        this.acctOpSvc = acctOpSvc;
    }

    @GetMapping("/list")
    public R<List<AcctVo>> list(@RequestParam(required = false) String keyword) {
        return R.ok(acctQuery.list(keyword));
    }

    @GetMapping("/{id}")
    public R<AcctVo> detail(@PathVariable Long id) {
        return R.ok(acctQuery.mine(id));
    }

    @GetMapping("/{id}/flows")
    public R<List<AcctFlow>> flows(@PathVariable Long id,
                                   @RequestParam(required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                   @RequestParam(required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate f = from == null ? LocalDate.now().minusDays(30) : from;
        LocalDate t = to == null ? LocalDate.now() : to;
        if (t.isBefore(f)) t = f;
        // to 按"含当天"处理：前端选到 11-05，就要包含 11-05 全天的流水
        return R.ok(acctQuery.flows(id, f.atStartOfDay(), t.plusDays(1).atStartOfDay()));
    }

    // ── 下面四个是写操作，只有财务和管理员能调 ──────────────────────
    @PostMapping("/open")
    public R<Long> open(@RequestBody AcctOpenParam p) {
        mustFinance();
        if (p == null) throw BizErr.of(ErrCode.PARAM_MISS, "参数为空");
        return R.ok(acctOpSvc.open(p.getStuNo(), p.getStuName(), p.getDept()));
    }

    @PostMapping("/lose")
    public R<Void> lose(@RequestParam String cardNo) {
        mustFinance();
        acctOpSvc.lose(cardNo);
        return R.ok();
    }

    @PostMapping("/unlock")
    public R<Void> unlock(@RequestParam String cardNo) {
        mustFinance();
        acctOpSvc.unlock(cardNo);
        return R.ok();
    }

    @PostMapping("/close")
    public R<Void> close(@RequestParam Long acctId) {
        mustFinance();
        acctOpSvc.close(acctId);
        return R.ok();
    }

    /** 权限判断放接口层，服务层保持"谁调都执行"的纯粹语义 */
    private void mustFinance() {
        WhoIsIt.CurUser u = WhoIsIt.must();
        if (u.role() != 3 && u.role() != 9) {
            throw BizErr.of(ErrCode.NO_RIGHT, "只有财务或管理员可以维护卡户");
        }
    }
}
