package com.peterscode.ecommerce_management_system.service.mpesa;

import com.peterscode.ecommerce_management_system.exception.BadRequestException;
import com.peterscode.ecommerce_management_system.model.dto.response.MPesaAuthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Dedicated M-PESA API client for OAuth token management.
 * Extracted from PaymentServiceImpl to fix @Cacheable self-invocation issue.
 * Spring AOP proxies only intercept external calls, not self-invocations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MpesaTokenService {

    private final RestTemplate restTemplate;

    @Value("${mpesa.api.url}")
    private String mpesaApiUrl;

    @Value("${mpesa.consumer.key}")
    private String consumerKey;

    @Value("${mpesa.consumer.secret}")
    private String consumerSecret;

    @Value("${mpesa.oauth.endpoint}")
    private String oauthEndpoint;

    /**
     * Get M-PESA OAuth access token with caching.
     * Token is cached to avoid hitting Safaricom's rate limits on auth endpoint.
     *
     * @return valid M-PESA access token
     * @throws BadRequestException if token retrieval fails
     */
    @Cacheable(value = "mpesaToken", unless = "#result == null")
    public String getAccessToken() {
        try {
            String auth = consumerKey + ":" + consumerSecret;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + encodedAuth);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            String url = mpesaApiUrl + oauthEndpoint;

            log.debug("Requesting M-PESA access token from: {}", url);

            ResponseEntity<MPesaAuthResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, MPesaAuthResponse.class
            );

            if (response.getBody() == null || response.getBody().getAccessToken() == null) {
                throw new BadRequestException("Failed to get M-PESA access token");
            }

            log.debug("M-PESA access token obtained successfully");
            return response.getBody().getAccessToken();

        } catch (RestClientException e) {
            log.error("Failed to get M-PESA access token", e);
            throw new BadRequestException("M-PESA authentication failed: " + e.getMessage());
        }
    }
}

