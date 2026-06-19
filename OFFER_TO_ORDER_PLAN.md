# План: Оферти с Продуктова Селекция, Per-Item Отстъпки и Конвертиране в Поръчка

> Статус: ✅ Завършено + Одит приключен  
> Последна актуализация: 2026-06-19  
> Билд проверка след всяка фаза преди продължаване.

---

## Обхват

1. Администраторът избира кои продукти от кошницата да влязат в офертата (не задължително всички).
2. За всеки продукт се задава индивидуална отстъпка (%) → автоматично се изчислява офертна цена.
3. Администраторът конвертира приета оферта директно в поръчка от admin панела.

---

## ФАЗА 1 — Backend: Per-Item Discount

### Задачи

- [x] **1.1** `PersonalOffer.OfferItem` (inner class) — добавяме поле `private BigDecimal discountPercent;`
- [x] **1.2** `PersonalOfferCreateDto.OfferItemDto` — добавяме `private BigDecimal discountPercent;`
- [x] **1.3** `PersonalOfferService.createAndSend()` — map-ваме `discountPercent` при изграждане на `OfferItem`

### Засегнати файлове
- `src/main/java/com/techstore/entity/PersonalOffer.java`
- `src/main/java/com/techstore/dto/request/PersonalOfferCreateDto.java`
- `src/main/java/com/techstore/service/PersonalOfferService.java`

### Бележки
- Без DB миграция — `offer_items` е JSONB, схемата е гъвкава.
- `PersonalOfferResponseDto` сериализира `List<PersonalOffer.OfferItem>` директно → новото поле ще се върне автоматично без промяна там.

### ✅ Билд проверка Фаза 1
- [x] `./mvnw clean package -DskipTests` минава без грешки

---

## ФАЗА 2 — Frontend: SendOfferModal редизайн

### Задачи

- [x] **2.1** `SendOfferModal.jsx` — добавяме `selected: true` към всеки item в state
- [x] **2.2** `SendOfferModal.jsx` — добавяме `discountPercent` към всеки item в state (default `""`)
- [x] **2.3** `SendOfferModal.jsx` — добавяме checkbox за всеки продукт (checked by default); деселектиран → изключен от офертата
- [x] **2.4** `SendOfferModal.jsx` — добавяме per-item discount % input:
  - При промяна на `discountPercent` → `offerPrice = originalPrice * (1 - discount/100)`
  - При директна промяна на `offerPrice` → `discountPercent = round((1 - offerPrice/originalPrice) * 100, 2)`
- [x] **2.5** `SendOfferModal.jsx` — премахваме глобалния `discountPercent` input (заменен от per-item)
- [x] **2.6** `SendOfferModal.jsx` — добавяме summary row: `"Общо оферта: X.XX € / X.XX лв."` (само selected items)
- [x] **2.7** `SendOfferModal.jsx` — submit изпраща само `selected === true` items

### Засегнати файлове
- `care-tech-ui/src/pages/admin/Customers/SendOfferModal.jsx`

### Бележки
- `UserCartModal.jsx` остава непроменен.
- `offersSlice.js` — `sendPersonalOffer` thunk не се променя (payload структурата е съвместима).

---

## ФАЗА 3 — Backend: Инфраструктура за конвертиране

### Задачи

- [x] **3.1** `OrderItemRequestDTO` (inner class на `OrderCreateRequestDTO`) — добавяме `private BigDecimal customPriceEuro;` (optional)
- [x] **3.2** `OrderCreateRequestDTO` — добавяме `private Boolean skipCartClear = false;`
- [x] **3.3** `OrderService.createOrder()` — условна цена на order item
- [x] **3.4** `OrderService.createOrder()` — skipCartClear логика

### Засегнати файлове
- `src/main/java/com/techstore/dto/request/OrderCreateRequestDTO.java`
- `src/main/java/com/techstore/service/OrderService.java`

### Бележки
- `customPriceEuro` е optional → съществуващи поръчки (нормален checkout) не се засягат.
- `skipCartClear = false` по default → съществуващото поведение е запазено.

