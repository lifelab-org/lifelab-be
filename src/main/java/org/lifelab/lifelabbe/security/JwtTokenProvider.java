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

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}