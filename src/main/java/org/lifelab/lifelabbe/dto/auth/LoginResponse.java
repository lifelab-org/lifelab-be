package org.lifelab.lifelabbe.dto.auth;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {
    private Long id;
    private Long kakaoId;
    private String nickname;
    private String profileImageUrl;
    private String email;
}
