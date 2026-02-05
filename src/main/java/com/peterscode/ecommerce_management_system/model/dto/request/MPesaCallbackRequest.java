package com.peterscode.ecommerce_management_system.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class MPesaCallbackRequest {

    @JsonProperty("Body")
    private Body body;

    @Data
    public static class Body {
        @JsonProperty("stkCallback")
        private StkCallback stkCallback;
    }

    @Data
    public static class StkCallback {
        @JsonProperty("MerchantRequestID")
        private String merchantRequestID;

        @JsonProperty("CheckoutRequestID")
        private String checkoutRequestID;

        @JsonProperty("ResultCode")
        private Integer resultCode;

        @JsonProperty("ResultDesc")
        private String resultDesc;

        @JsonProperty("CallbackMetadata")
        private CallbackMetadata callbackMetadata;
    }

    @Data
    public static class CallbackMetadata {
        @JsonProperty("Item")
        private List<MetadataItem> item;
    }

    @Data
    public static class MetadataItem {
        @JsonProperty("Name")
        private String name;

        @JsonProperty("Value")
        private Object value; // "Object" is crucial here!
    }
}