### ✅ Билд проверка Фаза 3
- [x] `./mvnw clean package -DskipTests` минава без грешки

---

## ФАЗА 4 — Backend: CONVERTED статус + нов DTO

### Задачи

- [x] **4.1** `PersonalOffer.OfferStatus` — добавяме `CONVERTED` към enum
- [x] **4.2** Нов файл `ConvertOfferToOrderRequestDto.java`:
  ```java
  private String customerPhone;        // задължителен
  private String shippingAddress;      // задължителен
  private String shippingCity;         // задължителен
  private String shippingPostalCode;
  private String shippingCountry;      // default "Bulgaria"
  private PaymentMethod paymentMethod; // задължителен
  private ShippingMethod shippingMethod;
  private Boolean isToSpeedyOffice;
  private Long shippingSpeedySiteId;
  private Long shippingSpeedyOfficeId;
  private String shippingSpeedySiteName;
  private String shippingSpeedyOfficeName;
  private String customerNotes;
  ```

### Засегнати файлове
- `src/main/java/com/techstore/entity/PersonalOffer.java`
- `src/main/java/com/techstore/dto/request/ConvertOfferToOrderRequestDto.java` *(нов)*

### Бележки
- `status VARCHAR(20)` без CHECK constraint в DB → без Flyway миграция за новия статус.

### ✅ Билд проверка Фаза 4
- [x] `./mvnw clean package -DskipTests` минава без грешки

---

## ФАЗА 5 — Backend: Service метод + Endpoint

### Задачи

- [x] **5.1** `PersonalOfferService` — нов метод `convertToOrder(Long offerId, ConvertOfferToOrderRequestDto dto)`
- [x] **5.2** Inject `OrderService` в `PersonalOfferService` (добавяме в constructor чрез `@RequiredArgsConstructor`)
- [x] **5.3** `AdminController` — нов endpoint `POST /api/admin/offers/{offerId}/convert-to-order`

### Засегнати файлове
- `src/main/java/com/techstore/service/PersonalOfferService.java`
- `src/main/java/com/techstore/controller/AdminController.java`

### Бележки
- Ако продукт от офертата е изтрит от каталога → `ResourceNotFoundException` с ясно съобщение.
- Circular dependency риск: `PersonalOfferService` → `OrderService` → OK (OrderService не зависи от PersonalOfferService).

### ✅ Билд проверка Фаза 5
- [x] `./mvnw clean package -DskipTests` минава без грешки

---

## ФАЗА 6 — Frontend: Конвертиране в поръчка

### Задачи

- [x] **6.1** `offersSlice.js` — нов thunk `convertOfferToOrder`
- [x] **6.2** `offersSlice.js` — нов state: `convertStatus`, `convertedOrder`, `convertError`; reducer `resetConvertStatus`
- [x] **6.3** `offersSlice.js` — extraReducers за `convertOfferToOrder` (pending/fulfilled/rejected)
- [x] **6.4** `OffersLayout.jsx` — добавяме `CONVERTED` в `STATUS_CONFIG`
- [x] **6.5** `OffersLayout.jsx` — в `OfferDetailModal` добавяме бутон "Обърни в поръчка" (скрит за REJECTED/CONVERTED)
- [x] **6.6** Нов `ConvertOfferToOrderModal.jsx` — секция с offer items + форма + success state
- [x] **6.7** `OffersLayout.jsx` — при успешно конвертиране: refresh на офертите

### Засегнати файлове
- `care-tech-ui/src/redux/offersSlice.js`
- `care-tech-ui/src/pages/admin/Offers/OffersLayout.jsx`
- `care-tech-ui/src/pages/admin/Offers/ConvertOfferToOrderModal.jsx` *(нов)*

---

## Технически решения (резюме)

