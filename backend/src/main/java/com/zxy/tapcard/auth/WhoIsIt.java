package com.zxy.tapcard.auth;

import com.zxy.tapcard.common.BizErr;
import com.zxy.tapcard.common.ErrCode;

public final class WhoIsIt {

    private static final ThreadLocal<CurUser> HOLDER = new ThreadLocal<>();

    private WhoIsIt() {}

    public record CurUser(long uid, int role, Long acctId, String devNo, Long stallId) {}

    public static void put(CurUser u)  { HOLDER.set(u); }

    /** 可能为 null，用于允许匿名的场景 */
    public static CurUser cur()        { return HOLDER.get(); }

    /** 绝大多数业务用这个：拿不到就当登录失效 */
    public static CurUser must() {
        CurUser u = HOLDER.get();
        if (u == null) throw BizErr.of(ErrCode.TOKEN_BAD);
        return u;
    }

    public static void clear()         { HOLDER.remove(); }

    // 便捷判断，避免业务层到处写 magic number
    public static boolean isStudent()  { CurUser u = cur(); return u != null && u.role() == 1; }
    public static boolean isMerchant() { CurUser u = cur(); return u != null && u.role() == 2; }
    public static boolean isFinance()  { CurUser u = cur(); return u != null && u.role() == 3; }
}
