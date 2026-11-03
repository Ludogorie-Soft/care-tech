package com.techstore.service;

import com.techstore.dto.request.CartItemRequestDto;
import com.techstore.dto.response.CartItemResponseDto;
import com.techstore.dto.response.CartSummaryDto;
import com.techstore.dto.response.ProductSummaryDto;
import com.techstore.entity.CartItem;
import com.techstore.entity.Product;
import com.techstore.entity.User;
import com.techstore.enums.ProductStatus;
import com.techstore.exception.BusinessLogicException;
import com.techstore.exception.ResourceNotFoundException;
import com.techstore.mapper.ParameterMapper;
import com.techstore.repository.CartItemRepository;
import com.techstore.repository.ProductRepository;
import com.techstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ParameterMapper parameterMapper;

    @Transactional(readOnly = true)
    public CartSummaryDto getCartSummary(Long userId, String language) {
        return summarize(userId, language, false);
    }

    /**
     * The customer's cart as the admin sees it: what can be bought, plus what the customer put in and
     * can no longer buy (a product that went unavailable stays in cart_items), listed apart.
     */
    @Transactional(readOnly = true)
    public CartSummaryDto getCartSummaryForAdmin(Long userId, String language) {
        return summarize(userId, language, true);
    }

    private CartSummaryDto summarize(Long userId, String language, boolean withUnavailable) {
        Map<Boolean, List<CartItem>> byAvailability = cartItemRepository.findByUserIdOrderByCreatedAtAsc(userId)
                .stream()
                .filter(ci -> ci.getProduct() != null)
                .collect(Collectors.partitioningBy(ci -> ProductService.isVisibleOnSite(ci.getProduct())));

        List<CartItem> availableItems = byAvailability.get(true);
        List<CartItemResponseDto> itemDtos = availableItems.stream()
                .map(cartItem -> mapToCartItemResponse(cartItem, cartItem.getProduct(), language))
                .toList();

        CartSummaryDto summary = new CartSummaryDto();
        summary.setItems(itemDtos);
        summary.setTotalItems(availableItems.stream().mapToInt(CartItem::getQuantity).sum());
        // From the listed items only: a sum over every row priced products the customer cannot buy.
        summary.setTotalPrice(itemDtos.stream()
                .map(CartItemResponseDto::getItemTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        if (withUnavailable) {
            summary.setUnavailableItems(byAvailability.get(false).stream()
                    .map(cartItem -> mapToCartItemResponse(cartItem, cartItem.getProduct(), language))
                    .toList());
        }

        return summary;
    }

    @Transactional
    public CartItemResponseDto addToCart(Long userId, CartItemRequestDto request, String language) {
        log.info("Adding product {} to cart for user {}", request.getProductId(), userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        if (product.getStatus() != ProductStatus.AVAILABLE
                || !Boolean.TRUE.equals(product.getActive())
                || !Boolean.TRUE.equals(product.getShow())) {
            throw new BusinessLogicException("Продуктът не е наличен.");
        }

        Optional<CartItem> existingItem = cartItemRepository.findByUserIdAndProductId(userId, request.getProductId());

        CartItem cartItem;
        if (existingItem.isPresent()) {
            cartItem = existingItem.get();
            cartItem.setQuantity(cartItem.getQuantity() + request.getQuantity());
        } else {
            cartItem = new CartItem();
            cartItem.setUser(user);
            cartItem.setProduct(product);
            cartItem.setQuantity(request.getQuantity());
        }

        cartItem = cartItemRepository.save(cartItem);

        CartItemResponseDto dto = mapToCartItemResponse(cartItem, product, language);

        return dto;
    }

    @Transactional
    public CartItemResponseDto updateCartItem(Long userId, Long cartItemId, Integer quantity, String language) {
        log.info("Updating cart item {} for user {} with quantity {}", cartItemId, userId, quantity);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));

        if (!cartItem.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Cart item not found for this user");
        }

        cartItem.setQuantity(quantity);
        cartItem = cartItemRepository.save(cartItem);

        return mapToCartItemResponse(cartItem, cartItem.getProduct(), language);
    }

    @Transactional
    public void removeFromCart(Long userId, Long cartItemId) {
        log.info("Removing cart item {} for user {}", cartItemId, userId);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));

        if (!cartItem.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Cart item not found for this user");
        }

        cartItemRepository.delete(cartItem);
    }

    @Transactional
    public void clearCart(Long userId) {
        log.info("Clearing cart for user {}", userId);
        cartItemRepository.deleteByUserId(userId);
    }

    public CartItemResponseDto mapToCartItemResponse(CartItem cartItem, Product cartItem1, String language) {
        CartItemResponseDto dto = new CartItemResponseDto();
        dto.setId(cartItem.getId());
        dto.setProduct(convertProductToResponseDTO(cartItem.getProduct(), language));
        dto.setQuantity(cartItem.getQuantity());
        // An unavailable product in the admin's view may have no price left.
        dto.setItemTotal(cartItem1.getFinalPrice() == null ? null
                : cartItem1.getFinalPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        dto.setCreatedAt(cartItem.getCreatedAt());
        dto.setUpdatedAt(cartItem.getUpdatedAt());
        return dto;
    }

    private ProductSummaryDto convertProductToResponseDTO(Product product, String lang) {
        ProductSummaryDto dto = new ProductSummaryDto();
        dto.setId(product.getId());
        dto.setNameEn(product.getNameEn());
        dto.setNameBg(product.getNameBg());
        dto.setFinalPrice(product.getFinalPrice());
        dto.setDiscount(product.getDiscount());

        dto.setPrimaryImageUrl(product.getPrimaryImageUrl());

//        if (product.getPrimaryImageUrl() != null) {
//            dto.setPrimaryImageUrl("/api/images/product/" + product.getId() + "/primary");
//        }
        return dto;
    }
}