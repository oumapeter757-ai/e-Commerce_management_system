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
import com.peterscode.ecommerce_management_system.service.CouponService;
import com.peterscode.ecommerce_management_system.service.InventoryService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;
    private final InventoryService inventoryService;
    private final CouponService couponService;
    private final CartMapper cartMapper;

    /** Guest cart expiry duration in days */
    private static final int GUEST_CART_EXPIRY_DAYS = 7;

    // ==================== CORE CART OPERATIONS ====================

    @Override
    public CartResponse getCart(Long userId) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseGet(() -> createCartForUser(userId));
        return cartMapper.toResponse(cart);
    }

    @Override
    public CartResponse getCartBySessionId(String sessionId) {
        validateSessionId(sessionId);
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
        validateSessionId(sessionId);
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
        Cart cart = getCartOrThrow(userId);
        CartItem cartItem = getCartItemOrThrow(cartItemId);
        validateCartItemOwnership(cart, cartItem);

        if (quantity <= 0) {
            throw new BadRequestException("Quantity must be greater than zero");
        }

        // Enforce max buy quantity
        enforceMaxBuyQuantity(cartItem.getProduct(), quantity);

        // Check available stock
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
        Cart cart = getCartOrThrow(userId);
        CartItem cartItem = getCartItemOrThrow(cartItemId);
        validateCartItemOwnership(cart, cartItem);

        cart.removeItem(cartItem);
        cartItemRepository.delete(cartItem);

        Cart savedCart = cartRepository.save(cart);
        return cartMapper.toResponse(savedCart);
    }

    @Override
    @Transactional
    public CartResponse clearCart(Long userId) {
        Cart cart = getCartOrThrow(userId);

        cart.clearItems();
        cartItemRepository.deleteByCartId(cart.getId());

        Cart savedCart = cartRepository.save(cart);
        log.info("Cart cleared for user: {}", userId);
        return cartMapper.toResponse(savedCart);
    }

    @Override
    @Transactional
    public CartResponse applyCoupon(Long userId, String couponCode) {
        Cart cart = getCartOrThrow(userId);

        // Sanitize coupon code input
        String sanitized = sanitizeInput(couponCode);
        if (sanitized == null || sanitized.isBlank()) {
            throw new BadRequestException("Invalid coupon code");
        }

        // Validate coupon and calculate discount via CouponService
        BigDecimal orderTotal = cart.getSelectedItemsTotal() != null
                ? cart.getSelectedItemsTotal() : cart.getSubtotal();
        BigDecimal discount = couponService.validateAndApplyCoupon(sanitized, orderTotal, userId);

        cart.setCouponCode(sanitized.toUpperCase().trim());
        cart.setDiscountAmount(discount);

        // Handle free shipping coupons
        if (couponService.isFreeShippingCoupon(sanitized)) {
            cart.setEstimatedShippingCost(BigDecimal.ZERO);
        }

        cart.recalculateTotals();

        return cartMapper.toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse removeCoupon(Long userId) {
        Cart cart = getCartOrThrow(userId);

        cart.setCouponCode(null);
        cart.setDiscountAmount(BigDecimal.ZERO);
        cart.recalculateTotals();

        return cartMapper.toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public void mergeGuestCartWithUserCart(String sessionId, Long userId) {
        validateSessionId(sessionId);

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

            Integer availableStock = inventoryService.getStock(guestItem.getProduct().getId());

            if (existingItem.isPresent()) {
                CartItem item = existingItem.get();
                int newQty = Math.min(item.getQuantity() + guestItem.getQuantity(), availableStock);
                item.setQuantity(newQty);
                item.calculateTotalPrice();
            } else {
                int qty = Math.min(guestItem.getQuantity(), availableStock);
                if (qty > 0) {
                    CartItem newItem = CartItem.builder()
                            .cart(userCart)
                            .product(guestItem.getProduct())
                            .quantity(qty)
                            .unitPrice(guestItem.getUnitPrice())
                            .priceAtAddition(guestItem.getPriceAtAddition())
                            .selected(true)
                            .savedForLater(false)
                            .build();
                    newItem.calculateTotalPrice();
                    userCart.addItem(newItem);
                }
            }
        }

        userCart.recalculateTotals();
        cartRepository.save(userCart);
        cartRepository.delete(guestCart);
        log.info("Merged guest cart {} into user cart {}", sessionId, userId);
    }

    @Override
    @Transactional
    public void deleteCart(Long userId) {
        cartRepository.deleteByUserId(userId);
    }

    // ==================== KILIMALL-STYLE FEATURES ====================

    @Override
    @Transactional
    public CartResponse toggleItemSelection(Long userId, List<Long> itemIds, boolean selected) {
        Cart cart = getCartOrThrow(userId);

        cart.getItems().stream()
                .filter(item -> itemIds.contains(item.getId()))
                .forEach(item -> {
                    validateCartItemOwnership(cart, item);
                    item.setSelected(selected);
                });

        cart.recalculateTotals();
        Cart savedCart = cartRepository.save(cart);

        log.info("Toggled selection for {} items in cart for user: {}", itemIds.size(), userId);
        return cartMapper.toResponse(savedCart);
    }

    @Override
    @Transactional
    public CartResponse selectAllItems(Long userId) {
        Cart cart = getCartOrThrow(userId);

        cart.getItems().stream()
                .filter(item -> !Boolean.TRUE.equals(item.getSavedForLater()))
                .forEach(item -> item.setSelected(true));

        cart.recalculateTotals();
        return cartMapper.toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse deselectAllItems(Long userId) {
        Cart cart = getCartOrThrow(userId);

        cart.getItems().stream()
                .filter(item -> !Boolean.TRUE.equals(item.getSavedForLater()))
                .forEach(item -> item.setSelected(false));

        cart.recalculateTotals();
        return cartMapper.toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse removeSelectedItems(Long userId, List<Long> itemIds) {
        Cart cart = getCartOrThrow(userId);

        List<CartItem> toRemove = cart.getItems().stream()
                .filter(item -> itemIds.contains(item.getId()))
                .peek(item -> validateCartItemOwnership(cart, item))
                .toList();

        toRemove.forEach(item -> {
            cart.removeItem(item);
            cartItemRepository.delete(item);
        });

        cart.recalculateTotals();
        Cart savedCart = cartRepository.save(cart);

        log.info("Bulk removed {} items from cart for user: {}", toRemove.size(), userId);
        return cartMapper.toResponse(savedCart);
    }

    @Override
    @Transactional
    public CartResponse saveForLater(Long userId, Long cartItemId) {
        Cart cart = getCartOrThrow(userId);
        CartItem cartItem = getCartItemOrThrow(cartItemId);
        validateCartItemOwnership(cart, cartItem);

        cartItem.setSavedForLater(true);
        cartItem.setSelected(false);
        cart.recalculateTotals();

        Cart savedCart = cartRepository.save(cart);
        log.info("Item {} saved for later for user: {}", cartItemId, userId);
        return cartMapper.toResponse(savedCart);
    }

    @Override
    @Transactional
    public CartResponse moveToCart(Long userId, Long cartItemId) {
        Cart cart = getCartOrThrow(userId);
        CartItem cartItem = getCartItemOrThrow(cartItemId);
        validateCartItemOwnership(cart, cartItem);

        // Check stock is still available before moving back
        Integer availableStock = inventoryService.getStock(cartItem.getProduct().getId());
        if (availableStock < cartItem.getQuantity()) {
            if (availableStock <= 0) {
                throw new InsufficientStockException("Product is out of stock");
            }
            cartItem.setQuantity(availableStock);
            cartItem.calculateTotalPrice();
        }

        // Update price to current price if changed
        BigDecimal currentPrice = cartItem.getProduct().getActualPrice();
        if (currentPrice != null && currentPrice.compareTo(cartItem.getUnitPrice()) != 0) {
            cartItem.setUnitPrice(currentPrice);
            cartItem.calculateTotalPrice();
        }

        cartItem.setSavedForLater(false);
        cartItem.setSelected(true);
        cart.recalculateTotals();

        Cart savedCart = cartRepository.save(cart);
        log.info("Item {} moved back to cart for user: {}", cartItemId, userId);
        return cartMapper.toResponse(savedCart);
    }

    @Override
    @Transactional
    public CartResponse updateItemNotes(Long userId, Long cartItemId, String notes) {
        Cart cart = getCartOrThrow(userId);
        CartItem cartItem = getCartItemOrThrow(cartItemId);
        validateCartItemOwnership(cart, cartItem);

        // Sanitize notes input (XSS prevention)
        cartItem.setNotes(sanitizeInput(notes));
        Cart savedCart = cartRepository.save(cart);

        return cartMapper.toResponse(savedCart);
    }

    @Override
    public CartResponse getCartSummary(Long userId) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseGet(() -> createCartForUser(userId));

        // Refresh prices from product catalog to detect changes
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            if (product != null) {
                BigDecimal currentPrice = product.getActualPrice();
                // Update unit price to reflect current pricing
                if (currentPrice != null && currentPrice.compareTo(item.getUnitPrice()) != 0) {
                    item.setUnitPrice(currentPrice);
                    item.calculateTotalPrice();
                }
            }
        }
        cart.recalculateTotals();

        return cartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse estimateShipping(Long userId, Long addressId) {
        Cart cart = getCartOrThrow(userId);

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        // IDOR prevention: verify address belongs to user
        if (!address.getUser().getId().equals(userId)) {
            throw new BadRequestException("Address does not belong to user");
        }

        // Shipping cost estimation based on region
        BigDecimal shippingCost = calculateShippingEstimate(cart, address);
        cart.setEstimatedShippingCost(shippingCost);
        cart.recalculateTotals();

        Cart savedCart = cartRepository.save(cart);
        return cartMapper.toResponse(savedCart);
    }

    // ==================== PRIVATE HELPERS ====================

    private Cart getCartOrThrow(Long userId) {
        return cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + userId));
    }

    private CartItem getCartItemOrThrow(Long cartItemId) {
        return cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + cartItemId));
    }

    /** IDOR prevention: verify cart item belongs to the user's cart */
    private void validateCartItemOwnership(Cart cart, CartItem cartItem) {
        if (!cartItem.getCart().getId().equals(cart.getId())) {
            log.warn("SECURITY: Cart item {} does not belong to cart {}", cartItem.getId(), cart.getId());
            throw new BadRequestException("Cart item does not belong to this cart");
        }
    }

    /** Session fixation prevention: validate guest session ID format */
    private void validateSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new BadRequestException("Session ID is required");
        }
        // Only allow UUID-format session IDs (max 36 chars, alphanumeric + hyphens)
        if (sessionId.length() > 36 || !sessionId.matches("^[a-zA-Z0-9\\-]+$")) {
            throw new BadRequestException("Invalid session ID format");
        }
    }

    /** XSS prevention: strip HTML/script tags from user input */
    private String sanitizeInput(String input) {
        if (input == null) return null;
        return input.replaceAll("<[^>]*>", "")
                .replaceAll("(?i)<script.*?>.*?</script>", "")
                .replaceAll("(?i)javascript:", "")
                .replaceAll("(?i)on\\w+=", "")
                .trim();
    }

    /** Enforce per-product max buy quantity limit */
    private void enforceMaxBuyQuantity(Product product, int requestedQuantity) {
        Integer maxBuy = product.getMaxBuyQuantity();
        if (maxBuy != null && requestedQuantity > maxBuy) {
            throw new BadRequestException(
                    "Maximum purchase quantity for this product is " + maxBuy);
        }
    }

    private Cart createCartForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return cartRepository.save(Cart.builder().user(user).build());
    }

    private Cart createCartForGuest(String sessionId) {
        return cartRepository.save(Cart.builder()
                .sessionId(sessionId)
                .expiresAt(LocalDateTime.now().plusDays(GUEST_CART_EXPIRY_DAYS))
                .build());
    }

    private void addOrUpdateCartItem(Cart cart, CartItemRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!Boolean.TRUE.equals(product.getIsActive())) {
            throw new BadRequestException("Product is not available");
        }

        // Enforce max buy quantity
        enforceMaxBuyQuantity(product, request.getQuantity());

        // Check available stock via InventoryService
        Integer availableStock = inventoryService.getStock(product.getId());
        if (availableStock < request.getQuantity()) {
            throw new InsufficientStockException("Insufficient stock. Available: " + availableStock);
        }

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId())
                        && !Boolean.TRUE.equals(item.getSavedForLater()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + request.getQuantity();

            enforceMaxBuyQuantity(product, newQuantity);

            if (availableStock < newQuantity) {
                throw new InsufficientStockException("Cannot add more. Max available: " + availableStock);
            }

            item.setQuantity(newQuantity);
            item.calculateTotalPrice();
        } else {
            // Use actual price (discounted if available) — NEVER trust client-side price
            BigDecimal priceToUse = product.getActualPrice();

            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .unitPrice(priceToUse)
                    .priceAtAddition(priceToUse) // Record price at time of adding
                    .selected(true)
                    .savedForLater(false)
                    .maxBuyQuantity(product.getMaxBuyQuantity())
                    .build();

            newItem.calculateTotalPrice();
            cart.addItem(newItem);
        }

        cart.recalculateTotals();
    }

    /** Calculate shipping estimate based on address region */
    private BigDecimal calculateShippingEstimate(Cart cart, Address address) {
        if (cart.isEmpty()) return BigDecimal.ZERO;

        // Flat-rate shipping table (configurable per region)
        String city = address.getCity() != null ? address.getCity().toLowerCase().trim() : "";

        BigDecimal baseCost;
        if (city.contains("nairobi")) {
            baseCost = new BigDecimal("200"); // KES 200 within Nairobi
        } else if (city.contains("mombasa") || city.contains("kisumu") || city.contains("nakuru")) {
            baseCost = new BigDecimal("350"); // KES 350 major cities
        } else {
            baseCost = new BigDecimal("500"); // KES 500 other regions
        }

        // Free shipping for orders above KES 5000
        if (cart.getSelectedItemsTotal() != null
                && cart.getSelectedItemsTotal().compareTo(new BigDecimal("5000")) >= 0) {
            return BigDecimal.ZERO;
        }

        return baseCost;
    }
}

