package com.peterscode.ecommerce_management_system.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MpesaTransactionStatusResponse {
    @JsonProperty("ResponseCode")
    private String responseCode;

    @JsonProperty("ResultCode")
    private String resultCode;

    @JsonProperty("ResultDesc")
    private String resultDesc;
}