package org.lifelab.lifelabbe.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.lifelab.lifelabbe.common.ErrorCode;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final String cookieName;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = null;
        if (request.getCookies() != null) {
            token = Arrays.stream(request.getCookies())
                    .filter(c -> cookieName.equals(c.getName()))
                    .map(Cookie::getValue).findFirst().orElse(null);
        }

        if (token != null) {
            try {
                Claims claims = jwtTokenProvider.parseClaims(token);
                var auth = new UsernamePasswordAuthenticationToken(claims.getSubject(), null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {

                // 토큰 만료/무효 시 403 + JSON 반환
                SecurityContextHolder.clearContext();

                response.setStatus(ErrorCode.KAKAO_COOKIE_EXPIRED.status());
                response.setContentType("application/json;charset=UTF-8");

                response.getWriter().write("""
                    {
                      "result": "Fail",
                      "status": 403,
                      "success": null,
                      "error": {
                        "code": "KAKAO_COOKIE_EXPIRED",
                        "message": "로그인이 만료되었습니다. 다시 로그인해주세요."
                      }
                    }
                    """);

                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}