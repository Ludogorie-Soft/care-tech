# План: Функционалност за Сравнение на Продукти

## Общ преглед

Сравнението на продукти е **изцяло frontend** функционалност — не се налагат промени в бекенда. Продуктовите данни (включително `specifications`) вече се връщат от `GET /api/products/{id}?language=bg`. Списъкът за сравнение се съхранява в Redux + `localStorage`.

**Лимит:** максимум 4 продукта едновременно.

---

## Архитектурни решения

| Въпрос | Решение |
|---|---|
| Съхранение | Redux slice + localStorage (без persist middleware — ръчно) |
| Backend промени | Няма — данните са налични |
| Максимум продукти | 4 (индустриален стандарт) |
| Достъпност | Публична страница, не изисква login |
| Маршрут | `/compare` |

---

## Засегнати файлове

### Нови файлове (Frontend — `care-tech-ui`)
```
src/redux/compareSlice.js
src/components/products/CompareButton.jsx
src/components/compare/CompareTray.jsx
src/pages/ComparePage.jsx
```

### Модифицирани файлове (Frontend)
```
src/redux/store.js          — добавяне на compareReducer
src/App.js                  — добавяне на Route /compare + рендериране на CompareTray
src/components/products/ProductCard.jsx   — добавяне на CompareButton
src/pages/ProductPage.jsx   — добавяне на CompareButton
```

### Нови файлове (Backend) — НЯМА

---

## Стъпки за изпълнение

---

### СТЪПКА 1 — `compareSlice.js`

**Файл:** `src/redux/compareSlice.js`

**State:**
```js
{
  items: []  // масив от product обекти (max 4)
}
```

**Actions:**
- `addToCompare(product)` — добавя продукт; ако вече е в списъка — игнорира; ако са 4 — не добавя (caller показва toast)
- `removeFromCompare(productId)` — маха по id
- `clearCompare()` — изчиства всичко

**Selectors (export):**
- `selectCompareItems` — `state.compare.items`
- `selectCompareCount` — `state.compare.items.length`
- `selectIsInCompare(id)` — `state.compare.items.some(p => p.id === id)`

**localStorage sync:**  
В slice reducer след всяка мутация записваме в `localStorage.setItem('compareItems', JSON.stringify(state.items))`.  
При инициализация: `initialState.items = JSON.parse(localStorage.getItem('compareItems') || '[]')`.

---

### СТЪПКА 2 — `CompareButton.jsx`

**Файл:** `src/components/products/CompareButton.jsx`

**Props:** `{ product, size = 16 }`

**Логика:**
```jsx
const isInCompare = useSelector(state => selectIsInCompare(state, product.id))
const count = useSelector(selectCompareCount)

const handleClick = (e) => {
  e.stopPropagation()
  if (isInCompare) {
    dispatch(removeFromCompare(product.id))
  } else if (count >= 4) {
    showToast('Можете да сравнявате най-много 4 продукта наведнъж', 'error')
  } else {
    dispatch(addToCompare(product))   // записваме целия product обект
    showToast('Добавен за сравнение', 'success')
  }
}
```

**Иконa:** `MdCompareArrows` от `react-icons/md`

**Изглед:** идентичен стил с останалите action бутони в `ProductCard` — `p-2 sm:p-2.5 rounded-md border shadow-md transition-all active:scale-95`. Когато е добавен: синьо запълнена (`bg-blue-600 text-white border-blue-600`), иначе: `bg-white/10 text-black border-gray-300`.

**Tooltip:** `"ДОБАВИ ЗА СРАВНЕНИЕ"` / `"ПРЕМАХНИ ОТ СРАВНЕНИЕ"`

---

### СТЪПКА 3 — Промяна в `ProductCard.jsx`

**Файл:** `src/components/products/ProductCard.jsx`

Добавяме `CompareButton` в action row-а (последен бутон вдясно, след сърцето):

```jsx
// В секцията с бутоните:
<CompareButton product={p} size={16} />
```

Поредност на бутоните: `[Купи сега] [Добави в количка] [Любими] [Сравни]`

---

### СТЪПКА 4 — Промяна в `ProductPage.jsx`

**Файл:** `src/pages/ProductPage.jsx`

Добавяме `CompareButton` близо до бутоните за "Добави в количката" / TBI.  
Бутонът е по-голям от картата — `size={20}`, с текст "Добави за сравнение" / "Премахни от сравнение" до иконата.

```jsx
<CompareButton product={item} size={20} />
```

Позиция: ред под основните action бутони, или inline с тях.

---

