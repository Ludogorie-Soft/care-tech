package com.techstore.service;

import com.techstore.config.ShippingConfig;
import com.techstore.entity.CartItem;
import com.techstore.entity.Order;
import com.techstore.entity.OrderItem;
import com.techstore.entity.PersonalOffer;
import com.techstore.entity.Product;
import com.techstore.entity.User;
import com.techstore.enums.OrderStatus;
import com.techstore.enums.PaymentMethod;
import com.techstore.enums.ShippingMethod;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Renders every customer email through EmailService with the real templates and checks what the
 * client asked for: working links, real contact data, the logo, the current year, the admin's note,
 * the delivery address and euro-only prices.
 */
class EmailServiceTemplatesTest {

    private static final String APP_URL = "https://www.caretech.bg";
    private static final String LOGO_URL = APP_URL + "/email-logo.png";
    private static final String REVIEW_URL = "https://g.page/r/CeArIZmgBuhGEBE/review";
    private static final String SPEEDY_URL = "https://www.speedy.bg/public/index.php/bg/track-shipment";

    /** Routes that exist in care-tech-ui/src/App.js; emails must not link anywhere else on the site. */
    private static final Set<String> SITE_ROUTES = Set.of("", "/contacts", "/about", "/policy", "/cart",
            "/profile/my-orders", "/profile/my-offers");
    private static final Pattern ORDER_ROUTE = Pattern.compile("/orders/\\d+");
    private static final Pattern RESET_ROUTE = Pattern.compile("/reset-password\\?token=[\\w-]+");
    private static final Pattern HREF = Pattern.compile("href=\"([^\"]*)\"");

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        ShippingConfig shippingConfig = new ShippingConfig();
        ReflectionTestUtils.setField(shippingConfig, "deliveryDays", 2);

        emailService = new EmailService(mailSender, engine, mock(S3Service.class), shippingConfig);
        ReflectionTestUtils.setField(emailService, "fromEmail", "orders@caretech.bg");
        ReflectionTestUtils.setField(emailService, "appName", "CareTech");
        ReflectionTestUtils.setField(emailService, "appUrl", APP_URL);
        ReflectionTestUtils.setField(emailService, "infoEmail", "info@caretech.bg");
        ReflectionTestUtils.setField(emailService, "logoUrl", LOGO_URL);
        ReflectionTestUtils.setField(emailService, "reviewUrl", REVIEW_URL);

