-- =============================================================================
-- Реорганизация на Asbis категории
-- Стратегия: разпускане на Asbis root-ове — децата влизат директно под
-- съответния Vali/Tekra root. Asbis root-ите се скриват (show_flag = false).
--
-- Изпълни СЛЕД: fix_asbis_category_names_bg.sql
-- НЕ е Flyway миграция — изпълнява се ръчно след syncAsbisCategories()
--
-- ВАЖНО: external_id идва от Vali API и е стабилен между бази данни.
--        Само вътрешният id (PK) се различава между инстанции.
-- =============================================================================

BEGIN;

-- =============================================================================
-- 1. КОМПЮТЪРНИ КОМПОНЕНТИ  (external_id = 373)
-- =============================================================================

-- Процесори → dissolve
UPDATE categories SET parent_id = (SELECT id FROM categories WHERE external_id = 373)
WHERE parent_id = (SELECT id FROM categories WHERE name_bg = 'Процесори'         AND platform = 'ASBIS' AND parent_id IS NULL);
UPDATE categories SET show_flag = false WHERE name_bg = 'Процесори'              AND platform = 'ASBIS' AND parent_id IS NULL;

-- Памет → dissolve
UPDATE categories SET parent_id = (SELECT id FROM categories WHERE external_id = 373)
WHERE parent_id = (SELECT id FROM categories WHERE name_bg = 'Памет'             AND platform = 'ASBIS' AND parent_id IS NULL);
UPDATE categories SET show_flag = false WHERE name_bg = 'Памет'                  AND platform = 'ASBIS' AND parent_id IS NULL;

-- Видео адаптери → dissolve
UPDATE categories SET parent_id = (SELECT id FROM categories WHERE external_id = 373)
WHERE parent_id = (SELECT id FROM categories WHERE name_bg = 'Видео адаптери'    AND platform = 'ASBIS' AND parent_id IS NULL);
UPDATE categories SET show_flag = false WHERE name_bg = 'Видео адаптери'         AND platform = 'ASBIS' AND parent_id IS NULL;

-- Дънни платки → dissolve
UPDATE categories SET parent_id = (SELECT id FROM categories WHERE external_id = 373)
WHERE parent_id = (SELECT id FROM categories WHERE name_bg = 'Дънни платки'      AND platform = 'ASBIS' AND parent_id IS NULL);
UPDATE categories SET show_flag = false WHERE name_bg = 'Дънни платки'           AND platform = 'ASBIS' AND parent_id IS NULL;

-- Компютърни кутии → dissolve
UPDATE categories SET parent_id = (SELECT id FROM categories WHERE external_id = 373)
WHERE parent_id = (SELECT id FROM categories WHERE name_bg = 'Компютърни кутии'  AND platform = 'ASBIS' AND parent_id IS NULL);
UPDATE categories SET show_flag = false WHERE name_bg = 'Компютърни кутии'       AND platform = 'ASBIS' AND parent_id IS NULL;

-- Охладители → dissolve
UPDATE categories SET parent_id = (SELECT id FROM categories WHERE external_id = 373)
WHERE parent_id = (SELECT id FROM categories WHERE name_bg = 'Охладители'        AND platform = 'ASBIS' AND parent_id IS NULL);
UPDATE categories SET show_flag = false WHERE name_bg = 'Охладители'             AND platform = 'ASBIS' AND parent_id IS NULL;

-- SSD → dissolve
UPDATE categories SET parent_id = (SELECT id FROM categories WHERE external_id = 373)
WHERE parent_id = (SELECT id FROM categories WHERE name_bg = 'SSD'               AND platform = 'ASBIS' AND parent_id IS NULL);
UPDATE categories SET show_flag = false WHERE name_bg = 'SSD'                    AND platform = 'ASBIS' AND parent_id IS NULL;

-- Оптични устройства → dissolve
UPDATE categories SET parent_id = (SELECT id FROM categories WHERE external_id = 373)
WHERE parent_id = (SELECT id FROM categories WHERE name_bg = 'Оптични устройства' AND platform = 'ASBIS' AND parent_id IS NULL);
UPDATE categories SET show_flag = false WHERE name_bg = 'Оптични устройства'     AND platform = 'ASBIS' AND parent_id IS NULL;

