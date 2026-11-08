package com.zxy.tapcard.web.settle;

import com.zxy.tapcard.auth.WhoIsIt;
import com.zxy.tapcard.biz.settle.SettleSvc;
import com.zxy.tapcard.common.BizErr;
import com.zxy.tapcard.common.ErrCode;
import com.zxy.tapcard.common.R;
import com.zxy.tapcard.dao.OpUserDao;
import com.zxy.tapcard.model.auth.OpUser;
import com.zxy.tapcard.model.settle.SettleBill;
import com.zxy.tapcard.model.settle.SettleState;
import com.zxy.tapcard.web.vo.SettleVo;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settle")
public class SettleApi {

    /** 大屏上的预算额度，演示用固定值；真要做成可配置就放 SysParamBag */
    private static final BigDecimal DEMO_BUDGET = new BigDecimal("100.00");

    private final SettleSvc settleSvc;
    private final OpUserDao opUserDao;

    public SettleApi(SettleSvc settleSvc, OpUserDao opUserDao) {
        this.settleSvc = settleSvc;
        this.opUserDao = opUserDao;
    }

    public record GenParam(LocalDate day, Map<String, Integer> reported) {}

    @PostMapping("/generate")
    public R<List<SettleVo>> generate(@RequestBody GenParam p) {
        mustFinance();
        LocalDate day = (p == null || p.day() == null) ? LocalDate.now() : p.day();
        Map<String, Integer> reported = p == null ? null : p.reported();

        List<SettleVo> out = new ArrayList<>();
        String who = whoLabel();
        for (SettleBill sb : settleSvc.generate(day, reported)) out.add(toVo(sb, who));
        return R.ok(out);
    }

    @GetMapping("/list")
    public R<List<SettleVo>> list(@RequestParam(required = false)
                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day) {
        mustFinance();
        LocalDate d = day == null ? LocalDate.now() : day;

        String who = whoLabel();
        List<SettleVo> out = new ArrayList<>();
        for (SettleBill sb : settleSvc.list(d)) out.add(toVo(sb, who));
        return R.ok(out);
    }

    private SettleVo toVo(SettleBill sb, String who) {
        SettleVo v = new SettleVo();
        v.setId(sb.getId());
        v.setSettleDate(sb.getSettleDate());
        v.setDevNo(sb.getDevNo());
        v.setCnt(sb.getCnt());
        v.setDevCnt(sb.getDevCnt());
        v.setDiff((sb.getDevCnt() == null ? 0 : sb.getDevCnt()) - (sb.getCnt() == null ? 0 : sb.getCnt()));
        v.setAmt(sb.getAmt());
        v.setRebateAmt(sb.getRebateAmt());
        v.setPayAmt(sb.getAmt() == null ? BigDecimal.ZERO : sb.getAmt()
                .subtract(sb.getRebateAmt() == null ? BigDecimal.ZERO : sb.getRebateAmt()));
        v.setState(sb.getState());
        v.setStateName(stateName(sb.getState()));

        // 大屏描述直接来自装饰链，页面不用自己拼字符串
        v.setDesc(settleSvc.sheetOf(sb, 1, DEMO_BUDGET, who).desc());
        return v;
    }

    /** "财务 赵敏" 这种写法：留痕里出现姓名比出现工号更像真实系统 */
    private String whoLabel() {
        WhoIsIt.CurUser cu = WhoIsIt.cur();
        if (cu == null) return "系统";
        OpUser u = opUserDao.selectById(cu.uid());
        String name = (u == null || u.getEmpName() == null || u.getEmpName().isBlank())
                ? "工号 " + cu.uid() : u.getEmpName();
        return roleName(cu.role()) + " " + name;
    }

    private String roleName(int role) {
        return switch (role) {
            case 1 -> "学生";
            case 2 -> "商户";
            case 3 -> "财务";
            case 9 -> "管理员";
            default -> "用户";
        };
    }

    private String stateName(Integer s) {
        if (s == null) return "";
        if (s == SettleState.WAIT_CHECK.code()) return "待核对";
        if (s == SettleState.BALANCED.code()) return "已平账";
        if (s == SettleState.DIFF.code()) return "有差异";
        return "未知";
    }

    private void mustFinance() {
        WhoIsIt.CurUser u = WhoIsIt.must();
        if (u.role() != 3 && u.role() != 9) {
            throw BizErr.of(ErrCode.NO_RIGHT, "只有财务或管理员可以操作结算");
        }
    }
}
