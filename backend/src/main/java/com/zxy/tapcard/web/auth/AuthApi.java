package com.zxy.tapcard.web.auth;

import com.zxy.tapcard.auth.TokenKit;
import com.zxy.tapcard.auth.WhoIsIt;
import com.zxy.tapcard.common.BizErr;
import com.zxy.tapcard.common.ErrCode;
import com.zxy.tapcard.common.R;
import com.zxy.tapcard.dao.OpUserDao;
import com.zxy.tapcard.model.auth.OpUser;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthApi {

    private final OpUserDao opUserDao;
    private final TokenKit tokenKit;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthApi(OpUserDao opUserDao, TokenKit tokenKit) {
        this.opUserDao = opUserDao;
        this.tokenKit = tokenKit;
    }

    public record LoginParam(String empNo, String pwd) {}

    public record LoginVo(String token, long uid, String empName, int role,
                          Long acctId, String devNo, Long stallId) {}

    @PostMapping("/login")
    public R<LoginVo> login(@RequestBody LoginParam p) {
        if (p == null || p.empNo() == null || p.empNo().isBlank()
                || p.pwd() == null || p.pwd().isBlank()) {
            throw BizErr.of(ErrCode.PARAM_MISS, "工号与密码不能为空");
        }

        OpUser u = opUserDao.byEmpNo(p.empNo().trim());
        // 账号不存在与密码错误返回同一个码，不给撞库留信息
        if (u == null || !encoder.matches(p.pwd(), u.getPwdHash())) {
            throw BizErr.of(ErrCode.PWD_BAD, "工号 " + p.empNo());
        }
        if (u.getState() != null && u.getState() == 2) {
            throw BizErr.of(ErrCode.NO_ROLE, "账号已停用: " + p.empNo());
        }

        WhoIsIt.CurUser cu = new WhoIsIt.CurUser(
                u.getId(), u.getRole(), u.getAcctId(), u.getDevNo(), u.getStallId());
        String token = tokenKit.sign(cu);

        return R.ok(new LoginVo(token, u.getId(), u.getEmpName(), u.getRole(),
                u.getAcctId(), u.getDevNo(), u.getStallId()));
    }

    @GetMapping("/me")
    public R<LoginVo> me() {
        WhoIsIt.CurUser cu = WhoIsIt.must();
        OpUser u = opUserDao.selectById(cu.uid());
        String name = u == null ? "" : u.getEmpName();
        return R.ok(new LoginVo(null, cu.uid(), name, cu.role(),
                cu.acctId(), cu.devNo(), cu.stallId()));
    }
}
