package com.peterscode.ecommerce_management_system.model.dto.common;

import com.peterscode.ecommerce_management_system.model.entity.Address;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressDTO {

    private Long id;
    private String fullName;
    private String phoneNumber;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private Address.AddressType addressType;
    private Boolean isDefault;
    private String landmark;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String fullAddress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}