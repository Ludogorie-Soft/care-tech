package com.techstore.dto.response;

import com.techstore.entity.PersonalOffer;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PersonalOfferResponseDto {

    private Long id;
    private Long userId;
    private String userFullName;
    private String userEmail;
    private String title;
    private String message;
    private List<PersonalOffer.OfferItem> items;
    private BigDecimal discountPercent;
    private LocalDateTime expiresAt;
    private String status;
    private LocalDateTime createdAt;
    private boolean expired;

    public static PersonalOfferResponseDto from(PersonalOffer offer) {
        PersonalOfferResponseDto dto = new PersonalOfferResponseDto();
        dto.setId(offer.getId());
        dto.setUserId(offer.getUser().getId());
        dto.setUserFullName(offer.getUser().getFullName());
        dto.setUserEmail(offer.getUser().getEmail());
        dto.setTitle(offer.getTitle());
        dto.setMessage(offer.getMessage());
        dto.setItems(offer.getOfferItems());
        dto.setDiscountPercent(offer.getDiscountPercent());
        dto.setExpiresAt(offer.getExpiresAt());
        dto.setStatus(offer.getStatus().name());
        dto.setCreatedAt(offer.getCreatedAt());
        dto.setExpired(offer.getExpiresAt() != null && offer.getExpiresAt().isBefore(LocalDateTime.now()));
        return dto;
    }
}
