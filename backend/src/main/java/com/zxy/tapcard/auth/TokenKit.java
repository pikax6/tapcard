package com.zxy.tapcard.auth;

import com.zxy.tapcard.common.BizErr;
import com.zxy.tapcard.common.ErrCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class TokenKit {

    private final SecretKey key;
    private final long expireMillis;

    public TokenKit(@Value("${tapcard.jwt.secret}") String secret,
                    @Value("${tapcard.jwt.expire-minutes}") long minutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMillis = minutes * 60_000L;
    }

    public String sign(WhoIsIt.CurUser u) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(u.uid()))
                .claim("role",    u.role())
                .claim("acctId",  u.acctId())
                .claim("devNo",   u.devNo())
                .claim("stallId", u.stallId())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMillis))
                .signWith(key)              // 0.12 写法
                .compact();
    }

    public WhoIsIt.CurUser parse(String token) {
        try {
            Claims c = Jwts.parser()
                    .verifyWith(key)        // 0.11 是 setSigningKey
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return new WhoIsIt.CurUser(
                    Long.parseLong(c.getSubject()),
                    c.get("role", Integer.class),
                    c.get("acctId",  Long.class),
                    c.get("devNo",   String.class),
                    c.get("stallId", Long.class));
        } catch (Exception e) {
            throw BizErr.of(ErrCode.TOKEN_BAD, e.getClass().getSimpleName());
        }
    }
}