| Проблем | Решение |
|---------|---------|
| `createOrder()` изтрива кошницата | `skipCartClear = true` при конвертиране |
| Цените идват от DB, не от офертата | `customPriceEuro` в `OrderItemRequestDTO` |
| Discount е в цената → не дублираме | `discountAmount = ZERO` при `customPriceEuro != null` |
| CONVERTED статус в DB | `VARCHAR(20)` без CHECK → без миграция |
| JSONB schema за per-item discount | JSONB е schema-less → без миграция |
| Продукт изтрит след офертата | `ResourceNotFoundException` с ясно съобщение |

---

## Прогрес — Основни фази

| Фаза | Описание | Статус | Билд |
|------|----------|--------|------|
| 1 | Backend: per-item discountPercent | ✅ Завършено | ✅ |
| 2 | Frontend: SendOfferModal редизайн | ✅ Завършено | — |
| 3 | Backend: customPriceEuro + skipCartClear | ✅ Завършено | ✅ |
| 4 | Backend: CONVERTED статус + DTO | ✅ Завършено | ✅ |
| 5 | Backend: convertToOrder service + endpoint | ✅ Завършено | ✅ |
| 6 | Frontend: offersSlice + ConvertOfferToOrderModal | ✅ Завършено | — |

---

## ОДИТ — Намерени проблеми

> Открити след завършване на всички фази. Задължително се оправят преди deploy.

---

### 🔴 ОДИТ-1 — КРИТИЧНО: Price injection via публичен checkout

**Тежест:** Критична — сигурностна уязвимост  
**Засегнати файлове:** `OrderController.java:44`, `OrderService.java`

`customPriceEuro` е поле в `OrderItemRequestDTO`. `POST /api/orders` е `permitAll()` — всеки анонимен потребител може да изпрати `customPriceEuro: 0.01` и да създаде поръчка с произволна цена. Същото важи за `skipCartClear`.

**Fix:** В `OrderController.createOrder()` нулираме custom полетата преди подаване към service-а:
```java
request.getItems().forEach(item -> item.setCustomPriceEuro(null));
request.setSkipCartClear(false);
```

- [x] **О1.1** `OrderController.createOrder()` — нулиране на `customPriceEuro` и `skipCartClear`
- [x] ✅ Билд след О1

---

### 🔴 ОДИТ-2 — КРИТИЧНО: Frontend enum mismatch — конвертирането връща 400

**Тежест:** Критична — функционалността е напълно нефункционална  
**Засегнат файл:** `ConvertOfferToOrderModal.jsx:9-20`

Frontend изпраща стойности, несъществуващи в backend enum-ите:

| Frontend изпраща | Backend enum | Проблем |
|---|---|---|
| `"CASH"` | `PaymentMethod` | ❌ трябва `"CASH_ON_DELIVERY"` |
| `"CARD"` | `PaymentMethod` | ❌ трябва `"CREDIT_CARD"` или `"DEBIT_CARD"` |
| `"HOME_DELIVERY"` | `ShippingMethod` | ❌ трябва `"STANDARD"` |
| `"PICKUP"` | `ShippingMethod` | ❌ не съществува (enum: SPEEDY, FREE, STANDARD, EXPRESS) |

**Fix:** Синхронизиране с реалните enum стойности.

- [x] **О2.1** `ConvertOfferToOrderModal.jsx` — поправяне на `PAYMENT_OPTIONS` и `SHIPPING_OPTIONS`

---

### 🟠 ОДИТ-3 — ВИСОК: Race condition при двойно конвертиране

**Тежест:** Висока — дублирани поръчки при едновременни заявки  
**Засегнат файл:** `PersonalOfferService.java:73`

Два едновременни admin request-а минават проверката на статуса преди някой от тях да запише `CONVERTED`, и се създават две поръчки от една оферта.

