package com.peterscode.ecommerce_management_system.service.impl;


import com.peterscode.ecommerce_management_system.exception.BadRequestException;
import com.peterscode.ecommerce_management_system.exception.ResourceNotFoundException;
import com.peterscode.ecommerce_management_system.mapper.ReviewMapper;
import com.peterscode.ecommerce_management_system.model.dto.request.ReviewRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.ReviewResponse;
import com.peterscode.ecommerce_management_system.model.entity.Order;
import com.peterscode.ecommerce_management_system.model.entity.Product;
import com.peterscode.ecommerce_management_system.model.entity.Review;
import com.peterscode.ecommerce_management_system.model.entity.ReviewVote;
import com.peterscode.ecommerce_management_system.model.entity.User;
import com.peterscode.ecommerce_management_system.model.enums.OrderStatus;
import com.peterscode.ecommerce_management_system.repository.OrderRepository;
import com.peterscode.ecommerce_management_system.repository.ProductRepository;
import com.peterscode.ecommerce_management_system.repository.ReviewRepository;
import com.peterscode.ecommerce_management_system.repository.ReviewVoteRepository;
import com.peterscode.ecommerce_management_system.repository.UserRepository;
import com.peterscode.ecommerce_management_system.service.ReviewService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ReviewVoteRepository reviewVoteRepository; // Required for secure voting
    private final ReviewMapper reviewMapper;

    @Override
    @Transactional
    public ReviewResponse createReview(ReviewRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        // SECURITY: Prevent Review Bombing (One review per product per user)
        // This check works even if they don't provide an orderId
        if (reviewRepository.existsByProductIdAndUserId(request.getProductId(), userId)) {
            throw new BadRequestException("You have already reviewed this product.");
        }

        Review review = reviewMapper.toEntity(request);
        review.setUser(user);
        review.setProduct(product);

        // SECURITY: Sanitize inputs to prevent XSS attacks
        review.setTitle(HtmlUtils.htmlEscape(request.getTitle()));
        review.setComment(HtmlUtils.htmlEscape(request.getComment()));

        // Logic for Verified Purchases
        if (request.getOrderId() != null) {
            Order order = orderRepository.findById(request.getOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + request.getOrderId()));

            // SECURITY: Ensure order belongs to user
            if (!order.getUser().getId().equals(userId)) {
                throw new BadRequestException("This order does not belong to you");
            }

            if (order.getStatus() != OrderStatus.DELIVERED) {
                throw new BadRequestException("You can only review products from delivered orders");
            }

            boolean productInOrder = order.getOrderItems().stream()
                    .anyMatch(item -> item.getProduct().getId().equals(request.getProductId()));

            if (!productInOrder) {
                throw new BadRequestException("This product is not in the specified order");
            }

            review.setOrder(order);
            review.setVerifiedPurchase(true);
        } else {
            review.setVerifiedPurchase(false);
        }

        Review savedReview = reviewRepository.save(review);
        log.info("Review created for product {} by user {}", request.getProductId(), userId);

        return reviewMapper.toResponse(savedReview);
    }

    @Override
    public ReviewResponse getReviewById(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));
        return reviewMapper.toResponse(review);
    }

    @Override
    public Page<ReviewResponse> getProductReviews(Long productId, Pageable pageable) {
        return reviewRepository.findByProductId(productId, pageable)
                .map(reviewMapper::toResponse);
    }

    @Override
    public Page<ReviewResponse> getApprovedProductReviews(Long productId, Pageable pageable) {
        return reviewRepository.findByProductIdAndIsApprovedTrue(productId, pageable)
                .map(reviewMapper::toResponse);
    }

    @Override
    public Page<ReviewResponse> getUserReviews(Long userId, Pageable pageable) {
        return reviewRepository.findByUserId(userId, pageable)
                .map(reviewMapper::toResponse);
    }

    @Override
    public Page<ReviewResponse> getPendingReviews(Pageable pageable) {
        return reviewRepository.findByIsApprovedFalse(pageable)
                .map(reviewMapper::toResponse);
    }

    @Override
    @Transactional
    public ReviewResponse approveReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        review.setIsApproved(true);
        Review approvedReview = reviewRepository.save(review);
        log.info("Review {} approved", reviewId);

        return reviewMapper.toResponse(approvedReview);
    }

    @Override
    @Transactional
    public ReviewResponse rejectReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        review.setIsApproved(false);
        Review rejectedReview = reviewRepository.save(review);
        log.info("Review {} rejected", reviewId);

        return reviewMapper.toResponse(rejectedReview);
    }

    @Override
    @Transactional
    public ReviewResponse addAdminResponse(Long reviewId, String response) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        // SECURITY: Sanitize admin response too
        review.setAdminResponse(HtmlUtils.htmlEscape(response));
        review.setRespondedAt(LocalDateTime.now());
        Review updatedReview = reviewRepository.save(review);
        log.info("Admin response added to review {}", reviewId);

        return reviewMapper.toResponse(updatedReview);
    }

    @Transactional
    @Override
    public ReviewResponse markReviewHelpful(Long reviewId, Long userId) {
        return handleVote(reviewId, userId, true);
    }

    @Transactional
    @Override
    public ReviewResponse markReviewNotHelpful(Long reviewId, Long userId) {
        return handleVote(reviewId, userId, false);
    }

    /**
     * Helper method to handle secure voting logic.
     * Prevents users from voting multiple times on the same review.
     */
    private ReviewResponse handleVote(Long reviewId, Long userId, boolean isHelpful) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Optional<ReviewVote> existingVote = reviewVoteRepository.findByReviewAndUser(review, user);

        if (existingVote.isPresent()) {
            ReviewVote vote = existingVote.get();
            if (vote.getIsHelpful() == isHelpful) {
                // User clicked same button again -> Remove vote (Toggle off)
                reviewVoteRepository.delete(vote);
                if (isHelpful) review.decrementHelpful(); else review.decrementNotHelpful();
            } else {
                // User changed mind (e.g., from Helpful to Not Helpful)
                vote.setIsHelpful(isHelpful);
                reviewVoteRepository.save(vote);
                if (isHelpful) {
                    review.incrementHelpful();
                    review.decrementNotHelpful();
                } else {
                    review.incrementNotHelpful();
                    review.decrementHelpful();
                }
            }
        } else {
            // Create new vote
            ReviewVote newVote = new ReviewVote();
            newVote.setReview(review);
            newVote.setUser(user);
            newVote.setIsHelpful(isHelpful);
            reviewVoteRepository.save(newVote);

            if (isHelpful) review.incrementHelpful(); else review.incrementNotHelpful();
        }

        Review updatedReview = reviewRepository.save(review);
        return reviewMapper.toResponse(updatedReview);
    }

    @Override
    public Double getProductAverageRating(Long productId) {
        Double average = reviewRepository.getAverageRatingByProductId(productId);
        return average != null ? average : 0.0;
    }

    @Override
    public Long getProductReviewCount(Long productId) {
        return reviewRepository.countApprovedReviewsByProductId(productId);
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        // SECURITY: Authorization check
        if (!review.getUser().getId().equals(userId)) {
            throw new BadRequestException("You can only delete your own reviews");
        }

        // Clean up votes before deleting the review to avoid foreign key constraints
        // (Assuming you don't have CascadeType.REMOVE set on the Entity relationship)
        // reviewVoteRepository.deleteByReviewId(reviewId);

        reviewRepository.delete(review);
        log.info("Review {} deleted by user {}", reviewId, userId);
    }

    @Override
    public boolean hasUserReviewedProduct(Long userId, Long productId, Long orderId) {
        // Consolidated logic: Check if user reviewed this product at all
        return reviewRepository.existsByProductIdAndUserId(productId, userId);
    }
}