-- Твърди дискове → dissolve
UPDATE categories SET parent_id = (SELECT id FROM categories WHERE external_id = 373)
WHERE parent_id = (SELECT id FROM categories WHERE name_bg = 'Твърди дискове'    AND platform = 'ASBIS' AND parent_id IS NULL);
UPDATE categories SET show_flag = false WHERE name_bg = 'Твърди дискове'         AND platform = 'ASBIS' AND parent_id IS NULL;

-- Optane → dissolve
UPDATE categories SET parent_id = (SELECT id FROM categories WHERE external_id = 373)
WHERE parent_id = (SELECT id FROM categories WHERE name_bg = 'Optane'            AND platform = 'ASBIS' AND parent_id IS NULL);
UPDATE categories SET show_flag = false WHERE name_bg = 'Optane'                 AND platform = 'ASBIS' AND parent_id IS NULL;

-- =============================================================================
-- 2. КОМПЮТЪРНИ СИСТЕМИ  (external_id = 549)
-- =============================================================================

-- Настолни РС → dissolve
UPDATE categories SET parent_id = (SELECT id FROM categories WHERE external_id = 549)
WHERE parent_id = (SELECT id FROM categories WHERE name_bg = 'Настолни РС'           AND platform = 'ASBIS' AND parent_id IS NULL);
UPDATE categories SET show_flag = false WHERE name_bg = 'Настолни РС'                AND platform = 'ASBIS' AND parent_id IS NULL;

-- Настолни конфигурации → dissolve
UPDATE categories SET parent_id = (SELECT id FROM categories WHERE external_id = 549)
WHERE parent_id = (SELECT id FROM categories WHERE name_bg = 'Настолни конфигурации' AND platform = 'ASBIS' AND parent_id IS NULL);
UPDATE categories SET show_flag = false WHERE name_bg = 'Настолни конфигурации'      AND platform = 'ASBIS' AND parent_id IS NULL;

-- Сървъри → dissolve
UPDATE categories SET parent_id = (SELECT id FROM categories WHERE external_id = 549)
WHERE parent_id = (SELECT id FROM categories WHERE name_bg = 'Сървъри'               AND platform = 'ASBIS' AND parent_id IS NULL);
UPDATE categories SET show_flag = false WHERE name_bg = 'Сървъри'                    AND platform = 'ASBIS' AND parent_id IS NULL;

-- Сървърни компоненти → dissolve
UPDATE categories SET parent_id = (SELECT id FROM categories WHERE external_id = 549)
WHERE parent_id = (SELECT id FROM categories WHERE name_bg = 'Сървърни компоненти'   AND platform = 'ASBIS' AND parent_id IS NULL);
UPDATE categories SET show_flag = false WHERE name_bg = 'Сървърни компоненти'        AND platform = 'ASBIS' AND parent_id IS NULL;

-- =============================================================================
-- 3. ЛАПТОПИ, ТАБЛЕТИ И АКСЕСОАРИ  (external_id = 482)
-- =============================================================================

-- Преносими компютри → dissolve
UPDATE categories SET parent_id = (SELECT id FROM categories WHERE external_id = 482)
WHERE parent_id = (SELECT id FROM categories WHERE name_bg = 'Преносими компютри' AND platform = 'ASBIS' AND parent_id IS NULL);
UPDATE categories SET show_flag = false WHERE name_bg = 'Преносими компютри'      AND platform = 'ASBIS' AND parent_id IS NULL;

-- Таблети → dissolve
UPDATE categories SET parent_id = (SELECT id FROM categories WHERE external_id = 482)
WHERE parent_id = (SELECT id FROM categories WHERE name_bg = 'Таблети'            AND platform = 'ASBIS' AND parent_id IS NULL);
UPDATE categories SET show_flag = false WHERE name_bg = 'Таблети'                 AND platform = 'ASBIS' AND parent_id IS NULL;

