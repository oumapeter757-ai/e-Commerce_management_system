package com.peterscode.ecommerce_management_system.service;


import com.peterscode.ecommerce_management_system.model.dto.common.AddressDTO;
import com.peterscode.ecommerce_management_system.model.dto.request.AddressRequest;

import java.util.List;

public interface AddressService {

    AddressDTO createAddress(AddressRequest request, Long userId);

    AddressDTO getAddressById(Long addressId);

    List<AddressDTO> getUserAddresses(Long userId);

    AddressDTO getDefaultAddress(Long userId);

    AddressDTO updateAddress(Long addressId, AddressRequest request, Long userId);

    AddressDTO setDefaultAddress(Long addressId, Long userId);

    void deleteAddress(Long addressId, Long userId);

    Long getUserAddressCount(Long userId);
}