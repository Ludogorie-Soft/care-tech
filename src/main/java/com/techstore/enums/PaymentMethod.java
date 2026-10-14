package com.techstore.enums;

public enum PaymentMethod {
    CASH_ON_DELIVERY("Наложен платеж"),
    BANK_TRANSFER("Банков превод"),
    CREDIT_CARD("Кредитна карта"),
    DEBIT_CARD("Дебитна карта"),
    EPAY("ePay"),
    EASYPAY("EasyPay"),
    PAYPAL("PayPal"),
    TBI_LEASING("TBI Лизинг");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
