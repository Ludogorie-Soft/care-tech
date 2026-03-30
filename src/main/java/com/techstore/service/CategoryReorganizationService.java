package com.techstore.service;

import com.techstore.entity.Category;
import com.techstore.enums.Platform;
import com.techstore.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * CategoryReorganizationService - FINAL VERSION
 *
 * Дата: 27.01.2025
 *
 * Реорганизира категориите след Vali и Tekra sync
 * Създава пълната структура според бизнес изискванията
 *
 * ВАЖНО:
 * - НЕ създава нови категории ако вече съществуват
 * - САМО променя parent_id на съществуващите
 * - Продуктите автоматично "следват" категорията (category_id не се променя)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryReorganizationService {

    private final CategoryRepository categoryRepository;

    /**
     * Главен метод за реорганизация на всички категории
     */
    @Transactional
    public ReorganizationResult reorganizeAllCategories() {
        long startTime = System.currentTimeMillis();
        log.info("=== STARTING CATEGORY REORGANIZATION ===");

        try {
            // Load all categories
            List<Category> allCategories = categoryRepository.findAll();
            Map<String, Category> categoriesByName = allCategories.stream()
                    .collect(Collectors.toMap(
                            cat -> normalizeForSearch(cat.getNameBg()),
                            cat -> cat,
                            (existing, duplicate) -> existing
                    ));

            int created = 0, updated = 0, moved = 0, renamed = 0;

            // STEP 0: Преименувай Vali категории които трябва да се мапнат
            log.info("STEP 0: Renaming Vali categories");
            for (Map.Entry<String, String> rename : VALI_CATEGORY_RENAMES.entrySet()) {
                String oldName = rename.getKey();
                String newName = rename.getValue();

                Category category = categoriesByName.get(normalizeForSearch(oldName));
                if (category != null && !newName.equals(category.getNameBg())) {
                    String oldNameBg = category.getNameBg();
                    category.setNameBg(newName);
                    categoryRepository.save(category);

                    // Update cache
                    categoriesByName.remove(normalizeForSearch(oldNameBg));
                    categoriesByName.put(normalizeForSearch(newName), category);

                    renamed++;
                    log.info("Renamed: '{}' → '{}'", oldNameBg, newName);
                }
            }

            // STEP 1: Създай/актуализирай главни категории
            log.info("STEP 1: Creating/updating main categories");
            for (MainCategoryDef mainCat : MAIN_CATEGORIES) {
                Category category = categoriesByName.get(normalizeForSearch(mainCat.name));

                if (category == null) {
                    // Създай нова категория
                    category = new Category();
                    category.setNameBg(mainCat.name);
                    category.setNameEn(mainCat.nameEn);
                    category.setSlug(generateSlug(mainCat.name));
                    category.setParent(null);
                    category.setSortOrder(mainCat.sortOrder);
                    category.setShow(true);
                    category = categoryRepository.save(category);
                    categoriesByName.put(normalizeForSearch(category.getNameBg()), category);
                    created++;
                    log.info("Created main category: {}", mainCat.name);
                } else {
                    // Актуализирай съществуваща
                    boolean changed = false;

                    if (category.getParent() != null) {
                        category.setParent(null);
                        changed = true;
                    }

                    if (category.getSortOrder() != mainCat.sortOrder) {
                        category.setSortOrder(mainCat.sortOrder);
                        changed = true;
                    }

                    if (changed) {
                        categoryRepository.save(category);
                        updated++;
                    }
                }
            }

            // STEP 2: Организирай Vali категориите
            log.info("STEP 2: Organizing Vali categories");
            int valiMoved = reorganizeValiCategories(categoriesByName);
            moved += valiMoved;

            // STEP 3: Организирај Tekra "Видеонаблюдение"
            log.info("STEP 3: Organizing Tekra 'Видеонаблюдение'");
            int tekraMoved = reorganizeTekraCategories(categoriesByName);
            moved += tekraMoved;

            // STEP 4: Актуализирај всички category_path
            log.info("STEP 4: Updating all category paths");
            updateAllCategoryPaths();

            long duration = System.currentTimeMillis() - startTime;
            log.info("=== REORGANIZATION COMPLETE in {}ms ===", duration);
            log.info("Created: {}, Updated: {}, Moved: {}, Renamed: {}", created, updated, moved, renamed);

            return new ReorganizationResult(true, created, updated, moved, duration);

        } catch (Exception e) {
            log.error("=== REORGANIZATION FAILED ===", e);
            return new ReorganizationResult(false, 0, 0, 0, 0);
        }
    }

    /**
     * Реорганизира Vali категориите според структурата
     */
    private int reorganizeValiCategories(Map<String, Category> categoriesByName) {
        int moved = 0;

        for (SubCategoryDef subCat : VALI_SUBCATEGORIES) {
            Category parent = categoriesByName.get(normalizeForSearch(subCat.parentName));
            if (parent == null) {
                log.warn("Parent category not found: {}", subCat.parentName);
                continue;
            }

            Category category = categoriesByName.get(normalizeForSearch(subCat.name));
            if (category == null) {
                // Създај липсваща категория
                category = new Category();
                category.setNameBg(subCat.name);
                category.setNameEn(subCat.nameEn);
                category.setSlug(generateSlug(subCat.name));
                category.setParent(parent);
                category.setSortOrder(subCat.sortOrder);
                category.setPlatform(Platform.VALI);
                category.setShow(true);
                category = categoryRepository.save(category);
                categoriesByName.put(normalizeForSearch(category.getNameBg()), category);
                log.info("Created category: {}", subCat.name);
            } else {
                // Актуализирај съществуваща (САМО parent и sortOrder)
                boolean changed = false;

                if (!parent.equals(category.getParent())) {
                    category.setParent(parent);
                    changed = true;
                    moved++;
                }

                if (category.getSortOrder() != subCat.sortOrder) {
                    category.setSortOrder(subCat.sortOrder);
                    changed = true;
                }

                if (changed) {
                    categoryRepository.save(category);
                    log.debug("Updated category: {} (parent: {}, order: {})",
                            subCat.name, subCat.parentName, subCat.sortOrder);
                }
            }
        }

        return moved;
    }

    /**
     * Реорганизира Tekra "Видеонаблюдение" категориите
     */
    private int reorganizeTekraCategories(Map<String, Category> categoriesByName) {
        int moved = 0;

        // Намери "Видеонаблюдение"
        Category videoSurveillance = categoriesByName.get(normalizeForSearch("Видеонаблюдение"));
        if (videoSurveillance == null) {
            log.warn("'Видеонаблюдение' category not found");
            return 0;
        }

        // Структурата на Видеонаблюдение
        for (VideoSurveillanceDef vsDef : VIDEO_SURVEILLANCE_STRUCTURE) {
            Category parent;

            if (vsDef.parentName == null) {
                // Level 2 - директно под Видеонаблюдение
                parent = videoSurveillance;
            } else {
                // Level 3 - под някоя от level 2
                parent = categoriesByName.get(normalizeForSearch(vsDef.parentName));
                if (parent == null) {
                    log.warn("Parent not found for: {} (parent: {})", vsDef.name, vsDef.parentName);
                    continue;
                }
            }

            Category category = categoriesByName.get(normalizeForSearch(vsDef.name));
            if (category == null) {
                // Създај липсваща
                category = new Category();
                category.setNameBg(vsDef.name);
                category.setNameEn(vsDef.nameEn);
                category.setSlug(generateSlug(vsDef.name));
                category.setParent(parent);
                category.setSortOrder(vsDef.sortOrder);
                category.setPlatform(Platform.TEKRA);
                category.setShow(true);
                category = categoryRepository.save(category);
                categoriesByName.put(normalizeForSearch(category.getNameBg()), category);
                log.info("Created Tekra category: {}", vsDef.name);
            } else {
                // Актуализирај съществуваща (САМО parent и sortOrder)
                boolean changed = false;

                if (!parent.equals(category.getParent())) {
                    category.setParent(parent);
                    changed = true;
                    moved++;
                }

                if (category.getSortOrder() != vsDef.sortOrder) {
                    category.setSortOrder(vsDef.sortOrder);
                    changed = true;
                }

                if (changed) {
                    categoryRepository.save(category);
                    log.debug("Updated Tekra category: {}", vsDef.name);
                }
            }
        }

        return moved;
    }

    /**
     * Актуализира category_path за всички категории
     * V3 - БЕЗ N+1 проблем - Зарежда ВСИЧКО наведнъж
     */
    private void updateAllCategoryPaths() {
        log.info("Updating category paths...");
        long startTime = System.currentTimeMillis();

        // ✅ СТЪПКА 1: Зареди ВСИЧКИ категории с parent релациите наведнъж
        log.info("Loading all categories with parent relationships...");
        List<Category> allCategories = categoryRepository.findAll();

        // ✅ СТЪПКА 2: Build category map за бърз достъп
        Map<Long, Category> categoryMap = new HashMap<>();
        for (Category cat : allCategories) {
            categoryMap.put(cat.getId(), cat);
        }

        // ✅ СТЪПКА 3: Eager load ALL parent relationships (force Hibernate)
        log.info("Force loading {} parent relationships...", allCategories.size());
        int loadedParents = 0;
        for (Category cat : allCategories) {
            try {
                if (cat.getParent() != null) {
                    cat.getParent().getId(); // Force load
                    loadedParents++;
                }
            } catch (Exception e) {
                log.warn("Could not load parent for category {}: {}", cat.getId(), e.getMessage());
            }
        }
        log.info("Loaded {} parent relationships", loadedParents);

        // ✅ СТЪПКА 4: Generate paths БЕЗ database calls
        List<Category> toUpdate = new ArrayList<>();
        int processed = 0;
        int errors = 0;
        int circular = 0;

        for (Category category : allCategories) {
            try {
                String oldPath = category.getCategoryPath();

                // Generate NEW path with circular reference protection
                String newPath = generatePathSafe(category, categoryMap);

                if (!Objects.equals(oldPath, newPath)) {
                    category.setCategoryPath(newPath);
                    toUpdate.add(category);
                }

                processed++;
                if (processed % 100 == 0) {
                    log.info("Processed {}/{} categories", processed, allCategories.size());
                }

            } catch (CircularReferenceException e) {
                circular++;
                log.error("❌ CIRCULAR REFERENCE: {}", e.getMessage());
            } catch (Exception e) {
                errors++;
                log.error("Error processing category {}: {}", category.getId(), e.getMessage());
            }
        }

        long processingTime = System.currentTimeMillis() - startTime;
        log.info("Path generation complete in {}ms", processingTime);
        log.info("Stats: Processed={}, ToUpdate={}, Errors={}, Circular={}",
                processed, toUpdate.size(), errors, circular);

        // ✅ СТЪПКА 5: Batch update
        if (!toUpdate.isEmpty()) {
            log.info("Batch updating {} categories with new paths", toUpdate.size());
            categoryRepository.saveAll(toUpdate);
            categoryRepository.flush();
        }

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("✅ Category paths updated successfully in {}ms", totalTime);
    }

    /**
     * Generate category path БЕЗ database calls, с circular reference protection
     */
    private String generatePathSafe(Category category, Map<Long, Category> categoryMap) {
        List<String> pathParts = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        Category current = category;
        int depth = 0;

        while (current != null) {
            // Guard 1: Max depth
            if (depth > 20) {
                log.warn("⚠️ Max depth (20) reached for category: {}", category.getNameBg());
                break;
            }

            // Guard 2: Circular reference
            if (visited.contains(current.getId())) {
                throw new CircularReferenceException(
                        String.format("Category %d (%s) has circular parent reference",
                                current.getId(), current.getNameBg()));
            }
            visited.add(current.getId());

            // Build path part
            if (current.getTekraSlug() != null && !current.getTekraSlug().trim().isEmpty()) {
                pathParts.add(0, current.getTekraSlug());
            } else if (current.getSlug() != null) {
                String baseSlug = extractBaseSlug(current.getSlug(), current.getParent());
                pathParts.add(0, baseSlug);
            }

            // Move to parent БЕЗ нова заявка (вече е заредено)
            if (current.getParent() != null) {
                Long parentId = current.getParent().getId();
                current = categoryMap.get(parentId);

                // Ако parent не е в map (не трябва да се случи)
                if (current == null) {
                    log.warn("Parent {} not found in map for category {}",
                            parentId, category.getNameBg());
                    break;
                }
            } else {
                current = null;
            }

            depth++;
        }

        return pathParts.isEmpty() ? null : String.join("/", pathParts);
    }

    /**
     * Extract base slug (копие от Category entity)
     */
    private String extractBaseSlug(String fullSlug, Category parent) {
        if (parent == null || parent.getSlug() == null) {
            return fullSlug;
        }

        String parentSlug = parent.getSlug();
        if (fullSlug.startsWith(parentSlug + "-")) {
            return fullSlug.substring(parentSlug.length() + 1);
        }

        return fullSlug;
    }

    /**
     * Exception за circular reference detection
     */
    private static class CircularReferenceException extends RuntimeException {
        public CircularReferenceException(String message) {
            super(message);
        }
    }

    private String normalizeForSearch(String name) {
        if (name == null) return "";
        return name.toLowerCase()
                .trim()
                .replaceAll("\\s+", " ");
    }

    private String generateSlug(String name) {
        if (name == null) return "category";

        return name.toLowerCase()
                .replaceAll("[^a-zа-я0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    // ===================================================================
    // СТРУКТУРА НА КАТЕГОРИИТЕ
    // ===================================================================

    // Мапинг на Vali категории към желаните имена
    private static final Map<String, String> VALI_CATEGORY_RENAMES = Map.ofEntries(
            Map.entry("компютърни системи", "КОМПЮТРИ"),
            Map.entry("pc системи", "Настолни компютри"),
            Map.entry("сървърни системи", "Сървъри"),
            Map.entry("тънки клиенти", "Работни станции"),
            Map.entry("мобилни телефони и аксесоари", "Смарт часовници, телефони и аксесоари"),
            Map.entry("непрекъсваеми токозахранващи устройства", "Непрекъсваеми токозахранващи устройства")
    );

    private static final List<MainCategoryDef> MAIN_CATEGORIES = List.of(
            new MainCategoryDef("КОМПЮТРИ", "Computers", 1),
            new MainCategoryDef("Компютърни компоненти", "Computer Components", 2),
            new MainCategoryDef("Лаптопи, таблети и аксесоари", "Laptops, Tablets and Accessories", 3),
            new MainCategoryDef("Монитори и дисплеи", "Monitors and Displays", 4),
            new MainCategoryDef("Компютърна периферия", "Computer Peripherals", 5),
            new MainCategoryDef("Геймърска периферия", "Gaming Peripherals", 6),
            new MainCategoryDef("Принтери, скенери и консумативи", "Printers, Scanners and Consumables", 7),
            new MainCategoryDef("Рутери и мрежово оборудване", "Routers and Network Equipment", 8),
            new MainCategoryDef("Видеонаблюдение", "Video Surveillance", 9),
            new MainCategoryDef("Непрекъсваеми токозахранващи устройства", "UPS", 10),
            new MainCategoryDef("Сторидж у-ва и консумативи", "Storage and Consumables", 11),
            new MainCategoryDef("Батерии и зарядни у-ва", "Batteries and Chargers", 12),
            new MainCategoryDef("Кабели", "Cables", 13),
            new MainCategoryDef("Фото и видео аксесоари", "Photo and Video Accessories", 14),
            new MainCategoryDef("TV, Видео и аксесоари", "TV, Video and Accessories", 15),
            new MainCategoryDef("Електроника и роботика", "Electronics and Robotics", 16),
            new MainCategoryDef("Навигации, камери и аксесоари за коли", "Navigation, Cameras and Car Accessories", 17),
            new MainCategoryDef("Смарт часовници, телефони и аксесоари", "Smart Watches, Phones and Accessories", 18),
            new MainCategoryDef("Проектори, интерактивен под, стойки", "Projectors, Interactive Floor, Stands", 19),
            new MainCategoryDef("Софтуер", "Software", 20),
            new MainCategoryDef("Водно охлаждане", "Water Cooling", 21),
            new MainCategoryDef("VR - Виртуална реалност", "VR - Virtual Reality", 22),
            new MainCategoryDef("Офис продукти", "Office Products", 23),
            new MainCategoryDef("Инструменти и аксесоари", "Tools and Accessories", 24),
            new MainCategoryDef("STEM", "STEM", 25),
            new MainCategoryDef("СЕРВИЗНИ УСЛУГИ", "Service", 26)
    );

    // Vali подкатегории (само най-често използваните, останалите ще останат където са)
    private static final List<SubCategoryDef> VALI_SUBCATEGORIES = List.of(
            // КОМПЮТРИ (вече е преименувана от "Компютърни системи")
            new SubCategoryDef("КОМПЮТРИ", "Настолни компютри", "Desktop Computers", 1),
            new SubCategoryDef("КОМПЮТРИ", "Сървъри", "Servers", 2),
            new SubCategoryDef("КОМПЮТРИ", "Работни станции", "Workstations", 3),
            new SubCategoryDef("КОМПЮТРИ", "Аксесоари за компютри", "Computer Accessories", 4),

            // Компютърни компоненти
            new SubCategoryDef("Компютърни компоненти", "Дънни платки", "Motherboards", 1),
            new SubCategoryDef("Компютърни компоненти", "Solid State Drive (SSD) дискове", "SSD Drives", 2),
            new SubCategoryDef("Компютърни компоненти", "Процесори", "Processors", 3),
            new SubCategoryDef("Компютърни компоненти", "Хард дискове - 3.5\"", "Hard Drives 3.5\"", 4),
            new SubCategoryDef("Компютърни компоненти", "Охладители за процесори", "CPU Coolers", 5),
            new SubCategoryDef("Компютърни компоненти", "Хард дискове - 2.5\"", "Hard Drives 2.5\"", 6),
            new SubCategoryDef("Компютърни компоненти", "Памети", "RAM", 7),
            new SubCategoryDef("Компютърни компоненти", "Вентилатори", "Fans", 8),
            new SubCategoryDef("Компютърни компоненти", "Памети за лаптоп", "Laptop RAM", 9),
            new SubCategoryDef("Компютърни компоненти", "Контролери за вентилатори", "Fan Controllers", 10),
            new SubCategoryDef("Компютърни компоненти", "Видео карти", "Graphics Cards", 11),
            new SubCategoryDef("Компютърни компоненти", "Охладители за видео карти", "GPU Coolers", 12),
            new SubCategoryDef("Компютърни компоненти", "Термо пасти и подложки", "Thermal Paste and Pads", 13),
            new SubCategoryDef("Компютърни компоненти", "Захранвания", "Power Supplies", 14),
            new SubCategoryDef("Компютърни компоненти", "Чекмеджета за дискове", "Drive Caddies", 15),
            new SubCategoryDef("Компютърни компоненти", "Кутии за компютри", "PC Cases", 16),
            new SubCategoryDef("Компютърни компоненти", "TV Тунери и Видео устройства", "TV Tuners and Video Devices", 17),
            new SubCategoryDef("Компютърни компоненти", "Оптични устройства", "Optical Drives", 18),
            new SubCategoryDef("Компютърни компоненти", "Контролери / RAID Контролери", "Controllers / RAID Controllers", 19),
            new SubCategoryDef("Компютърни компоненти", "Входно-изходни контролери", "I/O Controllers", 20),
            new SubCategoryDef("Компютърни компоненти", "Звукови карти", "Sound Cards", 21),

            // Лаптопи, таблети и аксесоари
            new SubCategoryDef("Лаптопи, таблети и аксесоари", "Лаптопи", "Laptops", 1),
            new SubCategoryDef("Лаптопи, таблети и аксесоари", "Таблети", "Tablets", 2),
            new SubCategoryDef("Лаптопи, таблети и аксесоари", "Чанти за лаптопи", "Laptop Bags", 3),
            new SubCategoryDef("Лаптопи, таблети и аксесоари", "Графични таблети", "Graphics Tablets", 4),
            new SubCategoryDef("Лаптопи, таблети и аксесоари", "Захранвания за лаптопи", "Laptop Chargers", 5),
            new SubCategoryDef("Лаптопи, таблети и аксесоари", "Калъфи за таблети", "Tablet Cases", 6),
            new SubCategoryDef("Лаптопи, таблети и аксесоари", "Охлаждащи поставки за лаптопи", "Laptop Cooling Pads", 7),
            new SubCategoryDef("Лаптопи, таблети и аксесоари", "Поставки за таблет", "Tablet Stands", 8),
            new SubCategoryDef("Лаптопи, таблети и аксесоари", "Аксесоари за лаптопи/таблети", "Laptop/Tablet Accessories", 9),
            new SubCategoryDef("Лаптопи, таблети и аксесоари", "Аксесоари за графични таблети", "Graphics Tablet Accessories", 10),
            new SubCategoryDef("Лаптопи, таблети и аксесоари", "Заключващи устройства за лаптоп", "Laptop Locks", 11),
            new SubCategoryDef("Лаптопи, таблети и аксесоари", "Колички и шкафове за зареждане", "Charging Carts", 12),

            // Монитори и дисплеи - ✅ СПОДЕЛЕНИ С TEKRA
            new SubCategoryDef("Монитори и дисплеи", "Монитори", "Monitors", 1),
            new SubCategoryDef("Монитори и дисплеи", "Стойки за монитори", "Monitor Stands", 2),
            new SubCategoryDef("Монитори и дисплеи", "Интерактивни дисплеи", "Interactive Displays", 3),
            new SubCategoryDef("Монитори и дисплеи", "Системи за монтаж", "Mounting Systems", 4),

            // Компютърна периферия
            new SubCategoryDef("Компютърна периферия", "Мишки", "Mice", 1),
            new SubCategoryDef("Компютърна периферия", "USB памети", "USB Flash Drives", 2),
            new SubCategoryDef("Компютърна периферия", "Падове за мишки", "Mouse Pads", 3),
            new SubCategoryDef("Компютърна периферия", "Външни дискове", "External HDDs", 4),
            new SubCategoryDef("Компютърна периферия", "Клавиатури", "Keyboards", 5),
            new SubCategoryDef("Компютърна периферия", "Външни SSD", "External SSDs", 6),
            new SubCategoryDef("Компютърна периферия", "Слушалки", "Headphones", 7),
            new SubCategoryDef("Компютърна периферия", "USB хъбове", "USB Hubs", 8),
            new SubCategoryDef("Компютърна периферия", "Слушалки (тапи)", "Earbuds", 9),
            new SubCategoryDef("Компютърна периферия", "Уеб камери", "Webcams", 10),
            new SubCategoryDef("Компютърна периферия", "Микрофони", "Microphones", 11),
            new SubCategoryDef("Компютърна периферия", "Четци за карти", "Card Readers", 12),
            new SubCategoryDef("Компютърна периферия", "Звукови системи и тонколони", "Audio Systems and Speakers", 13),

            // Геймърска периферия
            new SubCategoryDef("Геймърска периферия", "Геймърски мишки", "Gaming Mice", 1),
            new SubCategoryDef("Геймърска периферия", "Гейминг конзоли", "Gaming Consoles", 2),
            new SubCategoryDef("Геймърска периферия", "Геймърски клавиатури", "Gaming Keyboards", 3),
            new SubCategoryDef("Геймърска периферия", "Волани и педали", "Wheels and Pedals", 4),
            new SubCategoryDef("Геймърска периферия", "Геймърски падове", "Gaming Mouse Pads", 5),
            new SubCategoryDef("Геймърска периферия", "Геймпадове", "Gamepads", 6),
            new SubCategoryDef("Геймърска периферия", "Геймърски слушалки", "Gaming Headsets", 7),
            new SubCategoryDef("Геймърска периферия", "Аксесоари за Волани", "Wheel Accessories", 8),
            new SubCategoryDef("Геймърска периферия", "Геймърски бюра", "Gaming Desks", 9),
            new SubCategoryDef("Геймърска периферия", "Геймърски аксесоари", "Gaming Accessories", 10),
            new SubCategoryDef("Геймърска периферия", "Геймърски столове", "Gaming Chairs", 11),
            new SubCategoryDef("Геймърска периферия", "Фигурки", "Figurines", 12),
            new SubCategoryDef("Геймърска периферия", "Компютърни и геймърски очила", "Computer and Gaming Glasses", 13),
            new SubCategoryDef("Геймърска периферия", "Дрехи и аксесоари", "Clothes and Accessories", 14),

            // Принтери, скенери и консумативи
            new SubCategoryDef("Принтери, скенери и консумативи", "Лазерни принтери", "Laser Printers", 1),
            new SubCategoryDef("Принтери, скенери и консумативи", "Мултифункционални устройства", "Multifunctional Devices", 2),
            new SubCategoryDef("Принтери, скенери и консумативи", "Принтери и МФУ устройства под наем", "Printers and MFPs for Rent", 3),
            new SubCategoryDef("Принтери, скенери и консумативи", "Принтери за текстил", "Textile Printers", 4),
            new SubCategoryDef("Принтери, скенери и консумативи", "Скенери", "Scanners", 5),
            new SubCategoryDef("Принтери, скенери и консумативи", "Аксесоари за принтери", "Printer Accessories", 6),
            new SubCategoryDef("Принтери, скенери и консумативи", "Аксесоари за скенери", "Scanner Accessories", 7),
            new SubCategoryDef("Принтери, скенери и консумативи", "Консумативи(тонери) за лазерни устройства", "Laser Consumables (Toners)", 8),
            new SubCategoryDef("Принтери, скенери и консумативи", "Консумативи за мастиленоструйни устройства", "Inkjet Consumables", 9),
            new SubCategoryDef("Принтери, скенери и консумативи", "Касети за матрични принтери", "Dot Matrix Cartridges", 10),
            new SubCategoryDef("Принтери, скенери и консумативи", "Бутилки с тонер", "Toner Bottles", 11),
            new SubCategoryDef("Принтери, скенери и консумативи", "Наливни мастила за принтери", "Refill Inks", 12),
            new SubCategoryDef("Принтери, скенери и консумативи", "Хартия за принтери", "Printer Paper", 13),
            new SubCategoryDef("Принтери, скенери и консумативи", "Лазерни гравиращи машини", "Laser Engraving Machines", 14),
            new SubCategoryDef("Принтери, скенери и консумативи", "3D Принтери", "3D Printers", 15),
            new SubCategoryDef("Принтери, скенери и консумативи", "3D Скенери", "3D Scanners", 16),
            new SubCategoryDef("Принтери, скенери и консумативи", "Консумативи за 3D принтери", "3D Printer Consumables", 17),
            new SubCategoryDef("Принтери, скенери и консумативи", "Аксесоари за 3D принтери", "3D Printer Accessories", 18),
            new SubCategoryDef("Принтери, скенери и консумативи", "Преносими принтери", "Portable Printers", 19),
            new SubCategoryDef("Принтери, скенери и консумативи", "Консумативи за преносими принтери", "Portable Printer Consumables", 20),

            // Рутери и мрежово оборудване
            new SubCategoryDef("Рутери и мрежово оборудване", "Рутери", "Routers", 1),
            new SubCategoryDef("Рутери и мрежово оборудване", "Мрежови карти", "Network Cards", 2),
            new SubCategoryDef("Рутери и мрежово оборудване", "Безжични рутери", "Wireless Routers", 3),
            new SubCategoryDef("Рутери и мрежово оборудване", "Защитни стени", "Firewalls", 4),
            new SubCategoryDef("Рутери и мрежово оборудване", "Суичове - управляеми", "Managed Switches", 5),
            new SubCategoryDef("Рутери и мрежово оборудване", "KVM Суичове", "KVM Switches", 6),
            new SubCategoryDef("Рутери и мрежово оборудване", "Суичове - неуправляеми", "Unmanaged Switches", 7),
            new SubCategoryDef("Рутери и мрежово оборудване", "Powerline адаптери", "Powerline Adapters", 8),
            new SubCategoryDef("Рутери и мрежово оборудване", "Access Point", "Access Points", 9),
            new SubCategoryDef("Рутери и мрежово оборудване", "IP Камери", "IP Cameras", 10),
            new SubCategoryDef("Рутери и мрежово оборудване", "Безжични адаптери", "Wireless Adapters", 11),
            new SubCategoryDef("Рутери и мрежово оборудване", "Кабелни канали и шлаух спирали", "Cable Ducts and Cable Spirals", 12),
            new SubCategoryDef("Рутери и мрежово оборудване", "Блутут адаптери", "Bluetooth Adapters", 13),
            new SubCategoryDef("Рутери и мрежово оборудване", "Комуникационни шкафове - RACK", "Server Racks", 14),

            // Непрекъсваеми токозахранващи устройства
            new SubCategoryDef("Непрекъсваеми токозахранващи устройства", "Управляеми UPS-и", "Managed UPS", 1),
            new SubCategoryDef("Непрекъсваеми токозахранващи устройства", "Неуправляеми UPS-и", "Unmanaged UPS", 2),
            new SubCategoryDef("Непрекъсваеми токозахранващи устройства", "Аксесоари UPS", "UPS Accessories", 3),
            new SubCategoryDef("Непрекъсваеми токозахранващи устройства", "Стабилизатори", "Voltage Stabilizers", 4),
            new SubCategoryDef("Непрекъсваеми токозахранващи устройства", "Инвертори", "Inverters", 5),

            // Сторидж у-ва и консумативи - ✅ СПОДЕЛЕНИ С TEKRA (Твърди дискове)
            new SubCategoryDef("Сторидж у-ва и консумативи", "Твърди дискове", "Hard Drives", 1),
            new SubCategoryDef("Сторидж у-ва и консумативи", "Аксесоари за дискове", "Drive Accessories", 2),

            // Батерии и зарядни у-ва
            new SubCategoryDef("Батерии и зарядни у-ва", "Батерии", "Batteries", 1),
            new SubCategoryDef("Батерии и зарядни у-ва", "Батерии за мобилни телефони", "Mobile Phone Batteries", 2),
            new SubCategoryDef("Батерии и зарядни у-ва", "Батерии за лаптопи", "Laptop Batteries", 3),
            new SubCategoryDef("Батерии и зарядни у-ва", "Батерии за фотоапарати и камкордери", "Camera Batteries", 4),
            new SubCategoryDef("Батерии и зарядни у-ва", "Батерии за UPS и СОТ-аларми", "UPS and Alarm Batteries", 5),
            new SubCategoryDef("Батерии и зарядни у-ва", "Батерии за безжични телефони", "Cordless Phone Batteries", 6),
            new SubCategoryDef("Батерии и зарядни у-ва", "Батерии за радиостанции", "Radio Batteries", 7),
            new SubCategoryDef("Батерии и зарядни у-ва", "Батерии за баркод скенери", "Barcode Scanner Batteries", 8),
            new SubCategoryDef("Батерии и зарядни у-ва", "Зарядни устройства и адаптери", "Chargers and Adapters", 9),
            new SubCategoryDef("Батерии и зарядни у-ва", "Батерии за дистанционни за кран", "Crane Remote Batteries", 10),
            new SubCategoryDef("Батерии и зарядни у-ва", "Батерии за прахосмукачки", "Vacuum Cleaner Batteries", 11),
            new SubCategoryDef("Батерии и зарядни у-ва", "Батерии за косачки роботи", "Robotic Mower Batteries", 12),

            // Кабели
            new SubCategoryDef("Кабели", "Видео кабели", "Video Cables", 1),
            new SubCategoryDef("Кабели", "Кабели за мобилни устройства", "Mobile Device Cables", 2),
            new SubCategoryDef("Кабели", "PC кабели", "PC Cables", 3),
            new SubCategoryDef("Кабели", "Захранващи кабели", "Power Cables", 4),
            new SubCategoryDef("Кабели", "Мрежови кабели", "Network Cables", 5),
            new SubCategoryDef("Кабели", "Оптични кабели", "Optical Cables", 6),
            new SubCategoryDef("Кабели", "Аудио кабели", "Audio Cables", 7),
            new SubCategoryDef("Кабели", "Антенни кабели", "Antenna Cables", 8),
            new SubCategoryDef("Кабели", "Кабели за принтери", "Printer Cables", 9),
            new SubCategoryDef("Кабели", "Адаптери, конвертори", "Adapters, Converters", 10),

            // Фото и видео аксесоари
            new SubCategoryDef("Фото и видео аксесоари", "Фотоалбуми", "Photo Albums", 1),
            new SubCategoryDef("Фото и видео аксесоари", "Фото рамки", "Photo Frames", 2),
            new SubCategoryDef("Фото и видео аксесоари", "Стативи /Триподи/", "Tripods", 3),
            new SubCategoryDef("Фото и видео аксесоари", "Селфи стикове", "Selfie Sticks", 4),
            new SubCategoryDef("Фото и видео аксесоари", "Чанти за камери и камкордери", "Camera Bags", 5),
            new SubCategoryDef("Фото и видео аксесоари", "Карти памет", "Memory Cards", 6),
            new SubCategoryDef("Фото и видео аксесоари", "Аксесоари за обективи", "Lens Accessories", 7),
            new SubCategoryDef("Фото и видео аксесоари", "Други фото и видео аксесоари", "Other Photo/Video Accessories", 8),

            // TV, Видео и аксесоари
            new SubCategoryDef("TV, Видео и аксесоари", "Телевизори", "Televisions", 1),
            new SubCategoryDef("TV, Видео и аксесоари", "Стойки за TV и високоговорители", "TV and Speaker Stands", 2),
            new SubCategoryDef("TV, Видео и аксесоари", "Дистанционни", "Remote Controls", 3),
            new SubCategoryDef("TV, Видео и аксесоари", "Антени", "Antennas", 4),
            new SubCategoryDef("TV, Видео и аксесоари", "Аксесоари за TV", "TV Accessories", 5),

            // Електроника и роботика
            new SubCategoryDef("Електроника и роботика", "Преносими тонколони", "Portable Speakers", 1),
            new SubCategoryDef("Електроника и роботика", "Аудио микрофони", "Audio Microphones", 2),
            new SubCategoryDef("Електроника и роботика", "Мултимедийни плейъри", "Media Players", 3),
            new SubCategoryDef("Електроника и роботика", "Радиа", "Radios", 4),
            new SubCategoryDef("Електроника и роботика", "eBook четци", "eBook Readers", 5),
            new SubCategoryDef("Електроника и роботика", "eBook аксесоари", "eBook Accessories", 6),
            new SubCategoryDef("Електроника и роботика", "Роботика", "Robotics", 7),

            // Навигации, камери и аксесоари за коли
            new SubCategoryDef("Навигации, камери и аксесоари за коли", "Навигации", "Navigation", 1),
            new SubCategoryDef("Навигации, камери и аксесоари за коли", "Камери/Рекордери", "Cameras/Recorders", 2),
            new SubCategoryDef("Навигации, камери и аксесоари за коли", "Аксесоари за автомобили", "Car Accessories", 3),

            // Смарт часовници, телефони и аксесоари
            new SubCategoryDef("Смарт часовници, телефони и аксесоари", "Мобилни телефони", "Mobile Phones", 1),
            new SubCategoryDef("Смарт часовници, телефони и аксесоари", "Смартчасовници", "Smart Watches", 2),
            new SubCategoryDef("Смарт часовници, телефони и аксесоари", "Фитнес гривни", "Fitness Bands", 3),
            new SubCategoryDef("Смарт часовници, телефони и аксесоари", "Каишки за смартчасовници", "Smart Watch Bands", 4),
            new SubCategoryDef("Смарт часовници, телефони и аксесоари", "Bluetooth слушалки", "Bluetooth Headphones", 5),
            new SubCategoryDef("Смарт часовници, телефони и аксесоари", "Външни батерии", "Power Banks", 6),
            new SubCategoryDef("Смарт часовници, телефони и аксесоари", "Зарядни за телефони", "Phone Chargers", 7),
            new SubCategoryDef("Смарт часовници, телефони и аксесоари", "Стилус - химикалки за телефони", "Stylus Pens", 8),
            new SubCategoryDef("Смарт часовници, телефони и аксесоари", "Калъфи за телефони", "Phone Cases", 9),
            new SubCategoryDef("Смарт часовници, телефони и аксесоари", "Поставки за мобилни телефони", "Phone Stands", 10),
            new SubCategoryDef("Смарт часовници, телефони и аксесоари", "Защитни фолиа / стъкла за телефони", "Screen Protectors", 11),
            new SubCategoryDef("Смарт часовници, телефони и аксесоари", "Други мобилни аксесоари", "Other Mobile Accessories", 12),

            // Проектори, интерактивен под, стойки
            new SubCategoryDef("Проектори, интерактивен под, стойки", "Проектори", "Projectors", 1),
            new SubCategoryDef("Проектори, интерактивен под, стойки", "Интерактивен под", "Interactive Floor", 2),
            new SubCategoryDef("Проектори, интерактивен под, стойки", "Екрани", "Screens", 3),
            new SubCategoryDef("Проектори, интерактивен под, стойки", "Стойки и аксесоари", "Stands and Accessories", 4),

            // Софтуер
            new SubCategoryDef("Софтуер", "Антивирусни програми", "Antivirus Software", 1),
            new SubCategoryDef("Софтуер", "Office & Desktop & Bussiness приложения", "Office & Desktop & Business Apps", 2),
            new SubCategoryDef("Софтуер", "Софтуер за управление на печата", "Print Management Software", 3),
            new SubCategoryDef("Софтуер", "Творчество и дизайн", "Creativity and Design", 4),
            new SubCategoryDef("Софтуер", "Софтуер за образованието", "Educational Software", 5),

            // Водно охлаждане
            new SubCategoryDef("Водно охлаждане", "Блокове за процесори", "CPU Blocks", 1),
            new SubCategoryDef("Водно охлаждане", "Блокове за видеокарти", "GPU Blocks", 2),
            new SubCategoryDef("Водно охлаждане", "Радиатори", "Radiators", 3),
            new SubCategoryDef("Водно охлаждане", "Помпи и резервоари", "Pumps and Reservoirs", 4),
            new SubCategoryDef("Водно охлаждане", "Фитинги", "Fittings", 5),
            new SubCategoryDef("Водно охлаждане", "Тръби", "Tubes", 6),
            new SubCategoryDef("Водно охлаждане", "Течности", "Coolants", 7),

            // VR - Виртуална реалност
            new SubCategoryDef("VR - Виртуална реалност", "VR очила", "VR Headsets", 1),
            new SubCategoryDef("VR - Виртуална реалност", "VR Ready PC", "VR Ready PC", 2),

            // Офис продукти
            new SubCategoryDef("Офис продукти", "Шредери", "Shredders", 1),
            new SubCategoryDef("Офис продукти", "Ламинатори", "Laminators", 2),
            new SubCategoryDef("Офис продукти", "Класьори за CD, DVD", "CD/DVD Binders", 3),
            new SubCategoryDef("Офис продукти", "Ламиниращо фолио", "Laminating Film", 4),
            new SubCategoryDef("Офис продукти", "Почистващи материали", "Cleaning Materials", 5),
            new SubCategoryDef("Офис продукти", "Гилотини", "Guillotines", 6),
            new SubCategoryDef("Офис продукти", "Носители CD, DVD, Blu-Ray", "CD, DVD, Blu-Ray Media", 7),

            // Инструменти и аксесоари
            new SubCategoryDef("Инструменти и аксесоари", "Комплекти за сервиране", "Service Kits", 1),
            new SubCategoryDef("Инструменти и аксесоари", "Кутии за храна", "Food Containers", 2),
            new SubCategoryDef("Инструменти и аксесоари", "Фенери", "Flashlights", 3),
            new SubCategoryDef("Инструменти и аксесоари", "Уреди за измерване и контрол", "Measuring and Control Devices", 4),
            new SubCategoryDef("Инструменти и аксесоари", "Инструменти", "Tools", 5),
            new SubCategoryDef("Инструменти и аксесоари", "Отвертки", "Screwdrivers", 6),
            new SubCategoryDef("Инструменти и аксесоари", "Метео - станции и термометри", "Weather Stations and Thermometers", 7),
            new SubCategoryDef("Инструменти и аксесоари", "Таймери", "Timers", 8),
            new SubCategoryDef("Инструменти и аксесоари", "Часовници", "Clocks", 9),

            // STEM
            new SubCategoryDef("STEM", "Роботика и кибер-физични системи", "Robotics and Cyber-Physical Systems", 1),
            new SubCategoryDef("STEM", "Дизайн и 3D прототипиране", "Design and 3D Prototyping", 2),
            new SubCategoryDef("STEM", "Природни науки", "Natural Sciences", 3),
            new SubCategoryDef("STEM", "Зелени технологии", "Green Technologies", 4),
            new SubCategoryDef("STEM", "Математика и информатика", "Mathematics and Computer Science", 5),
            new SubCategoryDef("STEM", "ВОСКС", "STEAM", 6),
            new SubCategoryDef("STEM", "Специализирано оборудване", "Specialized Equipment", 7)
    );

    // ✅ КОРИГИРАНА Видеонаблюдение структура
    private static final List<VideoSurveillanceDef> VIDEO_SURVEILLANCE_STRUCTURE = List.of(
            // Level 2 - Главни категории под Видеонаблюдение
            new VideoSurveillanceDef(null, "Твърди дискове", "Hard Drives", 1),
            new VideoSurveillanceDef(null, "Монитори и аксесоари", "Monitors and Accessories", 2),
            new VideoSurveillanceDef(null, "HD аналогови системи", "HD Analog Systems", 3),
            new VideoSurveillanceDef(null, "Консумативи", "Consumables", 4),
            new VideoSurveillanceDef(null, "NDAA сертифицирани системи", "NDAA Certified Systems", 5),
            new VideoSurveillanceDef(null, "IP системи", "IP Systems", 6),

            // Under "Монитори и аксесоари"
            new VideoSurveillanceDef("Монитори и аксесоари", "Монитори", "Monitors", 1),
            new VideoSurveillanceDef("Монитори и аксесоари", "Интерактивни дисплеи", "Interactive Displays", 2),
            new VideoSurveillanceDef("Монитори и аксесоари", "Консумативи", "Consumables", 3),

            // Under "HD аналогови системи"
            new VideoSurveillanceDef("HD аналогови системи", "Камери", "Cameras", 1),
            new VideoSurveillanceDef("HD аналогови системи", "Записващи устройства - DVR", "Recording Devices - DVR", 2),

            // Under "Консумативи" (main Level 2)
            new VideoSurveillanceDef("Консумативи", "Адаптери", "Adapters", 1),
            new VideoSurveillanceDef("Консумативи", "Захранващи блокове", "Power Supplies", 2),
            new VideoSurveillanceDef("Консумативи", "Конектори", "Connectors", 3),
            new VideoSurveillanceDef("Консумативи", "Видеобалуни", "Video Baluns", 4),
            new VideoSurveillanceDef("Консумативи", "Стойки и основи за камери", "Camera Mounts and Brackets", 5),
            new VideoSurveillanceDef("Консумативи", "Защити и изолатори", "Protectors and Insulators", 6),
            new VideoSurveillanceDef("Консумативи", "Други", "Other", 7),

            // Under "NDAA сертифицирани системи"
            new VideoSurveillanceDef("NDAA сертифицирани системи", "NVR", "NVR", 1),
            new VideoSurveillanceDef("NDAA сертифицирани системи", "Основи за монтаж на камери", "Camera Mounting Bases", 2),
            new VideoSurveillanceDef("NDAA сертифицирани системи", "Комплект", "Kit", 3),
            new VideoSurveillanceDef("NDAA сертифицирани системи", "Сървър", "Server", 4),
            new VideoSurveillanceDef("NDAA сертифицирани системи", "Рутери и мрежово оборудване", "Routers and Network Equipment", 5),
            new VideoSurveillanceDef("NDAA сертифицирани системи", "IP камери и смарт у-ва", "IP Cameras and Smart Devices", 6),

            // Under "IP системи"
            new VideoSurveillanceDef("IP системи", "Мрежови устройства", "Network Devices", 1),
            new VideoSurveillanceDef("IP системи", "Сървъри", "Servers", 2),
            new VideoSurveillanceDef("IP системи", "Камери", "Cameras", 3),
            new VideoSurveillanceDef("IP системи", "Записващи устройства - NVR", "Recording Devices - NVR", 4)
    );

    // ===================================================================
    // HELPER CLASSES
    // ===================================================================

    private static class MainCategoryDef {
        String name;
        String nameEn;
        int sortOrder;

        MainCategoryDef(String name, String nameEn, int sortOrder) {
            this.name = name;
            this.nameEn = nameEn;
            this.sortOrder = sortOrder;
        }
    }

    private static class SubCategoryDef {
        String parentName;
        String name;
        String nameEn;
        int sortOrder;

        SubCategoryDef(String parentName, String name, String nameEn, int sortOrder) {
            this.parentName = parentName;
            this.name = name;
            this.nameEn = nameEn;
            this.sortOrder = sortOrder;
        }
    }

    private static class VideoSurveillanceDef {
        String parentName;
        String name;
        String nameEn;
        int sortOrder;

        VideoSurveillanceDef(String parentName, String name, String nameEn, int sortOrder) {
            this.parentName = parentName;
            this.name = name;
            this.nameEn = nameEn;
            this.sortOrder = sortOrder;
        }
    }

    public static class ReorganizationResult {
        public boolean success;
        public int created;
        public int updated;
        public int moved;
        public long durationMs;

        public ReorganizationResult(boolean success, int created, int updated, int moved, long durationMs) {
            this.success = success;
            this.created = created;
            this.updated = updated;
            this.moved = moved;
            this.durationMs = durationMs;
        }
    }
}