package com.techstore.service;

import com.techstore.dto.request.PersonalOfferCreateDto;
import com.techstore.dto.response.PersonalOfferResponseDto;
import com.techstore.entity.PersonalOffer;
import com.techstore.entity.PersonalOffer.OfferItem;
import com.techstore.entity.PersonalOffer.OfferStatus;
import com.techstore.entity.User;
import com.techstore.exception.ResourceNotFoundException;
import com.techstore.exception.UnauthorizedException;
import com.techstore.repository.PersonalOfferRepository;
import com.techstore.repository.UserRepository;
import com.techstore.util.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonalOfferService {

    private final PersonalOfferRepository offerRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SecurityHelper securityHelper;

    @Transactional
    public PersonalOfferResponseDto createAndSend(Long userId, PersonalOfferCreateDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        PersonalOffer offer = new PersonalOffer();
        offer.setUser(user);
        offer.setTitle(dto.getTitle());
        offer.setMessage(dto.getMessage());
        offer.setDiscountPercent(dto.getDiscountPercent());
        offer.setExpiresAt(dto.getExpiresAt());
        offer.setStatus(OfferStatus.PENDING);

        List<OfferItem> items = dto.getItems().stream().map(i -> {
            OfferItem item = new OfferItem();
            item.setProductId(i.getProductId());
            item.setProductName(i.getProductName());
            item.setImageUrl(i.getImageUrl());
            item.setOriginalPrice(i.getOriginalPrice());
            item.setOfferPrice(i.getOfferPrice());
            item.setQuantity(i.getQuantity());
            return item;
        }).toList();
        offer.setOfferItems(items);

        offer = offerRepository.save(offer);
        log.info("Created personal offer {} for user {}", offer.getId(), userId);

        emailService.sendPersonalOfferEmail(user, offer);

        return PersonalOfferResponseDto.from(offer);
    }

    public Page<PersonalOfferResponseDto> getAllOffers(String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            try {
                OfferStatus offerStatus = OfferStatus.valueOf(status.toUpperCase());
                return offerRepository.findByStatusOrderByCreatedAtDesc(offerStatus, pageable)
                        .map(PersonalOfferResponseDto::from);
            } catch (IllegalArgumentException ignored) {}
        }
        return offerRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(PersonalOfferResponseDto::from);
    }

    public Page<PersonalOfferResponseDto> getOffersForUser(Long userId, Pageable pageable) {
        return offerRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(PersonalOfferResponseDto::from);
    }

    public Page<PersonalOfferResponseDto> getCurrentUserOffers(Pageable pageable) {
        Long userId = securityHelper.getCurrentUserId();
        return offerRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(PersonalOfferResponseDto::from);
    }

    public long countUnreadForCurrentUser() {
        Long userId = securityHelper.getCurrentUserId();
        return offerRepository.countByUserIdAndStatusIn(userId, List.of(OfferStatus.PENDING, OfferStatus.READ));
    }

    @Transactional
    public PersonalOfferResponseDto markAsRead(Long offerId) {
        Long userId = securityHelper.getCurrentUserId();
        PersonalOffer offer = offerRepository.findByIdAndUserId(offerId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found: " + offerId));

        if (offer.getStatus() == OfferStatus.PENDING) {
            offer.setStatus(OfferStatus.READ);
            offer = offerRepository.save(offer);
        }
        return PersonalOfferResponseDto.from(offer);
    }

    @Transactional
    public PersonalOfferResponseDto updateStatus(Long offerId, String statusStr) {
        Long userId = securityHelper.getCurrentUserId();
        PersonalOffer offer = offerRepository.findByIdAndUserId(offerId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found: " + offerId));

        OfferStatus newStatus;
        try {
            newStatus = OfferStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new com.techstore.exception.ValidationException("Invalid status: " + statusStr);
        }

        if (newStatus != OfferStatus.ACCEPTED && newStatus != OfferStatus.REJECTED) {
            throw new UnauthorizedException("Users can only ACCEPT or REJECT offers");
        }

        offer.setStatus(newStatus);
        return PersonalOfferResponseDto.from(offerRepository.save(offer));
    }
}
