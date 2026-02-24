package com.peterscode.ecommerce_management_system.service.impl;

import com.peterscode.ecommerce_management_system.exception.BadRequestException;
import com.peterscode.ecommerce_management_system.exception.ResourceNotFoundException;
import com.peterscode.ecommerce_management_system.mapper.WishlistMapper;
import com.peterscode.ecommerce_management_system.model.dto.request.CartItemRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.WishlistResponse;
import com.peterscode.ecommerce_management_system.model.entity.Product;
import com.peterscode.ecommerce_management_system.model.entity.User;
import com.peterscode.ecommerce_management_system.model.entity.Wishlist;
import com.peterscode.ecommerce_management_system.repository.ProductRepository;
import com.peterscode.ecommerce_management_system.repository.UserRepository;
import com.peterscode.ecommerce_management_system.repository.WishlistRepository;
import com.peterscode.ecommerce_management_system.service.CartService;
import com.peterscode.ecommerce_management_system.service.WishlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;
    private final WishlistMapper wishlistMapper;

    @Override
    public List<WishlistResponse> getWishlist(Long userId) {
        return wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(wishlistMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public WishlistResponse addToWishlist(Long userId, Long productId) {
        // Check if already in wishlist
        if (wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new BadRequestException("Product is already in your wishlist");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .product(product)
                .build();

        Wishlist saved = wishlistRepository.save(wishlist);
        log.info("Product {} added to wishlist for user {}", productId, userId);

        return wishlistMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void removeFromWishlist(Long userId, Long productId) {
        if (!wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new ResourceNotFoundException("Product not found in wishlist");
        }
        wishlistRepository.deleteByUserIdAndProductId(userId, productId);
        log.info("Product {} removed from wishlist for user {}", productId, userId);
    }

    @Override
    @Transactional
    public void clearWishlist(Long userId) {
        wishlistRepository.deleteByUserId(userId);
        log.info("Wishlist cleared for user {}", userId);
    }

    @Override
    public boolean isInWishlist(Long userId, Long productId) {
        return wishlistRepository.existsByUserIdAndProductId(userId, productId);
    }

    @Override
    public long getWishlistCount(Long userId) {
        return wishlistRepository.countByUserId(userId);
    }

    @Override
    @Transactional
    public void moveToCart(Long userId, Long productId) {
        // Remove from wishlist
        removeFromWishlist(userId, productId);

        // Add to cart with quantity 1
        CartItemRequest cartRequest = CartItemRequest.builder()
                .productId(productId)
                .quantity(1)
                .build();

        cartService.addItemToCart(userId, cartRequest);
        log.info("Product {} moved from wishlist to cart for user {}", productId, userId);
    }
}

