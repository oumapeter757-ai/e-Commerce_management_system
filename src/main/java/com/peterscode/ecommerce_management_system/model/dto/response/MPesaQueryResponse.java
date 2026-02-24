package com.peterscode.ecommerce_management_system.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MPesaQueryResponse {
    @JsonProperty("ResponseCode")
    private String responseCode;

    @JsonProperty("ResponseDescription")
    private String responseDescription;

    @JsonProperty("MerchantRequestID")
    private String merchantRequestID;

    @JsonProperty("CheckoutRequestID")
    private String checkoutRequestID;

    @JsonProperty("ResultCode")
    private String resultCode;

    @JsonProperty("ResultDesc")
    private String resultDesc;
}