-- Конектори (грешно наименование в Asbis XML — съдържа калъфи и опаковки) → dissolve
UPDATE categories SET parent_id = (SELECT id FROM categories WHERE external_id = 482)
WHERE parent_id = (SELECT id FROM categories WHERE name_bg = 'Конектори'          AND platform = 'ASBIS' AND parent_id IS NULL);
UPDATE categories SET show_flag = false WHERE name_bg = 'Конектори'               AND platform = 'ASBIS' AND parent_id IS NULL;

-- =============================================================================
-- 4. МОНИТОРИ И ДИСПЛЕИ  (external_id = 631)
-- =============================================================================

-- Дисплеи → dissolve
UPDATE categories SET parent_id = (SELECT id FROM categories WHERE external_id = 631)
WHERE parent_id = (SELECT id FROM categories WHERE name_bg = 'Дисплеи' AND platform = 'ASBIS' AND parent_id IS NULL);
UPDATE categories SET show_flag = false WHERE name_bg = 'Дисплеи'       AND platform = 'ASBIS' AND parent_id IS NULL;

-- =============================================================================
-- 5. КОМПЮТЪРНА ПЕРИФЕРИЯ  (external_id = 398)
-- =============================================================================

-- Периферия → dissolve
UPDATE categories SET parent_id = (SELECT id FROM categories WHERE external_id = 398)
WHERE parent_id = (SELECT id FROM categories WHERE name_bg = 'Периферия'          AND platform = 'ASBIS' AND parent_id IS NULL);
UPDATE categories SET show_flag = false WHERE name_bg = 'Периферия'               AND platform = 'ASBIS' AND parent_id IS NULL;

-- Флаш памети → dissolve
UPDATE categories SET parent_id = (SELECT id FROM categories WHERE external_id = 398)
WHERE parent_id = (SELECT id FROM categories WHERE name_bg = 'Флаш памети'        AND platform = 'ASBIS' AND parent_id IS NULL);
UPDATE categories SET show_flag = false WHERE name_bg = 'Флаш памети'             AND platform = 'ASBIS' AND parent_id IS NULL;

-- Периферни устройства → dissolve (1 дете: Флаш памети — дубликат)
UPDATE categories SET parent_id = (SELECT id FROM categories WHERE external_id = 398)
WHERE parent_id = (SELECT id FROM categories WHERE name_bg = 'Периферни устройства' AND platform = 'ASBIS' AND parent_id IS NULL);
UPDATE categories SET show_flag = false WHERE name_bg = 'Периферни устройства'    AND platform = 'ASBIS' AND parent_id IS NULL;

-- Мултимедиен хардуер → dissolve
UPDATE categories SET parent_id = (SELECT id FROM categories WHERE external_id = 398)
WHERE parent_id = (SELECT id FROM categories WHERE name_bg = 'Мултимедиен хардуер' AND platform = 'ASBIS' AND parent_id IS NULL);
UPDATE categories SET show_flag = false WHERE name_bg = 'Мултимедиен хардуер'     AND platform = 'ASBIS' AND parent_id IS NULL;

-- Аидио устройства (typo в Asbis XML) → dissolve
UPDATE categories SET parent_id = (SELECT id FROM categories WHERE external_id = 398)
WHERE parent_id = (SELECT id FROM categories WHERE name_bg = 'Аидио устройства'   AND platform = 'ASBIS' AND parent_id IS NULL);
UPDATE categories SET show_flag = false WHERE name_bg = 'Аидио устройства'        AND platform = 'ASBIS' AND parent_id IS NULL;

-- =============================================================================
-- 6. МРЕЖОВО ОБОРУДВАНЕ  (external_id = 412)
-- =============================================================================

-- Мрежови продукти → dissolve
UPDATE categories SET parent_id = (SELECT id FROM categories WHERE external_id = 412)
WHERE parent_id = (SELECT id FROM categories WHERE name_bg = 'Мрежови продукти'              AND platform = 'ASBIS' AND parent_id IS NULL);
UPDATE categories SET show_flag = false WHERE name_bg = 'Мрежови продукти'                   AND platform = 'ASBIS' AND parent_id IS NULL;