### СТЪПКА 5 — `CompareTray.jsx` (плаваща лента)

**Файл:** `src/components/compare/CompareTray.jsx`

**Поведение:**
- Появява се отдолу (fixed, bottom-0) с анимация `translate-y` когато `compareCount > 0`
- Показва thumbnails на продуктите (снимка + кратко ime, до 4)
- Всеки thumbnail има `X` бутон за премахване
- Бутон `"Сравни (N)"` → `navigate('/compare')`
- Бутон `"Изчисти"` → `dispatch(clearCompare())`
- Не се показва на `/compare` страницата

**Стил:** тъмен фон (`bg-gray-900/95 backdrop-blur`), бял текст, shadow-2xl, z-50.

**Рендерира се в:** `App.js` извън `<Routes>` (глобално), само когато не е admin route и не е `/compare`.

---

### СТЪПКА 6 — `ComparePage.jsx`

**Файл:** `src/pages/ComparePage.jsx`  
**Маршрут:** `/compare`

**Layout:** таблична структура с scroll хоризонтално на мобилни.

#### Структура на таблицата:
```
| Ред              | Продукт 1 | Продукт 2 | Продукт 3 | Продукт 4 |
|------------------|-----------|-----------|-----------|-----------|
| [снимка]         | img       | img       | img       | img       |
| Наименование     | nameBg    | ...       | ...       | ...       |
| Категория        | category  | ...       | ...       | ...       |
| Производител     | mfr       | ...       | ...       | ...       |
| Цена (с ДДС)     | €  / лв.  | ...       | ...       | ...       |
| --- Спецификации ---                                              |
| [param name]     | value     | value     | value     | value     |
| ...              | ...       | ...       | ...       | ...       |
| [Действия]       | В量ka+TBI | ...       | ...       | ...       |
```

#### Логика за спецификации:
- Събираме union на всички `parameterNameBg` от всички продукти
- За всеки продукт намираме стойността или показваме `—`
- **Highlight на разлики:** ако стойностите в един ред се различават → ред с лек жълт/оранжев фон (опционално)

#### Данни:
- Продуктите идват от Redux `state.compare.items` (вече заредени при добавяне)
- Ако потребителят дойде директно на `/compare` с празен списък → показваме съобщение "Нямате продукти за сравнение" + бутон "Разгледай продукти"

#### Action бутони per колона:
- `"Добави в количката"` → `dispatch(addItemToCart(...))`  
- `"Премахни"` → `dispatch(removeFromCompare(product.id))`

---

### СТЪПКА 7 — `store.js`

```js
import compareReducer from "./compareSlice";

// В rootReducer:
compare: compareReducer,
```

**Важно:** `compare` НЕ влиза в `whitelist` на redux-persist — синхронизираме ръчно с localStorage в slice-а (по-прост контрол).

---

### СТЪПКА 8 — `App.js`

```jsx
import ComparePage from "./pages/ComparePage";
import CompareTray from "./components/compare/CompareTray";

// В Routes:
<Route path="/compare" element={<ComparePage />} />

// В AppContent return, след NavBar:
<CompareTray />
```

---

## UI/UX детайли

| Елемент | Детайл |
|---|---|
| Иконa | `MdCompareArrows` (react-icons/md) |
| Цвят "добавен" | `bg-blue-600 text-white` |
| Tray анимация | `transition-transform duration-300 translate-y-full → translate-y-0` |
| Мобилна таблица | `overflow-x-auto`, минимална ширина на колона `160px` |
| Highlight разлики | Ред фон `bg-yellow-50` когато стойностите се различават |
| Toast при лимит | `"Можете да сравнявате най-много 4 продукта наведнъж"` |
| Празна страница | Илюстрация + `"Нямате продукти за сравнение"` + CTA бутон |

---

## Ред на изпълнение

```
1. compareSlice.js          (основа — всичко зависи от него)
2. store.js                 (регистрация на slice)
3. CompareButton.jsx        (reusable компонент)
4. ProductCard.jsx          (добавяне на бутона)
5. ProductPage.jsx          (добавяне на бутона)
6. CompareTray.jsx          (floating bar)
7. ComparePage.jsx          (страницата)
8. App.js                   (маршрут + CompareTray)
```

---

## Проверки преди старт

- [ ] `react-icons` е наличен в `care-tech-ui` (вече се ползва в ProductCard)
- [ ] `MdCompareArrows` е в `react-icons/md` ✓
- [ ] Няма нужда от нови npm пакети
- [ ] Няма нужда от backend промени
- [ ] Няма нужда от Flyway миграция
