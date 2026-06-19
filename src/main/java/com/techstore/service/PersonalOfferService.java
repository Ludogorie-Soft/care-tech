package com.techstore.service;

import com.techstore.dto.request.AcceptOfferRequestDto;
import com.techstore.dto.request.ConvertOfferToOrderRequestDto;
import com.techstore.dto.request.OrderCreateRequestDTO;
import com.techstore.dto.request.PersonalOfferCreateDto;
import com.techstore.dto.response.OrderResponseDTO;
import com.techstore.dto.response.PersonalOfferResponseDto;
import com.techstore.entity.PersonalOffer;
import com.techstore.entity.PersonalOffer.OfferItem;
import com.techstore.entity.PersonalOffer.OfferStatus;
import com.techstore.entity.User;
import com.techstore.exception.BusinessLogicException;
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

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonalOfferService {

    private final PersonalOfferRepository offerRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SecurityHelper securityHelper;
    private final OrderService orderService;

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
            item.setDiscountPercent(i.getDiscountPercent());
            item.setQuantity(i.getQuantity());
            return item;
        }).toList();
        offer.setOfferItems(items);

        offer = offerRepository.save(offer);
        log.info("Created personal offer {} for user {}", offer.getId(), userId);

        emailService.sendPersonalOfferEmail(user, offer);

        return PersonalOfferResponseDto.from(offer);
    }

    @Transactional
    public PersonalOfferResponseDto acceptWithDetails(Long offerId, AcceptOfferRequestDto dto) {
        Long userId = securityHelper.getCurrentUserId();
        PersonalOffer offer = offerRepository.findByIdAndUserIdWithLock(offerId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found: " + offerId));

        if (offer.getStatus() == OfferStatus.REJECTED) {
            throw new BusinessLogicException("Cannot accept a rejected offer.");
        }
        if (offer.getStatus() == OfferStatus.ACCEPTED) {
            throw new BusinessLogicException("This offer has already been accepted.");
        }
        if (offer.getStatus() == OfferStatus.CONVERTED) {
            throw new BusinessLogicException("This offer has already been ordered.");
        }
        if (offer.getExpiresAt() != null && offer.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessLogicException("This offer has expired.");
        }

        PersonalOffer.ShippingDetails details = new PersonalOffer.ShippingDetails();
        details.setCustomerPhone(dto.getCustomerPhone());
        details.setShippingAddress(dto.getShippingAddress());
        details.setShippingCity(dto.getShippingCity());
        details.setShippingPostalCode(dto.getShippingPostalCode());
        details.setShippingCountry(dto.getShippingCountry() != null ? dto.getShippingCountry() : "Bulgaria");
        details.setIsToSpeedyOffice(dto.getIsToSpeedyOffice() != null ? dto.getIsToSpeedyOffice() : false);
        details.setShippingSpeedySiteId(dto.getShippingSpeedySiteId());
        details.setShippingSpeedyOfficeId(dto.getShippingSpeedyOfficeId());
        details.setShippingSpeedySiteName(dto.getShippingSpeedySiteName());
        details.setShippingSpeedyOfficeName(dto.getShippingSpeedyOfficeName());
        details.setPaymentMethod(dto.getPaymentMethod());
        details.setShippingMethod(dto.getShippingMethod());
        details.setCustomerCompany(dto.getCustomerCompany());
        details.setCustomerVatNumber(dto.getCustomerVatNumber());
        details.setCustomerVatRegistered(dto.getCustomerVatRegistered() != null ? dto.getCustomerVatRegistered() : false);
        details.setCustomerNotes(dto.getCustomerNotes());

        offer.setStatus(OfferStatus.ACCEPTED);
        offer.setShippingDetails(details);
        offer = offerRepository.save(offer);

        log.info("Offer {} accepted by user {} with shipping details", offerId, userId);
        return PersonalOfferResponseDto.from(offer);
    }

    @Transactional
    public OrderResponseDTO convertToOrder(Long offerId, ConvertOfferToOrderRequestDto dto) {
        PersonalOffer offer = offerRepository.findByIdWithLock(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found: " + offerId));

        if (offer.getStatus() == OfferStatus.REJECTED) {
            throw new BusinessLogicException("Cannot convert a rejected offer to an order.");
        }
        if (offer.getStatus() == OfferStatus.CONVERTED) {
            throw new BusinessLogicException("This offer has already been converted to an order.");
        }
        if (offer.getExpiresAt() != null && offer.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessLogicException("Cannot convert an expired offer to an order.");
        }
        if (offer.getOfferItems() == null || offer.getOfferItems().isEmpty()) {
            throw new BusinessLogicException("Offer has no items.");
        }

        User user = offer.getUser();

        List<OrderCreateRequestDTO.OrderItemRequestDTO> orderItems = offer.getOfferItems().stream()
                .map(i -> {
                    OrderCreateRequestDTO.OrderItemRequestDTO item = new OrderCreateRequestDTO.OrderItemRequestDTO();
                    item.setProductId(i.getProductId());
                    item.setQuantity(i.getQuantity() != null ? i.getQuantity() : 1);
                    item.setCustomPriceEuro(i.getOfferPrice());
                    return item;
                }).toList();

        OrderCreateRequestDTO orderRequest = new OrderCreateRequestDTO();
        orderRequest.setItems(orderItems);
        orderRequest.setCustomerFirstName(user.getFirstName());
        orderRequest.setCustomerLastName(user.getLastName());
        orderRequest.setCustomerEmail(user.getEmail());
        orderRequest.setCustomerPhone(dto.getCustomerPhone());
        orderRequest.setCustomerCompany(dto.getCustomerCompany());
        orderRequest.setCustomerVatNumber(dto.getCustomerVatNumber());
        orderRequest.setCustomerVatRegistered(dto.getCustomerVatRegistered() != null ? dto.getCustomerVatRegistered() : false);
        orderRequest.setShippingAddress(dto.getShippingAddress());
        orderRequest.setShippingCity(dto.getShippingCity());
        orderRequest.setShippingPostalCode(dto.getShippingPostalCode());
        orderRequest.setShippingCountry(dto.getShippingCountry() != null ? dto.getShippingCountry() : "Bulgaria");
        orderRequest.setPaymentMethod(dto.getPaymentMethod());
        orderRequest.setShippingMethod(dto.getShippingMethod());
        orderRequest.setIsToSpeedyOffice(dto.getIsToSpeedyOffice() != null ? dto.getIsToSpeedyOffice() : false);
        orderRequest.setShippingSpeedySiteId(dto.getShippingSpeedySiteId());
        orderRequest.setShippingSpeedyOfficeId(dto.getShippingSpeedyOfficeId());
        orderRequest.setShippingSpeedySiteName(dto.getShippingSpeedySiteName());
        orderRequest.setShippingSpeedyOfficeName(dto.getShippingSpeedyOfficeName());
        orderRequest.setCustomerNotes(dto.getCustomerNotes());
        orderRequest.setTermsAgreed(true);
        orderRequest.setInsuranceOffer(false);
        orderRequest.setInstallmentOffer(false);
        orderRequest.setIsLeasingOrder(false);
        orderRequest.setSkipCartClear(true);
        orderRequest.setUseSameAddressForBilling(true);

        OrderResponseDTO createdOrder = orderService.createOrder(orderRequest);

        offer.setStatus(OfferStatus.CONVERTED);
        offerRepository.save(offer);

        log.info("Offer {} converted to order {}", offerId, createdOrder.getOrderNumber());
        return createdOrder;
    }

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public Page<PersonalOfferResponseDto> getOffersForUser(Long userId, Pageable pageable) {
        return offerRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(PersonalOfferResponseDto::from);
    }

    @Transactional(readOnly = true)
    public Page<PersonalOfferResponseDto> getCurrentUserOffers(Pageable pageable) {
        Long userId = securityHelper.getCurrentUserId();
        return offerRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(PersonalOfferResponseDto::from);
    }

    @Transactional(readOnly = true)
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

        if (newStatus != OfferStatus.REJECTED) {
            throw new UnauthorizedException("Use the /accept endpoint to accept an offer with shipping details.");
        }

        offer.setStatus(newStatus);
        return PersonalOfferResponseDto.from(offerRepository.save(offer));
    }
}
