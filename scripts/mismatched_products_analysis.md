# Анализ на грешно категоризирани продукти

> Генериран на: 2026-07-20  
> Анализирани продукти: 22521 (дамп от products_202607201221.sql)  
> Намерени несъответствия: **487** продукта в грешна категория

---

## 1. Карта на category_id → Категория

Изградена чрез анализ на имена на продукти и кръстосана с известните ID-та от скриптове 7, 18–23.

| category_id | Брой продукти | Наименование | Бележка |
|-------------|---------------|-------------|---------|
| 2 | 625 | Дънни платки | VALI  |
| 3 | 348 | Процесори | VALI  |
| 4 | 266 | Памети (VALI sub) | VALI  |
| 6 | 575 | Памети | VALI  |
| 7 | 43 | Памети SODIMM/лаптоп RAM | VALI  |
| 8 | 638 | Видео карти | VALI  |
| 9 | 175 | Захранвания | VALI  |
| 10 | 13 | Охладители (VALI) | VALI  |
| 11 | 637 | Кутии за компютри | VALI  |
| 12 | 248 | Вентилатори | VALI  |
| 17 | 177 | SSD | VALI  |
| 22 | 243 | Сървърни компоненти/кабели | ASBIS  |
| 32 | 213 | Настолни компютри | VALI  |
| 35 | 139 | Стойки/аксесоари misc | ASBIS/VALI  |
| 36 | 0 | Лаптопи, таблети и аксесоари (parent) | VALI  |
| 37 | 1251 | Лаптопи | VALI **104 несъответствия** |
| 39 | 36 | Таблети/Аксесоари за таблети | VALI  |
| 40 | 104 | Чанти за лаптопи | VALI  |
| 41 | 19 | Аксесоари за таблети | VALI  |
| 42 | 37 | Зарядни за лаптопи | VALI  |
| 43 | 46 | Стойки за лаптопи | VALI  |
| 47 | 47 | Аксесоари за лаптопи/таблети | VALI  |
| 50 | 336 | Монитори | VALI  |
| 51 | 59 | Стойки за монитори | VALI  |
| 52 | 28 | Интерактивни дисплеи | TEKRA/VALI  |
| 54 | 59 | Компютърна периферия (parent) | VALI  |
| 55 | 73 | Хард дискове External | VALI  |
| 56 | 83 | External SSD | VALI  |
| 57 | 261 | USB Flash памет | VALI  |
| 58 | 57 | USB Hub-ове | VALI  |
| 59 | 79 | Тонколони | VALI  |
| 60 | 78 | Уеб камери | VALI  |
| 61 | 168 | Клавиатури | VALI  |
| 62 | 285 | Мишки | VALI  |
| 63 | 29 | Четци за карти | VALI  |
| 64 | 28 | Подложки за мишки | VALI  |
| 65 | 94 | Микрофони | VALI  |
| 66 | 57 | Слушалки | VALI  |
| 67 | 16 | Earbuds/Слушалки тапи | VALI  |
| 71 | 12 | Сторидж у-ва | VALI  |
| 72 | 87 | UPS (AVR/Line-Interactive) | VALI  |
| 73 | 33 | UPS (Online) | VALI  |
| 78 | 54 | Принтери | VALI  |
| 79 | 9 | Мултифункционални устройства | VALI  |
| 83 | 23 | Скенери | VALI  |
| 86 | 779 | Консумативи за принтери (мастила/тонери) | VALI **12 несъответствия** |
| 99 | 37 | Рутери и мрежово оборудване | TEKRA/VALI  |
| 107 | 94 | Безжични рутери | VALI  |
| 109 | 81 | Суичове неуправляеми | VALI  |
| 110 | 60 | Суичове управляеми | VALI  |
| 113 | 6 | Защитни стени (Firewalls) | VALI  |
| 115 | 30 | Проектори | VALI  |
| 120 | 89 | Преносими тонколони | VALI  |
| 127 | 85 | Захранвания (Super Flower) | VALI  |
| 129 | 188 | USB кабели | VALI  |
| 131 | 120 | HDMI кабели | VALI  |
| 135 | 86 | Мрежови кабели (patch) | VALI  |
| 139 | 26 | Телевизори | VALI  |
| 150 | 160 | Memory карти | VALI  |
| 154 | 238 | Смартфони (Nokia/Realme/телефони) | MOST/ASBIS  |
| 156 | 30 | Смарт часовници | VALI  |
| 159 | 78 | Bluetooth слушалки | MOST  |
| 160 | 28 | Powerbank | MOST  |
| 167 | 104 | Геймърска периферия (parent) | VALI  |
| 168 | 13 | Гейминг конзоли | VALI  |
| 169 | 87 | Геймърски столове | VALI  |
| 170 | 18 | Геймърски бюра | VALI  |
| 171 | 95 | Геймърски слушалки | VALI  |
| 172 | 209 | Геймърски клавиатури | VALI  |
| 173 | 128 | Геймърски падове | VALI  |
| 174 | 147 | Геймърски мишки | VALI  |
| 175 | 37 | Волани и педали | VALI  |
| 176 | 46 | Аксесоари за волани | VALI  |
| 177 | 51 | Геймпадове | VALI  |
| 178 | 78 | Геймърски очила | VALI  |
| 179 | 221 | Геймърски аксесоари | VALI  |
| 181 | 356 | Играчки/Фигурки/LEGO/Колекционерски | ASBIS/MOST  |
| 183 | 13 | Аксесоари за кола | MOST  |
| 199 | 183 | Батерии (общи/алкални) | MOST/ASBIS  |
| 201 | 167 | Батерии за лаптопи | MOST  |
| 230 | 6 | Видеонаблюдение (root) | TEKRA  |
| 231 | 160 | IP камери | TEKRA  |
| 249 | 293 | Монитори (видеонаблюдение) | TEKRA/MOST  |
| 264 | 79 | B&O аксесоари (слушалки) | MOST  |
| 298 | 414 | Стойки/монтажни системи — ASBIS catch-all | ASBIS **38 несъответствия** |
| 325 | 770 | Аудио компоненти/Колони | ASBIS/MOST **30 несъответствия** |
| 366 | 89 | HDD External | MOST  |
| 380 | 408 | Кабели/мрежово — ASBIS catch-all | ASBIS **237 несъответствия** |
| 402 | 363 | Охладители/термо паста | ASBIS **44 несъответствия** |
| 406 | 209 | Компютърна периферия | ASBIS  |
| 413 | 294 | Дънни платки | ASBIS  |
| 415 | 237 | Памети | ASBIS  |
| 430 | 209 | Сървъри (assembled) | ASBIS  |
| 436 | 269 | Видео карти | ASBIS  |
| 440 | 69 | Лаптопи | ASBIS  |
| 443 | 518 | Геймърска периферия | ASBIS **22 несъответствия** |
| 454 | 81 | Дребни домакински уреди | ASBIS  |
| 455 | 87 | Вентилатори | ASBIS  |
| 489 | 130 | Аудио (B&O/Bang & Olufsen) | MOST  |
| 500 | 166 | IP камери/Смарт у-ва | MOST/ASBIS  |
| 513 | 259 | SSD | ASBIS  |
| 517 | 446 | Flash памет/SD карти | ASBIS  |

---

## 2. Грешно категоризирани продукти — по групи

**Общо: 487 несъответствия**

### Захранвания (PSU) в категория Кабели/мрежово

- **Текуща категория:** Кабели/мрежово ASBIS (380)
- **Предложена категория:** Захранвания (9)
- **Брой продукти:** 237

