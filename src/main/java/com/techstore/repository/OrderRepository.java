package com.techstore.repository;

import com.techstore.entity.Order;
import com.techstore.enums.OrderStatus;
import com.techstore.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    Long countByStatus(OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.paymentStatus = 'PAID'")
    BigDecimal sumTotalAmount();

    @Query("SELECT o FROM Order o WHERE YEAR(o.createdAt) = :year AND MONTH(o.createdAt) = :month")
    List<Order> findOrdersByYearAndMonth(@Param("year") int year, @Param("month") int month);

    // Revenue statistics
    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o " +
            "WHERE o.paymentStatus = 'PAID' " +
            "AND YEAR(o.createdAt) = YEAR(CURRENT_DATE) " +
            "AND MONTH(o.createdAt) = MONTH(CURRENT_DATE) " +
            "AND DAY(o.createdAt) = DAY(CURRENT_DATE)")
    BigDecimal sumDailyRevenue();

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o " +
            "WHERE o.paymentStatus = 'PAID' " +
            "AND YEAR(o.createdAt) = YEAR(CURRENT_DATE) " +
            "AND MONTH(o.createdAt) = MONTH(CURRENT_DATE)")
    BigDecimal sumMonthlyRevenue();

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o " +
            "WHERE o.paymentStatus = 'PAID' " +
            "AND YEAR(o.createdAt) = YEAR(CURRENT_DATE)")
    BigDecimal sumYearlyRevenue();

    // Order count statistics
    @Query("SELECT COUNT(o) FROM Order o " +
            "WHERE YEAR(o.createdAt) = YEAR(CURRENT_DATE) " +
            "AND MONTH(o.createdAt) = MONTH(CURRENT_DATE) " +
            "AND DAY(o.createdAt) = DAY(CURRENT_DATE)")
    Long countOrdersToday();

    @Query("SELECT COUNT(o) FROM Order o " +
            "WHERE YEAR(o.createdAt) = YEAR(CURRENT_DATE) " +
            "AND MONTH(o.createdAt) = MONTH(CURRENT_DATE)")
    Long countOrdersThisMonth();

    @Query("SELECT COUNT(o) FROM Order o " +
            "WHERE YEAR(o.createdAt) = YEAR(CURRENT_DATE)")
    Long countOrdersThisYear();

    // Customer statistics
    @Query("SELECT COUNT(DISTINCT o.customerEmail) FROM Order o")
    Long countTotalCustomers();

    @Query("SELECT COUNT(DISTINCT o.customerEmail) FROM Order o " +
            "WHERE YEAR(o.createdAt) = YEAR(CURRENT_DATE) " +
            "AND MONTH(o.createdAt) = MONTH(CURRENT_DATE) " +
            "AND DAY(o.createdAt) = DAY(CURRENT_DATE)")
    Long countNewCustomersToday();

    @Query("SELECT COUNT(DISTINCT o.customerEmail) FROM Order o " +
            "WHERE YEAR(o.createdAt) = YEAR(CURRENT_DATE) " +
            "AND MONTH(o.createdAt) = MONTH(CURRENT_DATE)")
    Long countNewCustomersThisMonth();

    @Query("SELECT COUNT(DISTINCT o.customerEmail) FROM Order o " +
            "WHERE YEAR(o.createdAt) = YEAR(CURRENT_DATE)")
    Long countNewCustomersThisYear();

    // Average order value
    @Query("SELECT COALESCE(AVG(o.total), 0) FROM Order o WHERE o.paymentStatus = 'PAID'")
    BigDecimal calculateAverageOrderValue();

    @Query("SELECT COALESCE(AVG(o.total), 0) FROM Order o " +
            "WHERE o.paymentStatus = 'PAID' " +
            "AND YEAR(o.createdAt) = YEAR(CURRENT_DATE) " +
            "AND MONTH(o.createdAt) = MONTH(CURRENT_DATE)")
    BigDecimal calculateAverageOrderValueThisMonth();

    // Payment statistics
    @Query("SELECT COUNT(o) FROM Order o WHERE o.paymentStatus = 'PAID'")
    Long countPaidOrders();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.paymentStatus != 'PAID'")
    Long countUnpaidOrders();

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o " +
            "WHERE o.paymentStatus != 'PAID' " +
            "AND o.status != 'CANCELLED'")
    BigDecimal sumUnpaidRevenue();

    // Min/Max order values
    @Query("SELECT COALESCE(MAX(o.total), 0) FROM Order o")
    BigDecimal findHighestOrderValue();

    @Query("SELECT COALESCE(MIN(o.total), 0) FROM Order o WHERE o.total > 0")
    BigDecimal findLowestOrderValue();

    // Active orders count
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status IN ('PENDING', 'PROCESSING', 'SHIPPED')")
    Long countActiveOrders();

    Long countByPaymentStatus(PaymentStatus paymentStatus);
}