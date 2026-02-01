package org.lifelab.lifelabbe.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.lifelab.lifelabbe.config.JwtProperties;
import org.lifelab.lifelabbe.domain.User;
import org.lifelab.lifelabbe.dto.kakao.KakaoTokenResponse;
import org.lifelab.lifelabbe.dto.kakao.KakaoUserResponse;
import org.lifelab.lifelabbe.security.JwtProvider;
import org.lifelab.lifelabbe.service.KakaoAuthService;
import org.lifelab.lifelabbe.service.UserAuthService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/kakao")
public class KakaoAuthController {
    private final KakaoAuthService kakaoAuthService;
    private final UserAuthService userAuthService;
    private final JwtProvider jwtProvider;
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

        String jwt = jwtProvider.createAccessToken(user.getId(), user.getKakaoId());

        Cookie cookie = new Cookie(jwtProperties.getCookieName(), jwt);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) (jwtProperties.getAccessTokenExpMinutes() * 60));

        response.addCookie(cookie);
        response.sendRedirect("/success.html");
    }
}