# План: Редактиране на поръчки от администратор

## Цел
Администраторът да може да редактира всички значими полета на поръчка от admin панела.

---

## Бекенд

### Стъпка 1 — `OrderUpdateRequestDTO`
- [x] Нов DTO с patch-семантика (всички полета nullable)
- Полета: status, paymentStatus, trackingNumber, adminNotes,
  customerFirstName, customerLastName, customerEmail, customerPhone,
  customerCompany, customerVatNumber,
  shippingAddress, shippingCity, shippingPostalCode, shippingCountry,
  invoiceNumber, invoiceDate, shippingCost

### Стъпка 2 — `OrderService.updateOrder()`
- [x] Нов метод с patch-семантика (null = не променяй)
- [x] При промяна на status → стреля `OrderStatusChangedEvent`
- [x] status = SHIPPED → auto-set shippedAt
- [x] status = DELIVERED → auto-set deliveredAt + paymentStatus = PAID
- [x] При промяна на shippingCost → recalculate totals

### Стъпка 3 — `AdminController` — нов endpoint
- [x] `PUT /api/admin/orders/{orderId}` → updateOrder()
- [x] Съществуващият `PUT /api/admin/orders/{orderId}/status` остава (backwards compat)

### Стъпка 4 — `OrderResponseDTO`
- [x] Добавяне на `paymentMethod` в response (вече присъства)
- [x] Проверка че всички новоредактируеми полета са включени в response-а — OK

---

## Фронтенд

### Стъпка 5 — `orderSlice.js` — нов thunk
- [x] `updateOrder` thunk → `PUT /api/admin/orders/{orderId}`
- [x] Добавяне в extraReducers (pending / fulfilled / rejected)

### Стъпка 6 — `EditOrderModal.jsx` — нов компонент
- [x] 4 секции (tabs):
  - **Статус**: status, paymentStatus, paymentMethod, trackingNumber, adminNotes
  - **Клиент**: firstName, lastName, email, phone, company, vatNumber
  - **Доставка**: address, city, postalCode, country, shippingCost
  - **Фактура**: invoiceNumber, invoiceDate
- [x] Валидация на задължителни полета (email)
- [x] Submit → dispatch(updateOrder) → onSuccess → refresh order

### Стъпка 7 — `OrderDetailPage.jsx` — интеграция
- [x] Добавяне на бутон "Редактирай поръчка"
- [x] Импорт и рендиране на `EditOrderModal`

---

## Правила
- Patch-семантика навсякъде: null/undefined поле = не пипай
- Build check след всяка бекенд стъпка
- Съществуващият UpdateOrderStatusModal и /status endpoint остават непроменени
- Не се редактират: продукти в поръчката, subtotal, taxAmount, total, orderNumber, createdAt