-- Устройства за съхранение на данни → dissolve (NAS устройство)
UPDATE categories SET parent_id = (SELECT id FROM categories WHERE external_id = 412)
WHERE parent_id = (SELECT id FROM categories WHERE name_bg = 'Устройства за съхранение на данни' AND platform = 'ASBIS' AND parent_id IS NULL);
UPDATE categories SET show_flag = false WHERE name_bg = 'Устройства за съхранение на данни'  AND platform = 'ASBIS' AND parent_id IS NULL;

-- =============================================================================
-- 7. ГЕЙМЪРСКА ПЕРИФЕРИЯ  (external_id = 554)
-- =============================================================================

-- Игри и мултимедия → dissolve
UPDATE categories SET parent_id = (SELECT id FROM categories WHERE external_id = 554)
WHERE parent_id = (SELECT id FROM categories WHERE name_bg = 'Игри и мултимедия' AND platform = 'ASBIS' AND parent_id IS NULL);
UPDATE categories SET show_flag = false WHERE name_bg = 'Игри и мултимедия'      AND platform = 'ASBIS' AND parent_id IS NULL;

-- =============================================================================
-- 8. МОБИЛНИ ТЕЛЕФОНИ И АКСЕСОАРИ  (external_id = 459)
--    (или "Смарт часовници, телефони и аксесоари" ако rename_categories.sql е изпълнен)
-- =============================================================================

-- Телефони → dissolve
UPDATE categories SET parent_id = (SELECT id FROM categories WHERE external_id = 459)
WHERE parent_id = (SELECT id FROM categories WHERE name_bg = 'Телефони'        AND platform = 'ASBIS' AND parent_id IS NULL);
UPDATE categories SET show_flag = false WHERE name_bg = 'Телефони'             AND platform = 'ASBIS' AND parent_id IS NULL;

-- Смарт устройства → dissolve (всички 12 деца под мобилните)
UPDATE categories SET parent_id = (SELECT id FROM categories WHERE external_id = 459)
WHERE parent_id = (SELECT id FROM categories WHERE name_bg = 'Смарт устройства' AND platform = 'ASBIS' AND parent_id IS NULL);
UPDATE categories SET show_flag = false WHERE name_bg = 'Смарт устройства'     AND platform = 'ASBIS' AND parent_id IS NULL;

-- =============================================================================
-- 9. КАБЕЛИ + БАТЕРИИ + НТУ — split на "Захранване и кабели"  (8 деца, 3 дестинации)
-- =============================================================================

-- Кабелите → Кабели (external_id = 674)
UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE external_id = 674)
WHERE name_bg IN ('HDMI Кабели', 'USB Кабели', 'VGA Кабели', 'Кабел')
  AND platform = 'ASBIS'
  AND parent_id = (SELECT id FROM categories WHERE name_bg = 'Захранване и кабели' AND platform = 'ASBIS' AND parent_id IS NULL);

-- Захранване → Компютърни компоненти (external_id = 373)
UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE external_id = 373)
WHERE name_bg = 'Захранване'
  AND platform = 'ASBIS'
  AND parent_id = (SELECT id FROM categories WHERE name_bg = 'Захранване и кабели' AND platform = 'ASBIS' AND parent_id IS NULL);

-- Батерия + Адаптер за Захранване → Батерии и зарядни у-ва (external_id = 481)
UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE external_id = 481)
WHERE name_bg IN ('Батерия', 'Адаптер за Захранване')
  AND platform = 'ASBIS'
  AND parent_id = (SELECT id FROM categories WHERE name_bg = 'Захранване и кабели' AND platform = 'ASBIS' AND parent_id IS NULL);

-- Непрекъсваемо Токозахранване → НТУ (external_id = 395)
UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE external_id = 395)
WHERE name_bg = 'Непрекъсваемо Токозахранване'
  AND platform = 'ASBIS'
  AND parent_id = (SELECT id FROM categories WHERE name_bg = 'Захранване и кабели' AND platform = 'ASBIS' AND parent_id IS NULL);

UPDATE categories SET show_flag = false WHERE name_bg = 'Захранване и кабели' AND platform = 'ASBIS' AND parent_id IS NULL;

