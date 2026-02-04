package org.lifelab.lifelabbe.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.lifelab.lifelabbe.config.JwtProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties props;

    // 0.12.x 버전부터는 SecretKey 타입을 사용해야 에러가 안 납니다.
    private SecretKey getSigningKey() {
        byte[] keyBytes = props.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(Long userId, Long kakaoId) {
        long now = System.currentTimeMillis();
        long expMs = props.getAccessTokenExpMinutes() * 60_000L;

        return Jwts.builder()
                .issuer(props.getIssuer())
                .subject(String.valueOf(userId))
                .claim("kakaoId", kakaoId)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expMs))
                .signWith(getSigningKey(), Jwts.SIG.HS256) // SIG.HS256이 여기서 쓰입니다.
                .compact();
    }

    // 에러 발생 지점 수정: parserBuilder() -> parser()
    public Claims parseClaims(String token) {
        return Jwts.parser()                    // 1. parserBuilder() 대신 parser() 사용
                .verifyWith(getSigningKey())    // 2. setSigningKey() 대신 verifyWith() 사용
                .build()
                .parseSignedClaims(token)       // 3. parseClaimsJws() 대신 parseSignedClaims() 사용
                .getPayload();                  // 4. getBody() 대신 getPayload() 사용
    }
}