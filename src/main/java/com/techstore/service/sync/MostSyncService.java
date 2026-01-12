package com.techstore.service.sync;

import com.techstore.entity.*;
import com.techstore.enums.Platform;
import com.techstore.enums.ProductStatus;
import com.techstore.repository.*;
import com.techstore.service.MostApiService;
import com.techstore.util.LogHelper;
import com.techstore.util.SyncHelper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import static com.techstore.util.LogHelper.LOG_STATUS_FAILED;
import static com.techstore.util.LogHelper.LOG_STATUS_SUCCESS;

@Service
@RequiredArgsConstructor
@Slf4j
public class MostSyncService {

    private final MostApiService mostApiService;
    private final CategoryRepository categoryRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final ProductRepository productRepository;
    private final ParameterRepository parameterRepository;
    private final ParameterOptionRepository parameterOptionRepository;
    private final EntityManager entityManager;
    private final SyncHelper syncHelper;
    private final LogHelper logHelper;

    private static final String USD_TO_EUR_RATE = "0.8565";

    private static final Map<String, String> MOST_CATEGORY_MAPPING;

    static {
        Map<String, String> map = new HashMap<>();

        // ✅ CPU - map to "Процесори" (ID 3)
        map.put("CPU", "Процесори");
        map.put("AMD Ryzen /AM4", "Процесори");
        map.put("AMD Ryzen /AM5", "Процесори");
        map.put("AMD Ryzen /Internal VGA", "Процесори");
        map.put("AMD Athlon /AM4", "Процесори");
        map.put("AMD A-series /AM4", "Процесори");
        map.put("Intel Core Ultra 200", "Процесори");
        map.put("Core Gen 10th /LGA1200", "Процесори");
        map.put("Core Gen 11th /LGA1200", "Процесори");
        map.put("Core Gen 12th /LGA1700", "Процесори");
        map.put("Core Gen 13th /LGA1700", "Процесори");
        map.put("Core Gen 14th /LGA1700", "Процесори");
        map.put("Celeron Gen10th /LGA1200", "Процесори");
        map.put("Pentium Gen10th /LGA1200", "Процесори");
        map.put("Intel Pentium /Internal VGA", "Процесори");
        map.put("LGA1851 /Intel Core Ultra 200", "Процесори");

        // ✅ GPU - map to "Видео карти" (ID 8)
        map.put("VIDEO CARD", "Видео карти");
        map.put("GeForce RTX5090", "Видео карти");
        map.put("GeForce RTX5080", "Видео карти");
        map.put("GeForce RTX5070TI", "Видео карти");
        map.put("GeForce RTX5070", "Видео карти");
        map.put("GeForce RTX5060TI", "Видео карти");
        map.put("GeForce RTX5060", "Видео карти");
        map.put("GeForce RTX5050", "Видео карти");
        map.put("GeForce RTX4090", "Видео карти");
        map.put("GeForce RTX4080 Super", "Видео карти");
        map.put("GeForce RTX4070TI Super", "Видео карти");
        map.put("GeForce RTX4070TI", "Видео карти");
        map.put("GeForce RTX4070 Super", "Видео карти");
        map.put("GeForce RTX4070", "Видео карти");
        map.put("GeForce RTX4060Ti", "Видео карти");
        map.put("GeForce RTX4060", "Видео карти");
        map.put("GeForce RTX3060", "Видео карти");
        map.put("GeForce RTX3050", "Видео карти");
        map.put("GeForce RTX2070", "Видео карти");
        map.put("GeForce GTX1650", "Видео карти");
        map.put("GeForce GTX1070TI", "Видео карти");
        map.put("GeForce GT1030", "Видео карти");
        map.put("GeForce GT730", "Видео карти");
        map.put("GeForce GT710", "Видео карти");
        map.put("Radeon RX9070XT", "Видео карти");
        map.put("Radeon RX9070", "Видео карти");
        map.put("Radeon RX9060XT", "Видео карти");
        map.put("Radeon RX7900XTX", "Видео карти");
        map.put("Radeon RX7900XT", "Видео карти");
        map.put("Radeon RX7900GRE", "Видео карти");
        map.put("Radeon RX7800XT", "Видео карти");
        map.put("Radeon RX7700XT", "Видео карти");
        map.put("Radeon RX7600XT", "Видео карти");
        map.put("Radeon RX7600", "Видео карти");
        map.put("Radeon RX6700", "Видео карти");
        map.put("Radeon RX6600", "Видео карти");
        map.put("Radeon RX6500XT", "Видео карти");
        map.put("Radeon RX 580", "Видео карти");

        // ✅ RAM - map to "Памети" (ID 6) and "Памети за лаптоп" (ID 7)
        map.put("RAM", "Памети");
        map.put("DDR5", "Памети");
        map.put("DDR4", "Памети");
        map.put("DDR3", "Памети");
        map.put("DDR5 for Notebook", "Памети за лаптоп");
        map.put("DDR4 for Notebook", "Памети за лаптоп");
        map.put("DDR3 for Notebook", "Памети за лаптоп");

        // ✅ Storage - map to existing HDD/SSD categories
        map.put("HDD", "Хард дискове - 3.5\""); // ID 16
        map.put("Internal 3.5 HDD", "Хард дискове - 3.5\""); // ID 16
        map.put("Internal 2.5 HDD / for NB", "Хард дискове - 2.5\""); // ID 15
        map.put("SSD 2.5", "Solid State Drive (SSD) дискове"); // ID 17
        map.put("SSD M2 2280 PCIE", "Solid State Drive (SSD) дискове"); // ID 17
        map.put("SSD M2 2280 SATA", "Solid State Drive (SSD) дискове"); // ID 17
        map.put("External 3.5 HDD", "Външни дискове"); // ID 55
        map.put("External  2.5 HDD", "Външни дискове"); // ID 55
        map.put("External SSD", "Външни SSD"); // ID 56
        map.put("USB Disk", "USB памети"); // ID 57
        map.put("FLASH", "USB памети"); // ID 57
        map.put("Flash", "USB памети"); // ID 57

        // ✅ Motherboards - map to "Дънни платки" (ID 2)
        map.put("MAIN BOARD", "Дънни платки");
        map.put("AMD AM5", "Дънни платки");
        map.put("AMD AM4", "Дънни платки");
        map.put("LGA1200 /Intel 10th and 11th Gen", "Дънни платки");
        map.put("LGA1700 /Intel 12th and 13th Gen", "Дънни платки");
        map.put("LGA1851 /Intel Core Ultra 200", "Дънни платки");

        // ✅ Cases - map to "Кутии за компютри" (ID 11)
        map.put("CASE", "Кутии за компютри");
        map.put("CASE without PSU", "Кутии за компютри");
        map.put("CASE with PSU", "Кутии за компютри");
        map.put("MINI", "Кутии за компютри");

        // ✅ PSU - map to "Захранвания" (ID 9)
        map.put("PSU", "Захранвания");

        // ✅ Cooling - map to "Охладители за процесори" (ID 4) and "Вентилатори" (ID 12)
        map.put("CPU Cooler", "Охладители за процесори"); // ID 4
        map.put("Water Cooler", "Водно охлаждане"); // ID 23
        map.put("CASE fan", "Вентилатори"); // ID 12
        map.put("FAN", "Вентилатори"); // ID 12
        map.put("Thermal Grease", "Термо пасти и подложки"); // ID 14

        // ✅ Keyboards - map to "Клавиатури" (ID 61)
        map.put("KEYBOARD", "Клавиатури");
        map.put("Gaming Wired", "Клавиатури");
        map.put("Gaming Wireless", "Клавиатури");
        map.put("Wired Desktop", "Клавиатури");
        map.put("Wireless Desktop", "Клавиатури");
        map.put("Bluetooth Desktop", "Клавиатури");

        // ✅ Mice - map to "Мишки" (ID 62)
        map.put("MOUSE", "Мишки");
        map.put("Wired", "Мишки");
        map.put("Wireless", "Мишки");
        map.put("Bluetooth", "Мишки");
        map.put("Gaming Pad", "Падове за мишки"); // ID 64
        map.put("Pad", "Падове за мишки"); // ID 64

        // ✅ Monitors - map to "Монитори и публични дисплеи" (ID 50)
        map.put("MONITOR", "Монитори и публични дисплеи");
        map.put("Gaming / 20-25 inch /", "Монитори и публични дисплеи");
        map.put("Gaming / 27 inch and bigger /", "Монитори и публични дисплеи");
        map.put("Home/Office / 27 inch and bigger /", "Монитори и публични дисплеи");
        map.put("Home/Office TN / 18-25 inch /", "Монитори и публични дисплеи");
        map.put("Home/Office IPS / 18-25 inch /", "Монитори и публични дисплеи");
        map.put("Ultrawide / 21:9 /", "Монитори и публични дисплеи");
        map.put("Touch Screen", "Монитори и публични дисплеи");
        map.put("VA Panel", "Монитори и публични дисплеи");
        map.put("OLED", "Монитори и публични дисплеи");

        // ✅ TV & Projectors
        map.put("TV", "Телевизори"); // ID 139
        map.put("TV+Monitor", "Телевизори"); // ID 139
        map.put("Projectors", "Проектори"); // ID 115

        // ✅ Audio
        map.put("Speakers", "Звукови системи и тонколони"); // ID 59
        map.put("Headset and mic", "Слушалки"); // ID 66

        // ✅ Computers - map to "PC системи" (ID 32)
        map.put("COMPUTER", "PC системи");
        map.put("PC", "PC системи");
        map.put("DESKTOP", "PC системи");
        map.put("ALL-IN-ONE", "PC системи");
        map.put("GAMING", "PC системи");

        // ✅ Notebooks - map to "Лаптопи" (ID 37)
        map.put("NOTEBOOK", "Лаптопи");
        map.put("NB ASUS", "Лаптопи");
        map.put("NB LENOVO", "Лаптопи");
        map.put("NB MSI", "Лаптопи");
        map.put("NB ACER", "Лаптопи");
        map.put("NB Others", "Лаптопи");
        map.put("NB Accessories", "Аксесоари за лаптопи/таблети"); // ID 47

        // ✅ Tablets - map to "Таблети" (ID 38)
        map.put("Tablet", "Таблети");
        map.put("Tablet LENOVO", "Таблети");

        // ✅ Printers - map to "Лазерни принтери" (ID 79)
        map.put("PRINTER", "Лазерни принтери");
        map.put("Laser", "Лазерни принтери");
        map.put("Inc", "Лазерни принтери");
        map.put("Printers LaserJet - A4", "Лазерни принтери");
        map.put("Laser Multifunctional", "Мултифункционални устройства"); // ID 80
        map.put("Inc Multifunctional", "Мултифункционални устройства"); // ID 80
        map.put("SCANER", "Скенери"); // ID 83

        // ✅ Consumables - map to "Консумативи за лазерни принтери и копири" (ID 86)
        map.put("CARTRIDGE", "Консумативи за лазерни принтери и копири");
        map.put("HP CONSUMABLES", "Консумативи за лазерни принтери и копири");
        map.put("HP Ink Cartr", "Касети за струйни принтери"); // ID 87
        map.put("HP Laser Cartr", "Консумативи за лазерни принтери и копири");
        map.put("HP Desinjet Cartr", "Касети за струйни принтери");
        map.put("HP Expired Cartr", "Консумативи за лазерни принтери и копири");
        map.put("Ink Cartr", "Касети за струйни принтери");
        map.put("Laser Cartr", "Консумативи за лазерни принтери и копири");
        map.put("Cable for Prinrer", "Кабели за принтери"); // ID 134

        // ✅ Network - map to existing network categories
        map.put("LAN", "Мрежово оборудване");
        map.put("LAN Switch", "Суичове - без контрол"); // ID 109
        map.put("WL Router", "Безжични рутери"); // ID 107
        map.put("LAN Card", "Мрежови карти"); // ID 111
        map.put("WL Card / USB / Device", "Безжични адаптери"); // ID 104
        map.put("LAN Cable", "Мрежови кабели"); // ID 135
        map.put("LAN Accessories", "Аксесоари за лаптопи/таблети"); // Generic

        // ✅ Power - map to "Неуправляеми UPS-и" (ID 74)
        map.put("UPS", "Неуправляеми UPS-и");
        map.put("Line-Interactive", "Неуправляеми UPS-и");
        map.put("Online", "Управляеми UPS-и"); // ID 73
        map.put("Power Bank", "Външни батерии"); // ID 159
        map.put("Batteries", "Батерии и зарядни у-ва"); // ID 197

        // ✅ Other peripherals
        map.put("WEB CAMERA", "Уеб камери"); // ID 60
        map.put("Game Controller", "Геймпадове"); // ID 176
        map.put("GSM", "Мобилни телефони"); // ID 154

        // ✅ Software & Media
        map.put("SOFTWARE", "Софтуер"); // ID 191
        map.put("GAMES", "Софтуер"); // No specific games category
        map.put("M - MEDIA", "Носители CD, DVD, Blu-Ray"); // ID 190
        map.put("CD MEDIA", "Носители CD, DVD, Blu-Ray");
        map.put("CD", "Носители CD, DVD, Blu-Ray");

        // ✅ Accessories - generic mapping
        map.put("Accessories", "Аксесоари за лаптопи/таблети");
        map.put("PSG Accessories", "Геймърски аксесоари"); // ID 178
        map.put("Servers Accessories", "Аксесоари за компютри"); // ID 35
        map.put("CONTR I/O", "Входно-изходни контролери"); // ID 22
        map.put("Calculator", "Офис продукти"); // ID 183
        map.put("Feature", "Аксесоари за лаптопи/таблети");
        map.put("Smart", "Аксесоари за лаптопи/таблети");

        // ✅ Brands - map to accessories
        map.put("HP", "Аксесоари за лаптопи/таблети");
        map.put("ASUS", "Аксесоари за лаптопи/таблети");
        map.put("ACER", "Аксесоари за лаптопи/таблети");
        map.put("LENOVO", "Аксесоари за лаптопи/таблети");
        map.put("BENQ", "Аксесоари за лаптопи/таблети");
        map.put("LG", "Аксесоари за лаптопи/таблети");
        map.put("TENDA", "Безжични рутери");
        map.put("DISNEY", "Аксесоари за лаптопи/таблети");

        // ✅ Generic
        map.put("Other", "Аксесоари за лаптопи/таблети");
        map.put("Others", "Аксесоари за лаптопи/таблети");
        map.put("Professianal", "Аксесоари за лаптопи/таблети");

        MOST_CATEGORY_MAPPING = Collections.unmodifiableMap(map);
    }