-- =============================================================================
-- 10. ДРЕБНИ ДОМАКИНСКИ УРЕДИ — остава видим Asbis root
--     Всички домакински категории се събират тук.
--     external_id = 491 се преименува на "Инструменти и аксесоари" от
--     rename_categories.sql — не поставяме домакинска техника там.
-- =============================================================================

-- "Дребни домакински уреди" root остава show_flag = true (не се скрива)

-- Грижа за здравето → dissolve под "Дребни домакински уреди"
UPDATE categories SET parent_id = (SELECT id FROM categories WHERE name_bg = 'Дребни домакински уреди' AND platform = 'ASBIS' AND parent_id IS NULL)
WHERE parent_id = (SELECT id FROM categories WHERE name_bg = 'Грижа за здравето' AND platform = 'ASBIS' AND parent_id IS NULL);
UPDATE categories SET show_flag = false WHERE name_bg = 'Грижа за здравето'      AND platform = 'ASBIS' AND parent_id IS NULL;

-- Едра домакинска техника → дете на "Дребни домакински уреди"
UPDATE categories SET parent_id = (SELECT id FROM categories WHERE name_bg = 'Дребни домакински уреди' AND platform = 'ASBIS' AND parent_id IS NULL)
WHERE name_bg = 'Едра домакинска техника' AND platform = 'ASBIS' AND parent_id IS NULL;

-- LED осветление → дете на "Дребни домакински уреди"
UPDATE categories SET parent_id = (SELECT id FROM categories WHERE name_bg = 'Дребни домакински уреди' AND platform = 'ASBIS' AND parent_id IS NULL)
WHERE name_bg = 'LED осветление' AND platform = 'ASBIS' AND parent_id IS NULL;

-- =============================================================================
-- 11. ОФИС ПРОДУКТИ  (external_id = 513)
-- =============================================================================

-- Мултиборд → re-parent (запазва се като subcategory под Офис продукти)
UPDATE categories SET parent_id = (SELECT id FROM categories WHERE external_id = 513)
WHERE name_bg = 'Мултиборд' AND platform = 'ASBIS' AND parent_id IS NULL;

-- =============================================================================
-- 12. СТОРИДЖ У-ВА И КОНСУМАТИВИ  (external_id = 568)
-- =============================================================================

-- Медии → dissolve
UPDATE categories SET parent_id = (SELECT id FROM categories WHERE external_id = 568)
WHERE parent_id = (SELECT id FROM categories WHERE name_bg = 'Медии' AND platform = 'ASBIS' AND parent_id IS NULL);
UPDATE categories SET show_flag = false WHERE name_bg = 'Медии'       AND platform = 'ASBIS' AND parent_id IS NULL;

-- =============================================================================
-- 13. ФОТО И ВИДЕО АКСЕСОАРИ  (external_id = 453)
-- =============================================================================

-- Цифрови фотоапарати → dissolve (1 дете: Авто видеорегистратор)
UPDATE categories SET parent_id = (SELECT id FROM categories WHERE external_id = 453)
WHERE parent_id = (SELECT id FROM categories WHERE name_bg = 'Цифрови фотоапарати' AND platform = 'ASBIS' AND parent_id IS NULL);
UPDATE categories SET show_flag = false WHERE name_bg = 'Цифрови фотоапарати'      AND platform = 'ASBIS' AND parent_id IS NULL;

-- =============================================================================
-- 14. ЕЛЕКТРОНИКА И РОБОТИКА  (external_id = 625)
--     split на "Мултимедийни компоненти"
-- =============================================================================

UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE external_id = 373)   -- Компютърни компоненти
WHERE name_bg = 'Звукова Карта'
  AND platform = 'ASBIS'
  AND parent_id = (SELECT id FROM categories WHERE name_bg = 'Мултимедийни компоненти' AND platform = 'ASBIS' AND parent_id IS NULL);

UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE external_id = 625)   -- Електроника и роботика
WHERE name_bg = 'Цифров медия плеър'
  AND platform = 'ASBIS'
  AND parent_id = (SELECT id FROM categories WHERE name_bg = 'Мултимедийни компоненти' AND platform = 'ASBIS' AND parent_id IS NULL);