| SKU | name_en | Текуща кат. | Предложена кат. |
|-----|---------|-------------|-----------------|
| `TS_ECO_POWER_600W` | TS Eco Power Supply TrendSonic AC 115/230V, 50/60Hz, DC 3.3/5/12V, 600W, 20+4 pi | 380 | 9 |
| `SF-750F14HG` | Super Flower Leadex III 750W 80 PLUS GOLD, Full Cable Management, black, 5 years | 380 | 9 |
| `SF-750Z12DB(DA)` | Super Flower Zillion DB Bronze 750W ATX 3.1 80 Plus Bronze, Flat Black Cables, 1 | 380 | 9 |
| `SF-850F14RG` | Super Flower Leadex III 850W ARGB 80 PLUS GOLD, Full Cable Management, white, 5  | 380 | 9 |
| `TC-1300T` | Asrock TC-1300T, 1300W, 80 Plus Titanium, Fully Modular PSU, Japanese Capacitors | 380 | 9 |
| `STC650` | COUGAR STC650 PSU, 80 plus White, 650W | 380 | 9 |
| `TC-1650T` | Asrock TC-1650T, 1650W, 80 Plus Titanium, Fully Modular PSU, Japanese Capacitors | 380 | 9 |
| `STC600` | COUGAR STC600 PSU, 80 plus White, 600W | 380 | 9 |
| `CP-9020297-EU` | CORSAIR RM1000e, 1000 Watt, ATX 3.1, PCIe 5.1, Cybenetics GOLD Certified, Fully  | 380 | 9 |
| `CP-9020270-EU` | CORSAIR RM850x, 850 Watt, ATX 3.1, Cybenetics Gold Certified, Fully Modular | 380 | 9 |
| `SL-750G` | Asrock SL-750G, 750W, 80 Plus GOLD, Fully Modular PSU, Japanese Capacitors, 2x E | 380 | 9 |
| `SL-850GW` | Asrock SL-850GW, 850W, 80 Plus GOLD, Fully Modular PSU, Japanese Capacitors, 2x  | 380 | 9 |
| `SF-850F14TG` | Super Flower Leadex V Gold Pro 850W 80 Plus Gold, Full Modular, Compact 130mm Si | 380 | 9 |
| `SF-1000F14GE` | Super Flower Leadex III Gold 1000W ATX 3.1, 80 Plus Gold, Fully Modular, Flat Bl | 380 | 9 |
| `SL-850G` | Asrock SL-850G, 850W, 80 Plus GOLD, Fully Modular PSU, Japanese Capacitors, 2x E | 380 | 9 |
| `SF-850F14GE(WH)` | Super Flower Leadex III Gold 850W ATX 3.1 80 Plus Gold, Fully Modular, Flat Whit | 380 | 9 |
| `SF-1000F14PE` | Super Flower Leadex VI Platinum Pro 1000W, 80 Plus Platinum, Fully Modular, 12VH | 380 | 9 |
| `SF-650P14XE` | Super Flower Legion GX 650W 80 Plus Gold PRO, 90+efficiency Semi-modular, 5 year | 380 | 9 |
| `SF-550Z12DB` | Super Flower Zillion DB Bronze 550W ATX 2.4, 80 Plus Bronze, Flat Black Cables,  | 380 | 9 |
| `SF-850F14PE` | Super Flower Leadex VI Platinum Pro 850W, 80 Plus Platinum, Fully Modular, 12VHP | 380 | 9 |
| `SF-1600F14HT` | Super Flower Leadex Titanium 1600W, 80 Plus Titanium, Fully Modular, 140mm Dual  | 380 | 9 |
| `SF-1000F14HT` | Super Flower Leadex 80 Plus Titanium 1000W, Fully Modular, 140mm Dual Ball Beari | 380 | 9 |
| `SF-850P14XE` | Super Flower Legion GX Gold PRO 850W, 80 Plus Gold, Semi-modular, 12VHPWR Cable  | 380 | 9 |
| `SL-650G` | Asrock SL-650G, 650W, 80 Plus GOLD, Fully Modular PSU, Japanese Capacitors, 2x E | 380 | 9 |
| `SF-850F14TP` | Super Flower Leadex V Platinum Pro 850W, 80 Plus Platinum, Fully Modular, 12VHPW | 380 | 9 |
| `SF-1000F14TG` | Super Flower Leadex V Gold Pro 1000W 80 Plus Gold, Full Modular, Compact 130mm S | 380 | 9 |
| `SF-750R14HE` | Super Flower Leadex III 750W 80 Plus Bronze PRO, Fully Modular, 3 years warranty | 380 | 9 |
| `SF-650P14XE_HX` | Super Flower Legion HX 650W 80 Plus Gold, 90+efficiency Fixed cables, 5 years wa | 380 | 9 |
| `SF-650F14MP` | Super Flower Leadex 650W 80 PLUS PLATINUM, Full Cable Management, black, 5 years | 380 | 9 |
| `SF-650F14HG` | Super Flower Leadex III 650W 80 PLUS GOLD, Full Cable Management, black, 5 years | 380 | 9 |
| `SF-1300F14GE(WH)` | Super Flower Leadex III Gold 1300W ATX 3.1 80 Plus Gold, Fully Modular, Flat Whi | 380 | 9 |
| `SF-650F14RG` | Super Flower Leadex III 650W ARGB 80 PLUS GOLD, Full Cable Management, white, 5  | 380 | 9 |
| `UPS-TOWER-EU` | Ubiquiti UPS-Tower-EU UniFi managed 1kVA uninterruptible power supply with 5 sur | 380 | 9 |
| `PRO-850G` | Asrock PRO-850G, 850W, 80 Plus GOLD, Non Modular PSU, 1x EPS 12V CPU(4+4pin), 2x | 380 | 9 |
| `SL-1000GW` | Asrock SL-1000GW, 1000W, 80 Plus GOLD, Fully Modular PSU, Japanese Capacitors, 2 | 380 | 9 |
| `SF-750P14XE` | Super Flower Legion GX Gold PRO 750W, 80 Plus Gold, Semi-modular, 120mm F.D.B. S | 380 | 9 |
| `SF-850Z12DB` | Super Flower Zillion DB Bronze 850W ATX 2.4, 80 Plus Bronze, Flat Black Cables,  | 380 | 9 |
| `SL-1200GW` | Asrock SL-1200GW, 1200W, 80 Plus GOLD, Fully Modular PSU, Japanese Capacitors, 2 | 380 | 9 |
| `SL-1000G` | Asrock SL-1000G, 1000W, 80 Plus GOLD, Fully Modular PSU, Japanese Capacitors, 2x | 380 | 9 |
| `SF-2800F14HP` | Super Flower Leadex Titanium 2800W ATX 3.1 80 Plus Titanium, 4x16P Cables, Fully | 380 | 9 |
| `SF-1000F14XP` | Super Flower Leadex VII Platinum PRO 1000W ATX 3.1, 80 Plus Platinum, Fully Modu | 380 | 9 |
| `SF-1200F14XP(WH)` | Super Flower Leadex VII Platinum 1200W ATX 3.1, 80 Plus Platinum, Fully Modular, | 380 | 9 |
| `SF-1000F14MP` | Super Flower Leadex Platinum SE 1000W, 80 Plus Platinum, Fully Modular, 12VHPWR  | 380 | 9 |
| `SF-1250Z12FG` | Super Flower Zillion FG Gold 1250W ATX 3.1, 80 Plus Gold, Fully Modular, Flat Bl | 380 | 9 |
| `SF-1000F14TP` | Super Flower Leadex V Platinum Pro 1000W, 80 Plus Platinum, Fully Modular, 12VHP | 380 | 9 |
| `SF-750F14GE` | Super Flower Leadex III Gold 750W ATX 3.1, 80 Plus Gold, Fully Modular, Flat Bla | 380 | 9 |
| `SF-550Z12DW` | Super Flower Zillion DW White 550W ATX 2.4, 80 Plus White, Flat Black Cables, 12 | 380 | 9 |
| `SF-650R14HE` | Super Flower Leadex III 650W 80 Plus Bronze PRO, Fully Modular, 3 years warranty | 380 | 9 |
| `SF-1000F14EG` | Super Flower Leadex II 1000W 80 Plus Gold, 92+ efficiency, LED connectors, Full  | 380 | 9 |
| `SF-850F14XP(WH)` | Super Flower Leadex VII Platinum PRO 850W ATX 3.1, 80 Plus Platinum, Fully Modul | 380 | 9 |
| ... | *и още 187 продукта* | | |

### Калъфи/чанти/раници в категория Лаптопи

- **Текуща категория:** Лаптопи (37)
- **Предложена категория:** Чанти за лаптопи (40)
- **Брой продукти:** 68

