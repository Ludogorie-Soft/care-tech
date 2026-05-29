package com.techstore.repository;

import com.techstore.entity.LeasingApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface LeasingApplicationRepository extends JpaRepository<LeasingApplication, Long> {

    Optional<LeasingApplication> findByTbiOrderId(Integer tbiOrderId);

    Optional<LeasingApplication> findByTbiApplicationId(String tbiApplicationId);

    Optional<LeasingApplication> findByStoreOrderId(String storeOrderId);

    Page<LeasingApplication> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<LeasingApplication> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    long countByStatus(String status);

    @Query("SELECT COALESCE(SUM(a.amountEur), 0) FROM LeasingApplication a WHERE a.status IN ('ContractSigned', 'Paid')")
    BigDecimal sumSignedAmountEur();

    @Query("SELECT COALESCE(SUM(a.amountEur), 0) FROM LeasingApplication a WHERE a.status = 'Approved'")
    BigDecimal sumApprovedAmountEur();
}
