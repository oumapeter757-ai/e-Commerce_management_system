package com.peterscode.ecommerce_management_system.repository;

import com.peterscode.ecommerce_management_system.model.entity.User;
import com.peterscode.ecommerce_management_system.model.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Find users by role
     */
    Page<User> findByRole(Role role, Pageable pageable);

    /**
     * Find enabled users only
     */
    Page<User> findByEnabledTrue(Pageable pageable);

    /**
     * Search users by name or email
     */
    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<User> searchUsers(@Param("search") String search, Pageable pageable);

    /**
     * Find users created between dates
     */
    @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    List<User> findUsersCreatedBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Count users by role
     */
    long countByRole(Role role);

    /**
     * Count active users
     */
    long countByEnabledTrue();

    /**
     * Find users by role and enabled status
     */
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.enabled = :isEnabled")
    Page<User> findByRoleAndEnabled(
            @Param("role") Role role,
            @Param("isEnabled") boolean isEnabled,
            Pageable pageable
    );

    /**
     * Update user enabled status
     */
    @Modifying
    @Query("UPDATE User u SET u.enabled = :isEnabled WHERE u.id = :userId")
    int updateUserEnabledStatus(@Param("userId") Long userId, @Param("isEnabled") boolean isEnabled);

    /**
     * Find recently registered users
     */
    @Query("SELECT u FROM User u WHERE u.createdAt >= :since ORDER BY u.createdAt DESC")
    List<User> findRecentlyRegistered(@Param("since") LocalDateTime since);
}