| SKU | name_en | Текуща кат. | Предложена кат. |
|-----|---------|-------------|-----------------|
| `xxx` | ASUS AP1602 BACKPACK  GR 16 | 37 | 40 |
| `GX41K68624` | LENOVO YOGA 14.5 SLEEVE GRAY | 37 | 40 |
| `90-XB4000BA00010` | ASUS NEREUS CARRY BAG 16 IN BK | 37 | 40 |
| `90XB001P-BSL090` | ASUS VERSASLEAVE X /WH COVER | 37 | 40 |
| `90XB015P-BSL010` | ASUS HD7 PERS.COVER PINK | 37 | 40 |
| `90XB015P-BSL020` | ASUS HD7 PERS.COVER YG | 37 | 40 |
| `90XB015P-BSL070` | ASUS TRICOVER ME102A WHITE | 37 | 40 |
| `90XB09JN-BBP020` | ASUS BP1800 ROG BACKPACK  18 | 37 | 40 |
| `90XB015P-BSL0M0` | ASUS TRICOVER /PHO HD7  BLACK | 37 | 40 |
| `90XB015P-BSL0N0` | ASUS TRICOVER /PHO HD7  WHITE | 37 | 40 |
| `90XB015P-BSL0D0` | ASUS TRICOVER ME180A  WHITE | 37 | 40 |
| `90XB015P-BSL1J0` | ASUS MAGSMART COVER/SR/ ME176C | 37 | 40 |
| `90XB015P-BSL1M0` | ASUS MAGSMART COVER/YEL/ME176C | 37 | 40 |
| `90XB015P-BSL1K0` | ASUS MAGSMART COVER/BL/ME176C | 37 | 40 |
| `90XB001P-BSL020` | ASUS VERSASLEAVE 7 /WH COVER | 37 | 40 |
| `90XB001P-BSL010` | ASUS VERSASLEAVE 7 /BK COVER | 37 | 40 |
| `90XB001P-BSL030` | ASUS VERSASLEAVE 7 /BL COVER | 37 | 40 |
| `90-XB3TOKSL00230` | ASUS PREMIUM COVER NEXUS7/BLCK | 37 | 40 |
| `90-XB3TOKSL001Q0` | ASUS TRAVEL COVER NEXUS7/ORANG | 37 | 40 |
| `90XB06S0-BBP020` | ASUS BP4701 BACKPACK BLK 15-17 | 37 | 40 |
| `90XB0AM0-BBP000` | ASUS PP2600 PROART BACKPACK BL | 37 | 40 |
| `90XXB0A20-BBP000` | ASUS AP1602 BACKPACK  GREY 16 | 37 | 40 |
| `LC.BAG0A.002` | ACER NEO 10.1INCH SLEEVE | 37 | 40 |
| `A500 PROTECTIVE CASE` | ACER A500 PROTECTIVE CASE | 37 | 40 |
| `LC.BAG0A.042` | ACER A100 PROTECTIVE CASE | 37 | 40 |
| `90XB09X0-BBP000` | ASUS BP3801 ROG SLASH BACKPACK | 37 | 40 |
| `90XB06L0-BBP000` | ASUS BP2701 ROG BACKPACK 17 | 37 | 40 |
| `90XB08L0-BBP050` | ASUS AP4600 BACKPACK 16 BLACK | 37 | 40 |
| `NP.BAG11.007` | ACER PORTFOLIO CASE A1-810 WHI | 37 | 40 |
| `NP.BAG1A.290` | ACER ROLLTOP BACKPACK | 37 | 40 |
| `NP.BAG1A.189` | ACER NB CARRY CASE 15.6 ABG558 | 37 | 40 |
| `NP.BAG11.00A` | ACER PORTFOLIO CASE W3-810 GRY | 37 | 40 |
| `NP.BAG11.00B` | ACER PORTFOLIO CASE B1-710 WHI | 37 | 40 |
| `NP.BAG11.00C` | ACER PORTF CASE B1-710 D.GRAY | 37 | 40 |
| `NP.BAG11.009` | ACER PORTF CASE W3-810 WHITE | 37 | 40 |
| `GP.BAG11.02E` | ACER NITRO BACKPACK ABG147 | 37 | 40 |
| `GP.BAG11.02A` | ACER 15.6 NITRO MF BACKPACK | 37 | 40 |
| `GP.BAG11.034` | ACER URBAN BACKPACK 15.6 GY&GR | 37 | 40 |
| `GX40Q75214` | LENOVO BACKPACK B510 15.6 | 37 | 40 |
| `GX40Z50941` | LENOVO 14 URBAN SLEEVE CASE | 37 | 40 |
| `GX41H70101` | LENOVO IDEAPAD MODERN BACKPACK | 37 | 40 |
| `GX41C86982` | LENOVO LEGION ACTIVE BACKPACK | 37 | 40 |
| `90XB0A60-BSL000` | ASUS AC1600 CARRY BAG GREY 15 | 37 | 40 |
| `90XB015P-BSL0J0` | ASUS SIDE FLIP COVER /NOTE6/WH | 37 | 40 |
| `90XB015P-BSL000` | ASUS HD7 PERS.COVER PB | 37 | 40 |
| `90XB015P-BSL0P0` | ASUS TRICOVER /PHO HD7 RED | 37 | 40 |
| `90XB015P-BSL0EO` | ASUS TRICOVER ME180A  RED | 37 | 40 |
| `90XB015P-BSL0C0` | ASUS TRICOVER ME180A  BLACK | 37 | 40 |
| `90XB015P-BSL1L0` | ASUS MAGSMART COVER/RED/ME176C | 37 | 40 |
| `90-XB3TOKSL001N0` | ASUS TRAVEL COVER NEXUS7/BLUE | 37 | 40 |
| ... | *и още 18 продукта* | | |

### Вентилатори в категория Охладители (ASBIS)

- **Текуща категория:** Охладители/термо паста ASBIS (402)
- **Предложена категория:** Вентилатори ASBIS (455)
- **Брой продукти:** 44