UPDATE categories SET show_flag = false WHERE name_bg = 'Мултимедийни компоненти' AND platform = 'ASBIS' AND parent_id IS NULL;

-- =============================================================================
-- 15. СОФТУЕР  (external_id = 497)
-- =============================================================================

UPDATE categories SET parent_id = (SELECT id FROM categories WHERE external_id = 497)
WHERE parent_id = (SELECT id FROM categories WHERE name_bg = 'Софтуеър' AND platform = 'ASBIS' AND parent_id IS NULL);
UPDATE categories SET show_flag = false WHERE name_bg = 'Софтуеър'      AND platform = 'ASBIS' AND parent_id IS NULL;

-- =============================================================================
-- 16. АДАПТЕРИ — split  (3 деца, 2 дестинации)
-- =============================================================================

-- Адаптери (generic) + Контролерна карта → Компютърни компоненти
UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE external_id = 373)
WHERE name_bg IN ('Адаптери', 'Контролерна карта')
  AND platform = 'ASBIS'
  AND parent_id = (SELECT id FROM categories WHERE name_bg = 'Адаптери' AND platform = 'ASBIS' AND parent_id IS NULL);

-- Порт Репликатор → Лаптопи, таблети и аксесоари
UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE external_id = 482)
WHERE name_bg = 'Порт Репликатор'
  AND platform = 'ASBIS'
  AND parent_id = (SELECT id FROM categories WHERE name_bg = 'Адаптери' AND platform = 'ASBIS' AND parent_id IS NULL);

UPDATE categories SET show_flag = false WHERE name_bg = 'Адаптери' AND platform = 'ASBIS' AND parent_id IS NULL;

-- =============================================================================
-- 17. АКСЕСОАРИ — split  (20 деца, 7 дестинации)
-- =============================================================================

-- → Компютърна периферия
UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE external_id = 398)
WHERE name_bg IN ('Bluetooth Слушалки', 'Подложка за мишка', 'Уеб камера',
                  'Аксесоари', 'Аксесоари - резервни части')
  AND platform = 'ASBIS'
  AND parent_id = (SELECT id FROM categories WHERE name_bg = 'Аксесоари' AND platform = 'ASBIS' AND parent_id IS NULL);

-- → Лаптопи, таблети и аксесоари
UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE external_id = 482)
WHERE name_bg = 'Охладителни стойки за мобилни компютри'
  AND platform = 'ASBIS'
  AND parent_id = (SELECT id FROM categories WHERE name_bg = 'Аксесоари' AND platform = 'ASBIS' AND parent_id IS NULL);

-- → Мрежово оборудване
UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE external_id = 412)
WHERE name_bg = 'Мрежови аксесоари'
  AND platform = 'ASBIS'
  AND parent_id = (SELECT id FROM categories WHERE name_bg = 'Аксесоари' AND platform = 'ASBIS' AND parent_id IS NULL);

-- → Мобилни телефони и аксесоари
UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE external_id = 459)
WHERE name_bg IN ('iPhone аксесоари', 'Безжични зарядни', 'Дистанционно Управление')
  AND platform = 'ASBIS'
  AND parent_id = (SELECT id FROM categories WHERE name_bg = 'Аксесоари' AND platform = 'ASBIS' AND parent_id IS NULL);

-- → Навигации, камери и аксесоари за коли  (external_id = 469)
UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE external_id = 469)
WHERE name_bg = 'Аксесоари за автомобили'
  AND platform = 'ASBIS'
  AND parent_id = (SELECT id FROM categories WHERE name_bg = 'Аксесоари' AND platform = 'ASBIS' AND parent_id IS NULL);

-- → Дребни домакински уреди (Asbis root)
UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE name_bg = 'Дребни домакински уреди' AND platform = 'ASBIS' AND parent_id IS NULL)
WHERE name_bg IN ('Аксесоари за прахосмукачки', 'Аксесоари за пречиствателни устройства',
                  'Аксесоари за напитки', 'Аксесоари за грижа за зъби')
  AND platform = 'ASBIS'
  AND parent_id = (SELECT id FROM categories WHERE name_bg = 'Аксесоари' AND platform = 'ASBIS' AND parent_id IS NULL);

