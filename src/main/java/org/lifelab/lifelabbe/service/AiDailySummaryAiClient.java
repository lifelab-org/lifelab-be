package org.lifelab.lifelabbe.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lifelab.lifelabbe.common.ErrorCode;
import org.lifelab.lifelabbe.common.GlobalException;
import org.lifelab.lifelabbe.dto.ai.DailySummaryDtos.AiDailySummaryAiResponse;
import org.lifelab.lifelabbe.dto.ai.DailySummaryDtos.AiDailySummaryGenerateRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiDailySummaryAiClient {

    private final RestTemplate restTemplate;

    @Value("${ai.base-url:http://localhost:8000}")
    private String baseUrl;

    public AiDailySummaryAiResponse generate(AiDailySummaryGenerateRequest req) {
        URI uri = URI.create(normalizeBaseUrl(baseUrl) + "/ai/daily-summary");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(MediaType.parseMediaTypes("application/json"));

        HttpEntity<AiDailySummaryGenerateRequest> entity = new HttpEntity<>(req, headers);

        try {
            ResponseEntity<AiDailySummaryAiResponse> res =
                    restTemplate.exchange(uri, HttpMethod.POST, entity, AiDailySummaryAiResponse.class);

            if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) {

                log.warn("AI response not OK. status={}, body={}", res.getStatusCode(), res.getBody());
                throw new GlobalException(ErrorCode.AI_400);
            }

            return res.getBody();

        } catch (HttpStatusCodeException e) {
            // FastAPI가 422/400/500 등으로 응답한 경우
            String body = safeBody(e);
            log.warn("AI call failed. status={}, url={}, responseBody={}",
                    e.getStatusCode(), uri, body);


            if (e.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
                throw new GlobalException(ErrorCode.AI_400);
            }
            if (e.getStatusCode().is4xxClientError()) {
                throw new GlobalException(ErrorCode.AI_400);
            }
            throw new GlobalException(ErrorCode.SERVER_500);

        } catch (ResourceAccessException e) {
            // 타임아웃/연결거부 등 네트워크 계열
            log.error("AI server unreachable. url={}, msg={}", uri, e.getMessage());
            throw new GlobalException(ErrorCode.SERVER_500);

        } catch (Exception e) {
            // 직렬화/역직렬화 실패 등
            log.error("AI call unexpected error. url={}, msg={}", uri, e.getMessage(), e);
            throw new GlobalException(ErrorCode.SERVER_500);
        }
    }

    private String normalizeBaseUrl(String url) {
        if (url == null || url.isBlank()) return "http://localhost:8000";

        if (url.endsWith("/")) return url.substring(0, url.length() - 1);
        return url;
    }

    private String safeBody(HttpStatusCodeException e) {
        try {
            String s = e.getResponseBodyAsString();
            return (s == null || s.isBlank()) ? "<empty>" : s;
        } catch (Exception ex) {
            return "<unreadable>";
        }
    }
}