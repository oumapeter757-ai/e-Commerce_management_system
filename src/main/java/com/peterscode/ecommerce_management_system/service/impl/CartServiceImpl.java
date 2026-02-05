package com.peterscode.ecommerce_management_system.service.impl;

import com.peterscode.ecommerce_management_system.exception.BadRequestException;
import com.peterscode.ecommerce_management_system.exception.InsufficientStockException;
import com.peterscode.ecommerce_management_system.exception.ResourceNotFoundException;
import com.peterscode.ecommerce_management_system.mapper.CartMapper;
import com.peterscode.ecommerce_management_system.model.dto.request.CartItemRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.CartResponse;
import com.peterscode.ecommerce_management_system.model.entity.*;
import com.peterscode.ecommerce_management_system.repository.*;
import com.peterscode.ecommerce_management_system.service.CartService;
import com.peterscode.ecommerce_management_system.service.InventoryService; // <--- Import InventoryService
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService; // <--- Inject InventoryService
    private final CartMapper cartMapper;

    @Override
    public CartResponse getCart(Long userId) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseGet(() -> createCartForUser(userId));
        return cartMapper.toResponse(cart);
    }

    @Override
    public CartResponse getCartBySessionId(String sessionId) {
        Cart cart = cartRepository.findBySessionIdWithItems(sessionId)
                .orElseGet(() -> createCartForGuest(sessionId));
        return cartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addItemToCart(Long userId, CartItemRequest request) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseGet(() -> createCartForUser(userId));

        addOrUpdateCartItem(cart, request);
        Cart savedCart = cartRepository.save(cart);

        log.info("Item added to cart for user: {}", userId);
        return cartMapper.toResponse(savedCart);
    }

    @Override
    @Transactional
    public CartResponse addItemToGuestCart(String sessionId, CartItemRequest request) {
        Cart cart = cartRepository.findBySessionIdWithItems(sessionId)
                .orElseGet(() -> createCartForGuest(sessionId));

        addOrUpdateCartItem(cart, request);
        Cart savedCart = cartRepository.save(cart);

        log.info("Item added to guest cart: {}", sessionId);
        return cartMapper.toResponse(savedCart);
    }

    @Override
    @Transactional
    public CartResponse updateCartItem(Long userId, Long cartItemId, Integer quantity) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + userId));

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + cartItemId));

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new BadRequestException("Cart item does not belong to this cart");
        }

        // --- UPDATED: Check stock via InventoryService ---
        // inventoryService.getStock() returns the AVAILABLE stock (Total - Reserved)
        Integer availableStock = inventoryService.getStock(cartItem.getProduct().getId());

        if (availableStock < quantity) {
            throw new InsufficientStockException("Insufficient stock. Available: " + availableStock);
        }

        cartItem.setQuantity(quantity);
        cartItem.calculateTotalPrice();

        cart.recalculateTotals();
        Cart savedCart = cartRepository.save(cart);

        log.info("Cart item {} updated to quantity: {}", cartItemId, quantity);
        return cartMapper.toResponse(savedCart);
    }

    @Override
    @Transactional
    public CartResponse removeItemFromCart(Long userId, Long cartItemId) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + userId));

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + cartItemId));

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new BadRequestException("Cart item does not belong to this cart");
        }

        cart.removeItem(cartItem);
        cartItemRepository.delete(cartItem);

        Cart savedCart = cartRepository.save(cart);
        return cartMapper.toResponse(savedCart);
    }

    @Override
    @Transactional
    public CartResponse clearCart(Long userId) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + userId));

        cart.clearItems();
        cartItemRepository.deleteByCartId(cart.getId());

        Cart savedCart = cartRepository.save(cart);
        log.info("Cart cleared for user: {}", userId);

        return cartMapper.toResponse(savedCart);
    }

    @Override
    @Transactional
    public CartResponse applyCoupon(Long userId, String couponCode) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        cart.setCouponCode(couponCode);
        // cart.setDiscountAmount(calculateDiscount(...));
        cart.recalculateTotals();

        return cartMapper.toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse removeCoupon(Long userId) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        cart.setCouponCode(null);
        cart.setDiscountAmount(BigDecimal.ZERO);
        cart.recalculateTotals();

        return cartMapper.toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public void mergeGuestCartWithUserCart(String sessionId, Long userId) {
        Optional<Cart> guestCartOpt = cartRepository.findBySessionIdWithItems(sessionId);
        if (guestCartOpt.isEmpty()) {
            return;
        }

        Cart guestCart = guestCartOpt.get();
        if (guestCart.getItems().isEmpty()) {
            cartRepository.delete(guestCart);
            return;
        }

        Cart userCart = cartRepository.findByUserIdWithItems(userId)
                .orElseGet(() -> createCartForUser(userId));

        for (CartItem guestItem : guestCart.getItems()) {
            Optional<CartItem> existingItem = userCart.getItems().stream()
                    .filter(item -> item.getProduct().getId().equals(guestItem.getProduct().getId()))
                    .findFirst();

            // Check stock using InventoryService
            Integer availableStock = inventoryService.getStock(guestItem.getProduct().getId());

            if (existingItem.isPresent()) {
                CartItem item = existingItem.get();
                int newQty = item.getQuantity() + guestItem.getQuantity();

                // Validate combined stock
                if (availableStock >= newQty) {
                    item.setQuantity(newQty);
                    item.calculateTotalPrice();
                } else {
                    // Max out at available stock
                    item.setQuantity(availableStock);
                    item.calculateTotalPrice();
                }
            } else {
                // Clone item to user cart
                CartItem newItem = CartItem.builder()
                        .cart(userCart)
                        .product(guestItem.getProduct())
                        .quantity(guestItem.getQuantity())
                        .unitPrice(guestItem.getUnitPrice())
                        .build();

                // Stock check for new item
                if (availableStock < newItem.getQuantity()){
                    newItem.setQuantity(availableStock);
                }

                if (newItem.getQuantity() > 0) {
                    newItem.calculateTotalPrice();
                    userCart.addItem(newItem);
                }
            }
        }

        userCart.recalculateTotals();
        cartRepository.save(userCart);

        // Delete guest cart after merge
        cartRepository.delete(guestCart);
        log.info("Merged guest cart {} into user cart {}", sessionId, userId);
    }

    @Override
    @Transactional
    public void deleteCart(Long userId) {
        cartRepository.deleteByUserId(userId);
    }

    // --- Helpers ---

    private Cart createCartForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return cartRepository.save(Cart.builder().user(user).build());
    }

    private Cart createCartForGuest(String sessionId) {
        return cartRepository.save(Cart.builder().sessionId(sessionId).build());
    }

    private void addOrUpdateCartItem(Cart cart, CartItemRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!Boolean.TRUE.equals(product.getIsActive())) {
            throw new BadRequestException("Product is not available");
        }

        // --- UPDATED: Check available stock via InventoryService ---
        Integer availableStock = inventoryService.getStock(product.getId());

        if (availableStock < request.getQuantity()) {
            throw new InsufficientStockException("Insufficient stock. Available: " + availableStock);
        }

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + request.getQuantity();

            if (availableStock < newQuantity) {
                throw new InsufficientStockException("Cannot add more. Max available: " + availableStock);
            }

            item.setQuantity(newQuantity);
            item.calculateTotalPrice();
        } else {
            BigDecimal priceToUse = (product.getDiscountPrice() != null
                    && product.getDiscountPrice().compareTo(BigDecimal.ZERO) > 0)
                    ? product.getDiscountPrice()
                    : product.getActualPrice(); // Assuming Product has getActualPrice helper

            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .unitPrice(priceToUse)
                    .build();

            newItem.calculateTotalPrice();
            cart.addItem(newItem);
        }

        cart.recalculateTotals();
    }
}