package org.lifelab.lifelabbe.service;

import lombok.RequiredArgsConstructor;
import org.lifelab.lifelabbe.common.ErrorCode;
import org.lifelab.lifelabbe.common.GlobalException;
import org.lifelab.lifelabbe.dto.ai.AiCommentGenerateRequest;
import org.lifelab.lifelabbe.dto.ai.AiCommentGenerateResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class AiCommentAiClient {

    private final RestTemplate restTemplate;

    @Value("${ai.base-url}")
    private String aiBaseUrl;

    public String generateComment(AiCommentGenerateRequest req) {
        try {
            String url = aiBaseUrl + "/ai/comments";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<AiCommentGenerateRequest> entity = new HttpEntity<>(req, headers);

            ResponseEntity<AiCommentGenerateResponse> response =
                    restTemplate.exchange(url, HttpMethod.POST, entity, AiCommentGenerateResponse.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || response.getBody().comment() == null) {
                // AI 서버가 이상한 응답을 준 경우
                throw new GlobalException(ErrorCode.SERVER_500);
            }

            return response.getBody().comment();

        } catch (RestClientException e) {
            // AI 서버 다운/네트워크 문제
            throw new GlobalException(ErrorCode.SERVER_500);
        }
    }
}