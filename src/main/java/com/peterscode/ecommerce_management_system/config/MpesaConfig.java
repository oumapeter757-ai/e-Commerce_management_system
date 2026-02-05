package com.peterscode.ecommerce_management_system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "mpesa")
@Data
public class MpesaConfig {
    private String consumerKey;
    private String consumerSecret;
    private String grantType;
    private String authUrl;
    private String stkPushUrl;
    private String queryUrl;
    private String shortcode;
    private String passkey;
    private String callbackUrl;
}