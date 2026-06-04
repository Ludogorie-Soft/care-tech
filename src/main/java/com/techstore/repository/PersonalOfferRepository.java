package com.techstore.repository;

import com.techstore.entity.PersonalOffer;
import com.techstore.entity.PersonalOffer.OfferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface PersonalOfferRepository extends JpaRepository<PersonalOffer, Long> {

    Page<PersonalOffer> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<PersonalOffer> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<PersonalOffer> findByStatusOrderByCreatedAtDesc(OfferStatus status, Pageable pageable);

    long countByUserIdAndStatus(Long userId, OfferStatus status);

    long countByUserIdAndStatusIn(Long userId, Collection<OfferStatus> statuses);

    Optional<PersonalOffer> findByIdAndUserId(Long id, Long userId);
}