-- → Видеонаблюдение (Tekra root — lookup по name + parent IS NULL)
UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE name_bg = 'Видеонаблюдение' AND parent_id IS NULL)
WHERE name_bg = 'Аксесоари за системи за видеонаблюдение'
  AND platform = 'ASBIS'
  AND parent_id = (SELECT id FROM categories WHERE name_bg = 'Аксесоари' AND platform = 'ASBIS' AND parent_id IS NULL);

-- → Компютърни компоненти
UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE external_id = 373)
WHERE name_bg IN ('Аксесоари за настолни компютри', 'Монтажен Хардуер')
  AND platform = 'ASBIS'
  AND parent_id = (SELECT id FROM categories WHERE name_bg = 'Аксесоари' AND platform = 'ASBIS' AND parent_id IS NULL);

-- → Батерии и зарядни у-ва
UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE external_id = 481)
WHERE name_bg = 'Зарядно устройство'
  AND platform = 'ASBIS'
  AND parent_id = (SELECT id FROM categories WHERE name_bg = 'Аксесоари' AND platform = 'ASBIS' AND parent_id IS NULL);

-- → Офис продукти
UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE external_id = 513)
WHERE name_bg = 'Аксесоари за мултиборд'
  AND platform = 'ASBIS'
  AND parent_id = (SELECT id FROM categories WHERE name_bg = 'Аксесоари' AND platform = 'ASBIS' AND parent_id IS NULL);

UPDATE categories SET show_flag = false WHERE name_bg = 'Аксесоари' AND platform = 'ASBIS' AND parent_id IS NULL;

-- =============================================================================
-- 18. РОБОТИЗИРАНИ РЕШЕНИЯ — частично (consumer robots излизат)
--     Роботизирани манипулатори, Роботизирани ръце, Робот за доставки ОСТАВАТ
--     под Роботизирани решения (root остава видим)
-- =============================================================================

-- Кухненски робот + Робот почистващ → Дребни домакински уреди (Asbis root)
UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE name_bg = 'Дребни домакински уреди' AND platform = 'ASBIS' AND parent_id IS NULL)
WHERE name_bg IN ('Кухненски робот', 'Робот почистващ')
  AND platform = 'ASBIS'
  AND parent_id = (SELECT id FROM categories WHERE name_bg = 'Роботизирани решения' AND platform = 'ASBIS' AND parent_id IS NULL);

-- Роботизирани решения root остава show_flag = true — само индустриални

-- =============================================================================
-- 19. ДЕЦА НА "ДРУГИ"  (40 деца → 8 дестинации)
-- =============================================================================

-- → Геймърска периферия
UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE external_id = 554)
WHERE name_bg IN ('Гейминг база за волан', 'Гейминг волан за рейсинг',
                  'Гейминг педали за рейсинг', 'Гейминг ръчна спирачка',
                  'Гейминг симрейсинг аксесоари', 'Гейминг симрейсинг кокпит',
                  'Гейминг скоростен лост')
  AND platform = 'ASBIS'
  AND parent_id = (SELECT id FROM categories WHERE name_bg = 'Други' AND platform = 'ASBIS' AND parent_id IS NULL);

-- → Монитори и дисплеи
UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE external_id = 631)
WHERE name_bg = 'Гейминг монитор'
  AND platform = 'ASBIS'
  AND parent_id = (SELECT id FROM categories WHERE name_bg = 'Други' AND platform = 'ASBIS' AND parent_id IS NULL);

-- → Компютърна периферия (VC у-ва + Wi-Fi говорител + OTG)
UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE external_id = 398)
WHERE name_bg IN ('Видеоконферентни докинг станции', 'Видеоконферентни слушалки',
                  'Видеоконферентни уеб камери', 'Мултимедия - Wi-Fi говорител',
                  'Аксесоари OTG')
  AND platform = 'ASBIS'
  AND parent_id = (SELECT id FROM categories WHERE name_bg = 'Други' AND platform = 'ASBIS' AND parent_id IS NULL);

