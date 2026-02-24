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

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<User> findByRole(Role role, Pageable pageable);

    Page<User> findByEnabledTrue(Pageable pageable);

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

    long countByRole(Role role);

    long countByEnabledTrue();

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.enabled = :isEnabled")
    Page<User> findByRoleAndEnabled(
            @Param("role") Role role,
            @Param("isEnabled") boolean isEnabled,
            Pageable pageable
    );

    @Modifying
    @Query("UPDATE User u SET u.enabled = :isEnabled WHERE u.id = :userId")
    int updateUserEnabledStatus(@Param("userId") Long userId, @Param("isEnabled") boolean isEnabled);

    @Query("SELECT u FROM User u WHERE u.createdAt >= :since ORDER BY u.createdAt DESC")
    List<User> findRecentlyRegistered(@Param("since") LocalDateTime since);
}