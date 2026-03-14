package org.lifelab.lifelabbe.service;

import lombok.RequiredArgsConstructor;
import org.lifelab.lifelabbe.domain.User;
import org.lifelab.lifelabbe.dto.kakao.KakaoUserResponse;
import org.lifelab.lifelabbe.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAuthService {

    private final UserRepository userRepository;

    @Transactional
    public User findOrCreate(KakaoUserResponse kakaoUser) {
        Long kakaoId = kakaoUser.getId();

        String nickname = null;
        String profileImageUrl = null;
        String email = null;

        if (kakaoUser.getKakaoAccount() != null) {
            email = kakaoUser.getKakaoAccount().getEmail();
            if (kakaoUser.getKakaoAccount().getProfile() != null) {
                nickname = kakaoUser.getKakaoAccount().getProfile().getNickname();
                profileImageUrl = kakaoUser.getKakaoAccount().getProfile().getProfileImageUrl();
            }
        }

        User user = userRepository.findByKakaoId(kakaoId).orElse(null);

        if (user == null) {
            user = userRepository.save(
                    User.builder()
                            .kakaoId(kakaoId)
                            .nickname(nickname)
                            .profileImageUrl(profileImageUrl)
                            .email(email)
                            .build()
            );
        } else {
            user.updateProfile(nickname, profileImageUrl, email);
        }

        return user;
    }
}
