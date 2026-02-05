package com.peterscode.ecommerce_management_system.service;


import com.peterscode.ecommerce_management_system.model.dto.response.MPesaStkPushResponse;
import com.peterscode.ecommerce_management_system.model.dto.response.MpesaTransactionStatusResponse;

import java.math.BigDecimal;

public interface MpesaService {
    String getAccessToken();
    MPesaStkPushResponse initiateStkPush(String phoneNumber, BigDecimal amount, String accountReference, String description);
    MpesaTransactionStatusResponse queryTransactionStatus(String checkoutRequestId);
    String formatPhoneNumber(String phoneNumber);
    String generateTimestamp();
    String generatePassword(String timestamp);
}