        when(mailSender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage((Session) null));
    }

    // ── Order emails ────────────────────────────────────────────────────────────

    @Test
    void confirmationShowsEuroOnlyAndTheOffice() throws Exception {
        emailService.sendOrderConfirmationEmail(officeOrder());
        String html = sentHtml();

        assertCustomerEmail(html);
        assertThat(html).contains("100.00 €", "20.00 €", "120.00 €", "БЕЗПЛАТНО")
                .contains("Офис Speedy:", "гр. Сливен [8800] пл. Васил Левски No 11")
                .contains(">Speedy<")
                .doesNotContain("SPEEDY", "8800, 8800", APP_URL + "/terms")
                .contains("href=\"" + APP_URL + "/policy\"");
    }

    @Test
    void confirmationForAddressDelivery() throws Exception {
        emailService.sendOrderConfirmationEmail(addressOrder());
        String html = sentHtml();

        assertCustomerEmail(html);
        assertThat(html).contains("Адрес за доставка", "царибродска 82-84", "гр. СОФИЯ, СОФИЯ (СТОЛИЦА), 1000")
                .contains("Според тарифите")
                .doesNotContain("Офис Speedy:");
    }

    @Test
    void adminNotificationShowsEuroOnly() throws Exception {
        emailService.sendNewOrderNotificationToAdmin(officeOrder());
        String html = sentHtml();

        assertThat(html).contains("src=\"" + LOGO_URL + "\"", "120.00 €", ">Speedy<")
                .doesNotContain("лв", "BGN", "SPEEDY", "CareTech</h1>");
    }

    @Test
    void statusUpdateShowsTheNoteAsInformation() throws Exception {
        Order order = officeOrder();
        order.setStatus(OrderStatus.PROCESSING);
        order.setTrackingNumber(null);

        emailService.sendOrderStatusUpdateEmail(order, OrderStatus.CONFIRMED, "Ще се обадим утре.\nБлагодарим!");
        String html = sentHtml();

        assertCustomerEmail(html);
        assertThat(html).contains("Информация", "Ще се обадим утре.<br/>Благодарим!")
                .contains("Предишен статус", "Потвърдена", "В обработка", "Промяна в статуса")
                .doesNotContain("Номер за проследяване")
                .contains("href=\"" + APP_URL + "/profile/my-orders\"");
    }

    @Test
    void statusUpdateForTheSameStatusDoesNotSayItChanged() throws Exception {
        Order order = officeOrder();
        order.setStatus(OrderStatus.PROCESSING);

        emailService.sendOrderStatusUpdateEmail(order, OrderStatus.PROCESSING, null);
        String html = sentHtml();

        assertCustomerEmail(html);
        assertThat(html).contains("Актуализация на поръчката", "Текущ статус")
                .doesNotContain("Предишен статус", "Информация");
    }

    @Test
    void shippedToOfficeLinksTrackingAndShowsTheOffice() throws Exception {
        emailService.sendOrderShippedEmail(officeOrder(), "Очаквайте SMS от Speedy.");
        String html = sentHtml();

        assertCustomerEmail(html);
        assertThat(html).contains("href=\"" + SPEEDY_URL + "?shipmentNumber=61234567890\"")
                .contains("Номер за проследяване", "61234567890")
                .contains("Офис на Speedy:", "гр. Сливен [8800] пл. Васил Левски No 11")
                .contains("до 2 работни дни", ">Speedy<")
                .contains("Пратката ще пристигне в избрания офис на Speedy")
                .contains("Информация", "Очаквайте SMS от Speedy.")
                .doesNotContain("SPEEDY", "1-3 работни дни", "на посочения адрес има някой", "Адрес:<");
    }

    @Test
    void shippedToAddressWithoutTrackingNumber() throws Exception {
        Order order = addressOrder();
        order.setTrackingNumber("  ");

        emailService.sendOrderShippedEmail(order, "   ");
        String html = sentHtml();

        assertCustomerEmail(html);
        assertThat(html).contains("href=\"" + SPEEDY_URL + "\"")
                .doesNotContain("shipmentNumber", "Номер за проследяване", "Информация")
                .contains("Адрес за доставка:", "царибродска 82-84", "гр. СОФИЯ, СОФИЯ (СТОЛИЦА), 1000")
                .contains("на посочения адрес има някой");
    }

    @Test
    void deliveredLinksTheGoogleReviewAndShowsTheNote() throws Exception {
        Order order = officeOrder();
        order.setStatus(OrderStatus.DELIVERED);

        emailService.sendOrderDeliveredEmail(order,
                "Благодарим Ви!\nОставете отзив: https://g.page/r/CeArIZmgBuhGEBM/review");
        String html = sentHtml();

        assertCustomerEmail(html);
        // Five stars and the button
        assertThat(countOccurrences(html, "href=\"" + REVIEW_URL + "\"")).isEqualTo(6);
        assertThat(html).contains("Оставете отзив", "Информация", "Благодарим Ви!<br/>")
                .contains("<a href=\"https://g.page/r/CeArIZmgBuhGEBM/review\"");
    }

    @Test
    void deliveredWithoutNoteHasNoInformationBlock() throws Exception {
        emailService.sendOrderDeliveredEmail(officeOrder(), null);
        String html = sentHtml();

        assertCustomerEmail(html);
        assertThat(html).doesNotContain("Информация");
    }

    @Test
    void cancelledHasRealContactsAndTheReason() throws Exception {
        Order order = officeOrder();
        order.setStatus(OrderStatus.CANCELLED);

        emailService.sendOrderCancelledEmail(order, "Няма наличност.");
        String html = sentHtml();

        assertCustomerEmail(html);
        assertThat(html).contains("Причина за отмяна", "Няма наличност.")
                .contains("href=\"mailto:info@caretech.bg\"", "href=\"tel:+359899850777\"", "0899 850 777")
                .contains("Възстановяване", "Нова поръчка")
                .contains("Свържете се с нас")
                .doesNotContain("Чат", "Потвърждение", "support@caretech.com", "888123456", "Информация");
    }

    @Test
    void cancelledWithoutReasonHasNoReasonBlock() throws Exception {
        emailService.sendOrderCancelledEmail(officeOrder(), null);
        String html = sentHtml();

        assertCustomerEmail(html);
        assertThat(html).doesNotContain("Причина за отмяна");
    }

    @Test
    void leasingRejected() throws Exception {
        emailService.sendLeasingRejectedEmail(officeOrder(), "Лаптоп");
        String html = sentHtml();

        assertCustomerEmail(html);
        assertThat(html).contains("TBI Fusion Pay", "href=\"" + APP_URL + "/profile/my-orders\"");
    }

    // ── Account and marketing emails ────────────────────────────────────────────

    @Test
    void passwordReset() throws Exception {
        emailService.sendPasswordResetEmail("client@example.com", "abc-123");
        String html = sentHtml();

        assertCustomerEmail(html);
        assertThat(html).contains(APP_URL + "/reset-password?token=abc-123", "автоматично генерирано");
    }

    @Test
    void passwordChanged() throws Exception {
        emailService.sendPasswordChangedEmail("client@example.com", "Иван");
        String html = sentHtml();

        assertCustomerEmail(html);
        assertThat(html).contains("автоматично генерирано");
    }

    @Test
    void abandonedCartShowsThePriceWithVat() throws Exception {
        Product product = new Product();
        product.setId(7L);
        product.setNameBg("Рутер");
        product.setFinalPrice(new BigDecimal("100.00"));
        product.setPrimaryImageUrl("/uploads/router.jpg");
        CartItem item = new CartItem();
        item.setProduct(product);
        item.setQuantity(1);

        emailService.sendAbandonedCartEmail(user(), List.of(item));
        String html = sentHtml();

        assertCustomerEmail(html);
        assertThat(html).contains("120.00", "href=\"" + APP_URL + "/cart\"")
                .contains("src=\"" + APP_URL + "/api/images/product/7/primary\"");
    }

    @Test
    void personalOfferCrossesOutTheOldPriceWithVat() throws Exception {
        PersonalOffer.OfferItem discounted = new PersonalOffer.OfferItem();
        discounted.setProductName("Монитор");
        discounted.setOriginalPrice(new BigDecimal("100.00")); // without VAT → 120.00 € on the site
        discounted.setOfferPrice(new BigDecimal("110.00"));    // with VAT
        PersonalOffer.OfferItem samePrice = new PersonalOffer.OfferItem();
        samePrice.setProductName("Мишка");
        samePrice.setOriginalPrice(new BigDecimal("10.00"));
        samePrice.setOfferPrice(new BigDecimal("12.00"));

        PersonalOffer offer = new PersonalOffer();
        offer.setTitle("Оферта");
        offer.setOfferItems(List.of(discounted, samePrice));

        emailService.sendPersonalOfferEmail(user(), offer);
        String html = sentHtml();

        assertCustomerEmail(html);
        assertThat(html).contains(">120.00 €<", ">110.00 €<", ">12.00 €<")
                .doesNotContain(">10.00 €<", ">100.00 €<")
                .contains("href=\"" + APP_URL + "/profile/my-offers\"");
        // Same price with VAT → nothing crossed out for the mouse
        assertThat(countOccurrences(html, ">12.00 €<")).isEqualTo(1);
    }

    // ── Shared checks ───────────────────────────────────────────────────────────

    /** What every customer email must have after the client's review. */
    private void assertCustomerEmail(String html) {
        assertThat(html).contains("src=\"" + LOGO_URL + "\"")
                .doesNotContain("CareTech</h1>")
                .contains("href=\"" + APP_URL + "/contacts\"", "href=\"" + APP_URL + "/about\"", ">За нас<")
                .doesNotContain("Поддръжка", "/support", "/contact\"", "href=\"#\"")
                .doesNotContain("лв", "BGN", "1.95583")
                .doesNotContain("null");

        Matcher year = Pattern.compile("&copy; <span>(\\d{4})</span>").matcher(html);
        assertThat(year.find()).as("copyright line").isTrue();
        assertThat(year.group(1)).isEqualTo(String.valueOf(Year.now(ZoneId.of("Europe/Sofia")).getValue()));

        Matcher href = HREF.matcher(html);
        while (href.find()) {
            String url = href.group(1);
            if (url.startsWith(APP_URL)) {
                String path = url.substring(APP_URL.length());
                assertThat(SITE_ROUTES.contains(path) || ORDER_ROUTE.matcher(path).matches()
                        || RESET_ROUTE.matcher(path).matches())
                        .as("link to a page that exists: %s", url).isTrue();
            }
        }
    }

    /** The send methods catch their own exceptions, so always check the email actually went out. */
    private String sentHtml() throws Exception {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        String html = htmlOf(captor.getValue().getContent());
        assertThat(html).as("HTML body").isNotNull();
        return html;
    }

    private static String htmlOf(Object content) throws Exception {
        if (content instanceof String text) {
            return text.contains("<html") ? text : null;
        }
        if (content instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                String html = htmlOf(multipart.getBodyPart(i).getContent());
                if (html != null) {
                    return html;
                }
            }
        }
        return null;
    }

    private static int countOccurrences(String text, String part) {
        int count = 0;
        for (int i = text.indexOf(part); i >= 0; i = text.indexOf(part, i + part.length())) {
            count++;
        }
        return count;
    }

    // ── Fixtures, shaped like real production orders ────────────────────────────

    private static Order officeOrder() {
        Order order = baseOrder(43L);
        order.setIsToSpeedyOffice(true);
        order.setShippingSpeedyOfficeName("гр. Сливен [8800] пл. Васил Левски No 11");
        order.setShippingAddress("гр. Сливен [8800] пл. Васил Левски No 11");
        order.setShippingCity("гр. Сливен, Сливен, 8800");
        order.setShippingSpeedySiteName("гр. Сливен, Сливен, 8800");
        order.setShippingPostalCode("8800");
        return order;
    }

    private static Order addressOrder() {
        Order order = baseOrder(20L);
        order.setIsToSpeedyOffice(false);
        order.setShippingAddress("царибродска 82-84");
        order.setShippingCity("гр. СОФИЯ, СОФИЯ (СТОЛИЦА), 1000");
        order.setShippingPostalCode("1000");
        return order;
    }

    private static Order baseOrder(Long id) {
        Order order = new Order();
        order.setId(id);
        order.setCreatedAt(LocalDateTime.of(2026, 10, 14, 11, 0));
        order.setOrderNumber("ORD-2026-000" + id);
        order.setStatus(OrderStatus.SHIPPED);
        order.setCustomerFirstName("Иван");
        order.setCustomerLastName("Петров");
        order.setCustomerEmail("client@example.com");
        order.setCustomerPhone("0888000000");
        order.setShippingMethod(ShippingMethod.SPEEDY);
        order.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        order.setTrackingNumber("61234567890");

        OrderItem item = new OrderItem();
        item.setProductName("Лаптоп");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("100.00"));
        item.setLineTotal(new BigDecimal("100.00"));
        List<OrderItem> items = new ArrayList<>();
        items.add(item);
        order.setOrderItems(items);

        order.setSubtotal(new BigDecimal("100.00"));
        order.setTaxAmount(new BigDecimal("20.00"));
        order.setShippingCost(BigDecimal.ZERO);
        order.setTotal(new BigDecimal("120.00"));
        return order;
    }

    private static User user() {
        User user = new User();
        user.setFirstName("Иван");
        user.setLastName("Петров");
        user.setEmail("client@example.com");
        return user;
    }
}
