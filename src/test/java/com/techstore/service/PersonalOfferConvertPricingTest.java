package com.techstore.service;

import com.techstore.dto.request.ConvertOfferToOrderRequestDto;
import com.techstore.dto.request.OrderCreateRequestDTO;
import com.techstore.dto.request.OrderCreateRequestDTO.OrderItemRequestDTO;
import com.techstore.dto.response.OrderResponseDTO;
import com.techstore.entity.PersonalOffer;
import com.techstore.entity.PersonalOffer.OfferItem;
import com.techstore.entity.PersonalOffer.OfferStatus;
import com.techstore.entity.User;
import com.techstore.enums.PaymentMethod;
import com.techstore.repository.CartItemRepository;
import com.techstore.repository.PersonalOfferRepository;
import com.techstore.repository.UserRepository;
import com.techstore.util.SecurityHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The contract the admin offer modal relies on: originalPrice is the product price WITHOUT VAT,
 * offerPrice is what the customer pays WITH VAT, and the order gets the price without VAT.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PersonalOfferService.convertToOrder — цени без/с ДДС")
class PersonalOfferConvertPricingTest {

    @Mock PersonalOfferRepository offerRepository;
    @Mock UserRepository userRepository;
    @Mock EmailService emailService;
    @Mock SecurityHelper securityHelper;
    @Mock OrderService orderService;
    @Mock CartItemRepository cartItemRepository;

    @InjectMocks PersonalOfferService service;

    @Test
    @DisplayName("Оферта на цената от сайта (с ДДС) → цена без ДДС в поръчката, без отстъпка")
    void untouchedOfferKeepsTheSitePrice() {
        OrderItemRequestDTO item = convert(item(7L, "100.00", "120.00"));

        assertThat(item.getCustomPriceEuro()).isEqualByComparingTo("100.00");
        assertThat(item.getDiscountPercent()).isNull();
    }

    @Test
    @DisplayName("10 % отстъпка от цената с ДДС → 90.00 без ДДС и 10 % в поръчката")
    void discountIsMeasuredOnThePriceWithVat() {
        OrderItemRequestDTO item = convert(item(7L, "100.00", "108.00"));

        assertThat(item.getCustomPriceEuro()).isEqualByComparingTo("90.00");
        assertThat(item.getDiscountPercent()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("Старият бъг: офертна цена = цената без ДДС → клиентът плаща ~16.67 % по-малко")
    void preVatOfferPriceIsADiscount() {
        // This is what the modal used to send by default; the backend rightly reads it as a discount
        OrderItemRequestDTO item = convert(item(7L, "100.00", "100.00"));

        assertThat(item.getCustomPriceEuro()).isEqualByComparingTo("83.33");
        assertThat(item.getDiscountPercent()).isEqualByComparingTo("16.67");
    }

    private OrderItemRequestDTO convert(OfferItem offerItem) {
        User user = new User();
        user.setId(5L);
        user.setFirstName("Иван");
        user.setLastName("Петров");
        user.setEmail("client@example.com");

        PersonalOffer offer = new PersonalOffer();
        offer.setId(1L);
        offer.setUser(user);
        offer.setStatus(OfferStatus.ACCEPTED);
        offer.setOfferItems(List.of(offerItem));
        when(offerRepository.findByIdWithLock(1L)).thenReturn(Optional.of(offer));
        when(orderService.createOrder(any())).thenReturn(OrderResponseDTO.builder().orderNumber("ORD-1").build());

        ConvertOfferToOrderRequestDto dto = new ConvertOfferToOrderRequestDto();
        dto.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        service.convertToOrder(1L, dto);

        ArgumentCaptor<OrderCreateRequestDTO> captor = ArgumentCaptor.forClass(OrderCreateRequestDTO.class);
        verify(orderService).createOrder(captor.capture());
        return captor.getValue().getItems().get(0);
    }

    private static OfferItem item(Long productId, String originalWithoutVat, String offerWithVat) {
        OfferItem item = new OfferItem();
        item.setProductId(productId);
        item.setProductName("Лаптоп");
        item.setOriginalPrice(new BigDecimal(originalWithoutVat));
        item.setOfferPrice(new BigDecimal(offerWithVat));
        item.setQuantity(1);
        return item;
    }
}
