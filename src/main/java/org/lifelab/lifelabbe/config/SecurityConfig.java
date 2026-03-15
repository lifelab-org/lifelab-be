package org.lifelab.lifelabbe.config;

import lombok.RequiredArgsConstructor;
import org.lifelab.lifelabbe.security.JwtAuthenticationFilter;
import org.lifelab.lifelabbe.security.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // CSRF 비활성화 (JWT 사용 시 일반적으로 끔)
                .csrf(csrf -> csrf.disable())

                // 세션 사용 안함 (JWT 기반 인증)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 요청 권한 설정
                .authorizeHttpRequests(auth -> auth

                        // 로그인 및 공개 API
                        .requestMatchers(
                                "/",
                                "/health",
                                "/error",
                                "/api/auth/kakao/**",
                                "/success.html"
                        ).permitAll()

                        // 그 외 모든 API는 인증 필요
                        .anyRequest().authenticated()
                )

                // JWT 인증 필터 등록
                .addFilterBefore(
                        new JwtAuthenticationFilter(
                                jwtTokenProvider,
                                jwtProperties.getCookieName()
                        ),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
