package org.lifelab.lifelabbe.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.lifelab.lifelabbe.config.JwtProperties;
import org.lifelab.lifelabbe.domain.User;
import org.lifelab.lifelabbe.dto.kakao.KakaoTokenResponse;
import org.lifelab.lifelabbe.dto.kakao.KakaoUserResponse;
import org.lifelab.lifelabbe.security.JwtTokenProvider;
import org.lifelab.lifelabbe.service.KakaoAuthService;
import org.lifelab.lifelabbe.service.UserAuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/kakao")
public class KakaoAuthController {
    private final KakaoAuthService kakaoAuthService;
    private final UserAuthService userAuthService;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    @GetMapping("/login")
    public void login(HttpServletResponse response) throws IOException {
        response.sendRedirect(kakaoAuthService.getAuthorizeUrl());
    }

    @GetMapping("/callback")
    public void callback(@RequestParam("code") String code, HttpServletResponse response) throws IOException {
        KakaoTokenResponse token = kakaoAuthService.getToken(code);
        KakaoUserResponse kakaoUser = kakaoAuthService.getUser(token.getAccessToken());
        User user = userAuthService.findOrCreate(kakaoUser);

        String jwt = jwtTokenProvider.createAccessToken(user.getId(), user.getKakaoId());

        ResponseCookie cookie = ResponseCookie.from(jwtProperties.getCookieName(), jwt)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(Duration.ofMinutes(jwtProperties.getAccessTokenExpMinutes()))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        response.sendRedirect("https://lifelab-nine.vercel.app/");
    }
}