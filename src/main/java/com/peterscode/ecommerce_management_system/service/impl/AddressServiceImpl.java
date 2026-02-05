package com.peterscode.ecommerce_management_system.service.impl;


import com.peterscode.ecommerce_management_system.exception.BadRequestException;
import com.peterscode.ecommerce_management_system.exception.ResourceNotFoundException;
import com.peterscode.ecommerce_management_system.mapper.AddressMapper;
import com.peterscode.ecommerce_management_system.model.dto.common.AddressDTO;
import com.peterscode.ecommerce_management_system.model.dto.request.AddressRequest;
import com.peterscode.ecommerce_management_system.model.entity.Address;
import com.peterscode.ecommerce_management_system.model.entity.User;
import com.peterscode.ecommerce_management_system.repository.AddressRepository;
import com.peterscode.ecommerce_management_system.repository.UserRepository;
import com.peterscode.ecommerce_management_system.service.AddressService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final AddressMapper addressMapper;

    @Override
    @Transactional
    public AddressDTO createAddress(AddressRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Address address = addressMapper.toEntity(request);
        address.setUser(user);

        // Smart Logic: If it's the FIRST address, force it to be Default
        long addressCount = addressRepository.countByUserId(userId);
        if (addressCount == 0) {
            address.setIsDefault(true);
        } else if (Boolean.TRUE.equals(request.getIsDefault())) {
            // If user explicitly asked for default, clear previous defaults
            addressRepository.clearDefaultAddresses(userId);
            address.setIsDefault(true);
        } else {
            address.setIsDefault(false);
        }

        Address savedAddress = addressRepository.save(address);
        log.info("Address created with ID: {} for user: {}", savedAddress.getId(), userId);
        return addressMapper.toDTO(savedAddress);
    }

    @Override
    public AddressDTO getAddressById(Long addressId) {
        // Note: Since this interface method doesn't take userId, we cannot check ownership here.
        // Ideally, this should be used carefully or restricted at the Controller level.
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));
        return addressMapper.toDTO(address);
    }

    @Override
    public List<AddressDTO> getUserAddresses(Long userId) {
        return addressRepository.findByUserIdOrderByDefaultAndCreatedAt(userId)
                .stream()
                .map(addressMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public AddressDTO getDefaultAddress(Long userId) {
        Address address = addressRepository.findFirstByUserIdAndIsDefaultTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No default address set for user: " + userId));
        return addressMapper.toDTO(address);
    }

    @Override
    @Transactional
    public AddressDTO updateAddress(Long addressId, AddressRequest request, Long userId) {
        // Secure fetch: Ensure address belongs to the user
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found or does not belong to user"));

        addressMapper.updateEntityFromRequest(request, address);

        // Handle Default Toggle
        if (Boolean.TRUE.equals(request.getIsDefault()) && !address.getIsDefault()) {
            addressRepository.clearDefaultAddresses(userId);
            address.setIsDefault(true);
        }
        // Prevent un-checking default if it's the ONLY address (Business Rule)
        else if (Boolean.FALSE.equals(request.getIsDefault()) && address.getIsDefault()) {
            long count = addressRepository.countByUserId(userId);
            if (count == 1) {
                throw new BadRequestException("Cannot remove default status from your only address.");
            }
        }

        Address updatedAddress = addressRepository.save(address);
        log.info("Address {} updated for user {}", addressId, userId);
        return addressMapper.toDTO(updatedAddress);
    }

    @Override
    @Transactional
    public AddressDTO setDefaultAddress(Long addressId, Long userId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        if (!address.getIsDefault()) {
            addressRepository.clearDefaultAddresses(userId);
            address.setIsDefault(true);
            addressRepository.save(address);
        }

        return addressMapper.toDTO(address);
    }

    @Override
    @Transactional
    public void deleteAddress(Long addressId, Long userId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        boolean wasDefault = address.getIsDefault();
        addressRepository.delete(address);
        addressRepository.flush(); // Ensure deletion happens before we search for a replacement

        // Smart Logic: If default was deleted, assign a new default
        if (wasDefault) {
            // Find the most recent remaining address
            addressRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
                    .ifPresent(newDefault -> {
                        newDefault.setIsDefault(true);
                        addressRepository.save(newDefault);
                        log.info("Auto-assigned new default address: {}", newDefault.getId());
                    });
        }

        log.info("Address {} deleted for user {}", addressId, userId);
    }

    @Override
    public Long getUserAddressCount(Long userId) {
        return addressRepository.countByUserId(userId);
    }
}