    private static final Map<String, String> PARAMETER_NAME_TRANSLATION;

    static {
        Map<String, String> paramMap = new HashMap<>();

        // === GPU PARAMETERS ===
        paramMap.put("Memory size", "Размер на паметта");
        paramMap.put("Memory Interface", "Интерфейс на паметта");
        paramMap.put("Memory type", "Тип памет");
        paramMap.put("Memory Clock", "Честота на паметта");
        paramMap.put("Memory Bandwidth", "Пропускателна способност на паметта");
        paramMap.put("Graphics Engine", "Графичен двигател");
        paramMap.put("Engine Clock", "Честота на ядрото");
        paramMap.put("Core Clock", "Честота на ядрото");
        paramMap.put("CUDA Core", "CUDA ядра");
        paramMap.put("Card Bus", "Шина");
        paramMap.put("OpenGL", "OpenGL");
        paramMap.put("Graphics", "Графика");
        paramMap.put("System Power", "Системна мощност");
        paramMap.put("Power Consumption", "Консумация на мощност");

        // === CPU PARAMETERS ===
        paramMap.put("Socket", "Сокет");
        paramMap.put("Number of cores", "Брой ядра");
        paramMap.put("Threads", "Брой нишки");
        paramMap.put("Frequency", "Честота");
        paramMap.put("TDP", "TDP");
        paramMap.put("Cache", "Кеш памет");
        paramMap.put("CPU Model", "Модел процесор");
        paramMap.put("CPU Socket Support", "Поддържани сокети");
        paramMap.put("CPU's supported", "Поддържани процесори");

        // === RAM PARAMETERS ===
        paramMap.put("Capacity", "Капацитет");
        paramMap.put("Type", "Тип");
        paramMap.put("Speed", "Скорост");
        paramMap.put("Memory speed", "Скорост на паметта");
        paramMap.put("Memory Specifications", "Спецификации на паметта");
        paramMap.put("Form Factor", "Форм фактор");
        paramMap.put("CAS Latency", "CAS латентност");
        paramMap.put("Voltage", "Волтаж");
        paramMap.put("Kit of", "Комплект от");
        paramMap.put("Compliant Devices", "Съвместими устройства");
        paramMap.put("ECC Registered", "ECC регистрирана");

        // === STORAGE PARAMETERS ===
        paramMap.put("Interface", "Интерфейс");
        paramMap.put("R/W Speed", "Скорост четене/запис");
        paramMap.put("Read Speed", "Скорост на четене");
        paramMap.put("Write Speed", "Скорост на запис");
        paramMap.put("HDD", "Твърд диск");
        paramMap.put("SSD", "SSD");
        paramMap.put("Storage", "Памет");
        paramMap.put("HDD RPM", "Обороти на HDD");
        paramMap.put("Disk Specifications", "Спецификации на диска");
        paramMap.put("Total Bytes Written (TBW)", "Общо записани байтове (TBW)");
        paramMap.put("MTBF", "Средно време между повреди");
        paramMap.put("MTBF [hours]", "MTBF [часове]");
        paramMap.put("Flash Storage", "Флаш памет");
        paramMap.put("Rescue Data Recovery", "Възстановяване на данни");

        // === MOTHERBOARD PARAMETERS ===
        paramMap.put("Chipset", "Чипсет");
        paramMap.put("Memory Slots", "Слотове за памет");
        paramMap.put("Max Memory", "Максимална памет");
        paramMap.put("Expansion Slots", "Слотове за разширение");
        paramMap.put("Slots for devices", "Слотове за устройства");
        paramMap.put("USB", "USB");
        paramMap.put("LAN", "Мрежа");
        paramMap.put("Audio", "Аудио");
        paramMap.put("BIOS", "BIOS");
        paramMap.put("Native PCIe Lanes", "Нативни PCIe линии");
        paramMap.put("Built-in Devices", "Вградени устройства");

        // === MONITOR PARAMETERS ===
        paramMap.put("Screen size", "Размер на екрана");
        paramMap.put("Screen Resolution", "Резолюция на екрана");
        paramMap.put("Screen type", "Тип на екрана");
        paramMap.put("Resolution", "Резолюция");
        paramMap.put("Brightness", "Яркост");
        paramMap.put("Contrast", "Контраст");
        paramMap.put("Response Time", "Време за отклик");
        paramMap.put("Viewing Angle", "Ъгъл на видимост");
        paramMap.put("Refresh rate", "Честота на опресняване");
        paramMap.put("Signal Frequency", "Сигнална честота");
        paramMap.put("Pixel Pitch", "Разстояние между пиксели");
        paramMap.put("Color Saturation", "Цветова наситеност");
        paramMap.put("Color Gamut", "Цветова гама");
        paramMap.put("Curvature", "Кривина");
        paramMap.put("Aspect Ratio", "Съотношение");
        paramMap.put("Display", "Дисплей");
        paramMap.put("Touch Screen", "Сензорен екран");
        paramMap.put("Maximum Display", "Максимален дисплей");

        // === PSU PARAMETERS ===
        paramMap.put("Power", "Мощност");
        paramMap.put("Power Supply", "Захранване");
        paramMap.put("Power Description", "Описание на захранването");
        paramMap.put("Power Adapter", "Адаптер за захранване");
        paramMap.put("AC Input", "AC вход");
        paramMap.put("DC Output", "DC изход");
        paramMap.put("PFC Type", "Тип PFC");
        paramMap.put("Input (Watt)", "Вход (Вата)");

        // === COOLING PARAMETERS ===
        paramMap.put("Cooling", "Охлаждане");
        paramMap.put("Fan", "Вентилатор");
        paramMap.put("Fan Information", "Информация за вентилатора");
        paramMap.put("Rotational speed [RPM]", "Скорост на въртене [RPM]");
        paramMap.put("Maximum air flow [CFM]", "Максимален въздушен поток [CFM]");
        paramMap.put("Max. pressure (mm H2O)", "Максимално налягане (mm H2O)");
        paramMap.put("Acoustical noise [dB (A)]", "Акустичен шум [dB (A)]");
        paramMap.put("Bearing", "Лагер");
        paramMap.put("Number of blades", "Брой перки");
        paramMap.put("Size [mm]", "Размер [mm]");
        paramMap.put("Pump Specification", "Спецификация на помпата");

        // === PERIPHERALS - KEYBOARD ===
        paramMap.put("Keyboard", "Клавиатура");
        paramMap.put("Backlighting", "Подсветка");
        paramMap.put("Media keys", "Медийни клавиши");
        paramMap.put("Cable", "Кабел");

        // === PERIPHERALS - MOUSE ===
        paramMap.put("Movement Resolution", "Разделителна способност на движението");
        paramMap.put("Buttons", "Бутони");
        paramMap.put("Wireless", "Безжична");

        // === NETWORK PARAMETERS ===
        paramMap.put("Network Data Rate", "Скорост на мрежата");
        paramMap.put("Network band", "Мрежова честотна лента");
        paramMap.put("Data Rate", "Скорост на данните");
        paramMap.put("Ports", "Портове");
        paramMap.put("Transmission Mode", "Режим на предаване");
        paramMap.put("Network Cables", "Мрежови кабели");
        paramMap.put("LED Indications", "LED индикации");
        paramMap.put("LED Indicator", "LED индикатор");
        paramMap.put("Protocol", "Протокол");
        paramMap.put("Standarts", "Стандарти");
        paramMap.put("Standards", "Стандарти");
        paramMap.put("Wi-Fi", "Wi-Fi");
        paramMap.put("Bluetooth", "Bluetooth");
        paramMap.put("Connectivity", "Свързаност");
        paramMap.put("VPN Support", "VPN поддръжка");
        paramMap.put("Management", "Управление");
        paramMap.put("Antenna Type", "Тип антена");
        paramMap.put("Network Protocols", "Мрежови протоколи");

        // === LAPTOP/MOBILE PARAMETERS ===
        paramMap.put("OS", "Операционна система");
        paramMap.put("OS Support", "Поддръжка на ОС");
        paramMap.put("Battery", "Батерия");
        paramMap.put("Camera", "Камера");
        paramMap.put("Camera Rear", "Задна камера");
        paramMap.put("Camera Front", "Предна камера");
        paramMap.put("Camera Hardware Profile", "Хардуерен профил на камерата");
        paramMap.put("SIM", "SIM");
        paramMap.put("Card Slot", "Слот за карта");
        paramMap.put("Sensors", "Сензори");
        paramMap.put("Pen", "Писалка");
        paramMap.put("NPU", "NPU");

        // === PRINTER PARAMETERS ===
        paramMap.put("Print Speed", "Скорост на печат");
        paramMap.put("Print resolution", "Резолюция на печат");
        paramMap.put("Copy Speed", "Скорост на копиране");
        paramMap.put("Scan Speed", "Скорост на сканиране");
        paramMap.put("Scanner Resolution", "Резолюция на скенера");
        paramMap.put("Paper size", "Размер на хартията");
        paramMap.put("Functions", "Функции");
        paramMap.put("Duty Cycle", "Работен цикъл");
        paramMap.put("Cartridge Type", "Тип касета");
        paramMap.put("Cartridge yield", "Капацитет на касетата");
        paramMap.put("Tray", "Тава");

        // === PROJECTOR PARAMETERS ===
        paramMap.put("Projection Lens", "Проекционна леща");
        paramMap.put("Lamp", "Лампа");
        paramMap.put("Image", "Изображение");

        // === CASE PARAMETERS ===
        paramMap.put("Mechanical Design", "Механичен дизайн");
        paramMap.put("Materials", "Материали");
        paramMap.put("Material", "Материал");

        // === GENERIC PARAMETERS ===
        paramMap.put("Brand", "Марка");
        paramMap.put("Model", "Модел");
        paramMap.put("Manufacturer", "Производител");
        paramMap.put("Color", "Цвят");
        paramMap.put("Body Color", "Цвят на корпуса");
        paramMap.put("Weight", "Тегло");
        paramMap.put("Dimensions", "Размери");
        paramMap.put("Physical Characteristics", "Физически характеристики");
        paramMap.put("Warranty", "Гаранция");
        paramMap.put("Features", "Функции");
        paramMap.put("Special features", "Специални функции");
        paramMap.put("Specifications", "Спецификации");
        paramMap.put("Technical Information", "Техническа информация");
        paramMap.put("Performance", "Производителност");
        paramMap.put("Family", "Семейство");
        paramMap.put("Other", "Други");
        paramMap.put("Accessories", "Аксесоари");
        paramMap.put("Package", "Пакет");
        paramMap.put("Compatible", "Съвместим");
        paramMap.put("Compatibility", "Съвместимост");
        paramMap.put("Essentials", "Основни");

        // === CONNECTORS & PORTS ===
        paramMap.put("I/O", "Входове/Изходи");
        paramMap.put("Connector", "Конектор");
        paramMap.put("Connectors", "Конектори");
        paramMap.put("Interfaces/Ports", "Интерфейси/Портове");
        paramMap.put("Input", "Вход");
        paramMap.put("Output", "Изход");
        paramMap.put("Cable length (mm)", "Дължина на кабела (mm)");
        paramMap.put("Cable Information", "Информация за кабела");

        // === ENVIRONMENTAL ===
        paramMap.put("Temperature", "Температура");
        paramMap.put("Operating temperature", "Работна температура");
        paramMap.put("Humidity", "Влажност");
        paramMap.put("Operating humidity", "Работна влажност");
        paramMap.put("Environment", "Околна среда");
        paramMap.put("Safety and Environmental", "Безопасност и екология");
        paramMap.put("Certifications", "Сертификати");
        paramMap.put("Compliance and Standarts", "Съответствие и стандарти");

        // === UPS PARAMETERS ===
        paramMap.put("Alarms", "Аларми");
        paramMap.put("Auidible Alarms", "Звукови аларми");
        paramMap.put("Communication", "Комуникация");

        // === MISC PARAMETERS ===
        paramMap.put("Mnfr Part #", "Каталожен номер");
        paramMap.put("Mnfr ID#", "ID производител");
        paramMap.put("System requirements", "Системни изисквания");
        paramMap.put("Security", "Сигурност");
        paramMap.put("ODD", "Оптично устройство");
        paramMap.put("BOX / with FAN", "Кутия / с вентилатор");
        paramMap.put("Format", "Формат");
        paramMap.put("Class", "Клас");
        paramMap.put("Controller", "Контролер");

        PARAMETER_NAME_TRANSLATION = Collections.unmodifiableMap(paramMap);
    }


