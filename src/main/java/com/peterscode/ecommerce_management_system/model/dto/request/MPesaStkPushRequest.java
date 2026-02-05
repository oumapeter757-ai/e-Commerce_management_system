package com.peterscode.ecommerce_management_system.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * M-PESA STK Push Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MPesaStkPushRequest {

    @JsonProperty("BusinessShortCode")
    private String businessShortCode;

    @JsonProperty("Password")
    private String password;

    @JsonProperty("Timestamp")
    private String timestamp;

    @JsonProperty("TransactionType")
    private String transactionType;  // CustomerPayBillOnline or CustomerBuyGoodsOnline

    @JsonProperty("Amount")
    private Integer amount;  // Must be Integer, not BigDecimal

    @JsonProperty("PartyA")
    private String partyA;  // Customer phone number

    @JsonProperty("PartyB")
    private String partyB;  // Business shortcode

    @JsonProperty("PhoneNumber")
    private String phoneNumber;  // Customer phone number (same as PartyA)

    @JsonProperty("CallBackURL")
    private String callBackURL;

    @JsonProperty("AccountReference")
    private String accountReference;  // Order number or unique reference

    @JsonProperty("TransactionDesc")
    private String transactionDesc;  // Description of the transaction
}