**Fix:** Pessimistic lock в `PersonalOfferRepository` + нов query метод за fetch-with-lock:
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT o FROM PersonalOffer o WHERE o.id = :id")
Optional<PersonalOffer> findByIdWithLock(@Param("id") Long id);
```
И ползване на `findByIdWithLock` в `convertToOrder()`.

- [x] **О3.1** `PersonalOfferRepository.java` — добавяме `findByIdWithLock`
- [x] **О3.2** `PersonalOfferService.convertToOrder()` — ползваме `findByIdWithLock`
- [x] ✅ Билд след О3

---

### 🟠 ОДИТ-4 — ВИСОК: Нулева/отрицателна custom цена без валидация

**Тежест:** Висока — поръчка с нулева цена  
**Засегнат файл:** `OrderCreateRequestDTO.java` — `OrderItemRequestDTO.customPriceEuro`

Полето няма `@DecimalMin` constraint. Admin може да зададе `customPriceEuro = 0`.

**Fix:**
```java
@DecimalMin(value = "0.01", message = "Custom price must be greater than zero")
private BigDecimal customPriceEuro;
```

- [x] **О4.1** `OrderCreateRequestDTO.OrderItemRequestDTO` — добавяме `@DecimalMin(value = "0.01")` на `customPriceEuro`
- [x] ✅ Билд след О4

---

### 🟡 ОДИТ-5 — СРЕДЕН: Липсващ CONVERTED филтър

**Тежест:** Средна — UX пропуск  
**Засегнат файл:** `OffersLayout.jsx:16-22`

`STATUS_FILTERS` не включва `CONVERTED`. Администраторите не могат да филтрират само конвертираните оферти.

**Fix:** Добавяне на `{ value: "CONVERTED", label: "Конвертирани" }` към `STATUS_FILTERS`.

- [x] **О5.1** `OffersLayout.jsx` — добавяме CONVERTED в `STATUS_FILTERS`

---

### 🟡 ОДИТ-6 — СРЕДЕН: Глобален `discountPercent` на ниво оферта е сираче

**Тежест:** Средна — data inconsistency, объркване  
**Засегнати файлове:** `PersonalOffer.java`, `PersonalOfferResponseDto.java`, `OffersLayout.jsx`

DB колоната `discount_percent` и entity полето съществуват, но frontend вече не ги попълва. Старите оферти показват глобален %-badge; новите не показват нищо. Решение: запазваме полето в entity/DB (backwards compatibility), но премахваме badge-а от UI за новите оферти или го показваме само ако > 0.

**Fix:** В `OffersLayout.jsx` detail modal badge-ът `offer.discountPercent` вече е налично, само трябва да се остави — работи коректно (показва се само `{offer.discountPercent && ...}`). Няма нужда от backend промяна. Документираме като known state.

- [x] **О6.1** Документиране — без code промяна; полето е backwards-compatible

---

### 🟢 ОДИТ-7 — МИНОР: Неизползван import

**Тежест:** Минорна  
**Засегнат файл:** `PersonalOfferService.java:12`

```java
import com.techstore.enums.PaymentMethod; // не се използва
```

- [x] **О7.1** `PersonalOfferService.java` — премахване на неизползвания import

---

### 🟢 ОДИТ-8 — МИНОР: Липса `@Transactional(readOnly = true)` на read методи

**Тежест:** Минорна — performance  
**Засегнат файл:** `PersonalOfferService.java`

`getAllOffers()`, `getOffersForUser()`, `getCurrentUserOffers()`, `countUnreadForCurrentUser()` — без `readOnly = true`.

- [x] **О8.1** `PersonalOfferService.java` — добавяме `@Transactional(readOnly = true)` на четирите read метода

---

## Прогрес — Одит

| ID | Тежест | Описание | Статус | Билд |
|----|--------|----------|--------|------|
| О1 | 🔴 Критично | Price injection на публичен endpoint | ✅ | ✅ |
| О2 | 🔴 Критично | Enum mismatch — 400 при конвертиране | ✅ | — |
| О3 | 🟠 Висок | Race condition при двойно конвертиране | ✅ | ✅ |
| О4 | 🟠 Висок | Нулева custom цена без валидация | ✅ | ✅ |
| О5 | 🟡 Среден | Липсва CONVERTED филтър | ✅ | — |
| О6 | 🟡 Среден | Глобален discountPercent — сираче | ✅ | — |
| О7 | 🟢 Минор | Неизползван import | ✅ | ✅ |
| О8 | 🟢 Минор | Липса readOnly на read методи | ✅ | ✅ |
