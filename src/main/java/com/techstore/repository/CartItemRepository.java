package com.techstore.repository;

import com.techstore.entity.CartItem;
import com.techstore.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserIdOrderByCreatedAtAsc(Long userId);

    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);

    void deleteByUserId(Long userId);

    void deleteByUserEmail(String email);

    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.user.id = :userId AND ci.product.id IN :productIds")
    void deleteByUserIdAndProductIdIn(@Param("userId") Long userId, @Param("productIds") java.util.Collection<Long> productIds);

    @Query("SELECT SUM(ci.quantity * p.finalPrice) FROM CartItem ci " +
            "JOIN ci.product p WHERE ci.user.id = :userId")
    BigDecimal calculateTotalPriceByUserId(@Param("userId") Long userId);

    @Query("SELECT SUM(ci.quantity) FROM CartItem ci WHERE ci.user.id = :userId")
    Integer countTotalItemsByUserId(@Param("userId") Long userId);

    /**
     * Find users with abandoned carts:
     * - have at least one cart item
     * - all items were last updated before the given cutoff (no recent activity)
     * - no reminder sent yet, OR last reminder was sent before the resend cutoff
     */
    @Query("""
            SELECT DISTINCT ci.user FROM CartItem ci
            WHERE ci.user.active = true
              AND ci.user.emailVerified = true
              AND ci.user.role = com.techstore.entity.User.Role.USER
              AND (
                SELECT MAX(ci2.updatedAt) FROM CartItem ci2 WHERE ci2.user = ci.user
              ) < :activityCutoff
              AND (
                ci.user.abandonedCartReminderSentAt IS NULL
                OR ci.user.abandonedCartReminderSentAt < :resendCutoff
              )
            """)
    List<User> findUsersWithAbandonedCarts(
            @Param("activityCutoff") LocalDateTime activityCutoff,
            @Param("resendCutoff") LocalDateTime resendCutoff
    );
}