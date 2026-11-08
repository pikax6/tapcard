package com.zxy.tapcard.auth;

import com.zxy.tapcard.common.BizErr;
import com.zxy.tapcard.common.ErrCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginWall implements HandlerInterceptor {

    private final TokenKit tokenKit;

    public LoginWall(TokenKit tokenKit) { this.tokenKit = tokenKit; }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) {
        if (!(handler instanceof HandlerMethod)) return true;   // 静态资源放行

        String h = req.getHeader("Authorization");
        if (h == null || !h.startsWith("Bearer ")) {
            throw BizErr.of(ErrCode.TOKEN_BAD, "missing header");
        }

        String token = h.substring(7).trim();
        WhoIsIt.put(tokenKit.parse(token));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse resp,
                                Object handler, Exception ex) {
        WhoIsIt.clear();        // 必须清，否则线程复用会串号
    }
}
