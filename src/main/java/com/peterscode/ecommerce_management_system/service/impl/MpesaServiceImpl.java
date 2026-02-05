package com.peterscode.ecommerce_management_system.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peterscode.ecommerce_management_system.config.MpesaConfig;
import com.peterscode.ecommerce_management_system.model.dto.response.MPesaStkPushResponse;
import com.peterscode.ecommerce_management_system.model.dto.response.MpesaTransactionStatusResponse;
import com.peterscode.ecommerce_management_system.service.MpesaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MpesaServiceImpl implements MpesaService {

    private final MpesaConfig mpesaConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public String getAccessToken() {
        try {
            String auth = mpesaConfig.getConsumerKey() + ":" + mpesaConfig.getConsumerSecret();
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + encodedAuth);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    mpesaConfig.getAuthUrl() + "?grant_type=client_credentials",
                    HttpMethod.GET, entity, String.class);

            Map result = objectMapper.readValue(response.getBody(), Map.class);
            return (String) result.get("access_token");
        } catch (Exception e) {
            log.error("Error getting access token: {}", e.getMessage());
            throw new RuntimeException("Failed to get M-Pesa access token");
        }
    }

    @Override
    public MPesaStkPushResponse initiateStkPush(String phoneNumber, BigDecimal amount, String accountReference, String description) {
        try {
            String accessToken = getAccessToken();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String password = Base64.getEncoder().encodeToString(
                    (mpesaConfig.getShortcode() + mpesaConfig.getPasskey() + timestamp).getBytes(StandardCharsets.UTF_8)
            );

            Map<String, Object> body = new HashMap<>();
            body.put("BusinessShortCode", mpesaConfig.getShortcode());
            body.put("Password", password);
            body.put("Timestamp", timestamp);
            body.put("TransactionType", "CustomerPayBillOnline");
            body.put("Amount", amount.intValue()); // M-Pesa Sandbox often requires Integer amounts
            body.put("PartyA", phoneNumber);
            body.put("PartyB", mpesaConfig.getShortcode());
            body.put("PhoneNumber", phoneNumber);
            body.put("CallBackURL", mpesaConfig.getCallbackUrl());
            body.put("AccountReference", accountReference);
            body.put("TransactionDesc", description);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<MPesaStkPushResponse> response = restTemplate.exchange(
                    mpesaConfig.getStkPushUrl(), HttpMethod.POST, entity, MPesaStkPushResponse.class);

            return response.getBody();
        } catch (Exception e) {
            log.error("Error initiating STK push: {}", e.getMessage());
            throw new RuntimeException("Failed to initiate M-Pesa payment");
        }
    }

    @Override
    public MpesaTransactionStatusResponse queryTransactionStatus(String checkoutRequestId) {
        try {
            String accessToken = getAccessToken();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String password = Base64.getEncoder().encodeToString(
                    (mpesaConfig.getShortcode() + mpesaConfig.getPasskey() + timestamp).getBytes(StandardCharsets.UTF_8)
            );

            Map<String, Object> body = new HashMap<>();
            body.put("BusinessShortCode", mpesaConfig.getShortcode());
            body.put("Password", password);
            body.put("Timestamp", timestamp);
            body.put("CheckoutRequestID", checkoutRequestId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<MpesaTransactionStatusResponse> response = restTemplate.exchange(
                    mpesaConfig.getQueryUrl(), HttpMethod.POST, entity, MpesaTransactionStatusResponse.class);

            return response.getBody();
        } catch (Exception e) {
            log.error("Error querying transaction: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return null;
        String digits = phoneNumber.replaceAll("[^0-9]", "");
        if (digits.startsWith("0")) return "254" + digits.substring(1);
        if (digits.startsWith("7")) return "254" + digits;
        if (digits.startsWith("+254")) return digits.substring(1);
        return digits;
    }

    @Override
    public String generateTimestamp() {
        return "";
    }

    @Override
    public String generatePassword(String timestamp) {
        return "";
    }
}