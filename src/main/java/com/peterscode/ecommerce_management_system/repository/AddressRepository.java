package com.peterscode.ecommerce_management_system.repository;

import com.peterscode.ecommerce_management_system.model.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    // 1. Basic Fetch
    List<Address> findByUserId(Long userId);

    // 2. Fetch for Display (Default address first, then newest)
    // Refined to ensure boolean sort works across all DBs
    @Query("SELECT a FROM Address a WHERE a.user.id = :userId ORDER BY a.isDefault DESC, a.createdAt DESC")
    List<Address> findByUserIdOrderByDefaultAndCreatedAt(@Param("userId") Long userId);

    // 3. Security Check: Find Address ONLY if it belongs to User
    // (Spring Data JPA can generate this implementation automatically without @Query)
    Optional<Address> findByIdAndUserId(Long id, Long userId);

    // 4. Find the current Default Address
    // Changed to 'findFirst' to prevent crashes if DB accidentally has duplicates
    Optional<Address> findFirstByUserIdAndIsDefaultTrue(Long userId);

    // 5. Smart Feature: Check if user has ANY address
    // (Used to set the very first address created as Default automatically)
    boolean existsByUserId(Long userId);

    // 6. Smart Feature: Find the newest address
    // (Used to re-assign default if the current default is deleted)
    Optional<Address> findTopByUserIdOrderByCreatedAtDesc(Long userId);

    // 7. Management: Reset all defaults for a user
    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.user.id = :userId")
    void clearDefaultAddresses(@Param("userId") Long userId);

    // 8. Stats
    long countByUserId(Long userId);

    // 9. Cleanup
    void deleteByUserId(Long userId);

    // Optional: Keep this only if you have an 'AddressType' enum in your Entity
    // List<Address> findByUserIdAndAddressType(Long userId, Address.AddressType addressType);
}