package com.peterscode.ecommerce_management_system.repository;

import com.peterscode.ecommerce_management_system.model.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUserId(Long userId);

    Optional<Cart> findBySessionId(String sessionId);

    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items WHERE c.user.id = :userId")
    Optional<Cart> findByUserIdWithItems(@Param("userId") Long userId);

    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items WHERE c.sessionId = :sessionId")
    Optional<Cart> findBySessionIdWithItems(@Param("sessionId") String sessionId);

    boolean existsByUserId(Long userId);

    boolean existsBySessionId(String sessionId);

    @Query("SELECT c FROM Cart c WHERE c.updatedAt < :date AND c.user IS NULL")
    List<Cart> findAbandonedGuestCarts(@Param("date") LocalDateTime date);

    @Modifying
    @Query("DELETE FROM Cart c WHERE c.updatedAt < :date AND c.user IS NULL")
    void deleteAbandonedGuestCarts(@Param("date") LocalDateTime date);

    void deleteByUserId(Long userId);

    void deleteBySessionId(String sessionId);
}