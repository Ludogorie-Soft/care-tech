package com.techstore.service;

import com.techstore.entity.Order;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailFormattingTest {

    @Nested
    class NoteHtml {

        @Test
        void blankNoteGivesNull() {
            assertThat(EmailFormatting.noteHtml(null)).isNull();
            assertThat(EmailFormatting.noteHtml("")).isNull();
            assertThat(EmailFormatting.noteHtml("  \n  ")).isNull();
        }

        @Test
        void plainTextIsTrimmedAndKeptAsIs() {
            assertThat(EmailFormatting.noteHtml("  Няма наличност.  ")).isEqualTo("Няма наличност.");
        }

        @Test
        void lineBreaksAreKept() {
            assertThat(EmailFormatting.noteHtml("Ред 1\r\nРед 2\nРед 3\rРед 4"))
                    .isEqualTo("Ред 1<br/>Ред 2<br/>Ред 3<br/>Ред 4");
        }

        @Test
        void htmlIsEscaped() {
            String html = EmailFormatting.noteHtml("<script>alert('x')</script> & <b>bold</b>");
            assertThat(html).doesNotContain("<script>", "<b>")
                    .contains("&lt;script&gt;", "&amp;", "&lt;b&gt;");
        }

        @Test
        void linksBecomeClickable() {
            String html = EmailFormatting.noteHtml("Отзив тук:\nhttps://g.page/r/CeArIZmgBuhGEBE/review");
            assertThat(html).isEqualTo("Отзив тук:<br/><a href=\"https://g.page/r/CeArIZmgBuhGEBE/review\" "
                    + "style=\"color:#ffffff; text-decoration:underline;\">https://g.page/r/CeArIZmgBuhGEBE/review</a>");
        }

        @Test
        void sentencePunctuationStaysOutsideTheLink() {
            String html = EmailFormatting.noteHtml("Вижте https://www.caretech.bg/promotions. Благодарим!");
            assertThat(html).contains("href=\"https://www.caretech.bg/promotions\"")
                    .contains("promotions</a>. Благодарим!");
        }

        @Test
        void ampersandInUrlIsEscapedInTheHref() {
            String html = EmailFormatting.noteHtml("https://www.pazaruvaj.com/s?a=1&b=2");
            assertThat(html).contains("href=\"https://www.pazaruvaj.com/s?a=1&amp;b=2\"");
        }

        @Test
        void quoteCannotBreakOutOfTheHref() {
            String html = EmailFormatting.noteHtml("https://evil.example/\"onmouseover=\"alert(1)");
            // The quote ends the URL, so the rest is escaped text, never an attribute
            assertThat(html).contains("href=\"https://evil.example/\"")
                    .doesNotContain("\"onmouseover=\"")
                    .contains("&quot;onmouseover=&quot;");
        }

        @Test
        void javascriptUrlsAreNotLinked() {
            String html = EmailFormatting.noteHtml("javascript:alert(1)");
            assertThat(html).doesNotContain("<a ").isEqualTo("javascript:alert(1)");
        }
    }

    @Nested
    class DeliveryLines {

        @Test
        void officeNameThatAlreadyNamesTheTownNeedsNoTownLine() {
            Order order = office("гр. Сливен [8800] пл. Васил Левски No 11", "гр. Сливен, Сливен, 8800", "8800");
            assertThat(EmailFormatting.deliveryLines(order))
                    .containsExactly("гр. Сливен [8800] пл. Васил Левски No 11");
        }

        @Test
        void officeNameWithoutTownGetsTheTownLine() {
            Order order = office("Спиди Банишора", "гр. СОФИЯ, СОФИЯ (СТОЛИЦА), 1000", "1000");
            assertThat(EmailFormatting.deliveryLines(order))
                    .containsExactly("Спиди Банишора", "гр. СОФИЯ, СОФИЯ (СТОЛИЦА), 1000");
        }

        @Test
        void streetAddressGetsTheTownWithoutRepeatingThePostalCode() {
            Order order = address("царибродска 82-84", "гр. СОФИЯ, СОФИЯ (СТОЛИЦА), 1000", "1000");
            assertThat(EmailFormatting.deliveryLines(order))
                    .containsExactly("царибродска 82-84", "гр. СОФИЯ, СОФИЯ (СТОЛИЦА), 1000");
        }

        @Test
        void postalCodeIsAddedWhenTheTownDoesNotHaveIt() {
            Order order = address("ул. Баба Илийца 1", "гр. Враца", "3000");
            assertThat(EmailFormatting.deliveryLines(order))
                    .containsExactly("ул. Баба Илийца 1", "гр. Враца, 3000");
        }

        @Test
        void fullAddressTypedIntoBothFieldsIsShownOnce() {
            Order order = address("Studentski Kompleks, ul. Akademik Yordan Trifonov 7b, 21, 1700 Sofia,",
                    "Studentski Kompleks, ul. Akademik Yordan Trifonov 7b, 21, 1700 Sofia, Bulgaria", null);
            assertThat(EmailFormatting.deliveryLines(order))
                    .containsExactly("Studentski Kompleks, ul. Akademik Yordan Trifonov 7b, 21, 1700 Sofia, Bulgaria");
        }

        @Test
        void missingPostalCodeIsNeverPrintedAsNull() {
            Order order = address("ул. Христо Ботев 2", "гр. Пловдив", null);
            assertThat(EmailFormatting.deliveryLines(order))
                    .containsExactly("ул. Христо Ботев 2", "гр. Пловдив");
        }

        @Test
        void officeFallsBackToTheShippingAddressAndSiteName() {
            Order order = office(null, null, null);
            order.setShippingAddress("Офис Враца Център");
            order.setShippingSpeedySiteName("гр. Враца");
            assertThat(EmailFormatting.deliveryLines(order)).containsExactly("Офис Враца Център", "гр. Враца");
        }

        @Test
        void leasingOrderWithoutAnyAddressGivesNoLines() {
            Order order = new Order();
            order.setShippingCountry(null);
            assertThat(EmailFormatting.deliveryLines(order)).isEmpty();
            assertThat(EmailFormatting.toSpeedyOffice(order)).isFalse();
        }

        private Order office(String officeName, String city, String postalCode) {
            Order order = new Order();
            order.setIsToSpeedyOffice(true);
            order.setShippingSpeedyOfficeName(officeName);
            order.setShippingAddress(officeName);
            order.setShippingCity(city);
            order.setShippingSpeedySiteName(city);
            order.setShippingPostalCode(postalCode);
            return order;
        }

        private Order address(String street, String city, String postalCode) {
            Order order = new Order();
            order.setIsToSpeedyOffice(false);
            order.setShippingAddress(street);
            order.setShippingCity(city);
            order.setShippingPostalCode(postalCode);
            return order;
        }
    }

    @Test
    void trackingUrlFillsInTheShipmentNumber() {
        assertThat(EmailFormatting.trackingUrl(" 61234567890 "))
                .isEqualTo("https://www.speedy.bg/public/index.php/bg/track-shipment?shipmentNumber=61234567890");
        assertThat(EmailFormatting.trackingUrl("AB 12&3"))
                .endsWith("?shipmentNumber=AB+12%263");
    }

    @Test
    void trackingUrlWithoutNumberIsTheTrackingPage() {
        assertThat(EmailFormatting.trackingUrl(null)).isEqualTo(EmailFormatting.SPEEDY_TRACKING_URL);
        assertThat(EmailFormatting.trackingUrl("  ")).isEqualTo(EmailFormatting.SPEEDY_TRACKING_URL);
    }

    @Test
    void deliveryDaysText() {
        assertThat(EmailFormatting.deliveryDaysText(1)).isEqualTo("до 1 работен ден");
        assertThat(EmailFormatting.deliveryDaysText(2)).isEqualTo("до 2 работни дни");
    }
}