| SKU | name_en | Текуща кат. | Предложена кат. |
|-----|---------|-------------|-----------------|
| `WINDPOWER_964_RGB` | Windpower 964 RGB EN46478, Black Anodized, 90mm RGB PWM Fan, Single Rainbow LED  | 402 | 455 |
| `WINDPOWER_WP964` | Xigmatek Windpower WP964 EN42357 Intel: LGA 2066/2011-v3/2011/1366/115x; AMD: AM | 402 | 455 |
| `WINDPOWER_WP1266` | Xigmatek Windpower_WP1266 EN42388; Intel: LGA 2066/2011-v3/2011/1366/115x; AMD:  | 402 | 455 |
| `WINDPOWER_WP1264` | Xigmatek Windpower_WP1264 EN42371; Intel: LGA 2066/2011-v3/2011/1366/115x; AMD:  | 402 | 455 |
| `MAG_CORELIQUID_C360` | MAG CORELIQUID C360, Global, 3x ARGB Fan, ARGB Block, Color Box, LGA 1150/1151/1 | 402 | 455 |
| `WINDPOWER_964_RGB_ARCTIC` | Windpower 964 RGB Arctic EN47604, White Top Fin, 92mm White RGB PWM Fan, Single  | 402 | 455 |
| `FD-A-ADJ2-001` | FD ADJUST 2 CONTR FOR ARGB FAN | 402 | 455 |
| `PRODIGY_ST1266` | Xigmatek Prodigy ST1266 HDP EN9665,  Intel LGA Socket 775/ 1150/ 1151/ 1155/ 115 | 402 | 455 |
| `PRODIGY_ST963` | Xigmatek Prodigy ST963 HDT EN9658, Intel LGA Socket 775/ 1150/ 1151/ 1155/ 1156/ | 402 | 455 |
| `MPG_CORELIQUID_P13_360_WHITE` | MSI MPG CORELIQUID P13 360 WHITE, 3x120mm Pre-installed CycloBlade 9 ARGB Daisy  | 402 | 455 |
| `MAG_CORELIQUID_A13_360` | MSI MAG CORELIQUID A13 360, 3x120mm Pre-installed ARGB Daisy Chain Fans, Replaca | 402 | 455 |
| `MAG_CORELIQUID_A15_240` | MSI MAG CORELIQUID A15 240, 2x120mm Pre-installed ARGB Daisy Chain Fans, Adjusta | 402 | 455 |
| `MAG_COREFROZR_AA13_WHITE` | MSI MAG COREFROZR AA13 WHITE, 1x120mm ARGB Fan, TDP 240W, Direct Touch Heat-pipe | 402 | 455 |
| `MAG_CORELIQUID_A15_360` | MSI MAG CORELIQUID A15 360, 3x120mm Pre-installed ARGB Daisy Chain Fans, Adjusta | 402 | 455 |
| `CO-9050040-WW` | Corsair ML120 Pro, 120mm Premium Magnetic Levitation Fan | 402 | 455 |
| `CO-9050075-WW` | Corsair ML120 PRO RGB, 120mm Premium Magnetic Levitation RGB LED PWM Fan, Single | 402 | 455 |
| `CO-9050081-WW` | Corsair AF120 LED Low Noise Cooling Fan, Single Pack - Blue | 402 | 455 |
| `CO-9050086-WW` | Corsair AF140 LED Low Noise Cooling Fan, Single Pack - Red | 402 | 455 |
| `CO-9050050-WW` | CORSAIR ML140, 140mm Premium Magnetic Levitation Fan, Single Pack | 402 | 455 |
| `MAG_CORELIQUID_A13_240_WHITE` | MSI MAG CORELIQUID A13 240 WHITE, 2x120mm Pre-installed ARGB Daisy Chain Fans, R | 402 | 455 |
| `MAG_CORELIQUID_A13_360_WHITE` | MSI MAG CORELIQUID A13 360 WHITE, 3x120mm Pre-installed ARGB Daisy Chain Fans, R | 402 | 455 |
| `CO-9050087-WW` | Corsair AF140 LED Low Noise Cooling Fan, Single Pack - Blue | 402 | 455 |
| `CO-9050093-WW` | SP120 RGB PRO, 120mm RGB LED Fan, Single Pack | 402 | 455 |
| `CO-9050049-WW` | CORSAIR ML120, 120mm Premium Magnetic Levitation Fan, Single Pack | 402 | 455 |
| `CO-9050080-WW` | Corsair AF120 LED Low Noise Cooling Fan, Single Pack - Red | 402 | 455 |
| `CO-9050095-WW` | SP140 RGB PRO, 140mm RGB LED Fan, Single Pack | 402 | 455 |
| `CO-9050079-WW` | Corsair AF120 LED Low Noise Cooling Fan, Single Pack - White | 402 | 455 |
| `4N004-06-20G` | SAPPHIRE ARGB FAN FOR NITRO+ | 402 | 455 |
| `CG3MFZA850001` | COUGAR Forza 85, 6 Heat pipes, Nickel Plated Copper Base, 1x MHP120 Fan, Screwdr | 402 | 455 |
| `CG3MFZ1350001` | COUGAR Forza 135, 6mm Heat pipe, Nickel Plated Copper Base, 1x MHP120 Fan, 1x MH | 402 | 455 |
| `AIR_KILLER_PRO` | Air Killer Pro EN47895 Black, X22A FAN, ARGB Top Cover, Reinforced Plastic Steel | 402 | 455 |
| `AIR_KILLER_PRO_ARCTIC` | Xigmatek Air Killer Pro Arctic EN47925 White, X22A Arctic Fan, ARGB Top Cover, R | 402 | 455 |
| `AK4_DIGITAL_ARCTIC` | AK4 Digital Arctic (WH, ARGB & Digital LED Panel Top Cover, ARGB PWM Fan, Intel  | 402 | 455 |
| `MEG_CORELIQUID_S360` | MSI MEG CORELIQUID S360, 2.4” Customizable IPS Display, 3x MEG SILENT GALE P12 2 | 402 | 455 |
| `MAG_CORELIQUID_A13_240` | MSI MAG CORELIQUID A13 240, 2x120mm Pre-installed ARGB Daisy Chain Fans, Replaca | 402 | 455 |
| `AIR_KILLER_S_ARCTIC` | Air Killer S Arctic, EN47932, White, X22C Arctic Fan, White Top Cover, Reinforce | 402 | 455 |
| `CO-9050085-WW` | Corsair AF140 LED Low Noise Cooling Fan, Single Pack - White | 402 | 455 |
| `CO-9050077-WW` | Corsair ML140 PRO RGB, 140mm Premium Magnetic Levitation RGB LED PWM Fan, Single | 402 | 455 |
| `MAG_COREFROZR_AA13` | MSI MAG COREFROZR AA13, 1x120mm ARGB Fan, TDP 240W, Direct Touch Heat-pipes, Int | 402 | 455 |
| `MPG_CORELIQUID_P13_360` | MSI MPG CORELIQUID P13 360, 3x120mm Pre-installed CycloBlade 9 ARGB Daisy Chain  | 402 | 455 |
| `AK4_DIGITAL` | AK4 Digital (Black, ARGB & Digital LED Panel Top Cover, ARGB PWM Fan, Intel Back | 402 | 455 |
| `CO-9050072-WW` | Corsair LL Series, LL120 RGB, 120mm Dual Light Loop RGB LED PWM Fan, 3 Fan Pack  | 402 | 455 |
| `CG3MFZA500001` | COUGAR Forza 50, 6mm Heat pipe, Nickel Plated Copper Base, 1x MHP120 Fan, Screwd | 402 | 455 |
| `AIR_KILLER_S` | Air Killer S EN47901 Black, X22C Fan, Black Top Cover, Reinforced Plastic Steel  | 402 | 455 |

### Слушалки/headset в категория Аудио/Колони

- **Текуща категория:** Аудио/Колони (325)
- **Предложена категория:** Слушалки (66)
- **Брой продукти:** 30

| SKU | name_en | Текуща кат. | Предложена кат. |
|-----|---------|-------------|-----------------|
| `981-000575` | LOGITECH H570E HEADSET STEREO | 325 | 66 |
| `981-000634` | LOGITECH WL HEADSET G533 | 325 | 66 |
| `GR270` | A4 GR270 HEADSET / WL+BT V5.2 | 325 | 66 |
| `FH300U` | A4 FH300U HEADSET /NEON | 325 | 66 |
| `981-000350` | LOGITECH HEADSET H150 STEREO | 325 | 66 |
| `MK-750` | A4 MK-750 EARPHONE METALIC | 325 | 66 |
| `BH300 GREEN` | A4 BH300 BT HEARPHONE GREEN | 325 | 66 |
| `B20 GRAY` | A4 B20 TWS EARBUDS GRAY | 325 | 66 |
| `MK-770` | A4 MK-770 EARPHONE METALIC | 325 | 66 |
| `MK-790` | A4 MK-790 EARPHONE METALIC | 325 | 66 |
| `G600i` | A4 BLOODY G600I RGB HEADSET | 325 | 66 |
| `GXD1B87065` | LENOVO H200 GAMING HEADSET | 325 | 66 |
| `981-000100` | LOGITECH PC USB HEADSET 960 | 325 | 66 |
| `HEADPHONE CARS` | DISNEY HEADPHONE CARS | 325 | 66 |
| `4XD1R31390` | LENOVO TWS EARBUDS X9 EDN | 325 | 66 |
| `R980T-BL` | Edifier R980T 2.0 Bookshelf Speakers, Dual RCA and 3.5mm headphone output, 0.5-i | 325 | 66 |
| `MR5-BL` | Edifier MR5 Active Studio Monitors (Tri-amped), BT V6.0, Balanced XLR, Balanced  | 325 | 66 |
| `GP.HDS11.02E` | ACER NITRO HEADSET II NHW200 | 325 | 66 |
| `GP.HDS11.00Z` | ACER WL EARPHONE AHR162 | 325 | 66 |
| `75261462` | XPG PRECOG STUDIO GAM HEADSET | 325 | 66 |
| `300637` | VSM HEADSET SPEAK DAY WHITE | 325 | 66 |
| `FD-HS-SCA1-01` | FD SCAPE DARK WL HEADSET | 325 | 66 |
| `8P00000251` | NOKIA TWS-852W EARBUDS | 325 | 66 |
| `FD-HS-SCA1-02` | FD SCAPE LIGHT WL HEADSET | 325 | 66 |
| `981-000271` | LOGITECH HEADSET H110 STEREO | 325 | 66 |
| `981-000475` | LOGITECH H340 USB HEADSET | 325 | 66 |
| `981-000593` | LOGITECH HEADSET H111 STEREO | 325 | 66 |
| `981-000589` | LOGITECH HEADSET H151 STEREO | 325 | 66 |
| `981-000480` | LOGITECH H540 USB HEADSET | 325 | 66 |
| `MR5-WH` | Edifier MR5 Active Studio Monitors (Tri-amped), BT V6.0, Balanced XLR, Balanced  | 325 | 66 |

### Уеб камери в категория Стойки/монтажни

- **Текуща категория:** Стойки/монтажни ASBIS (298)
- **Предложена категория:** Уеб камери/Компютърна периферия (60)
- **Брой продукти:** 20

| SKU | name_en | Текуща кат. | Предложена кат. |
|-----|---------|-------------|-----------------|
| `960-001055` | LOGITECH C920 Pro HD Webcam - USB | 298 | 60 |
| `LRG-SC910` | LORGAR Circulus 910, Streaming web camera, 5MP 2592X1944 max resolution, up to 6 | 298 | 60 |
| `CNE-CWC1` | CANYON Enhanced 1.3 Megapixels resolution webcam with USB2.0 connector | 298 | 60 |
| `CNE-CWC3N` | CANYON webcam C3 HD 720p Black | 298 | 60 |
| `960-001623` | LOGITECH Brio 100 Full HD Webcam - ROSE - USB | 298 | 60 |
| `960-001194` | LOGITECH BRIO 4K Stream Edition Webcam - BLACK - USB | 298 | 60 |
| `960-001421` | LOGITECH BRIO 500 Full HD Webcam - ROSE - USB | 298 | 60 |
| `960-001252` | LOGITECH C920S Pro HD Webcam - USB - EMEA - DERIVATIVES | 298 | 60 |
| `960-001088` | LOGITECH C922 Pro Stream Webcam - Tripod - BLACK - USB | 298 | 60 |
| `960-001559` | LOGITECH MX Brio 4K Ultra HD Webcam - GRAPHITE - 2.4GHZ - EMEA28-935 - B2C | 298 | 60 |
| `960-001617` | LOGITECH Brio 100 Full HD Webcam - OFF-WHITE - USB | 298 | 60 |
| `960-001554` | LOGITECH MX Brio 4K Ultra HD Webcam - PALE GREY - 2.4GHZ - EMEA28-935 - B2C | 298 | 60 |
| `960-001442` | LOGITECH Brio 300 Full HD webcam - OFF-WHITE - USB | 298 | 60 |
| `960-001364` | LOGITECH C505 HD Webcam - BLACK - USB | 298 | 60 |
| `10WAC9901` | Elgato Facecam MK.2, Premium 1080p60 webcam | 298 | 60 |
| `960-001448` | LOGITECH Brio 300 Full HD webcam - ROSE - USB | 298 | 60 |
| `960-001436` | LOGITECH Brio 300 Full HD webcam - GRAPHITE - USB | 298 | 60 |
| `960-001063` | LOGITECH C270 HD Webcam - BLACK - USB | 298 | 60 |
| `KEY_39887` | Web Camera LOGITECH C100 (1.3Mpixel, CMOS, USB 2.0) Черен/Сив | 298 | 60 |
| `960-001718` | LOGITECH WEBCAM - BRIO 4K - GRAPHITE - USB - EMEA28i-935 | 298 | 60 |

### Elgato стрийминг оборудване в Геймърска периферия

- **Текуща категория:** Геймърска периферия ASBIS (443)
- **Предложена категория:** Геймърски аксесоари (179)
- **Брой продукти:** 20

| SKU | name_en | Текуща кат. | Предложена кат. |
|-----|---------|-------------|-----------------|
| `10LAL9901` | Elgato Key Light Neo (w/o stand) | 443 | 179 |
| `10GAT9901` | Corsair Elgato Stream Deck XL - Advanced Stream Control with 32 customizable LCD | 443 | 179 |
| `10GAM9901` | Corsair Elgato Cam Link 4K | 443 | 179 |
| `10AAX9911` | Elgato Wave Desk Plate (white) | 443 | 179 |
| `10GBJ9901` | Elgato Stream Deck Neo | 443 | 179 |
| `10AAZ9901` | Elgato Stand for Key Light Neo | 443 | 179 |
| `10LAJ9911` | Elgato Key Light Neo Black | 443 | 179 |
| `10AAM9901` | Corsair Elgato Wave Mic Arm | 443 | 179 |
| `10GBF9901` | Corsair Stream Deck Foot Pedal | 443 | 179 |
| `10MAN9901` | Elgato Wave XLR MK.2 | 443 | 179 |
| `10ABA9901` | Elgato Clamp for Key Light Neo | 443 | 179 |
| `10AAX9901` | Elgato Wave Desk Plate (black) | 443 | 179 |
| `10LAJ9901` | Elgato Key Light Neo | 443 | 179 |
| `10GAI9931` | Elgato Stream Deck Mini Discord | 443 | 179 |
| `10AAY9901` | Elgato Wave Mic Arm High Rise MK2 | 443 | 179 |
| `10GBD9901` | Corsair Elgato Stream Deck + | 443 | 179 |
| `10AAT9911` | Elgato Wave Mic Arm Pro LP (White) | 443 | 179 |
| `10GBA9901` | Corsair Stream Deck MK.2 | 443 | 179 |
| `10AAT9901` | Elgato Wave Mic Arm Pro LP | 443 | 179 |
| `10AAU9901` | Elgato Wave Desk Stand | 443 | 179 |

### Слушалки в категория Стойки/монтажни

- **Текуща категория:** Стойки/монтажни ASBIS (298)
- **Предложена категория:** Слушалки (66)
- **Брой продукти:** 18

| SKU | name_en | Текуща кат. | Предложена кат. |
|-----|---------|-------------|-----------------|
| `WH950NB-IV` | Edifier WH950NB Wireless Over-Ear Headphones, 40mm Dynamic Titanium-Coated Drive | 298 | 66 |
| `W820NB PLUS V25-NAVY` | Edifier W820NB Plus v2025 Wireless Over-Ear Headphones, 40mm Dynamic Titanium-Co | 298 | 66 |
| `W820NB PLUS-IV` | Edifier W820NB Plus Wireless Over-Ear Headphones, 40mm Dynamic Titanium-Coated D | 298 | 66 |
| `W820NB PLUS-BL` | Edifier W820NB Plus Wireless Over-Ear Headphones, 40mm Dynamic Titanium-Coated D | 298 | 66 |
| `W820NB PLUS-GR` | Edifier W820NB Plus Wireless Over-Ear Headphones, 40mm Dynamic Titanium-Coated D | 298 | 66 |
| `W800BT SE-BL` | Edifier W800BT SE Wireless Over-Ear Headphones, 40mm Dynamic Titanium-Coated Dri | 298 | 66 |
| `W800BT SE-WH` | Edifier W800BT SE Wireless Over-Ear Headphones, 40mm Dynamic Titanium-Coated Dri | 298 | 66 |
| `W800BT PRO-IV` | Edifier W800BT Pro Wireless Over-Ear Headphones, ANC, 40mm Dynamic Titanium-Coat | 298 | 66 |
| `W800BT SE-GR` | Edifier W800BT SE Wireless Over-Ear Headphones, 40mm Dynamic Titanium-Coated Dri | 298 | 66 |
| `W800BT PLUS-WH` | Edifier W800BT Plus Wireless Over-Ear Headphones, 40mm Dynamic Drivers, BT V5.1, | 298 | 66 |
| `W800BT PLUS-BL` | Edifier W800BT Plus Wireless Over-Ear Headphones, 40mm Dynamic Drivers, BT V5.1, | 298 | 66 |
| `CNS-CBTHS10PU` | CANYON headset OnRiff 10 ANC Purple | 298 | 66 |
| `CNS-CBTHS10BK` | CANYON headset OnRiff 10 ANC Black | 298 | 66 |
| `CNS-CBTHS4W` | CANYON headset OnRiff 4 White | 298 | 66 |
| `CNS-CBTHS4P` | CANYON headset OnRiff 4 Pink | 298 | 66 |
| `CNS-CBTHS4BL` | CANYON headset OnRiff 4 Blue | 298 | 66 |
| `CNS-CBTHS3DG` | CANYON headset BTHS-3 Black | 298 | 66 |
| `CNS-CBTHS4B` | CANYON headset OnRiff 4 Black | 298 | 66 |

### Таблети в категория Лаптопи

- **Текуща категория:** Лаптопи (37)
- **Предложена категория:** Таблети/Аксесоари за таблети (39)
- **Брой продукти:** 13

| SKU | name_en | Текуща кат. | Предложена кат. |
|-----|---------|-------------|-----------------|
| `ZABG0005GR` | LENOVO TAB TB350XU | 37 | 39 |
| `ZADA0217GR` | LENOVO TAB TB330FU/ZADA0217GR | 37 | 39 |
| `ZADA0036GR` | LENOVO TAB TB330FU/ZADA0036GR | 37 | 39 |
| `ZADB0190GR` | LENOVO TAB TB330XU/ZADB0190GR | 37 | 39 |
| `ZACH0113GR` | LENOVO TAB TB370FU /ZACH0113GR | 37 | 39 |
| `ZAEF0024GR` | LENOVO TAB TB321FU/ZAEF0024GR | 37 | 39 |
| `ZAFR0475GR` | LENOVO TAB TB336FU/ ZAFR0475GR | 37 | 39 |
| `ZAG70071GR` | LENOVO TAB TB361FU/ ZAG70071GR | 37 | 39 |
| `ZAG60036GR` | LENOVO TAB TB710FU /ZAG60036GR | 37 | 39 |
| `ZAHT0160GR` | LENOVO TAB TB355FU ZAHT0160GR | 37 | 39 |
| `RMP2102 / 6930144` | REALME PAD RMP2102 128G/6G LTE | 37 | 39 |
| `ZAF10172GR` | LENOVO TAB TB305XU/ ZAF10172GR | 37 | 39 |
| `ZA940346BG` | LENOVO TAB P11 PLUS HELIO | 37 | 39 |

### Принтери/скенери в категория Консумативи

- **Текуща категория:** Консумативи за принтери (86)
- **Предложена категория:** Принтери (78)
- **Брой продукти:** 12

| SKU | name_en | Текуща кат. | Предложена кат. |
|-----|---------|-------------|-----------------|
| `ZD4A042-D0EE00EZ` | ZD4A042-D0EE00EZ LABEL PRINTER | 86 | 78 |
| `ZT41142-TOE0000Z` | ZEBRA ZT41142-TOE0000Z PRINTER | 86 | 78 |
| `L1910A` | L1910A SCANNER SJ5590 | 86 | 78 |
| `7MD72E` | 7MD72E LJ MFP M140WE PRINTER | 86 | 78 |
| `L1956A` | L1956A SCANNER SJ G4010 | 86 | 78 |
| `XENON 1950G` | HONEYWELL XENON 1950G SCANNER | 86 | 78 |
| `ZD220 TT` | ZEBRA ZD220 TT PRINTER | 86 | 78 |
| `ZD220 DT` | ZEBRA ZD220 DT PRINTER | 86 | 78 |
| `ZD421` | ZEBRA ZD421 4INCH PRINTER | 86 | 78 |
| `ZD4A042-30EM00EZ` | ZEBRA ZD4A042-30EM00EZ PRINTER | 86 | 78 |
| `ZD4A042-30EE00EZ` | ZEBRA ZD4A042-30EE00EZ PRINTER | 86 | 78 |
| `ZD411` | ZEBRA ZD411 2INCH PRINTER | 86 | 78 |

### Зарядни/адаптери в категория Лаптопи

- **Текуща категория:** Лаптопи (37)
- **Предложена категория:** Зарядни за лаптопи (42)
- **Брой продукти:** 11

| SKU | name_en | Текуща кат. | Предложена кат. |
|-----|---------|-------------|-----------------|
| `90XB013N-MPW0A0` | ASUS U65W-01 65W MINI ADAPTER | 37 | 42 |
| `90XB08MN-MPW060` | ASUS AD280-OOE ADAPTER | 37 | 42 |
| `90XB06VN-MPW000` | ASUS AD120-00C ADAPTER | 37 | 42 |
| `90XB06MN-MPW000` | ASUS ROG AD240-00E NB ADAPTER | 37 | 42 |
| `LC.OTH0A.011` | ACER CAR CHARGER 18W A100/500 | 37 | 42 |
| `GX20L29354` | LENOVO POWER ADAPTER 65W | 37 | 42 |
| `GX20Z46287` | LENOVO 170W SLIM AC ADAPTER | 37 | 42 |
| `90XB014N-MPW0P0` | ASUS U90W-01 90W ADAPTER | 37 | 42 |
| `90XB09BN-MPW010` | ASUS AD45-00C ADAPTER | 37 | 42 |
| `GX20P92529` | LENOVO POWER ADAPTER 65W USB-C | 37 | 42 |
| `GX20Z46306` | LENOVO POWER ADAPTER SLIM 230W | 37 | 42 |

### Протектори/фолио в категория Лаптопи

- **Текуща категория:** Лаптопи (37)
- **Предложена категория:** Аксесоари за лаптопи/таблети (47)
- **Брой продукти:** 6

| SKU | name_en | Текуща кат. | Предложена кат. |
|-----|---------|-------------|-----------------|
| `HP.FLM11.006` | ACER AG PROTECT FILM A1-81X | 37 | 47 |
| `HP.FLM11.00C` | ACER AG PROTECT FILM B1-71X | 37 | 47 |
| `HP.FLM11.009` | ACER AG PROTECT FILM W3-81X | 37 | 47 |
| `HP.FLM11.00F` | ACER AGLR PROTECT FILM A3-A10 | 37 | 47 |
| `NP.FLM1A.012` | ACER AGLR PROTECT FILM A1-830 | 37 | 47 |
| `NP.FLM1A.010` | ACER AGLR PROTECT FILM B1-72X | 37 | 47 |

### Folio клавиатури/кейсове в категория Лаптопи

- **Текуща категория:** Лаптопи (37)
- **Предложена категория:** Аксесоари за лаптопи/таблети (47)
- **Брой продукти:** 2

| SKU | name_en | Текуща кат. | Предложена кат. |
|-----|---------|-------------|-----------------|
| `90NX0460-P00030 XXXXXX` | ASUS SIMPRO XXXXX | 37 | 47 |
| `90XB00HP-BKB1W0` | ASUS FOLIO KEYBOARD /BT | 37 | 47 |

### Докинг станции в категория Лаптопи

- **Текуща категория:** Лаптопи (37)
- **Предложена категория:** Аксесоари за лаптопи/таблети (47)
- **Брой продукти:** 2

| SKU | name_en | Текуща кат. | Предложена кат. |
|-----|---------|-------------|-----------------|
| `LC.DCK0A.001` | ACER A500 DOCKING STATION | 37 | 47 |
| `40AF0135EU` | LENOVO HYBRID DOCK/ 40AF0135EU | 37 | 47 |

### Гаранции в категория Лаптопи — трябва скриване

- **Текуща категория:** Лаптопи (37)
- **Предложена категория:** → Скриване (show_flag=false)
- **Брой продукти:** 2

| SKU | name_en | Текуща кат. | Предложена кат. |
|-----|---------|-------------|-----------------|
| `SV.WCBAP.B03` | ACER 3Y WARRANTY CHROMEBOOK | 37 | 0 |
| `SV.WNBAP.B09` | ACER 3Y CARRY-IN WARR /TRAVELM | 37 | 0 |

### LEGO в категория Геймърска периферия

- **Текуща категория:** Геймърска периферия ASBIS (443)
- **Предложена категория:** Играчки/Фигурки/LEGO (181)
- **Брой продукти:** 2

| SKU | name_en | Текуща кат. | Предложена кат. |
|-----|---------|-------------|-----------------|
| `BRICQ MOTION PRIME SET 45400` | LEGO BRICQ MOTION PRIME | 443 | 181 |
| `LEGO BRICQ MOTION ESSENTIAL SE` | LEGO BRICQ MOTION ESSENTIAL | 443 | 181 |

---

## 3. Резюме по категория

| Група мисматч | Брой | Текуща кат. ID | Предложена кат. ID |
|---------------|------|----------------|-------------------|
| Захранвания (PSU) в категория Кабели/мрежово | **237** | 380 | 9 |
| Калъфи/чанти/раници в категория Лаптопи | **68** | 37 | 40 |
| Вентилатори в категория Охладители (ASBIS) | **44** | 402 | 455 |
| Слушалки/headset в категория Аудио/Колони | **30** | 325 | 66 |
| Уеб камери в категория Стойки/монтажни | **20** | 298 | 60 |
| Elgato стрийминг оборудване в Геймърска периферия | **20** | 443 | 179 |
| Слушалки в категория Стойки/монтажни | **18** | 298 | 66 |
| Таблети в категория Лаптопи | **13** | 37 | 39 |
| Принтери/скенери в категория Консумативи | **12** | 86 | 78 |
| Зарядни/адаптери в категория Лаптопи | **11** | 37 | 42 |
| Протектори/фолио в категория Лаптопи | **6** | 37 | 47 |
| Folio клавиатури/кейсове в категория Лаптопи | **2** | 37 | 47 |
| Докинг станции в категория Лаптопи | **2** | 37 | 47 |
| Гаранции в категория Лаптопи — трябва скриване | **2** | 37 | 0 |
| LEGO в категория Геймърска периферия | **2** | 443 | 181 |

**Общо несъответствия: 487 продукта** *(вижте § 5 за пълния допълнителен анализ — реалната цифра е ~878)*

---

## 4. Предложени SQL корекции

```sql
-- ============================================================
-- Script 24: Почистване на грешно категоризирани продукти
-- Генерирано от анализ на 2026-07-20
-- ============================================================

BEGIN;

-- 1. Захранвания (PSU) в кабели/misc (380) → Захранвания (9)
UPDATE products SET category_id = 9, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 380
  AND (
       name_en ILIKE '%80 plus%'
    OR name_en ILIKE '%80plus%'
    OR name_en ILIKE '% psu%'
    OR name_en ILIKE '%power supply%'
    OR name_en ILIKE '%atx 3.%'
    OR name_en ILIKE '% watt%'
  );

-- 2. Калъфи/чанти/раници в Лаптопи (37) → Чанти за лаптопи (40)
UPDATE products SET category_id = 40, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 37
  AND (
       name_en ILIKE '%sleeve%'
    OR name_en ILIKE '%carry case%'
    OR name_en ILIKE '%backpack%'
    OR name_en ILIKE '%portfolio case%'
    OR name_en ILIKE '%portf case%'
    OR name_en ILIKE '%cover%'
    OR name_en ILIKE '%bag %'
    OR name_en ILIKE '% bag'
    OR name_en ILIKE '%tricover%'
    OR name_en ILIKE '%versasleave%'
    OR name_en ILIKE '%magsmart%'
    OR name_bg ILIKE '%калъф%'
    OR name_bg ILIKE '%чанта%'
    OR name_bg ILIKE '%раница%'
  );

-- 3. Самостоятелни вентилатори в Охладители ASBIS (402) → Вентилатори ASBIS (455)
UPDATE products SET category_id = 455, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 402
  AND name_en ILIKE '% fan%'
  AND name_en NOT ILIKE '%cooler%'
  AND name_en NOT ILIKE '%liquid cool%'
  AND name_en NOT ILIKE '%water cool%'
  AND name_en NOT ILIKE '%cpu cool%'
  AND name_en NOT ILIKE '%heatsink%';

-- 4. Слушалки в Аудио/Колони (325) → Слушалки (66)
UPDATE products SET category_id = 66, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 325
  AND (
       name_en ILIKE '%headset%'
    OR name_en ILIKE '%headphone%'
    OR name_en ILIKE '%earphone%'
    OR name_en ILIKE '%earbuds%'
  );

-- 5. Уеб камери в Стойки ASBIS (298) → Уеб камери (60)
UPDATE products SET category_id = 60, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 298
  AND (name_en ILIKE '%webcam%' OR name_en ILIKE '%web cam%');

-- 6. Слушалки в Стойки ASBIS (298) → Слушалки (66)
UPDATE products SET category_id = 66, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 298
  AND (
       name_en ILIKE '%headphone%'
    OR name_en ILIKE '%headset%'
    OR name_en ILIKE '%over-ear%'
  );

-- 7. Зарядни/адаптери в Лаптопи (37) → Зарядни за лаптопи (42)
UPDATE products SET category_id = 42, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 37
  AND (
       name_en ILIKE '%charger%'
    OR name_en ILIKE '% adapter%'
    OR name_en ILIKE '%mini adapter%'
    OR name_en ILIKE '%nb adapter%'
  );

-- 8. Таблети в Лаптопи (37) → скриване или таблети (39)
UPDATE products SET category_id = 39, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 37
  AND (
       name_en ILIKE 'LENOVO TAB%'
    OR name_en ILIKE 'REALME PAD%'
    OR name_en ILIKE '% tablet %'
  );

-- 9. Принтери/скенери в Консумативи (86) → Принтери (78)
UPDATE products SET category_id = 78, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 86
  AND (name_en ILIKE '%printer%' OR name_en ILIKE '%scanner%')
  AND name_en NOT ILIKE '%toner%'
  AND name_en NOT ILIKE '%cartridge%'
  AND name_en NOT ILIKE '%ink%'
  AND name_en NOT ILIKE '%compatible%'
  AND name_en NOT ILIKE '%maintenance%';

-- 10. LEGO в Геймърска периферия ASBIS (443) → Играчки/Фигурки (181)
UPDATE products SET category_id = 181, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 443
  AND (name_en ILIKE 'LEGO%' OR name_bg ILIKE 'LEGO%');

-- 11. Elgato стрийминг оборудване в Gaming peripherals (443) → Геймърски аксесоари (179)
UPDATE products SET category_id = 179, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 443
  AND (
       name_en ILIKE '%elgato key light%'
    OR name_en ILIKE '%stream deck%'
    OR name_en ILIKE '%cam link%'
    OR name_en ILIKE '%elgato wave%'
  );

-- 12. Протектори/фолио в Лаптопи (37) → Аксесоари за лаптопи/таблети (47)
UPDATE products SET category_id = 47, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 37
  AND (
       name_en ILIKE '%protect film%'
    OR name_en ILIKE '%screen protector%'
    OR name_en ILIKE '%aglr protect%'
    OR name_en ILIKE '%ag protect%'
  );

-- 13. Докинг станции в Лаптопи (37) → Аксесоари за лаптопи/таблети (47)
UPDATE products SET category_id = 47, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 37
  AND (
       name_en ILIKE '%docking station%'
    OR name_en ILIKE '%hybrid dock%'
  );

-- 14. Folio клавиатури/кейсове в Лаптопи (37) → Аксесоари (47)
UPDATE products SET category_id = 47, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 37
  AND (
       name_en ILIKE '%folio keyboard%'
    OR name_en ILIKE '%folio%'
    OR name_en ILIKE '%simpro%'
  );

-- 15. Гаранции в Лаптопи (37) → скриване
UPDATE products SET show_flag = false, status = 'NOT_AVAILABLE', updated_at = CURRENT_TIMESTAMP
WHERE category_id = 37
  AND (name_en ILIKE '%warranty%' OR name_en ILIKE '% warr%');

COMMIT;

-- Проверка след промените:
SELECT c.name_bg, c.id, COUNT(p.id) AS products
FROM products p
JOIN categories c ON p.category_id = c.id
WHERE c.id IN (9, 37, 39, 40, 42, 43, 47, 60, 66, 78, 86, 179, 181, 298, 325, 380, 402, 443, 455)
GROUP BY c.id, c.name_bg
ORDER BY c.id;
```

---

## 5. Допълнителен анализ — скрити несъответствия

> Генериран на: 2026-07-20 (дълбок анализ)
> Методология: grep по category_id + Python парсинг на name_en за всеки продукт
> Предишният анализ е бил само keyword-matching — тук са разгледани ВСИЧКИ продукти по категория.

**Резултат: открити са още ~391 несъответствия → актуализиран общ брой: ~878 продукта в грешна категория.**

---

### 5.1 Категория 298 (ASBIS „стойки") — масивно претоварена catch-all категория

Категорията съдържа **414 продукта**, от които само ~84 са реални стойки/брекети/монтажни системи.
Предишният анализ е намерил само 38 несъответствия (20 webcam + 18 headset).
**Реалният брой грешно поставени продукти в 298: ~310.**

Новоидентифицирани групи (289 нови несъответствия):

| Тип продукт | Брой | Предложена категория |
|------------|------|---------------------|
| Калъфи/капаци за телефони GSmart/GigaByte (Guru, Mika, Maya, Rio, Tuku, Saga, Rey) | 48 | Нова подкат. аксесоари за телефони |
| Стойки за монитори/TV ONKRON, KIVI Motion, EDBAK | 43 | 51 (стойки за монитори) |
| Аксесоари за прахосмукачки (AENO, Eureka, CANYON — четки, HEPA филтри, торбички) | 39 | 454 (дребни домакински уреди) |
| Power bank-ове (CANYON OnPower, Silicon Power, Dell) | 22 | 160 (power bank) |
| Безжични зарядни (CANYON WCS, WS, Prestigio ReVolt) | 20 | нова подкат. зарядни |
| iPhone 16/17 калъфи и MagSafe аксесоари | 19 | 181 или нова подкат. |
| Подложки за мишка (CORSAIR MM100–MM700, Kingston HyperX, Logitech Desk Mat) | 19 | 64 (подложки за мишки) |
| Кухненски гаджети/аксесоари за вино (BUYDEEM, Prestigio, Champagne glasses) | 16 | 454 (дребни уреди) |
| Аксесоари за видеонаблюдение — junction box-ове, UBIQUITI UACC камерни аксесоари | 11 | 231 (IP камери) |
| Beoplay слушалки (H95, H100, HX) | 10 | 264 (B&O аксесоари) или 489 |
| Зарядни за кола и стойки за телефон в кола (CANYON OnDrive, OnGrip) | 9 | 183 (аксесоари за кола) |
| TPM модули (Supermicro AOM-TPM, MSI TPM 2.0, ASROCK COM-port bracket) | 8 | 430 (сървъри/компоненти) |
| Стойки за лаптопи (AXAGON STND-LQ, STND-VB, RAZER Laptop Cooling Pad) | 6 | 43 (стойки за лаптопи) |
| Стрийминг оборудване Elgato (Facecam MK.2, Prompter, Green Screen MT) | 5 | 179 (геймърски аксесоари) |
| USB хъбове и докинг (AXAGON HMC-*, HUE-STA/STC) | 4 | 58 (USB хъбове) |
| SIMAGIC педали и аксесоари (P2000 Heel Stop, Pedal Stop Collar) | 4 | 175 или 176 (волани и аксесоари) |
| Външни кутии за HDD/SSD (AXAGON EE25-XA6C, EEM2-20GD, Orico 2599US3) | 3 | 55 (External HDD) |
| Aqara smart home (NFC Card, Adjustable Cylinder) | 2 | 500 (смарт у-ва) |
| Apple AirTag (4 Pack) | 1 | нова подкат. или 183 |

**Нови несъответствия в 298: 289**

---

### 5.2 Категория 380 (ASBIS „кабели") — UPS-и добавени към PSU

Предишният анализ е открил 237 захранвания (PSU). Допълнително:

| Тип продукт | Брой | Предложена категория |
|------------|------|---------------------|
| UPS устройства (CyberPower 650VA–2200VA, DELTA N/RT/VX, Vertiv Liebert GXT5, Ubiquiti UPS-Tower) | 29 | 72 (UPS Line-Interactive) или 73 (UPS Online) |
| Misc. некабелни (POE инжектори, mini-SAS адаптери, Supermicro CBL-SAST) | 14 | различни |

**Нови несъответствия в 380: 29 UPS** (14-те misc може да са кабели/адаптери — спорно)

Специфични UPS намерени:
- CyberPower: BU650EG, UT850EG, VP700ELCD, VP1000ELCD, BR1000ELCD, VP1600ELCD, CP1350EPFCLCD, CP1600EPFCLCD, UT1500EG, UT2200EG
- DELTA: N UPS (1kVA–3kVA), RT UPS (1kVA–6kVA), VX UPS (600VA, 1500VA)
- Vertiv: Liebert GXT5 1ph UPS 1kVA
- Ubiquiti: UniFi Redundant Power System

---

### 5.3 Категория 402 (ASBIS „охладители") — дребни допълнения

Предишният анализ е открил 44 вентилатора. Допълнително:

| Тип продукт | Брой | Предложена категория |
|------------|------|---------------------|
| CORSAIR iCUE LINK LCD Module (Black, White) — LCD дисплей за помпа, не охладител | 2 | 179 (геймърски аксесоари) |

**Нови несъответствия в 402: 2**

---

### 5.4 Категория 325 (ASBIS „аудио/колони") — нови видове грешки

Предишният анализ е открил 30 слушалки/headset. Допълнително:

| Тип продукт | Брой | Предложена категория |
|------------|------|---------------------|
| Beovision Harmony HF2 Black — TV (не аудио колона) | 1 | 139 (телевизори) |
| KIVI KidsTV аксесоари ("Auto", "Color Frames") | 2 | 139 (телевизори) или 181 |
| WorkBooth One/Two — акустични кабини за офис | 2 | нова подкат. офис мебели |
| Corsair Elgato Stream Deck Mini — стрийминг устройство | 1 | 179 (геймърски аксесоари) |
| DISPL Sensor — дисплей/сензор устройство | 1 | нова подкат. |

**Нови несъответствия в 325: 7**

---

### 5.5 Категория 443 (ASBIS „геймърска периферия") — нови видове грешки

Предишният анализ е открил 22 несъответствия. Допълнително:

| Тип продукт | Брой | Предложена категория |
|------------|------|---------------------|
| Elgato Key Light Neo, Clamp, Stand (осветление за стрийминг/студио) | 4 | 179 (геймърски аксесоари) |
| 200kg Loadcell — компонент за симрейсинг педали | 1 | 175/176 |

**Нови несъответствия в 443: 5**

---

### 5.6 Категория 86 (консумативи за принтери) — скрити хардуерни продукти

Предишният анализ е открил 12 принтери/скенери. Допълнително открити **59** нови несъответствия:

| Тип продукт | Брой | Предложена категория |
|------------|------|---------------------|
| HP лаптопи и настолни (part кодове с ET/EA суфикс: ProBook, EliteBook, ZBook, ProDesk) | 34 | 37 (лаптопи) или 32 (настолни) |
| Zebra label/barcode принтери (ZD220, ZD411, ZD421, ZT411) — **не са принтерни консумативи** | 9 | 78 (принтери) |
| Honeywell/Datalogic баркод скенери (1202G, 1470G, Genesis 7680G, Xenon 1950G, DS2208, DS2278) | 6 | 78 (принтери) |
| MS лицензи (MS Office 2021 Pro+, MS Office Mac, MS Project 2024 Pro, MS Win 10/11) | 5 | нова подкат. или скриване |
| Acer настолни компютри (Aspire TC-1780, Nitro N50-650, Predator PO5-650 x2) | 4 | 32 (настолни компютри) |
| Parrot мини дронове (Jump Sumo BK/WH/KA, Jump Race) — **играчки/дронове** | 4 | 181 (играчки) |
| Аксесоари за лаптопи/HP (carrying case, cable lock, battery charger за лаптоп) | 3 | 40 (чанти) или 47 (аксесоари) |
| Специфични хардуерни компоненти (HP ProLiant server части) | 3 | 430 (сървъри) |

**Нови несъответствия в 86: 59** *(от 12 → 71 общо грешни)*

Примерни HP лаптопи намерени в консумативи за принтери:
`818L0EA EB 840 G10 FHD 14`, `816A3EA PB450G10 I7-1355U 15`, `9Y7S7ET PB 460 G11 16FHD`,
`9G1V1ET PB460G11 U5-125U 16`, `AD2L2ET PROBOOK 4 G1I 16`, `B72WQET ZB X G1I 16 U5 235H`,
`B68YWET EB X G1A 14 AMD RAI 9`, и др.

---

### 5.7 Категории 181, 199, 406 — резултат от проверка

| Категория | Проверено | Резултат |
|-----------|-----------|----------|
| **181** (играчки/фигурки) | 356 продукта | Без несъответствия — съдържа LEGO, Paladone, фигурки, лампи-статуетки с геймърска/поп-культура тематика. Всичко е в реда. |
| **199** (батерии) | 183 продукта | Без несъответствия — само алкални, NiMH, Li-ion батерии с правилни типове (AA, AAA, 9V, 18650 и др.). Без лаптоп батерии — те са в кат. 201. |
| **406** (компютърна периферия) | 209 продукта | Без несъответствия — съдържа само мишки, клавиатури и комбо набори (Canyon, Logitech, Dell, Clevetura). |

---

### 5.8 Актуализирана обобщена таблица

| Група мисматч | Брой (стар) | Брой (реален) | Текуща кат. ID | Предложена кат. ID |
|---------------|-------------|---------------|----------------|-------------------|
| Захранвания (PSU) в Кабели | **237** | **249** | 380 | 9 |
| Разни продукти в ASBIS Стойки | **38** | **310** | 298 | различни |
| — от тях: телефонни калъфи/GSmart | — | 48 | 298 | нова/183 |
| — от тях: TV/монитор стойки ONKRON/KIVI | — | 43 | 298 | 51 |
| — от тях: вак. аксесоари (AENO, Eureka) | — | 39 | 298 | 454 |
| — от тях: power bank-ове | — | 22 | 298 | 160 |
| — от тях: безжични зарядни | — | 20 | 298 | нова |
| — от тях: webcam (от предишен анализ) | 20 | 20 | 298 | 60 |
| — от тях: iPhone/MagSafe аксесоари | — | 19 | 298 | 181/нова |
| — от тях: подложки за мишка | — | 19 | 298 | 64 |
| — от тях: headset/слушалки (от предишен) | 18 | 18 | 298 | 66 |
| — от тях: кухненски гаджети | — | 16 | 298 | 454 |
| — от тях: камерни junction box/UBIQUITI | — | 11 | 298 | 231 |
| — от тях: Beoplay слушалки | — | 10 | 298 | 264/489 |
| — от тях: автомобилни зарядни | — | 9 | 298 | 183 |
| — от тях: TPM/сървърни модули | — | 8 | 298 | 430 |
| — от тях: стойки за лаптопи | — | 6 | 298 | 43 |
| UPS в Кабели/мрежово | **0** | **29** | 380 | 72/73 |
| Консумативи за принтери — грешен хардуер | **12** | **71** | 86 | различни |
| — HP лаптопи/настолни (ET/EA кодове) | — | 34 | 86 | 37/32 |
| — Zebra принтери/скенери (нови) | — | 9 | 86 | 78 |
| — Barcode скенери Honeywell | — | 6 | 86 | 78 |
| — MS лицензи | — | 5 | 86 | скриване |
| — Acer десктопи | — | 4 | 86 | 32 |
| — Parrot дронове | — | 4 | 86 | 181 |
| Калъфи/чанти в Лаптопи | **68** | **68** | 37 | 40 |
| Вентилатори в Охладители ASBIS | **44** | **44** | 402 | 455 |
| Слушалки в Аудио/Колони | **30** | **30** | 325 | 66 |
| Нови грешки в Аудио/Колони (Beovision, WorkBooth) | **0** | **7** | 325 | 139/179 |
| Elgato стрийминг в Геймърска периферия | **20** | **25** | 443 | 179 |
| LEGO в Геймърска периферия | **2** | **2** | 443 | 181 |
| iCUE LCD Module в Охладители | **0** | **2** | 402 | 179 |
| Таблети в Лаптопи | **13** | **13** | 37 | 39 |
| Принтери/скенери в Консумативи (стари) | **12** | **12** | 86 | 78 |
| Зарядни/адаптери в Лаптопи | **11** | **11** | 37 | 42 |
| Протектори/фолио в Лаптопи | **6** | **6** | 37 | 47 |
| Folio клавиатури/докинг в Лаптопи | **4** | **4** | 37 | 47 |
| Гаранции в Лаптопи | **2** | **2** | 37 | скриване |

**Актуализиран общ брой несъответствия: ~878 продукта**
*(487 от предишния анализ + 391 новооткрити)*

---

### 5.9 Приоритетни SQL корекции (нови — допълнение към Script 24)

```sql
-- ============================================================
-- Script 24-ext: Допълнителни корекции от дълбок анализ
-- ============================================================

BEGIN;

-- A. UPS устройства в Кабели/мрежово (380) → UPS Line-Interactive (72)
UPDATE products SET category_id = 72, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 380
  AND (
       name_en ILIKE '%ups %'
    OR name_en ILIKE '% ups%'
    OR name_en ILIKE '%uninterrupt%'
    OR name_en ILIKE '%cyberpower%'
    OR name_en ILIKE 'delta%ups%'
    OR name_en ILIKE '%liebert%'
    OR name_en ILIKE '%vertiv%'
    OR name_en ILIKE '%redundant power system%'
  );

-- B. Подложки за мишка в Стойки ASBIS (298) → Подложки за мишки (64)
UPDATE products SET category_id = 64, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 298
  AND (
       name_en ILIKE '%mouse pad%'
    OR name_en ILIKE '%mousepad%'
    OR name_en ILIKE '%desk mat%'
    OR name_en ILIKE '%gaming pad%'
    OR name_en ILIKE '% MM1%'
    OR name_en ILIKE '% MM2%'
    OR name_en ILIKE '% MM3%'
    OR name_en ILIKE '% MM5%'
    OR name_en ILIKE '% MM7%'
  );

-- C. Power bank в Стойки ASBIS (298) → Power bank (160)
UPDATE products SET category_id = 160, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 298
  AND (
       name_en ILIKE '%power bank%'
    OR name_en ILIKE '%powerbank%'
    OR name_en ILIKE '%onpower%'
    OR name_en ILIKE '%power companion%'
  );

-- D. HP лаптопи в Консумативи за принтери (86) → Лаптопи (37)
-- HP part numbers ending in ET = HP laptops/desktops
UPDATE products SET category_id = 37, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 86
  AND (
       name_en ~* '^[A-Z0-9]{5,10}ET '
    OR name_en ~* '^[A-Z0-9]{5,10}EA '
    OR name_en ILIKE 'LENOVO V15%'
    OR name_en ILIKE 'ACER PC%'
  );

-- E. Parrot дронове в Консумативи (86) → Играчки (181)
UPDATE products SET category_id = 181, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 86
  AND name_en ILIKE 'PARROT%';

-- F. MS лицензи в Консумативи (86) → скриване (или нова категория)
UPDATE products SET show_flag = false, status = 'NOT_AVAILABLE', updated_at = CURRENT_TIMESTAMP
WHERE category_id = 86
  AND (
       name_en ILIKE 'MS OFFICE%'
    OR name_en ILIKE 'MS WIN%'
    OR name_en ILIKE 'MS PROJECT%'
  );

-- G. Acer/HP настолни в Консумативи (86) → Настолни компютри (32)
UPDATE products SET category_id = 32, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 86
  AND (
       name_en ILIKE 'ACER PC%'
    OR name_en ILIKE '%PRODESK%'
    OR name_en ILIKE '%SFF%'
  );

-- H. Beoplay слушалки в Стойки ASBIS (298) → B&O аксесоари (264)
UPDATE products SET category_id = 264, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 298
  AND (
       name_en ILIKE 'Beoplay%'
    OR name_en ILIKE 'Beoremote%'
  );

-- I. ONKRON/KIVI TV стойки в ASBIS Стойки (298) → Стойки за монитори (51)
UPDATE products SET category_id = 51, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 298
  AND (
       name_en ILIKE 'ONKRON%'
    OR name_en ILIKE 'KIVI Motion%'
    OR name_en ILIKE 'EDBAK%'
    OR name_en ILIKE 'Wall mount KIVI%'
  );

-- J. iCUE LINK LCD Module в Охладители (402) → Геймърски аксесоари (179)
UPDATE products SET category_id = 179, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 402
  AND name_en ILIKE '%iCUE LINK LCD Module%';

-- K. Vacuum cleaner accessories в Стойки (298) → Дребни домакински уреди (454)
UPDATE products SET category_id = 454, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 298
  AND (
       name_en ILIKE '%vacuum%'
    OR name_en ILIKE '%hepa filter%'
    OR name_en ILIKE '%dust bag%'
    OR name_en ILIKE '%side brush%'
    OR name_en ILIKE '%main brush%'
    OR name_en ILIKE '%mop pad%'
    OR name_en ILIKE '%roller brush%'
    OR name_en ILIKE 'AENO%filter%'
    OR name_en ILIKE 'AENO%brush%'
    OR name_en ILIKE 'Eureka%'
    OR name_en ILIKE 'ARC00%'
  );

-- L. Beovision TV в Аудио (325) → Телевизори (139)
UPDATE products SET category_id = 139, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 325
  AND name_en ILIKE 'Beovision%';

-- M. Elgato Stream Deck в Аудио (325) → Геймърски аксесоари (179)
UPDATE products SET category_id = 179, updated_at = CURRENT_TIMESTAMP
WHERE category_id = 325
  AND name_en ILIKE '%Stream Deck%';

COMMIT;
```