    @Transactional
    public void syncMostManufacturers() {
        String syncType = "MOST_MANUFACTURERS";
        SyncLog syncLog = logHelper.createSyncLogSimple(syncType);
        long startTime = System.currentTimeMillis();

        try {
            log.info("Starting Most manufacturers synchronization - CREATE ONLY mode");

            Set<String> externalManufacturers = mostApiService.extractUniqueManufacturers();

            if (externalManufacturers.isEmpty()) {
                log.warn("No manufacturers found in Most API");
                logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, 0, 0, 0, 0, "No manufacturers found", startTime);
                return;
            }

            Map<String, Manufacturer> existingManufacturers = manufacturerRepository.findAll()
                    .stream()
                    .filter(m -> m.getName() != null && !m.getName().isEmpty())
                    .collect(Collectors.toMap(
                            m -> normalizeManufacturerName(m.getName()),
                            m -> m,
                            (existing, duplicate) -> {
                                log.warn("Duplicate manufacturer: {}, IDs: {} and {}, keeping first",
                                        existing.getName(), existing.getId(), duplicate.getId());
                                return existing;
                            }
                    ));

            log.info("Found {} existing manufacturers in database", existingManufacturers.size());

            long created = 0, skipped = 0;

            for (String manufacturerName : externalManufacturers) {
                try {
                    String normalizedName = normalizeManufacturerName(manufacturerName);
                    Manufacturer manufacturer = existingManufacturers.get(normalizedName);

                    if (manufacturer == null) {
                        // ✅ CREATE ONLY
                        manufacturer = createMostManufacturer(manufacturerName);
                        manufacturer = manufacturerRepository.save(manufacturer);
                        existingManufacturers.put(normalizedName, manufacturer);
                        created++;
                        log.debug("Created manufacturer: {}", manufacturerName);
                    } else {
                        // ✅ SKIP - already exists
                        skipped++;
                        log.trace("Manufacturer already exists, skipping: {}", manufacturerName);
                    }

                } catch (Exception e) {
                    log.error("Error processing manufacturer {}: {}", manufacturerName, e.getMessage());
                }
            }

            String message = String.format("Skipped %d existing", skipped);
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS,
                    (long) externalManufacturers.size(), created, 0, 0, message, startTime);
            log.info("Most manufacturers sync completed - Created: {}, Skipped: {}", created, skipped);

        } catch (Exception e) {
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0, e.getMessage(), startTime);
            log.error("Error during Most manufacturers synchronization", e);
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void syncMostCategories() {
        String syncType = "MOST_CATEGORIES";
        SyncLog syncLog = logHelper.createSyncLogSimple(syncType);
        long startTime = System.currentTimeMillis();

        try {
            log.info("Starting Most categories synchronization - CREATE ONLY mode with Bulgarian mapping");

            // Extract unique categories from Most API
            List<Map<String, Object>> allProducts = mostApiService.getAllProducts();
            Set<String> uniqueCategoryNames = new HashSet<>();

            for (Map<String, Object> product : allProducts) {
                String categoryName = getCategoryNameFromProduct(product);
                if (categoryName != null && !categoryName.isEmpty()) {
                    uniqueCategoryNames.add(categoryName);
                }
            }

            log.info("Found {} unique Most categories", uniqueCategoryNames.size());

            // Load existing categories by normalized Bulgarian names
            Map<String, Category> existingCategories = categoryRepository.findAll()
                    .stream()
                    .filter(c -> c.getNameBg() != null)
                    .collect(Collectors.toMap(
                            c -> normalizeCategoryName(c.getNameBg()),
                            c -> c,
                            (existing, duplicate) -> existing
                    ));

            log.info("Loaded {} existing categories from database", existingCategories.size());

            long created = 0, reused = 0, skipped = 0;

            for (String mostCategoryName : uniqueCategoryNames) {
                try {
                    // ✅ 1. Try to find mapping to Bulgarian name
                    String bulgarianName = MOST_CATEGORY_MAPPING.get(mostCategoryName);

                    if (bulgarianName == null) {
                        // No mapping found - use English name as fallback
                        log.warn("No Bulgarian mapping for Most category: '{}', using English name", mostCategoryName);
                        bulgarianName = mostCategoryName;
                    }

                    // ✅ 2. Check if category with this Bulgarian name already exists
                    String normalizedBulgarianName = normalizeCategoryName(bulgarianName);
                    Category existingCategory = existingCategories.get(normalizedBulgarianName);

                    if (existingCategory != null) {
                        // ✅ REUSE existing category
                        log.debug("Reusing existing category: '{}' for Most category '{}'",
                                bulgarianName, mostCategoryName);
                        reused++;
                    } else {
                        // ✅ CREATE new category
                        Category newCategory = new Category();
                        newCategory.setNameBg(bulgarianName);
                        newCategory.setNameEn(mostCategoryName); // Keep English as reference
                        newCategory.setSlug(syncHelper.createSlugFromName(bulgarianName));
                        newCategory.setPlatform(Platform.MOST);
                        newCategory.setShow(true);
                        newCategory.setSortOrder(0);

                        // Make slug unique
                        String uniqueSlug = generateUniqueSlug(newCategory.getSlug(), newCategory);
                        newCategory.setSlug(uniqueSlug);

                        newCategory = categoryRepository.save(newCategory);
                        existingCategories.put(normalizedBulgarianName, newCategory);

                        created++;
                        log.info("Created new category: '{}' (Bulgarian) from Most category '{}'",
                                bulgarianName, mostCategoryName);
                    }

                } catch (Exception e) {
                    log.error("Error processing Most category '{}': {}", mostCategoryName, e.getMessage());
                    skipped++;
                }
            }

            String message = String.format("Created: %d, Reused: %d, Skipped: %d", created, reused, skipped);
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS,
                    (long) uniqueCategoryNames.size(), created, 0, skipped, message, startTime);

            log.info("Most categories sync completed - {}", message);

        } catch (Exception e) {
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0, e.getMessage(), startTime);
            log.error("Error during Most categories synchronization", e);
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void syncMostParameters() {
        String syncType = "MOST_PARAMETERS";
        SyncLog syncLog = logHelper.createSyncLogSimple(syncType);
        long startTime = System.currentTimeMillis();

        try {
            log.info("Starting Most parameters synchronization - CREATE ONLY mode with Bulgarian translation");

            List<Map<String, Object>> allProducts = mostApiService.getAllProducts();
            Map<String, Map<String, Set<String>>> categorizedParameters = groupParametersByCategory(allProducts);

            if (categorizedParameters.isEmpty()) {
                log.warn("No parameters found in Most API");
                logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, 0, 0, 0, 0, "No parameters found", startTime);
                return;
            }

            long totalCreated = 0, skipped = 0, totalOptionsCreated = 0;
            int translatedCount = 0, notTranslatedCount = 0;

            // ✅ CRITICAL FIX: Load parameters with EAGER initialization
            List<Parameter> allParameters = parameterRepository.findAll();

            // Force initialization of categories collection
            for (Parameter p : allParameters) {
                if (p.getCategories() != null) {
                    p.getCategories().size(); // Triggers initialization
                }
            }

            Map<String, Parameter> globalParametersCache = allParameters.stream()
                    .filter(p -> p.getNameBg() != null)
                    .collect(Collectors.toMap(
                            p -> normalizeParameterName(p.getNameBg()),
                            p -> p,
                            (existing, duplicate) -> existing
                    ));

            log.info("Loaded {} existing parameters", globalParametersCache.size());

            for (Map.Entry<String, Map<String, Set<String>>> catEntry : categorizedParameters.entrySet()) {
                String categoryName = catEntry.getKey();
                Map<String, Set<String>> categoryParams = catEntry.getValue();

                try {
                    // ✅ Find category by Bulgarian name from mapping
                    String bulgarianCategoryName = MOST_CATEGORY_MAPPING.get(categoryName);
                    if (bulgarianCategoryName == null) {
                        bulgarianCategoryName = categoryName; // Fallback
                    }

                    Optional<Category> categoryOpt = findCategoryByName(bulgarianCategoryName);

                    if (categoryOpt.isEmpty()) {
                        log.warn("Category not found: {} (mapped from '{}')", bulgarianCategoryName, categoryName);
                        continue;
                    }

                    Category category = categoryOpt.get();

                    // ✅ Force initialization
                    if (category.getParameters() != null) {
                        category.getParameters().size();
                    }

                    log.debug("Processing {} parameters for category: {}", categoryParams.size(), bulgarianCategoryName);

                    for (Map.Entry<String, Set<String>> paramEntry : categoryParams.entrySet()) {
                        try {
                            String paramNameEnglish = paramEntry.getKey();
                            Set<String> paramValues = paramEntry.getValue();

                            // ✅ TRANSLATE PARAMETER NAME
                            String paramNameBulgarian = translateParameterName(paramNameEnglish);

                            if (paramNameBulgarian.equals(paramNameEnglish)) {
                                notTranslatedCount++;
                                log.debug("No translation for parameter: '{}', using English", paramNameEnglish);
                            } else {
                                translatedCount++;
                            }

                            String normalizedName = normalizeParameterName(paramNameBulgarian);
                            Parameter parameter = globalParametersCache.get(normalizedName);

                            if (parameter == null) {
                                // ✅ CREATE new parameter
                                parameter = new Parameter();
                                parameter.setNameBg(paramNameBulgarian);
                                parameter.setNameEn(paramNameEnglish);
                                parameter.setPlatform(Platform.MOST);
                                parameter.setOrder(50);
                                parameter.setCategories(new HashSet<>());
                                parameter.getCategories().add(category);
                                parameter.setCreatedBy("system");

                                parameter = parameterRepository.save(parameter);
                                globalParametersCache.put(normalizedName, parameter);
                                totalCreated++;

                                log.debug("Created parameter: '{}' (bg) / '{}' (en) for category '{}'",
                                        paramNameBulgarian, paramNameEnglish, bulgarianCategoryName);
                            } else {
                                // ✅ FIX: SAFE CATEGORY CHECK
                                Set<Category> parameterCategories = parameter.getCategories();
                                if (parameterCategories == null) {
                                    parameterCategories = new HashSet<>();
                                    parameter.setCategories(parameterCategories);
                                }

                                // Use category ID instead of contains()
                                boolean categoryAlreadyLinked = parameterCategories.stream()
                                        .anyMatch(cat -> cat.getId().equals(category.getId()));

                                if (!categoryAlreadyLinked) {
                                    parameterCategories.add(category);
                                    parameterRepository.save(parameter);
                                    log.debug("Added category '{}' to existing parameter '{}'",
                                            bulgarianCategoryName, paramNameBulgarian);
                                } else {
                                    log.trace("Parameter '{}' already exists in category, skipping", paramNameBulgarian);
                                }
                                skipped++;
                            }

                            // Sync parameter options
                            int optionsCreated = syncParameterOptionsCreateOnly(parameter, paramValues);
                            totalOptionsCreated += optionsCreated;

                        } catch (Exception e) {
                            log.error("Error processing parameter {}: {}", paramEntry.getKey(), e.getMessage());
                        }
                    }

                    // ✅ FLUSH after each category
                    entityManager.flush();

                } catch (Exception e) {
                    log.error("Error processing parameters for category {}: {}", categoryName, e.getMessage());

                    // ✅ Clear entity manager on error
                    try {
                        entityManager.clear();
                    } catch (Exception clearEx) {
                        log.error("Error clearing entity manager: {}", clearEx.getMessage());
                    }
                }
            }

            String message = String.format("Created: %d, Skipped: %d existing, Options: %d created. Translated: %d, Not translated: %d",
                    totalCreated, skipped, totalOptionsCreated, translatedCount, notTranslatedCount);

            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS,
                    totalCreated + skipped, totalCreated, 0, 0, message, startTime);

            log.info("Most parameters sync completed - {}", message);

        } catch (Exception e) {
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0, e.getMessage(), startTime);
            log.error("Error during Most parameters synchronization", e);
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void syncMostProducts() {
        String syncType = "MOST_PRODUCTS";
        SyncLog syncLog = logHelper.createSyncLogSimple(syncType);
        long startTime = System.currentTimeMillis();

        try {
            log.info("Starting Most products synchronization - MINIMAL UPDATE mode");

            List<Map<String, Object>> allProducts = mostApiService.getAllProducts();

            if (allProducts.isEmpty()) {
                log.warn("No products found in Most API");
                logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS, 0, 0, 0, 0, "No products found", startTime);
                return;
            }

            log.info("Pre-loading categories...");
            Map<String, Category> categoriesByName = categoryRepository.findAll()
                    .stream()
                    .filter(c -> c.getNameBg() != null)
                    .collect(Collectors.toMap(
                            c -> normalizeCategoryName(c.getNameBg()),
                            c -> c,
                            (existing, duplicate) -> existing
                    ));

            log.info("Loaded {} categories", categoriesByName.size());

            // ✅ LOAD MANUFACTURERS
            Map<String, Manufacturer> manufacturersMap = manufacturerRepository.findAll()
                    .stream()
                    .filter(m -> m.getName() != null)
                    .collect(Collectors.toMap(
                            m -> normalizeManufacturerName(m.getName()),
                            m -> m,
                            (existing, duplicate) -> existing
                    ));

            log.info("Loaded {} manufacturers", manufacturersMap.size());

            if (manufacturersMap.isEmpty()) {
                log.warn("⚠️ WARNING: No manufacturers found! Products may not have manufacturers assigned.");
            }

            long totalCreated = 0, totalUpdated = 0, totalErrors = 0;
            long skippedNoCategory = 0, skippedNoManufacturer = 0;

            for (int i = 0; i < allProducts.size(); i++) {
                Map<String, Object> rawProduct = allProducts.get(i);

                try {
                    String code = (String) rawProduct.get("code");
                    String name = (String) rawProduct.get("name");

                    if (code == null || code.isEmpty() || name == null || name.isEmpty()) {
                        log.debug("Skipping product with missing code or name");
                        totalErrors++;
                        continue;
                    }

                    // ✅ Find category using mapping
                    Category category = findProductCategoryByNameWithMapping(rawProduct, categoriesByName);

                    if (category == null) {
                        String mostCategory = (String) rawProduct.get("category");
                        String mostSubcategory = (String) rawProduct.get("subcategory");
                        log.warn("No category found for product '{}': category='{}', subcategory='{}'",
                                code, mostCategory, mostSubcategory);
                        skippedNoCategory++;
                        continue;
                    }

                    log.trace("Product '{}' → Category '{}'", code, category.getNameBg());

                    Product product = findOrCreateProduct(code, name);
                    boolean isNew = (product.getId() == null);

                    // ✅ Update product fields (CREATE ONLY or MINIMAL UPDATE)
                    boolean success = updateProductFromMost(product, rawProduct, category, manufacturersMap, isNew);

                    if (!success) {
                        skippedNoManufacturer++;
                        continue;
                    }

                    product = productRepository.save(product);

                    if (isNew) {
                        totalCreated++;
                    } else {
                        totalUpdated++;
                    }

                    if ((i + 1) % 50 == 0) {
                        log.info("Progress: {}/{} products processed", i + 1, allProducts.size());
                        entityManager.flush();
                        entityManager.clear();
                    }

                } catch (Exception e) {
                    totalErrors++;
                    log.error("Error processing product: {}", e.getMessage());

                    // Log full stack trace for first few errors
                    if (totalErrors <= 3) {
                        log.error("Full exception for debugging:", e);
                    }
                }
            }

            String message = String.format(
                    "Created: %d, Updated: %d, Skipped (No Category): %d, Skipped (No Manufacturer): %d, Errors: %d",
                    totalCreated, totalUpdated, skippedNoCategory, skippedNoManufacturer, totalErrors);

            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_SUCCESS,
                    totalCreated + totalUpdated, totalCreated, totalUpdated, totalErrors, message, startTime);

            log.info("Most products sync completed - {}", message);

        } catch (Exception e) {
            logHelper.updateSyncLogSimple(syncLog, LOG_STATUS_FAILED, 0, 0, 0, 0, e.getMessage(), startTime);
            log.error("Error during Most products synchronization", e);
            throw new RuntimeException(e);
        }
    }

    private Category findProductCategoryByNameWithMapping(Map<String, Object> product,
                                                          Map<String, Category> categoriesByName) {
        // Try subcategory first (more specific)
        String subcategory = (String) product.get("subcategory");
        if (subcategory != null && !subcategory.isEmpty()) {
            String bulgarianSubcategory = MOST_CATEGORY_MAPPING.get(subcategory);
            if (bulgarianSubcategory == null) {
                bulgarianSubcategory = subcategory; // Fallback
            }

            String normalizedSubcategory = normalizeCategoryName(bulgarianSubcategory);
            Category category = categoriesByName.get(normalizedSubcategory);
            if (category != null) {
                return category;
            }
        }

        // Fallback to main category
        String category = (String) product.get("category");
        if (category != null && !category.isEmpty()) {
            String bulgarianCategory = MOST_CATEGORY_MAPPING.get(category);
            if (bulgarianCategory == null) {
                bulgarianCategory = category; // Fallback
            }

            String normalizedCategory = normalizeCategoryName(bulgarianCategory);
            return categoriesByName.get(normalizedCategory);
        }

        return null;
    }

    private boolean updateProductFromMost(Product product, Map<String, Object> rawProduct,
                                          Category category, Map<String, Manufacturer> manufacturersMap,
                                          boolean isNew) {
        if (isNew) {
            // ✅ NEW PRODUCT - set all fields
            product.setCategory(category);
            product.setPlatform(Platform.MOST);

            String name = (String) rawProduct.get("name");
            product.setNameBg(name);
            product.setNameEn(name);

            // ✅ Manufacturer with validation
            String manufacturerName = (String) rawProduct.get("manufacturer");
            if (manufacturerName != null && !manufacturerName.isEmpty()) {
                String normalizedManufacturer = normalizeManufacturerName(manufacturerName);
                Manufacturer manufacturer = manufacturersMap.get(normalizedManufacturer);

                if (manufacturer != null) {
                    product.setManufacturer(manufacturer);
                } else {
                    log.warn("Manufacturer '{}' not found for product {}, skipping product",
                            manufacturerName, product.getSku());
                    return false; // Skip this product
                }
            } else {
                log.debug("Product {} has no manufacturer in source data", product.getSku());
            }

            // Images
            String mainImage = (String) rawProduct.get("main_picture_url");
            if (mainImage != null && !mainImage.isEmpty()) {
                product.setPrimaryImageUrl(mainImage);
            }

            @SuppressWarnings("unchecked")
            List<String> gallery = (List<String>) rawProduct.get("gallery");
            if (gallery != null && gallery.size() > 1) {
                List<String> additionalImages = gallery.subList(1, gallery.size());
                if (product.getAdditionalImages() != null) {
                    product.getAdditionalImages().clear();
                    product.getAdditionalImages().addAll(additionalImages);
                } else {
                    product.setAdditionalImages(new ArrayList<>(additionalImages));
                }
            }

            // ✅ WRAP IN TRY-CATCH to prevent parameter errors from killing the product
            try {
                setMostParametersToProduct(product, rawProduct);
            } catch (Exception e) {
                log.error("Error setting parameters for product {}: {}", product.getSku(), e.getMessage());
                product.setProductParameters(new HashSet<>());
            }
        }

        // ✅ ALWAYS UPDATE (for both new and existing)
        String productStatus = (String) rawProduct.get("product_status");
        boolean isAvailable = "Наличен".equals(productStatus);
        product.setStatus(isAvailable ? ProductStatus.AVAILABLE : ProductStatus.NOT_AVAILABLE);
        product.setShow(isAvailable);
        product.setCreatedBy("system");

        // ✅ ПРОМЯНА 2: Промяна на валутната конверсия според новите изисквания
        // Вече конвертираме само USD към EUR, а EUR оставяме както е
        String priceStr = (String) rawProduct.get("price");
        String currency = (String) rawProduct.get("currency");

        if (priceStr != null && !priceStr.isEmpty()) {
            try {
                BigDecimal price = new BigDecimal(priceStr);
                BigDecimal priceInEuro = convertPriceToEuro(price, currency); // ✅ Новата функция
                product.setPriceClient(priceInEuro);
            } catch (NumberFormatException e) {
                log.warn("Invalid price format: {}", priceStr);
            }
        }

        product.calculateFinalPrice();

        if (!isNew) {
            log.trace("Updated product {} - status: {}, priceClient: {} EUR",
                    product.getSku(), product.getStatus(), product.getPriceClient());
        }

        return true; // Success
    }

    private void setMostParametersToProduct(Product product, Map<String, Object> rawProduct) {
        try {
            if (product.getCategory() == null) {
                log.warn("Product {} has no category, cannot set parameters", product.getSku());
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, String> properties = (Map<String, String>) rawProduct.get("properties");
            if (properties == null || properties.isEmpty()) {
                log.debug("Product {} has no properties in source data", product.getSku());
                return;
            }

            // ✅ FIX 1: Запази съществуващите ProductParameter entities
            Set<ProductParameter> existingProductParams = product.getProductParameters();
            if (existingProductParams == null) {
                existingProductParams = new HashSet<>();
            }

            // ✅ FIX 2: Раздели на ръчни и автоматични параметри
            Set<ProductParameter> manualParameters = existingProductParams.stream()
                    .filter(pp -> pp.getParameter() != null)
                    .filter(this::isManualParameterForMost)
                    .collect(Collectors.toSet());

            Set<ProductParameter> autoParameters = new HashSet<>();
            int mappedCount = 0;
            int notFoundCount = 0;

            // ✅ Load parameters associated with product's category
            List<Parameter> categoryParameters = parameterRepository
                    .findByCategoryIdOrderByOrderAsc(product.getCategory().getId());

            // ✅ FIX 3: Index by ENGLISH name (nameEn) with normalization
            Map<String, Parameter> parametersByEnglishName = categoryParameters.stream()
                    .filter(p -> p.getNameEn() != null)
                    .collect(Collectors.toMap(
                            p -> normalizeParameterName(p.getNameEn()),
                            p -> p,
                            (existing, duplicate) -> {
                                log.warn("Duplicate parameter with English name: '{}', IDs: {} and {}, keeping first",
                                        existing.getNameEn(), existing.getId(), duplicate.getId());
                                return existing;
                            }
                    ));

            log.debug("Loaded {} parameters for category '{}' (indexed by English name)",
                    parametersByEnglishName.size(), product.getCategory().getNameBg());

            // ✅ Process all properties from Most API
            for (Map.Entry<String, String> prop : properties.entrySet()) {
                try {
                    String paramNameEnglish = prop.getKey(); // ← Most API gives English name
                    String paramValue = prop.getValue();

                    // Skip empty/invalid values
                    if (paramValue == null || paramValue.trim().isEmpty() || "-".equals(paramValue.trim())) {
                        log.trace("Skipping parameter '{}' with empty/invalid value for product {}",
                                paramNameEnglish, product.getSku());
                        continue;
                    }

                    // ✅ FIX 4: Search by normalized English name
                    String normalizedEnglishName = normalizeParameterName(paramNameEnglish);
                    Parameter parameter = parametersByEnglishName.get(normalizedEnglishName);

                    if (parameter == null) {
                        log.debug("Parameter not found by English name: '{}' (normalized: '{}'), categoryId={}, productSku={}",
                                paramNameEnglish, normalizedEnglishName,
                                product.getCategory().getId(), product.getSku());
                        notFoundCount++;
                        continue;
                    }

                    // ✅ Find or create parameter option
                    ParameterOption option = findOrCreateParameterOption(parameter, paramValue);
                    if (option == null) {
                        log.debug("Parameter option not found/created: parameter='{}', value='{}', productSku={}",
                                parameter.getNameBg(), paramValue, product.getSku());
                        notFoundCount++;
                        continue;
                    }

                    // ✅ Create ProductParameter
                    ProductParameter productParam = new ProductParameter();
                    productParam.setProduct(product);
                    productParam.setParameter(parameter);
                    productParam.setParameterOption(option);
                    autoParameters.add(productParam);

                    mappedCount++;
                    log.trace("Mapped parameter '{}' (en) / '{}' (bg) = '{}' for product {}",
                            parameter.getNameEn(), parameter.getNameBg(), paramValue, product.getSku());

                } catch (Exception e) {
                    log.error("Error mapping parameter '{}' for product {}: {}",
                            prop.getKey(), product.getSku(), e.getMessage());
                    notFoundCount++;
                }
            }

            // ✅ FIX 5: MERGE - combine manual + automatic parameters
            Set<ProductParameter> mergedParameters = new HashSet<>();
            mergedParameters.addAll(manualParameters);  // Manually added/modified
            mergedParameters.addAll(autoParameters);    // From Most API

            product.setProductParameters(mergedParameters);

            // ✅ Comprehensive logging
            if (mappedCount > 0 || notFoundCount > 0 || !manualParameters.isEmpty()) {
                log.info("Product {} parameter mapping: {} from Most API, {} manual (preserved), {} not found",
                        product.getSku(), mappedCount, manualParameters.size(), notFoundCount);
            } else {
                log.debug("Product {} has no parameters mapped", product.getSku());
            }

        } catch (Exception e) {
            log.error("Error setting Most parameters for product {}: {}", product.getSku(), e.getMessage());
            // Don't throw - allow product to be saved without parameters
        }
    }

    // ✅ Helper method to determine if a parameter is manually added/modified
    private boolean isManualParameterForMost(ProductParameter productParameter) {
        Parameter parameter = productParameter.getParameter();

        if (parameter == null) {
            return false;
        }

        // CRITERION 1: Check by Platform
        // If parameter has no platform or different platform than MOST, it's manual
        boolean isDifferentPlatform = (parameter.getPlatform() == null ||
                parameter.getPlatform() != Platform.MOST);

        // CRITERION 2: Check if created/modified by ADMIN
        boolean isCreatedByAdmin = isAdminUser(parameter.getCreatedBy());
        boolean isModifiedByAdmin = isAdminUser(parameter.getLastModifiedBy());

        // Parameter is manual if:
        // - Has different platform than MOST (or no platform)
        // OR
        // - Created/modified by ADMIN
        boolean isManual = isDifferentPlatform || isCreatedByAdmin || isModifiedByAdmin;

        if (isManual) {
            log.trace("Parameter '{}' identified as manual: platform={}, createdBy={}, lastModifiedBy={}",
                    parameter.getNameBg(), parameter.getPlatform(),
                    parameter.getCreatedBy(), parameter.getLastModifiedBy());
        }

        return isManual;
    }

    // ✅ Helper method for ADMIN user detection (case-insensitive, null-safe)
    private boolean isAdminUser(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }

        return "ADMIN".equalsIgnoreCase(username.trim()) ||
                "admin".equalsIgnoreCase(username.trim());
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    private Manufacturer createMostManufacturer(String name) {
        Manufacturer manufacturer = new Manufacturer();
        manufacturer.setName(name);
        manufacturer.setInformationName(name);
        manufacturer.setPlatform(Platform.MOST);
        return manufacturer;
    }

    private Map<String, Map<String, Set<String>>> groupParametersByCategory(List<Map<String, Object>> products) {
        Map<String, Map<String, Set<String>>> categorizedParams = new HashMap<>();

        for (Map<String, Object> product : products) {
            String categoryName = getCategoryNameFromProduct(product);
            if (categoryName == null) continue;

            @SuppressWarnings("unchecked")
            Map<String, String> properties = (Map<String, String>) product.get("properties");
            if (properties == null) continue;

            categorizedParams.putIfAbsent(categoryName, new HashMap<>());
            Map<String, Set<String>> categoryParams = categorizedParams.get(categoryName);

            for (Map.Entry<String, String> prop : properties.entrySet()) {
                String paramName = prop.getKey();
                String paramValue = prop.getValue();

                if (paramValue != null && !paramValue.isEmpty() && !"-".equals(paramValue)) {
                    categoryParams.putIfAbsent(paramName, new HashSet<>());
                    categoryParams.get(paramName).add(paramValue);
                }
            }
        }

        return categorizedParams;
    }

    private String getCategoryNameFromProduct(Map<String, Object> product) {
        String subcategory = (String) product.get("subcategory");
        if (subcategory != null && !subcategory.isEmpty()) {
            return subcategory;
        }
        return (String) product.get("category");
    }

    private Optional<Category> findCategoryByName(String categoryName) {
        if (categoryName == null || categoryName.isEmpty()) {
            return Optional.empty();
        }

        String normalizedName = normalizeCategoryName(categoryName);

        return categoryRepository.findAll().stream()
                .filter(c -> c.getNameBg() != null)
                .filter(c -> normalizedName.equals(normalizeCategoryName(c.getNameBg())))
                .findFirst();
    }

    private Product findOrCreateProduct(String code, String name) {
        List<Product> existing = productRepository.findProductsBySkuCode(code);

        if (!existing.isEmpty()) {
            if (existing.size() > 1) {
                log.warn("Found {} duplicates for SKU: {}, keeping first", existing.size(), code);
                for (int i = 1; i < existing.size(); i++) {
                    productRepository.delete(existing.get(i));
                }
            }
            return existing.get(0);
        }

        Product product = new Product();
        product.setSku(code);
        product.setReferenceNumber(code);
        return product;
    }

    private ParameterOption findOrCreateParameterOption(Parameter parameter, String value) {
        try {
            String normalizedValue = normalizeParameterValue(value);

            // Try to find existing
            List<ParameterOption> options = parameterOptionRepository
                    .findByParameterIdOrderByOrderAsc(parameter.getId());

            for (ParameterOption opt : options) {
                if (opt.getNameBg() != null &&
                        normalizedValue.equals(normalizeParameterValue(opt.getNameBg()))) {
                    return opt;
                }
            }

            // Create new
            ParameterOption newOption = new ParameterOption();
            newOption.setParameter(parameter);
            newOption.setNameBg(value);
            newOption.setNameEn(value);
            newOption.setOrder(options.size());

            return parameterOptionRepository.save(newOption);

        } catch (Exception e) {
            log.error("Error finding/creating parameter option: {}", e.getMessage());
            return null;
        }
    }

    // ✅ ПРОМЯНА 3: Нова функция за конвертиране на цена към евро
    private BigDecimal convertPriceToEuro(BigDecimal price, String currency) {
        if (price == null) {
            return BigDecimal.ZERO;
        }

        if ("EUR".equalsIgnoreCase(currency)) {
            // Цената вече е в евро, не правим конверсия
            log.trace("Price already in EUR: {}", price);
            return price.setScale(2, RoundingMode.HALF_UP);
        }

        if ("USD".equalsIgnoreCase(currency)) {
            // Конвертираме USD към EUR
            BigDecimal convertedPrice = price.multiply(new BigDecimal(USD_TO_EUR_RATE))
                    .setScale(2, RoundingMode.HALF_UP);
            log.trace("Converted USD {} to EUR {}", price, convertedPrice);
            return convertedPrice;
        }

        if ("BGN".equalsIgnoreCase(currency)) {
            // Ако все още има BGN в API-то, конвертираме към EUR
            BigDecimal convertedPrice = price.divide(new BigDecimal("1.95583"), 2, RoundingMode.HALF_UP);
            log.trace("Converted BGN {} to EUR {}", price, convertedPrice);
            return convertedPrice;
        }

        // Default: assume EUR (или друга валута, която не конвертираме)
        log.warn("Unknown currency: {}, assuming EUR", currency);
        return price.setScale(2, RoundingMode.HALF_UP);
    }

    // ✅ ПРОМЯНА 4: Премахваме старата функция convertPriceToBGN
    /*
    private BigDecimal convertPriceToBGN(BigDecimal price, String currency) {
        if (price == null) {
            return BigDecimal.ZERO;
        }

        if ("BGN".equalsIgnoreCase(currency)) {
            return price;
        }

        if ("USD".equalsIgnoreCase(currency)) {
            return price.multiply(new BigDecimal(USD_TO_BGN_RATE));
        }

        if ("EUR".equalsIgnoreCase(currency)) {
            return price.multiply(new BigDecimal(EUR_TO_BGN_RATE));
        }

        // Default: assume BGN
        log.warn("Unknown currency: {}, assuming BGN", currency);
        return price;
    }
    */

    private String generateUniqueSlug(String baseSlug, Category category) {
        if (!categoryRepository.existsBySlugAndIdNot(baseSlug, category.getId())) {
            return baseSlug;
        }

        int counter = 1;
        String numberedSlug;
        do {
            numberedSlug = baseSlug + "-" + counter;
            counter++;
        } while (categoryRepository.existsBySlugAndIdNot(numberedSlug, category.getId()));

        return numberedSlug;
    }

    private String normalizeManufacturerName(String name) {
        if (name == null) return "";
        return name.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    private String normalizeCategoryName(String name) {
        if (name == null) return "";
        return name.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    private String normalizeParameterName(String name) {
        if (name == null) return "";
        return name.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    private String normalizeParameterValue(String value) {
        if (value == null) return "";
        return value.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    private String translateParameterName(String englishName) {
        if (englishName == null || englishName.isEmpty()) {
            return englishName;
        }

        // Try exact match first
        String translation = PARAMETER_NAME_TRANSLATION.get(englishName);
        if (translation != null) {
            return translation;
        }

        // Try case-insensitive match
        for (Map.Entry<String, String> entry : PARAMETER_NAME_TRANSLATION.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(englishName)) {
                return entry.getValue();
            }
        }

        // No translation found - return original English name
        log.debug("No Bulgarian translation found for parameter: '{}'", englishName);
        return englishName;
    }

    private int syncParameterOptionsCreateOnly(Parameter parameter, Set<String> optionValues) {
        int created = 0;

        try {
            Map<String, ParameterOption> existingOptions = parameterOptionRepository
                    .findByParameterIdOrderByOrderAsc(parameter.getId())
                    .stream()
                    .filter(opt -> opt.getNameBg() != null)
                    .collect(Collectors.toMap(
                            opt -> normalizeParameterValue(opt.getNameBg()),
                            opt -> opt,
                            (existing, duplicate) -> existing
                    ));

            for (String optionValue : optionValues) {
                if (optionValue == null || optionValue.trim().isEmpty() || "-".equals(optionValue.trim())) {
                    continue;
                }

                String normalizedValue = normalizeParameterValue(optionValue);

                if (!existingOptions.containsKey(normalizedValue)) {
                    // ✅ CREATE ONLY
                    ParameterOption option = new ParameterOption();
                    option.setParameter(parameter);
                    option.setNameBg(optionValue);
                    option.setNameEn(optionValue);
                    option.setOrder(existingOptions.size() + created);

                    parameterOptionRepository.save(option);
                    existingOptions.put(normalizedValue, option);
                    created++;

                    log.trace("Created option '{}' for parameter '{}'", optionValue, parameter.getNameBg());
                } else {
                    log.trace("Option '{}' already exists, skipping", optionValue);
                }
            }

        } catch (Exception e) {
            log.error("Error syncing parameter options for {}: {}", parameter.getNameBg(), e.getMessage());
        }

        return created;
    }
}