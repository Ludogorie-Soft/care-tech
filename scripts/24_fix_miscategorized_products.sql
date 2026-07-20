-- ============================================================
-- Script 24: Почистване на грешно категоризирани продукти
-- Анализирани: 22 521 продукта | Намерени несъответствия: ~878
-- ============================================================
-- Засегнати категории (source → target):
--   380 (ASBIS кабели)   → 9 (захранвания), 72/73 (UPS)
--   402 (ASBIS охладители) → 455 (вентилатори), 179 (геймърски акс.)
--   325 (ASBIS аудио)    → 66 (слушалки), 139 (TV), 179 (геймърски акс.)
--   298 (ASBIS стойки)   → 51, 60, 64, 66, 160, 264, 43, 454
--   443 (ASBIS геймърска) → 179 (геймърски акс.), 181 (играчки)
--    86 (консумативи)    → 78 (принтери), 37 (лаптопи), 32 (настолни), 181 (играчки)
--    37 (лаптопи VALI)   → 40, 42, 47, 39 (аксесоари/таблети)
-- ============================================================


BEGIN;

-- ============================================================
-- БЛОК 1: Категория 380 (ASBIS „Кабели/мрежово") — 408 продукта
-- ============================================================

-- 1.1 Захранвания (PSU) → Захранвания (id=9)
--     237 продукта: Super Flower, CORSAIR, Asrock, Cougar серии
UPDATE products
SET category_id = 9, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 380
  AND (
       name_en ILIKE '%80 plus%'
    OR name_en ILIKE '%80plus%'
    OR name_en ILIKE '% psu%'
    OR name_en ILIKE '%power supply%'
    OR name_en ILIKE '%atx 3.%'
    OR name_en ILIKE '% watt%'
    OR name_en ILIKE '%leadex%'
    OR name_en ILIKE '%modular psu%'
  );

-- 1.2 UPS устройства → UPS Line-Interactive (id=72)
--     29 продукта: CyberPower, DELTA, Vertiv Liebert, Ubiquiti UPS
UPDATE products
SET category_id = 72, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 380
  AND (
       name_en ILIKE '%ups %'
    OR name_en ILIKE '% ups%'
    OR name_en ILIKE '%uninterruptible%'
    OR name_en ILIKE '%cyberpower%'
    OR name_en ILIKE '%liebert%'
    OR name_en ILIKE '%vertiv%'
    OR name_en ILIKE '%redundant power system%'
  );


-- ============================================================
-- БЛОК 2: Категория 402 (ASBIS „Охладители") — 363 продукта
-- ============================================================

-- 2.1 Case/system вентилатори → Вентилатори (id=455)
--     44 продукта: Corsair, Xigmatek, MSI, FD серии
--     Изключваме: CPU cooler-и, AIO liquid cooling, heatsink
UPDATE products
SET category_id = 455, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 402
  AND (
       name_en ILIKE '%argb fan%'
    OR name_en ILIKE '%rgb fan%'
    OR name_en ILIKE '% fan%'
    OR name_en ILIKE '%windpower%'
    OR name_en ILIKE '%ml120%'
    OR name_en ILIKE '%ml140%'
    OR name_en ILIKE '%ll120%'
    OR name_en ILIKE '%sp120%'
    OR name_en ILIKE '%sp140%'
    OR name_en ILIKE '%af120%'
    OR name_en ILIKE '%af140%'
  )
  AND name_en NOT ILIKE '%cooler%'
  AND name_en NOT ILIKE '%liquid%'
  AND name_en NOT ILIKE '%heatsink%'
  AND name_en NOT ILIKE '%cpu block%';

-- 2.2 CORSAIR iCUE LINK LCD Module → Геймърски аксесоари (id=179)
--     2 продукта: декоративен LCD модул за помпа, не е охладител
UPDATE products
SET category_id = 179, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 402
  AND name_en ILIKE '%icue link lcd module%';


-- ============================================================
-- БЛОК 3: Категория 325 (ASBIS „Аудио/Колони") — 770 продукта
-- ============================================================

-- 3.1 Слушалки / headset / earbuds → Слушалки (id=66)
--     30 продукта: Logitech H серия, A4, Edifier headphones, ACER headset
UPDATE products
SET category_id = 66, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 325
  AND (
       name_en ILIKE '%headset%'
    OR name_en ILIKE '%headphone%'
    OR name_en ILIKE '%earphone%'
    OR name_en ILIKE '%earbuds%'
    OR name_en ILIKE '%tws%'
  );

-- 3.2 Beovision TV → Телевизори (id=139)
--     1 продукт: Bang & Olufsen Beovision
UPDATE products
SET category_id = 139, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 325
  AND name_en ILIKE 'Beovision%';

-- 3.3 Elgato Stream Deck → Геймърски аксесоари (id=179)
--     1 продукт
UPDATE products
SET category_id = 179, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 325
  AND name_en ILIKE '%stream deck%';


-- ============================================================
-- БЛОК 4: Категория 298 (ASBIS „Стойки") — 414 продукта
-- ~84 са реални стойки/брекети, останалите ~310 са грешно поставени
-- ============================================================

-- 4.1 TV и монитор стойки ONKRON / KIVI / EDBAK → Стойки за монитори (id=51)
--     43 продукта — те СА стойки, но трябват в правилната стойки-категория
UPDATE products
SET category_id = 51, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 298
  AND (
       name_en ILIKE 'ONKRON%'
    OR name_en ILIKE 'KIVI Motion%'
    OR name_en ILIKE 'Wall Mount KIVI%'
    OR name_en ILIKE 'EDBAK%'
  );

-- 4.2 Уеб камери → Уеб камери (id=60)
--     20 продукта: Logitech C/Brio серия, Canyon, LORGAR
UPDATE products
SET category_id = 60, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 298
  AND (
       name_en ILIKE '%webcam%'
    OR name_en ILIKE '%web cam%'
    OR name_en ILIKE '%facecam%'
  );

-- 4.3 Подложки за мишки → Подложки за мишки (id=64)
--     19 продукта: CORSAIR MM серия, Logitech Desk Mat, Kingston HyperX
UPDATE products
SET category_id = 64, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 298
  AND (
       name_en ILIKE '%mouse pad%'
    OR name_en ILIKE '%mousepad%'
    OR name_en ILIKE '%desk mat%'
    OR name_en ILIKE '% mm100%'
    OR name_en ILIKE '% mm200%'
    OR name_en ILIKE '% mm300%'
    OR name_en ILIKE '% mm350%'
    OR name_en ILIKE '% mm500%'
    OR name_en ILIKE '% mm700%'
  );

-- 4.4 Слушалки / headset / Edifier / Canyon / Beoplay → Слушалки (id=66)
--     Edifier W820NB/W800BT, Canyon OnRiff, Beoplay H95/H100/HX
UPDATE products
SET category_id = 66, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 298
  AND (
       name_en ILIKE '%headphone%'
    OR name_en ILIKE '%headset%'
    OR name_en ILIKE '%over-ear%'
    OR name_en ILIKE 'Beoplay%'
  );

-- 4.5 Power bank-ове → Power bank (id=160)
--     22 продукта: CANYON OnPower, Silicon Power, Dell
UPDATE products
SET category_id = 160, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 298
  AND (
       name_en ILIKE '%power bank%'
    OR name_en ILIKE '%powerbank%'
    OR name_en ILIKE '%onpower%'
    OR name_en ILIKE '%portable charger%'
  );

-- 4.6 Стойки за лаптопи → Стойки за лаптопи (id=43)
--     6 продукта: AXAGON STND серия, Razer Laptop Cooling Stand
UPDATE products
SET category_id = 43, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 298
  AND (
       name_en ILIKE '%laptop stand%'
    OR name_en ILIKE '%notebook stand%'
    OR name_en ILIKE '%laptop cooling stand%'
    OR name_en ILIKE 'AXAGON STND%'
  );

-- 4.7 Аксесоари за прахосмукачки → Дребни домакински уреди (id=454)
--     39 продукта: AENO, Eureka, CANYON — HEPA филтри, четки, торбички
UPDATE products
SET category_id = 454, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 298
  AND (
       name_en ILIKE '%hepa filter%'
    OR name_en ILIKE '%dust bag%'
    OR name_en ILIKE '%side brush%'
    OR name_en ILIKE '%main brush%'
    OR name_en ILIKE '%mop pad%'
    OR name_en ILIKE '%roller brush%'
    OR name_en ILIKE '%vacuum%'
    OR name_en ILIKE 'Eureka%'
    OR name_en ILIKE 'AENO%filter%'
    OR name_en ILIKE 'AENO%brush%'
  );


-- ============================================================
-- БЛОК 5: Категория 443 (ASBIS „Геймърска периферия") — 518 продукта
-- ============================================================

-- 5.1 LEGO → Играчки/Фигурки (id=181)
--     2 продукта: LEGO BRICQ MOTION PRIME/ESSENTIAL
UPDATE products
SET category_id = 181, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 443
  AND name_en ILIKE 'LEGO%';

-- 5.2 Elgato стрийминг оборудване → Геймърски аксесоари (id=179)
--     20+ продукта: Key Light, Stream Deck, Cam Link, Wave серия
UPDATE products
SET category_id = 179, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 443
  AND (
       name_en ILIKE '%elgato%'
    OR name_en ILIKE '%stream deck%'
    OR name_en ILIKE '%cam link%'
    OR name_en ILIKE '%key light%'
  );


-- ============================================================
-- БЛОК 6: Категория 86 (Консумативи за принтери) — 779 продукта
-- ============================================================

-- 6.1 Zebra / Honeywell принтери и скенери → Принтери (id=78)
--     ~21 продукта: label принтери, баркод скенери
UPDATE products
SET category_id = 78, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 86
  AND (
       name_en ILIKE '%zebra%printer%'
    OR name_en ILIKE '%zebra%zd%'
    OR name_en ILIKE '%zebra%zt%'
    OR name_en ILIKE '%label printer%'
    OR name_en ILIKE '%scanner%'
    OR name_en ILIKE '%barcode%'
    OR name_en ILIKE '%honeywell xenon%'
    OR name_en ILIKE '%datalogic%'
  )
  AND name_en NOT ILIKE '%toner%'
  AND name_en NOT ILIKE '%cartridge%'
  AND name_en NOT ILIKE '%ink%'
  AND name_en NOT ILIKE '%label%roll%'
  AND name_en NOT ILIKE '%ribbon%';

-- 6.2 HP лаптопи (ProBook, EliteBook, ZBook) → Лаптопи (id=37)
--     34 продукта идентифицирани по ET/EA HP кодове и имена
UPDATE products
SET category_id = 37, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 86
  AND (
       name_en ILIKE '%probook%'
    OR name_en ILIKE '%elitebook%'
    OR name_en ILIKE '%zbook%'
    OR name_en ILIKE '%hp laptop%'
    OR name_en ~* 'HP (Portable|Notebook)'
  );

-- 6.3 HP / Acer настолни компютри → Настолни компютри (id=32)
--     4 продукта: Acer Aspire/Nitro/Predator PC, HP ProDesk
UPDATE products
SET category_id = 32, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 86
  AND (
       name_en ILIKE '%prodesk%'
    OR name_en ILIKE '%proone%'
    OR name_en ILIKE '%elitedesk%'
    OR name_en ILIKE 'ACER ASPIRE TC%'
    OR name_en ILIKE 'ACER NITRO N%'
    OR name_en ILIKE 'ACER PREDATOR PO%'
  );

-- 6.4 Parrot дронове → Играчки/Фигурки (id=181)
--     4 продукта: Jump Sumo BK/WH/KA, Jump Race
UPDATE products
SET category_id = 181, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 86
  AND name_en ILIKE 'PARROT%';

-- 6.5 Microsoft лицензи → скриване
--     5 продукта: Office 2021, Win 10/11, Project 2024
UPDATE products
SET show_flag = false, status = 'NOT_AVAILABLE', updated_at = CURRENT_TIMESTAMP
WHERE category_id = 86
  AND (
       name_en ILIKE 'MS OFFICE%'
    OR name_en ILIKE 'MS WIN%'
    OR name_en ILIKE 'MS PROJECT%'
    OR name_en ILIKE 'MICROSOFT OFFICE%'
    OR name_en ILIKE 'MICROSOFT WIN%'
  );


-- ============================================================
-- БЛОК 7: Категория 37 (Лаптопи VALI) — 1251 продукта
-- Скриптове 18-21 са написани, но не са приложени към текущия дамп.
-- ============================================================

-- 7.1 Чанти / раници / калъфи / ръкави → Чанти за лаптопи (id=40)
UPDATE products
SET category_id = 40, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 37
  AND (
       name_en ILIKE '%backpack%'
    OR name_en ILIKE '%carry case%'
    OR name_en ILIKE '%portfolio case%'
    OR name_en ILIKE '%portf case%'
    OR name_en ILIKE '%protective case%'
    OR name_en ILIKE '%sleeve%'
    OR name_en ILIKE '%bag%'
    OR name_en ILIKE '%tricover%'
    OR name_en ILIKE '%versasleave%'
    OR name_en ILIKE '%magsmart cover%'
  );

-- 7.2 Зарядни / адаптери → Зарядни за лаптопи (id=42)
UPDATE products
SET category_id = 42, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 37
  AND (
       name_en ILIKE '%charger%'
    OR name_en ILIKE '% adapter%'
    OR name_en ILIKE '%mini adapter%'
    OR name_en ILIKE '%nb adapter%'
    OR name_en ILIKE '%ac adapter%'
    OR name_en ILIKE '%power adapter%'
  );

-- 7.3 Таблети → Таблети (id=39)
UPDATE products
SET category_id = 39, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 37
  AND (
       name_en ILIKE 'LENOVO TAB%'
    OR name_en ILIKE 'REALME PAD%'
    OR name_en ILIKE '%tablet%'
  );

-- 7.4 Докинг / folio / фолиа / екранни протектори → Аксесоари за лаптопи (id=47)
UPDATE products
SET category_id = 47, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 37
  AND (
       name_en ILIKE '%docking station%'
    OR name_en ILIKE '%hybrid dock%'
    OR name_en ILIKE '% dock%'
    OR name_en ILIKE '%folio%'
    OR name_en ILIKE '%simpro%'
    OR name_en ILIKE '%protect film%'
    OR name_en ILIKE '%screen protector%'
    OR name_en ILIKE '%aglr protect%'
    OR name_en ILIKE '%ag protect%'
    OR name_en ILIKE '%dongle%'
  );

-- 7.5 Гаранции → скриване
UPDATE products
SET show_flag = false, status = 'NOT_AVAILABLE', updated_at = CURRENT_TIMESTAMP
WHERE category_id = 37
  AND (
       name_en ILIKE '%warranty%'
    OR name_en ILIKE '% warr%'
  );


COMMIT;

-- ============================================================
-- ПРОВЕРКА СЛЕД ПРОМЕНИТЕ
-- ============================================================
SELECT
    c.id,
    c.name_bg AS категория,
    COUNT(p.id) AS всички,
    COUNT(p.id) FILTER (WHERE p.show_flag = true) AS видими
FROM products p
JOIN categories c ON p.category_id = c.id
WHERE c.id IN (
    9, 32, 37, 39, 40, 42, 43, 47, 51, 60, 64, 66,
    72, 78, 86, 139, 160, 179, 181, 298, 325, 380,
    402, 443, 454, 455
)
GROUP BY c.id, c.name_bg
ORDER BY c.id;