-- → Мрежово оборудване
UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE external_id = 412)
WHERE name_bg IN ('Мрежово - Cloud Key Enterprise', 'Мрежово - Контрол на достъп',
                  'Настолен NAS', 'Антена')
  AND platform = 'ASBIS'
  AND parent_id = (SELECT id FROM categories WHERE name_bg = 'Други' AND platform = 'ASBIS' AND parent_id IS NULL);

-- → Дребни домакински уреди (Asbis root)
UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE name_bg = 'Дребни домакински уреди' AND platform = 'ASBIS' AND parent_id IS NULL)
WHERE name_bg IN ('Робот прахосмукачка', 'Прахосмукачка прътова', 'Прахосмукачка трансформер',
                  'Тенджери под налягане', 'Блендер', 'Иригатори',
                  'Аксесоари за вакуумзатваряне', 'Слънчев панел')
  AND platform = 'ASBIS'
  AND parent_id = (SELECT id FROM categories WHERE name_bg = 'Други' AND platform = 'ASBIS' AND parent_id IS NULL);

-- → Видеонаблюдение
UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE name_bg = 'Видеонаблюдение' AND parent_id IS NULL)
WHERE name_bg IN ('Сигурност - Видеорекордер', 'Сигурност - Комплект за видеонаблюдение',
                  'AI рекордер')
  AND platform = 'ASBIS'
  AND parent_id = (SELECT id FROM categories WHERE name_bg = 'Други' AND platform = 'ASBIS' AND parent_id IS NULL);

-- → Лаптопи, таблети и аксесоари
UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE external_id = 482)
WHERE name_bg = 'Реновиран лаптоп'
  AND platform = 'ASBIS'
  AND parent_id = (SELECT id FROM categories WHERE name_bg = 'Други' AND platform = 'ASBIS' AND parent_id IS NULL);

-- → Компютърни системи
UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE external_id = 549)
WHERE name_bg = 'Мини PC'
  AND platform = 'ASBIS'
  AND parent_id = (SELECT id FROM categories WHERE name_bg = 'Други' AND platform = 'ASBIS' AND parent_id IS NULL);

-- → Мобилни телефони и аксесоари
UPDATE categories
SET parent_id = (SELECT id FROM categories WHERE external_id = 459)
WHERE name_bg IN ('Смарт вътрешна сирена', 'Смарт тракер')
  AND platform = 'ASBIS'
  AND parent_id = (SELECT id FROM categories WHERE name_bg = 'Други' AND platform = 'ASBIS' AND parent_id IS NULL);

-- =============================================================================
-- 20. СКРИВАНЕ НА НЕРЕЛЕВАНТНИ / НЕЯСНИ КАТЕГОРИИ
-- =============================================================================

-- Грешка в данните — не е категория
UPDATE categories SET show_flag = false
WHERE name_bg = 'Несъответствие с Розета данните' AND platform = 'ASBIS';

-- Нерелевантни за B2C магазин или без ясен еквивалент
UPDATE categories SET show_flag = false
WHERE name_bg IN (
    'Готово за носене',
    'Модни аксесоари',
    'Производствени материали',
    'Удължена гаранция',
    'Аксесоари за сценична употреба',
    'Аксесоари за флексибилни устройства',
    'Видеоконферентни услуги'
) AND platform = 'ASBIS';

-- Скрий "Други" root — всички деца вече са разпределени
UPDATE categories SET show_flag = false
WHERE name_bg = 'Други' AND platform = 'ASBIS' AND parent_id IS NULL;

-- =============================================================================
-- ПРОВЕРКА — покажи всички видими Asbis категории след реорганизацията
-- (само root-ове и техните преки деца)
-- =============================================================================

SELECT
    COALESCE(p.name_bg, '── ROOT ──')  AS parent,
    c.name_bg                           AS category,
    c.platform,
    COUNT(ch.id)                        AS children
FROM categories c
LEFT JOIN categories p  ON p.id  = c.parent_id
LEFT JOIN categories ch ON ch.parent_id = c.id
WHERE c.platform = 'ASBIS'
  AND c.show_flag = true
  AND (c.parent_id IS NULL OR p.parent_id IS NULL)
GROUP BY p.name_bg, c.name_bg, c.platform
ORDER BY parent NULLS FIRST, c.name_bg;

COMMIT;
