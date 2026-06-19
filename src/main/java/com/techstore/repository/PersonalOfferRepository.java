package com.techstore.repository;

import com.techstore.entity.PersonalOffer;
import com.techstore.entity.PersonalOffer.OfferStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface PersonalOfferRepository extends JpaRepository<PersonalOffer, Long> {

    Page<PersonalOffer> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<PersonalOffer> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<PersonalOffer> findByStatusOrderByCreatedAtDesc(OfferStatus status, Pageable pageable);

    long countByUserIdAndStatus(Long userId, OfferStatus status);

    long countByUserIdAndStatusIn(Long userId, Collection<OfferStatus> statuses);

    Optional<PersonalOffer> findByIdAndUserId(Long id, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM PersonalOffer o WHERE o.id = :id")
    Optional<PersonalOffer> findByIdWithLock(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM PersonalOffer o WHERE o.id = :id AND o.user.id = :userId")
    Optional<PersonalOffer> findByIdAndUserIdWithLock(@Param("id") Long id, @Param("userId") Long userId);
}
