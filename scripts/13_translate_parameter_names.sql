-- =============================================================
-- Script: 13_translate_parameter_names.sql
-- Purpose: Translate parameter name_bg from English to Bulgarian
-- for parameters where name_bg = name_en (API-imported English names).
-- Safe to re-run: uses WHERE id = N, only updates specific IDs.
-- Future syncs do NOT overwrite name_bg (sync uses name_bg only for
-- new parameters; existing name_bg values are preserved).
-- =============================================================

-- ===== MOST (91 parameters) =====
UPDATE parameters SET name_bg = 'Препоръчана системна мощност' WHERE id = 5836; -- Recommended system power
UPDATE parameters SET name_bg = 'Захранващ адаптер' WHERE id = 5837; -- Power adapter
UPDATE parameters SET name_bg = 'Скорост на данните' WHERE id = 5838; -- Data rate
UPDATE parameters SET name_bg = 'Информация за вентилатора' WHERE id = 5839; -- Fan information
UPDATE parameters SET name_bg = 'Капацитет на касетата' WHERE id = 5840; -- Cartridge yield
UPDATE parameters SET name_bg = 'Изход' WHERE id = 5841; -- Output
UPDATE parameters SET name_bg = 'LED индикатори' WHERE id = 5842; -- LED indicators
UPDATE parameters SET name_bg = 'Писалка' WHERE id = 5843; -- Pen
UPDATE parameters SET name_bg = 'Размер на вентилатора [мм]' WHERE id = 5844; -- Fan size [mm]
UPDATE parameters SET name_bg = 'Влажност' WHERE id = 5846; -- Humidity
UPDATE parameters SET name_bg = 'Гаранция' WHERE id = 5847; -- Warranty
UPDATE parameters SET name_bg = 'Вход AC' WHERE id = 5848; -- AC Input
UPDATE parameters SET name_bg = 'Стандарти' WHERE id = 5849; -- Standarts
UPDATE parameters SET name_bg = 'Физически характеристики' WHERE id = 5850; -- Physical characteristics
UPDATE parameters SET name_bg = 'Тип касета' WHERE id = 5851; -- Cartridge type
UPDATE parameters SET name_bg = 'Мрежови кабели' WHERE id = 5852; -- Network cables
UPDATE parameters SET name_bg = 'Резолюция на екрана' WHERE id = 5853; -- Screen resolution
UPDATE parameters SET name_bg = 'Интерфейс на паметта' WHERE id = 5854; -- Memory interface
UPDATE parameters SET name_bg = 'Вход' WHERE id = 5856; -- Input
UPDATE parameters SET name_bg = 'Контраст' WHERE id = 5857; -- Contrast
UPDATE parameters SET name_bg = 'Охлаждане' WHERE id = 5858; -- Cooling
UPDATE parameters SET name_bg = 'Подсветка' WHERE id = 5859; -- Backlighting
UPDATE parameters SET name_bg = 'Тип екран' WHERE id = 5860; -- Screen type
UPDATE parameters SET name_bg = 'CUDA ядра' WHERE id = 5861; -- CUDA cores
UPDATE parameters SET name_bg = 'Описание на захранването' WHERE id = 5862; -- Power description
UPDATE parameters SET name_bg = 'Информация за кабела' WHERE id = 5863; -- Cable information
UPDATE parameters SET name_bg = 'Системни изисквания' WHERE id = 5865; -- System requirements
UPDATE parameters SET name_bg = 'Температура' WHERE id = 5866; -- Temperature
UPDATE parameters SET name_bg = 'Скорост на сканиране' WHERE id = 5867; -- Scan speed
UPDATE parameters SET name_bg = 'Камера' WHERE id = 5868; -- Camera
UPDATE parameters SET name_bg = 'Графичен процесор' WHERE id = 5869; -- Graphics engine
UPDATE parameters SET name_bg = 'Слотове за устройства' WHERE id = 5870; -- Slots for devices
UPDATE parameters SET name_bg = 'Безопасност и екология' WHERE id = 5871; -- Safety and Environmental
UPDATE parameters SET name_bg = 'Дължина на кабела (мм)' WHERE id = 5872; -- Cable length (mm)
UPDATE parameters SET name_bg = 'Честотна лента на мрежата' WHERE id = 5873; -- Network band
UPDATE parameters SET name_bg = 'Поддръжка на VPN' WHERE id = 5875; -- VPN support
UPDATE parameters SET name_bg = 'Естествена резолюция' WHERE id = 5876; -- Native resolution
UPDATE parameters SET name_bg = 'Време за реакция' WHERE id = 5877; -- Responce time
UPDATE parameters SET name_bg = 'Подсветка на клавиатурата' WHERE id = 5878; -- Keyboard backlit
UPDATE parameters SET name_bg = 'Интерфейси/Портове' WHERE id = 5879; -- Interfaces/Ports
UPDATE parameters SET name_bg = 'Живот на светлинния източник [часове]' WHERE id = 5880; -- Light source life [Hours]
UPDATE parameters SET name_bg = 'Механична конструкция' WHERE id = 5881; -- Mechanical design
UPDATE parameters SET name_bg = 'Графика' WHERE id = 5882; -- Graphics
UPDATE parameters SET name_bg = 'Спецификации' WHERE id = 5883; -- Specifications
UPDATE parameters SET name_bg = 'Цветна гама' WHERE id = 5884; -- Color gamut
UPDATE parameters SET name_bg = 'Изход DC' WHERE id = 5885; -- DC Output
UPDATE parameters SET name_bg = 'Комуникация' WHERE id = 5886; -- Communication
UPDATE parameters SET name_bg = 'Кабел' WHERE id = 5887; -- Cable
UPDATE parameters SET name_bg = 'Предна камера' WHERE id = 5888; -- Camera front
UPDATE parameters SET name_bg = 'URL на производителя' WHERE id = 5889; -- Vendor_url
UPDATE parameters SET name_bg = 'Изображение' WHERE id = 5890; -- Image
UPDATE parameters SET name_bg = 'Тест' WHERE id = 5891; -- Test
UPDATE parameters SET name_bg = 'Работен цикъл' WHERE id = 5892; -- Duty cycle
UPDATE parameters SET name_bg = 'Лагер' WHERE id = 5893; -- Bearing
UPDATE parameters SET name_bg = 'Честота на ядрото' WHERE id = 5894; -- Engine/Core clock
UPDATE parameters SET name_bg = 'Режим на предаване' WHERE id = 5895; -- Transmission mode
UPDATE parameters SET name_bg = 'Слот за карта' WHERE id = 5896; -- Card slot
UPDATE parameters SET name_bg = 'Тип PFC' WHERE id = 5897; -- PFC type
UPDATE parameters SET name_bg = 'Формат хартия' WHERE id = 5898; -- Paper size
UPDATE parameters SET name_bg = 'Материали' WHERE id = 5899; -- Materials
UPDATE parameters SET name_bg = 'Резолюция на печат' WHERE id = 5900; -- Print resolution
UPDATE parameters SET name_bg = 'Размер на паметта' WHERE id = 5901; -- Memmory size
UPDATE parameters SET name_bg = 'Скорост на печат' WHERE id = 5902; -- Print speed
UPDATE parameters SET name_bg = 'Съотношение на контраста' WHERE id = 5903; -- Contrast ratio
UPDATE parameters SET name_bg = 'Техническа информация' WHERE id = 5904; -- Technical information
UPDATE parameters SET name_bg = 'Задна камера' WHERE id = 5905; -- Camera rear
UPDATE parameters SET name_bg = 'Мощност [Вати]' WHERE id = 5906; -- Power [Watts]
UPDATE parameters SET name_bg = 'Спецификации на диска' WHERE id = 5907; -- Disk specifications
UPDATE parameters SET name_bg = 'Честотна лента на паметта' WHERE id = 5908; -- Memory bandwidth
UPDATE parameters SET name_bg = 'Задна камера' WHERE id = 5909; -- Rear camera
UPDATE parameters SET name_bg = 'ROM' WHERE id = 5910; -- ROM
UPDATE parameters SET name_bg = 'Резолюция на скенера' WHERE id = 5911; -- Scanner resolution
UPDATE parameters SET name_bg = 'Цвят на корпуса' WHERE id = 5912; -- Body color
UPDATE parameters SET name_bg = 'Клавиатура' WHERE id = 5914; -- Keyboard
UPDATE parameters SET name_bg = 'Резолюция на движение' WHERE id = 5915; -- Movement resolution
UPDATE parameters SET name_bg = 'Аларми' WHERE id = 5916; -- Alarms
UPDATE parameters SET name_bg = 'Лампа' WHERE id = 5917; -- Lamp
UPDATE parameters SET name_bg = 'Сензорен екран' WHERE id = 5918; -- Touch screen
UPDATE parameters SET name_bg = 'Поддръжка на ОС' WHERE id = 5919; -- OS support
UPDATE parameters SET name_bg = 'Размер на паметта' WHERE id = 5920; -- Memory size
UPDATE parameters SET name_bg = 'Скорост на копиране' WHERE id = 5921; -- Copy speed
UPDATE parameters SET name_bg = 'Номер на части / Продуктов код' WHERE id = 5922; -- Part number / Product code
UPDATE parameters SET name_bg = 'Условия на работа' WHERE id = 5924; -- Environment
UPDATE parameters SET name_bg = 'Производителност' WHERE id = 5925; -- Performance
UPDATE parameters SET name_bg = 'Максимален брой дисплеи' WHERE id = 5926; -- Maximum displays
UPDATE parameters SET name_bg = 'Мултимедийни клавиши' WHERE id = 5927; -- Media keys
UPDATE parameters SET name_bg = 'Управление' WHERE id = 5928; -- Management
UPDATE parameters SET name_bg = 'Честота на паметта' WHERE id = 5929; -- Memory clock
UPDATE parameters SET name_bg = 'Честота на опресняване' WHERE id = 5930; -- Refresh rate
UPDATE parameters SET name_bg = 'Проекционна леща' WHERE id = 5931; -- Projection lens
UPDATE parameters SET name_bg = 'Описание от производителя' WHERE id = 5932; -- Deskription of the manufacturer

-- ===== TEKRA (11 parameters) =====
UPDATE parameters SET name_bg = 'WDR' WHERE id = 1816; -- WDR
UPDATE parameters SET name_bg = 'Ethernet портове' WHERE id = 1817; -- Ethernet Portove
UPDATE parameters SET name_bg = 'PoE' WHERE id = 1818; -- Poe
UPDATE parameters SET name_bg = 'Безжични скорости' WHERE id = 1821; -- Bezzhichni Skorosti
UPDATE parameters SET name_bg = 'Вентилатор' WHERE id = 1824; -- Ventilator
UPDATE parameters SET name_bg = 'SFP портове' WHERE id = 1832; -- Sfp Portove
UPDATE parameters SET name_bg = 'Портове' WHERE id = 1834; -- Portove
UPDATE parameters SET name_bg = 'Тип захранване' WHERE id = 1841; -- Tip Zahranvane
UPDATE parameters SET name_bg = 'Wi-Fi' WHERE id = 1844; -- Wi Fi
UPDATE parameters SET name_bg = 'LPR' WHERE id = 1845; -- Lpr
UPDATE parameters SET name_bg = 'PoE бюджет' WHERE id = 1851; -- Poe Byudzhet

-- ===== VALI (10 parameters) =====
UPDATE parameters SET name_bg = 'Безжична връзка' WHERE id = 143; -- Wireless
UPDATE parameters SET name_bg = 'Двуслоен' WHERE id = 559; -- Dual Layer
UPDATE parameters SET name_bg = 'Архитектура на диска' WHERE id = 1077; -- Storage drive architecture
UPDATE parameters SET name_bg = 'USB 2.0 портове' WHERE id = 1082; -- USB 2.0 ports
UPDATE parameters SET name_bg = 'USB 3.0 портове' WHERE id = 1083; -- USB 3.0 ports
UPDATE parameters SET name_bg = 'Серийна връзка' WHERE id = 1084; -- Serial connection
UPDATE parameters SET name_bg = 'Серия' WHERE id = 1214; -- Line-up
UPDATE parameters SET name_bg = 'USB 3.1 портове' WHERE id = 1234; -- USB 3.1 ports
UPDATE parameters SET name_bg = 'Мощност' WHERE id = 1704; -- Power
UPDATE parameters SET name_bg = 'Безжична връзка' WHERE id = 3416; -- Wireless

-- ===== ASBIS (772 parameters) =====
UPDATE parameters SET name_bg = 'Нето тегло на картонената опаковка' WHERE id = 4042; -- Retail Packaging Net Weight Carton
UPDATE parameters SET name_bg = 'Нето тегло на пластмасовата опаковка' WHERE id = 4047; -- Retail Packaging Net Weight Plastic
UPDATE parameters SET name_bg = 'Flash памет' WHERE id = 4069; -- Flash Memory
UPDATE parameters SET name_bg = 'Инсталирана RAM' WHERE id = 4075; -- Installed RAM
UPDATE parameters SET name_bg = 'Тип мрежово устройство' WHERE id = 4080; -- Network Device Type
UPDATE parameters SET name_bg = 'Поддържани протоколи за връзка (нови)' WHERE id = 4081; -- Data Link Protocol Supports (New)
UPDATE parameters SET name_bg = 'Поддръжка на гласов асистент' WHERE id = 4082; -- Voice Assistant Support
UPDATE parameters SET name_bg = 'LED индикатори за статус' WHERE id = 4084; -- Status LED Indicators
UPDATE parameters SET name_bg = 'Поддържани мрежови стандарти' WHERE id = 4085; -- Supported Networking Compliant Standards
UPDATE parameters SET name_bg = 'Характеристики на рутера' WHERE id = 4086; -- Router Features
UPDATE parameters SET name_bg = 'Протоколи (OSI модел)' WHERE id = 4089; -- Protocols (OSI Model)
UPDATE parameters SET name_bg = 'Буферна памет' WHERE id = 4094; -- Buffer Memory
UPDATE parameters SET name_bg = 'Захранващ вход' WHERE id = 4101; -- Power Input
UPDATE parameters SET name_bg = 'Приложен слой' WHERE id = 4103; -- Application Layer
UPDATE parameters SET name_bg = 'Честотен диапазон' WHERE id = 4105; -- Frequency Range
UPDATE parameters SET name_bg = 'Характеристики на умния сензор' WHERE id = 4106; -- Smart Sensor Features
UPDATE parameters SET name_bg = 'Безжични стандарти' WHERE id = 4107; -- Wireless Standards
UPDATE parameters SET name_bg = 'LED индикатор за известия' WHERE id = 4111; -- Notification LED
UPDATE parameters SET name_bg = 'Описание на сензора' WHERE id = 4112; -- Sensor Description
UPDATE parameters SET name_bg = 'Разстояние на безжичната мрежа' WHERE id = 4113; -- Wireless Networking Distance
UPDATE parameters SET name_bg = 'Модел батерия' WHERE id = 4114; -- Battery Model
UPDATE parameters SET name_bg = 'Захранване по Ethernet (PoE)' WHERE id = 4117; -- Power over Ethernet
UPDATE parameters SET name_bg = 'Напрежение' WHERE id = 4118; -- Voltage
UPDATE parameters SET name_bg = 'Външен материал' WHERE id = 4119; -- External Material
UPDATE parameters SET name_bg = 'Функции и протоколи на защитната стена' WHERE id = 4120; -- Firewall Functions and Protocols
UPDATE parameters SET name_bg = 'Протокол AAA (удостоверяване, авторизация, отчитане)' WHERE id = 4121; -- Authentication, Authorization, Accounting (AAA Protocol)
UPDATE parameters SET name_bg = 'Серия' WHERE id = 4124; -- Family
UPDATE parameters SET name_bg = 'Среднотонов говорител' WHERE id = 4127; -- Midrange
UPDATE parameters SET name_bg = 'Високочестотен говорител' WHERE id = 4128; -- Tweeter
UPDATE parameters SET name_bg = 'Нискотонов говорител' WHERE id = 4131; -- Woofer
UPDATE parameters SET name_bg = 'Макс. аудио скорост на поток' WHERE id = 4134; -- Max. Audio Bit Rate
UPDATE parameters SET name_bg = 'Максимален капацитет на паметта' WHERE id = 4135; -- Maximum Memory Capacity
UPDATE parameters SET name_bg = 'Под-родителски продукт' WHERE id = 4136; -- SubSubParent Product
UPDATE parameters SET name_bg = 'Родителски продукт' WHERE id = 4137; -- SubParent Product
UPDATE parameters SET name_bg = 'Активно шумопотискане' WHERE id = 4147; -- Active Noise Cancellation
UPDATE parameters SET name_bg = 'Аудио линеен вход' WHERE id = 4150; -- Audio Line-In
UPDATE parameters SET name_bg = 'Пълнообхватен говорител' WHERE id = 4151; -- Full Range
UPDATE parameters SET name_bg = 'Брой нишки' WHERE id = 4155; -- Threads Quantity
UPDATE parameters SET name_bg = 'Размер на CPU кеша' WHERE id = 4162; -- CPU Cache Size
UPDATE parameters SET name_bg = 'Turbo тактова честота' WHERE id = 4163; -- Turbo Clock Rate
UPDATE parameters SET name_bg = 'Инсталиран гласов асистент' WHERE id = 4171; -- Installed Voice Assistant
UPDATE parameters SET name_bg = 'Капацитет на резервоара за вода' WHERE id = 4177; -- Water tank capacity
UPDATE parameters SET name_bg = 'Включени аксесоари за прахосмукачката' WHERE id = 4178; -- Vacuum Cleaners Included Accessories
UPDATE parameters SET name_bg = 'Засмукване (kPa)' WHERE id = 4179; -- Suction (kPa)
UPDATE parameters SET name_bg = 'Тип прахосмукачка' WHERE id = 4180; -- Vacuum Cleaner Type
UPDATE parameters SET name_bg = 'Капацитет за прах (л)' WHERE id = 4181; -- Dust Capacity (l)
UPDATE parameters SET name_bg = 'Тип уред' WHERE id = 4182; -- Appliance Type
UPDATE parameters SET name_bg = 'Филтър на прахосмукачката' WHERE id = 4183; -- Vacuum Cleaner Filter
UPDATE parameters SET name_bg = 'Ниво на шума Lc IEC (dB)' WHERE id = 4186; -- Noise Level Lc IEC (dB)
UPDATE parameters SET name_bg = 'Изходен ток' WHERE id = 4187; -- Output Current
UPDATE parameters SET name_bg = 'Прахосмукачка' WHERE id = 4190; -- Vacuum Cleaner
UPDATE parameters SET name_bg = 'Дължина на кабела (м)' WHERE id = 4192; -- Cord Length (m)
UPDATE parameters SET name_bg = 'Захранваща станция' WHERE id = 4193; -- Power Station
UPDATE parameters SET name_bg = 'Съвместими аксесоари' WHERE id = 4195; -- Compatible Accessories
UPDATE parameters SET name_bg = 'Тип решение за видеоконферентна връзка' WHERE id = 4201; -- Type of Video Conference Solution
UPDATE parameters SET name_bg = 'Дължина (мм)' WHERE id = 4202; -- Length (mm)
UPDATE parameters SET name_bg = 'Съдържание на опаковката' WHERE id = 4205; -- In the Box
UPDATE parameters SET name_bg = 'Диаметър (мм)' WHERE id = 4206; -- Diameter (mm)
UPDATE parameters SET name_bg = 'Презареждане на батерията' WHERE id = 4209; -- Battery Recharge
UPDATE parameters SET name_bg = 'Аудио с активно шумопотискане' WHERE id = 4210; -- Audio playback with Active Noise Cancellation
UPDATE parameters SET name_bg = 'Аудио възпроизвеждане' WHERE id = 4212; -- Audio playback
UPDATE parameters SET name_bg = 'Разговори' WHERE id = 4215; -- Talk
UPDATE parameters SET name_bg = 'Мощност на основния мотор (W)' WHERE id = 4217; -- Main Motor Power (W)
UPDATE parameters SET name_bg = 'Бележки за батерията' WHERE id = 4218; -- Battery Notes
UPDATE parameters SET name_bg = 'Вид уред' WHERE id = 4219; -- Kind of Appliance
UPDATE parameters SET name_bg = 'Регулируема пружинна сила' WHERE id = 4227; -- Adjustable Spring Force Level
UPDATE parameters SET name_bg = 'Брой в опаковка' WHERE id = 4236; -- Pieces in pack
UPDATE parameters SET name_bg = 'Дължина на опаковката (мм)' WHERE id = 4237; -- Pack Length (mm)
UPDATE parameters SET name_bg = 'Ширина на опаковката (мм)' WHERE id = 4238; -- Pack Width (mm)
UPDATE parameters SET name_bg = 'Бруто тегло на опаковката (кг)' WHERE id = 4239; -- Pack Weight Brutto (kg)
UPDATE parameters SET name_bg = 'Гаранционен срок (месеци)' WHERE id = 4241; -- Warranty Term (month)
UPDATE parameters SET name_bg = 'Бруто тегло на кутията (кг)' WHERE id = 4242; -- Box Weight Brutto (kg)
UPDATE parameters SET name_bg = 'Височина на опаковката (мм)' WHERE id = 4243; -- Pack Height (mm)
UPDATE parameters SET name_bg = 'Резервоар за чиста вода' WHERE id = 4246; -- Clean Water Container
UPDATE parameters SET name_bg = 'Тип тръба' WHERE id = 4248; -- Tube Type
UPDATE parameters SET name_bg = 'Продукти подлежащи на гаранционно връщане' WHERE id = 4251; -- Warranty Products Returnable
UPDATE parameters SET name_bg = 'Критерии за валидиране на гаранцията' WHERE id = 4252; -- Warranty Validation Criteria
UPDATE parameters SET name_bg = 'Мултимедиен тип' WHERE id = 4259; -- Multimedia Type
UPDATE parameters SET name_bg = 'Опаковки на палет (море) (бр)' WHERE id = 4261; -- Quantity of the packs per pallet (by sea)(pcs)
UPDATE parameters SET name_bg = 'Включена батерия' WHERE id = 4262; -- Battery Included
UPDATE parameters SET name_bg = 'Опаковки на палет (въздух) (бр)' WHERE id = 4263; -- Quantity of the packs per pallet (by air)(pcs)
UPDATE parameters SET name_bg = 'Брой видео входни канали' WHERE id = 4265; -- Video Input Channels Quantity
UPDATE parameters SET name_bg = 'Слот за разширение' WHERE id = 4270; -- Expansion Slot
UPDATE parameters SET name_bg = 'Тип система за съхранение' WHERE id = 4272; -- Storage System Type
UPDATE parameters SET name_bg = 'Технология на подсветката' WHERE id = 4304; -- Backlight Technology
UPDATE parameters SET name_bg = 'Температура на запояване' WHERE id = 4305; -- Sealing Temperature
UPDATE parameters SET name_bg = 'Видове продукти' WHERE id = 4306; -- Types of Product
UPDATE parameters SET name_bg = 'Структура' WHERE id = 4307; -- Structure
UPDATE parameters SET name_bg = 'Дебелина на торбата' WHERE id = 4308; -- Bag Thickness
UPDATE parameters SET name_bg = 'Вътрешен материал' WHERE id = 4309; -- Internal Material
UPDATE parameters SET name_bg = 'Номер на артикул' WHERE id = 4311; -- Item Number
UPDATE parameters SET name_bg = 'Диагонален ъгъл на видимост' WHERE id = 4313; -- Diagonal Viewing Angle
UPDATE parameters SET name_bg = 'Видове почистване' WHERE id = 4323; -- Types of cleaning
UPDATE parameters SET name_bg = 'Видове режими' WHERE id = 4326; -- Mode types
UPDATE parameters SET name_bg = 'Автоматично изключване' WHERE id = 4327; -- Auto turn off
UPDATE parameters SET name_bg = 'Индикация на батерията' WHERE id = 4328; -- Battery indication
UPDATE parameters SET name_bg = 'Сменяем резервоар за вода' WHERE id = 4329; -- Removable water tank
UPDATE parameters SET name_bg = 'Максимално непрекъснато ползване' WHERE id = 4330; -- Maximum continuous time use
UPDATE parameters SET name_bg = 'Индикация на режима' WHERE id = 4331; -- Mode indication
UPDATE parameters SET name_bg = 'Налягане на иригатора, bar' WHERE id = 4332; -- Irrigator pressure, bar
UPDATE parameters SET name_bg = 'Умни програми за почистване' WHERE id = 4334; -- Smart cleaning programs
UPDATE parameters SET name_bg = 'Въртящ се струй' WHERE id = 4335; -- Rotation jet
UPDATE parameters SET name_bg = 'Работно вр. след пълно зареждане с вода' WHERE id = 4337; -- Operating time on a full water refueling
UPDATE parameters SET name_bg = 'Честота на пулсиране на водата' WHERE id = 4338; -- Water Pulsation Frequency
UPDATE parameters SET name_bg = 'Резервоар за вода, мл' WHERE id = 4339; -- Water tank, ml
UPDATE parameters SET name_bg = 'Работно вр. при едно зареждане' WHERE id = 4340; -- Operating time on a single charge
UPDATE parameters SET name_bg = 'Индикация на зареждане' WHERE id = 4341; -- Charging indication
UPDATE parameters SET name_bg = 'Налягане на иригатора, PSI' WHERE id = 4342; -- Irrigator pressure, PSI
UPDATE parameters SET name_bg = 'Тип стол' WHERE id = 4345; -- Chair Type
UPDATE parameters SET name_bg = 'Огнеупорен ABS' WHERE id = 4356; -- Anti-fire ABS
UPDATE parameters SET name_bg = 'Обхват за улавяне на глас' WHERE id = 4360; -- Voice PickUp Range
UPDATE parameters SET name_bg = 'Вертикален ъгъл на видимост' WHERE id = 4361; -- Vertical Viewing Angle
UPDATE parameters SET name_bg = 'Брой аудио изходни канали' WHERE id = 4362; -- Audio Output Channels Quantity
UPDATE parameters SET name_bg = 'Брой аудио входни канали' WHERE id = 4363; -- Audio Input Channels Quantity
UPDATE parameters SET name_bg = 'Тип връзка' WHERE id = 4364; -- Connection type
UPDATE parameters SET name_bg = 'Период на удължаване' WHERE id = 4368; -- Extension Period
UPDATE parameters SET name_bg = 'Сензор' WHERE id = 4370; -- Sensor
UPDATE parameters SET name_bg = 'Бруто тегло' WHERE id = 4378; -- Gross Weight
UPDATE parameters SET name_bg = 'Вход/Изход за аларма' WHERE id = 4383; -- Alarm Input/Output
UPDATE parameters SET name_bg = 'Минимална възраст за употреба' WHERE id = 4385; -- Minimum Age to Start Using
UPDATE parameters SET name_bg = 'Категория аксесоари' WHERE id = 4389; -- Accessories Category
UPDATE parameters SET name_bg = 'Поддържа отдалечени връзки' WHERE id = 4391; -- Supports Remote Connections
UPDATE parameters SET name_bg = 'Тип монтаж' WHERE id = 4392; -- Mount Type
UPDATE parameters SET name_bg = 'Телефонна линия' WHERE id = 4399; -- Phone Line
UPDATE parameters SET name_bg = 'Тип кухненски уред' WHERE id = 4402; -- Kitchen Appliance Type
UPDATE parameters SET name_bg = 'Купа' WHERE id = 4403; -- Bowl
UPDATE parameters SET name_bg = 'Минимално времетраене на готвене' WHERE id = 4404; -- Minimum Cooking Time
UPDATE parameters SET name_bg = 'Брой програми за готвене' WHERE id = 4405; -- Cooking Programs Quantity
UPDATE parameters SET name_bg = 'Характеристики на нагревателя' WHERE id = 4406; -- Heater Features
UPDATE parameters SET name_bg = 'Максимална скорост' WHERE id = 4407; -- Maximum Speed
UPDATE parameters SET name_bg = 'Купа за блендер' WHERE id = 4408; -- Blender Bowl
UPDATE parameters SET name_bg = 'Защита' WHERE id = 4409; -- Protection
UPDATE parameters SET name_bg = 'Минимално ниво на шума' WHERE id = 4411; -- Minimum Noise Level
UPDATE parameters SET name_bg = 'Консумация в режим на изчакване' WHERE id = 4412; -- Power Consumption Stand By
UPDATE parameters SET name_bg = 'Максимално ниво на шума' WHERE id = 4413; -- Maximum Noise Level
UPDATE parameters SET name_bg = 'Брой програми' WHERE id = 4415; -- Programs Quantity
UPDATE parameters SET name_bg = 'Автоматични програми' WHERE id = 4416; -- Automatic Programs
UPDATE parameters SET name_bg = 'Минимална скорост' WHERE id = 4417; -- Minimum Speed
UPDATE parameters SET name_bg = 'Комплектът включва' WHERE id = 4418; -- Set Includes
UPDATE parameters SET name_bg = 'Материал на ножа' WHERE id = 4419; -- Knife Material
UPDATE parameters SET name_bg = 'Регулируем ход на клавиш' WHERE id = 4420; -- Adjustable Travel
UPDATE parameters SET name_bg = 'Езици в ръководството' WHERE id = 4422; -- Languages in paper Manual or Localization
UPDATE parameters SET name_bg = 'Страни за продажба' WHERE id = 4424; -- Countries where allowed to Sell
UPDATE parameters SET name_bg = 'Кутии на палет (море) (бр)' WHERE id = 4426; -- Quantity of the boxes per pallet (by sea)(pcs)
UPDATE parameters SET name_bg = 'Кутии на палет (въздух) (бр)' WHERE id = 4428; -- Quantity of the boxes per pallet (by air)(pcs)
UPDATE parameters SET name_bg = 'Включена документация' WHERE id = 4430; -- Included Documentation
UPDATE parameters SET name_bg = 'Брой конектори' WHERE id = 4435; -- Connectors Quantity
UPDATE parameters SET name_bg = 'Вграден кабел' WHERE id = 4436; -- Built-in Cable
UPDATE parameters SET name_bg = 'Специални характеристики' WHERE id = 4437; -- Special Features
UPDATE parameters SET name_bg = 'Дисплей на захранването' WHERE id = 4438; -- Power Display
UPDATE parameters SET name_bg = 'Фокус' WHERE id = 4446; -- Focus
UPDATE parameters SET name_bg = 'Размери на цветната кутия (мм)' WHERE id = 4447; -- Color box dimensions (mm)
UPDATE parameters SET name_bg = 'Бруто тегло на единична опаковка (кг)' WHERE id = 4448; -- Single pack gross weight (kg)
UPDATE parameters SET name_bg = 'Максимална резолюция на снимки' WHERE id = 4449; -- Maximum Photo Resolution
UPDATE parameters SET name_bg = 'Опаковки на палет' WHERE id = 4450; -- Packs per Pallet
UPDATE parameters SET name_bg = 'Тип безжично зарядно' WHERE id = 4451; -- Wireless Charger Type
UPDATE parameters SET name_bg = 'Брой свързани устройства' WHERE id = 4452; -- Quantity of Connected Devices
UPDATE parameters SET name_bg = 'Продуктова серия' WHERE id = 4453; -- Product Series
UPDATE parameters SET name_bg = 'Брой цикли на рязане на фолио' WHERE id = 4456; -- Number of cycles of foil cuts
UPDATE parameters SET name_bg = 'Подходящ за гърловини с външен диаметър' WHERE id = 4457; -- Suitable for bottlenecks with outer diameters
UPDATE parameters SET name_bg = 'Максимален диагонал' WHERE id = 4460; -- Maximum Diagonal Length
UPDATE parameters SET name_bg = 'Максимално тегло на екрана' WHERE id = 4461; -- Maximum Screen Weight
UPDATE parameters SET name_bg = 'Минимален диагонал' WHERE id = 4462; -- Minimum Diagonal Length
UPDATE parameters SET name_bg = 'Характеристики на дисковия корпус' WHERE id = 4471; -- Drive Cabinet Features
UPDATE parameters SET name_bg = 'Ергономични характеристики' WHERE id = 4477; -- Ergonomic Features
UPDATE parameters SET name_bg = 'Обикновен цвят' WHERE id = 4478; -- Simple Color
UPDATE parameters SET name_bg = 'Тип държач' WHERE id = 4485; -- Holder Type
UPDATE parameters SET name_bg = 'Безжично зареждане' WHERE id = 4498; -- Wireless Chargering
UPDATE parameters SET name_bg = 'Характеристики на стойка за кола' WHERE id = 4499; -- Car Cradle Features
UPDATE parameters SET name_bg = 'Обща височина' WHERE id = 4504; -- Overall Height
UPDATE parameters SET name_bg = 'Тип настройка' WHERE id = 4505; -- Adjustment Type
UPDATE parameters SET name_bg = 'Съкратено описание' WHERE id = 4506; -- Abbreviated Description
UPDATE parameters SET name_bg = 'Тип хардуер за монтаж' WHERE id = 4507; -- Mounting Hardware Type
UPDATE parameters SET name_bg = 'Тип опаковка' WHERE id = 4508; -- Package Type
UPDATE parameters SET name_bg = 'Родителски продукти' WHERE id = 4509; -- Parent Products
UPDATE parameters SET name_bg = 'Опаковки в кутия' WHERE id = 4510; -- Packs in Box
UPDATE parameters SET name_bg = 'Външен цвят' WHERE id = 4511; -- External Color
UPDATE parameters SET name_bg = 'Нето тегло на опаковката (кг)' WHERE id = 4512; -- Pack Weight Netto (kg)
UPDATE parameters SET name_bg = 'Съвместими устройства' WHERE id = 4513; -- Compliant Devices
UPDATE parameters SET name_bg = 'Материал на изработка' WHERE id = 4514; -- Fabrication Material
UPDATE parameters SET name_bg = 'Местоположение на устройството' WHERE id = 4515; -- Device Location
UPDATE parameters SET name_bg = 'Наименование на аксесоара' WHERE id = 4516; -- Accessory Name
UPDATE parameters SET name_bg = 'Капацитет на кана' WHERE id = 4519; -- Kettle Capacity
UPDATE parameters SET name_bg = 'Живот на батерията (стайна температура)' WHERE id = 4521; -- Battery Lifetime (Room Temperature)
UPDATE parameters SET name_bg = 'Изходна мощност' WHERE id = 4522; -- Power Output
UPDATE parameters SET name_bg = 'Обхват на фокуса' WHERE id = 4523; -- Focus Range
UPDATE parameters SET name_bg = 'Артикул' WHERE id = 4525; -- Article
UPDATE parameters SET name_bg = 'Ротация на рамото' WHERE id = 4526; -- Arm Rotation
UPDATE parameters SET name_bg = 'Тип камера' WHERE id = 4527; -- Camera Type
UPDATE parameters SET name_bg = 'Захранване вход/изход' WHERE id = 4534; -- Power In/Out
UPDATE parameters SET name_bg = 'Тегло на опаковката (кг)' WHERE id = 4537; -- Pack Weight (kg)
UPDATE parameters SET name_bg = 'Характеристики на четец за карти' WHERE id = 4545; -- Card Reader Features
UPDATE parameters SET name_bg = 'Производител' WHERE id = 4552; -- Manufacturer
UPDATE parameters SET name_bg = 'Макс. брой PCI Express ленти' WHERE id = 4555; -- Max Number of PCI Express Lanes
UPDATE parameters SET name_bg = 'Поддържана честота на паметта' WHERE id = 4558; -- Supported Memory Speed
UPDATE parameters SET name_bg = 'Брой P-ядра' WHERE id = 4559; -- Performance-cores Quantity
UPDATE parameters SET name_bg = 'Максимален TDP' WHERE id = 4560; -- Maximum TDP
UPDATE parameters SET name_bg = 'Turbo честота на E-ядра' WHERE id = 4562; -- Efficient-core Turbo Clock Rate
UPDATE parameters SET name_bg = 'Базова честота на P-ядра' WHERE id = 4563; -- Performance-core Base Clock Rate
UPDATE parameters SET name_bg = 'Turbo честота на P-ядра' WHERE id = 4564; -- Performance-core Turbo Clock Rate
UPDATE parameters SET name_bg = 'Базова честота на E-ядра' WHERE id = 4565; -- Efficient-core Base Clock Rate
UPDATE parameters SET name_bg = 'Брой E-ядра' WHERE id = 4566; -- Efficient-cores Quantity
UPDATE parameters SET name_bg = 'Набор от инструкции' WHERE id = 4567; -- Instruction Set
UPDATE parameters SET name_bg = 'CPU шина' WHERE id = 4568; -- CPU Bus
UPDATE parameters SET name_bg = 'Технология на производство' WHERE id = 4569; -- Production Technology
UPDATE parameters SET name_bg = 'Максимален размер на паметта' WHERE id = 4571; -- Maximum Memory Size
UPDATE parameters SET name_bg = 'Тактова честота на CPU' WHERE id = 4572; -- CPU Clock Rate
UPDATE parameters SET name_bg = 'Размер на кеша' WHERE id = 4573; -- Cache Size
UPDATE parameters SET name_bg = 'Мултипроцесиране' WHERE id = 4575; -- Multiprocessing
UPDATE parameters SET name_bg = 'Типове памет' WHERE id = 4576; -- Memory Types
UPDATE parameters SET name_bg = 'Поддържани канали на паметта' WHERE id = 4577; -- Memory Channels Supports
UPDATE parameters SET name_bg = 'Макс. температура на CPU' WHERE id = 4579; -- Maximum CPU Temperature
UPDATE parameters SET name_bg = 'Видеокарта' WHERE id = 4580; -- Graphic Card
UPDATE parameters SET name_bg = 'Макс. напрежение на CPU ядрото' WHERE id = 4581; -- Maximum CPU Core Voltage
UPDATE parameters SET name_bg = 'Характеристики на CPU' WHERE id = 4583; -- CPU Features
UPDATE parameters SET name_bg = 'Базова честота на GPU' WHERE id = 4586; -- Graphics Base Frequency
UPDATE parameters SET name_bg = 'Висок/Среден/Нисък говорител' WHERE id = 4592; -- Tweeter/Midrange/Woofer
UPDATE parameters SET name_bg = 'Аудио вход' WHERE id = 4595; -- Audio In
UPDATE parameters SET name_bg = 'Зареждане с кабел' WHERE id = 4596; -- Wired Charging
UPDATE parameters SET name_bg = 'Висок/Среднотонов говорител' WHERE id = 4598; -- Tweeter/Midrange
UPDATE parameters SET name_bg = 'Честотни ленти и честоти' WHERE id = 4610; -- Bands and Frequencies
UPDATE parameters SET name_bg = 'Обемен звук' WHERE id = 4614; -- Surround
UPDATE parameters SET name_bg = 'Страна' WHERE id = 4619; -- Side
UPDATE parameters SET name_bg = 'Среднотонов/Нискотонов говорител' WHERE id = 4620; -- Midrange/Woofer
UPDATE parameters SET name_bg = 'Нискотонов говорител' WHERE id = 4621; -- Subwoofer
UPDATE parameters SET name_bg = 'Висок/Пълнообхватен говорител' WHERE id = 4624; -- Tweeter/Full Range
UPDATE parameters SET name_bg = 'Слушалки' WHERE id = 4630; -- Headphones
UPDATE parameters SET name_bg = 'Височина на кутията (мм)' WHERE id = 4631; -- Box Height (mm)
UPDATE parameters SET name_bg = 'Ширина на кутията (мм)' WHERE id = 4632; -- Box Width (mm)
UPDATE parameters SET name_bg = 'Дължина на кутията (мм)' WHERE id = 4633; -- Box Length (mm)
UPDATE parameters SET name_bg = 'Аудио изход' WHERE id = 4636; -- Audio Out
UPDATE parameters SET name_bg = 'Ляво/Дясно' WHERE id = 4638; -- Left/Right
UPDATE parameters SET name_bg = 'Интерфейс за захранване' WHERE id = 4639; -- Power interface
UPDATE parameters SET name_bg = 'Обхват на въртене (хоризонтален/вертикален)' WHERE id = 4644; -- Rotation Range (Horizontal/Vertical)
UPDATE parameters SET name_bg = 'Ъгъл на видимост (FOV)' WHERE id = 4645; -- Field of View FOV
UPDATE parameters SET name_bg = 'Честотна лента на Wi-Fi' WHERE id = 4652; -- Wi-Fi Band Frequency
UPDATE parameters SET name_bg = 'PoE/Захранващ адаптер' WHERE id = 4654; -- Power over Ethernet/Power Adapter
UPDATE parameters SET name_bg = 'Продава се отделно' WHERE id = 4663; -- Sold Separately
UPDATE parameters SET name_bg = 'Усилване' WHERE id = 4665; -- Amplification
UPDATE parameters SET name_bg = 'Управляващ порт' WHERE id = 4669; -- Management Port
UPDATE parameters SET name_bg = 'Безжична честота' WHERE id = 4676; -- Wireless Frequency
UPDATE parameters SET name_bg = 'LAN/WAN' WHERE id = 4683; -- LAN/WAN
UPDATE parameters SET name_bg = 'Алгоритъм за криптиране на защитната стена' WHERE id = 4684; -- Firewall Encryption Algorithm
UPDATE parameters SET name_bg = 'Поддръжка на външни устройства' WHERE id = 4688; -- External Device Support
UPDATE parameters SET name_bg = 'Макс. поддържани точки за достъп' WHERE id = 4691; -- Maximum Access Points Supported
UPDATE parameters SET name_bg = 'Видео изход' WHERE id = 4698; -- Video output
UPDATE parameters SET name_bg = 'Захранване по кабел (PD)' WHERE id = 4699; -- Power delivery
UPDATE parameters SET name_bg = 'Четец за карти' WHERE id = 4700; -- Card reader
UPDATE parameters SET name_bg = 'Поддръжка за свързване на монитори' WHERE id = 4701; -- Support monitors connection
UPDATE parameters SET name_bg = 'Аудио изход' WHERE id = 4702; -- Audio output
UPDATE parameters SET name_bg = 'Компонентен видео изход' WHERE id = 4704; -- Component Video Output
UPDATE parameters SET name_bg = 'Захранващ щепсел' WHERE id = 4710; -- Power Plug
UPDATE parameters SET name_bg = 'Американски стандарт за жици (AWG)' WHERE id = 4711; -- American Wire Gauge (AWG)
UPDATE parameters SET name_bg = 'Десен конектор' WHERE id = 4718; -- Right Connector
UPDATE parameters SET name_bg = 'Ляв конектор' WHERE id = 4719; -- Left Connector
UPDATE parameters SET name_bg = 'Антена' WHERE id = 4721; -- Antenna
UPDATE parameters SET name_bg = 'Потребители на лиценз' WHERE id = 4723; -- Users per License
UPDATE parameters SET name_bg = 'Платформа' WHERE id = 4724; -- Platform
UPDATE parameters SET name_bg = 'Продукт' WHERE id = 4725; -- Product
UPDATE parameters SET name_bg = 'Тип продукт' WHERE id = 4727; -- Type of Product
UPDATE parameters SET name_bg = 'Валидност на продукта' WHERE id = 4728; -- Product Validity
UPDATE parameters SET name_bg = 'Системни изисквания за PC' WHERE id = 4730; -- Software - Requirments For PC
UPDATE parameters SET name_bg = 'Тип клиент' WHERE id = 4731; -- Type of Customer
UPDATE parameters SET name_bg = 'Системни изисквания за таблет' WHERE id = 4733; -- Software - Requirments For Tablet
UPDATE parameters SET name_bg = 'Сегмент' WHERE id = 4734; -- Segment
UPDATE parameters SET name_bg = 'Детайли за лиценза' WHERE id = 4738; -- License Details
UPDATE parameters SET name_bg = 'Наименование на пула от софтуер' WHERE id = 4739; -- Software Pool Name
UPDATE parameters SET name_bg = 'Локализация' WHERE id = 4740; -- Localization
UPDATE parameters SET name_bg = 'Носител за разпространение' WHERE id = 4741; -- Distribution Media
UPDATE parameters SET name_bg = 'Пул от приложения' WHERE id = 4742; -- Applications Pool
UPDATE parameters SET name_bg = 'Необходими данни за крайния клиент' WHERE id = 4743; -- End Customer Details Required
UPDATE parameters SET name_bg = 'Радиус на кривина' WHERE id = 4759; -- Curve Radius
UPDATE parameters SET name_bg = 'Платформа за смарт ТВ' WHERE id = 4770; -- Smart TV Platform
UPDATE parameters SET name_bg = 'Характеристики на гласовия асистент' WHERE id = 4775; -- Voice Assistant Features
UPDATE parameters SET name_bg = 'Номинална консумация на мощност' WHERE id = 4782; -- Nominal Power Consumption
UPDATE parameters SET name_bg = 'Макс. скорост на произволно четене' WHERE id = 4790; -- Maximum Random Read Rate
UPDATE parameters SET name_bg = 'Характеристики на SSD' WHERE id = 4792; -- Solid State Drive Features
UPDATE parameters SET name_bg = 'Общо записани байтове (TBW)' WHERE id = 4793; -- Total Bytes Written (TBW)
UPDATE parameters SET name_bg = 'Макс. скорост на произволен запис' WHERE id = 4795; -- Maximum Random Write Rate
UPDATE parameters SET name_bg = 'Макс. скорост на последователно четене' WHERE id = 4797; -- Maximum Sequential Read Rate
UPDATE parameters SET name_bg = 'Макс. скорост на последователен запис' WHERE id = 4798; -- Maximum Sequential Write Rate
UPDATE parameters SET name_bg = 'Записвания на ден (DWPD)' WHERE id = 4799; -- Drive Writes Per Day (DWPD)
UPDATE parameters SET name_bg = 'Форм фактор на вътрешния диск' WHERE id = 4807; -- Hard Drive Internal Form Factor
UPDATE parameters SET name_bg = 'Включен кабел' WHERE id = 4808; -- Cable Included
UPDATE parameters SET name_bg = 'Изисква операционна система' WHERE id = 4809; -- Requires Operating System
UPDATE parameters SET name_bg = 'Капацитет за съхранение' WHERE id = 4810; -- Storage Capacity
UPDATE parameters SET name_bg = 'Външен канал за данни' WHERE id = 4811; -- Data Channel External
UPDATE parameters SET name_bg = 'Външна скорост на данните' WHERE id = 4813; -- External Data Bit Rate
UPDATE parameters SET name_bg = 'Тип външно устройство' WHERE id = 4814; -- Type of External Drive
UPDATE parameters SET name_bg = 'Включен софтуер' WHERE id = 4818; -- Software Included
UPDATE parameters SET name_bg = 'Характеристики на твърдия диск' WHERE id = 4829; -- Hard Drive Features
UPDATE parameters SET name_bg = 'Размери' WHERE id = 4841; -- Dimensions
UPDATE parameters SET name_bg = 'Серия' WHERE id = 4847; -- Series
UPDATE parameters SET name_bg = 'Модел' WHERE id = 4848; -- Model
UPDATE parameters SET name_bg = 'Поддръжка' WHERE id = 4851; -- Support
UPDATE parameters SET name_bg = 'Брой LP E-ядра' WHERE id = 4874; -- Low Power Efficient-cores Quantity
UPDATE parameters SET name_bg = 'Инсталирана видео памет' WHERE id = 4875; -- Installed Video Memory
UPDATE parameters SET name_bg = 'Максимален инсталируем RAM' WHERE id = 4876; -- Maximum Installable RAM
UPDATE parameters SET name_bg = 'Максимална влажност' WHERE id = 4884; -- Maximum Humidity
UPDATE parameters SET name_bg = 'Процесор (CPU)' WHERE id = 4886; -- CPU
UPDATE parameters SET name_bg = 'Стандарти за защита' WHERE id = 4891; -- Protection Standards
UPDATE parameters SET name_bg = 'Свързаност' WHERE id = 4894; -- Connectivity
UPDATE parameters SET name_bg = 'Ниво на продукта' WHERE id = 4895; -- Product Level
UPDATE parameters SET name_bg = 'Ефективност' WHERE id = 4899; -- Efficiency
UPDATE parameters SET name_bg = 'Модулно захранване' WHERE id = 4900; -- Modular Power Supply
UPDATE parameters SET name_bg = 'Изход захранване и данни' WHERE id = 4916; -- Power & Data Output
UPDATE parameters SET name_bg = 'Характеристики на захранването' WHERE id = 4939; -- Power Features
UPDATE parameters SET name_bg = 'Характеристики на захранващия адаптер' WHERE id = 4966; -- Power Adapter and Power Supply Features
UPDATE parameters SET name_bg = 'Ширина (мм)' WHERE id = 4967; -- Width (mm)
UPDATE parameters SET name_bg = 'Тип захранващо устройство' WHERE id = 4968; -- Power Device Type
UPDATE parameters SET name_bg = 'Брой инсталирани вентилатори' WHERE id = 4969; -- Fans Installed Quantity
UPDATE parameters SET name_bg = 'Местоположение на захранването' WHERE id = 4970; -- Power Device Location
UPDATE parameters SET name_bg = 'Защита на веригата' WHERE id = 4971; -- Circuit Protection
UPDATE parameters SET name_bg = 'Дълбочина (мм)' WHERE id = 4972; -- Depth (mm)
UPDATE parameters SET name_bg = 'Височина (мм)' WHERE id = 4974; -- Height (mm)
UPDATE parameters SET name_bg = 'Входно напрежение' WHERE id = 4976; -- Input Voltage
UPDATE parameters SET name_bg = 'Входна честота' WHERE id = 4977; -- Input Frequency
UPDATE parameters SET name_bg = 'Диаметър на вентилатора' WHERE id = 4978; -- Fan Diameter
UPDATE parameters SET name_bg = 'Максимална изходна мощност' WHERE id = 4979; -- Maximum Output Power
UPDATE parameters SET name_bg = 'Брой захранващи единици' WHERE id = 4980; -- Power Device Unit Quantity
UPDATE parameters SET name_bg = 'Камерна система' WHERE id = 4993; -- Camera System
UPDATE parameters SET name_bg = 'Протоколи' WHERE id = 4996; -- Protocols
UPDATE parameters SET name_bg = 'Характеристики на фото камерата' WHERE id = 5000; -- Camera Photo Features
UPDATE parameters SET name_bg = 'Графичен процесор (GPU)' WHERE id = 5001; -- GPU
UPDATE parameters SET name_bg = 'Тегло' WHERE id = 5002; -- Weight
UPDATE parameters SET name_bg = 'Wi-Fi Direct' WHERE id = 5004; -- Wi-Fi Direct
UPDATE parameters SET name_bg = 'Видео резолюция' WHERE id = 5010; -- Video Resolution
UPDATE parameters SET name_bg = 'Сърфиране в интернет' WHERE id = 5013; -- Internet Surfing
UPDATE parameters SET name_bg = 'Честота на кадрите' WHERE id = 5014; -- Video Frame rate
UPDATE parameters SET name_bg = 'Видео запис' WHERE id = 5015; -- Video Record
UPDATE parameters SET name_bg = 'Поддържани аудио формати' WHERE id = 5017; -- Supported Audio Formats
UPDATE parameters SET name_bg = 'Wi-Fi точка за достъп' WHERE id = 5018; -- Wi-Fi Hotspot
UPDATE parameters SET name_bg = 'Поддържани видео формати' WHERE id = 5021; -- Supported Video Formats
UPDATE parameters SET name_bg = 'Технология на камерата' WHERE id = 5022; -- Camera Technology
UPDATE parameters SET name_bg = 'Времетраене на видео възпроизвеждане' WHERE id = 5024; -- Video Playback Time
UPDATE parameters SET name_bg = 'Брой вътрешни гнезда 3.5/2.5 инча' WHERE id = 5026; -- Number of 3.5-inch / 2.5-inch Internal Drive Bays
UPDATE parameters SET name_bg = 'Поддържан форм фактор на дънна платка' WHERE id = 5030; -- Supported Mainboard Form Factor
UPDATE parameters SET name_bg = 'Брой слотове за разширение с пълна височина' WHERE id = 5034; -- Number of Expansion Slots Full Height
UPDATE parameters SET name_bg = 'Брой Hot-Swap гнезда 3.5/2.5 инча' WHERE id = 5035; -- Number of 3.5-inch / 2.5-inch Hot-Swap Drive Bays
UPDATE parameters SET name_bg = 'Аудио линеен вход/Микрофонен вход' WHERE id = 5036; -- Audio Line-In/Microphone-In
UPDATE parameters SET name_bg = 'Брой резервни захранвания' WHERE id = 5039; -- Redundant Power Device Installed Quantity
UPDATE parameters SET name_bg = 'Тип брава на кутията' WHERE id = 5043; -- Case Lock Type
UPDATE parameters SET name_bg = 'Метод на носене' WHERE id = 5044; -- Carry Method
UPDATE parameters SET name_bg = 'Брой джобове' WHERE id = 5046; -- Pockets Quantity
UPDATE parameters SET name_bg = 'Брой вътрешни джобове' WHERE id = 5047; -- Internal Pockets Quantity
UPDATE parameters SET name_bg = 'Брой външни джобове' WHERE id = 5048; -- External Pockets Quantity
UPDATE parameters SET name_bg = 'Тип комплект' WHERE id = 5056; -- Kit Type
UPDATE parameters SET name_bg = 'Шум на помпата' WHERE id = 5059; -- Pump Noise
UPDATE parameters SET name_bg = 'Очакван живот на помпата' WHERE id = 5061; -- Pump Life Expectancy
UPDATE parameters SET name_bg = 'Скорост на помпата' WHERE id = 5071; -- Pump Speed
UPDATE parameters SET name_bg = 'Минимална номинална скорост' WHERE id = 5074; -- Rated Speed Min
UPDATE parameters SET name_bg = 'Поддържано номинално напрежение' WHERE id = 5075; -- Rated Voltage Support
UPDATE parameters SET name_bg = 'Поддържан конектор за вентилатор' WHERE id = 5076; -- Fan Connector Supported
UPDATE parameters SET name_bg = 'Режим на управление на вентилатора' WHERE id = 5077; -- Fan Control Mode
UPDATE parameters SET name_bg = 'Брой канали за вентилатори' WHERE id = 5078; -- Fan Channels Quantity
UPDATE parameters SET name_bg = 'Максимален TDP' WHERE id = 5079; -- Max TDP
UPDATE parameters SET name_bg = 'Мин. работна температура' WHERE id = 5082; -- Minimum Operating Ambient Temperature
UPDATE parameters SET name_bg = 'Макс. работна температура' WHERE id = 5084; -- Maximum Operating Ambient Temperature
UPDATE parameters SET name_bg = 'G-сензор' WHERE id = 5085; -- G-Sensor
UPDATE parameters SET name_bg = 'Резолюция на предната камера' WHERE id = 5089; -- Front Camera Video Resolution
UPDATE parameters SET name_bg = 'Тип продукт' WHERE id = 5092; -- Product type
UPDATE parameters SET name_bg = 'Доставчик' WHERE id = 5093; -- Vendor
UPDATE parameters SET name_bg = 'Тип клавиши' WHERE id = 5099; -- Keys Type
UPDATE parameters SET name_bg = 'Стил на опаковката' WHERE id = 5106; -- Package Style
UPDATE parameters SET name_bg = 'Захват' WHERE id = 5107; -- Grip
UPDATE parameters SET name_bg = 'Подсветка' WHERE id = 5109; -- Backlight
UPDATE parameters SET name_bg = 'Сила на натиск' WHERE id = 5113; -- Pressing Force
UPDATE parameters SET name_bg = 'Ход на клавиша' WHERE id = 5114; -- Travel Distance
UPDATE parameters SET name_bg = 'Усещане при натиск' WHERE id = 5116; -- Key Feel
UPDATE parameters SET name_bg = 'Тип връзка на мишката/тракпада' WHERE id = 5118; -- Pointing Device Connection Type
UPDATE parameters SET name_bg = 'Профил' WHERE id = 5123; -- Profile
UPDATE parameters SET name_bg = 'Брой мултимедийни функции' WHERE id = 5124; -- Number of Multimedia Functions
UPDATE parameters SET name_bg = 'Точка на задействане' WHERE id = 5125; -- Actuation Point
UPDATE parameters SET name_bg = 'Сензор на мишката' WHERE id = 5127; -- Mouse Sensor
UPDATE parameters SET name_bg = 'Стандарт за запис' WHERE id = 5129; -- Recording Standard
UPDATE parameters SET name_bg = 'Wi-Fi антена' WHERE id = 5135; -- Wi-Fi Antenna
UPDATE parameters SET name_bg = 'Архитектура на LAN' WHERE id = 5138; -- LAN Architecture
UPDATE parameters SET name_bg = 'Микрофонен вход' WHERE id = 5160; -- Microphone-In
UPDATE parameters SET name_bg = 'Стандарт на CPU сокета' WHERE id = 5161; -- CPU Socket Standard
UPDATE parameters SET name_bg = 'Видео чипсет' WHERE id = 5162; -- Video Chipset
UPDATE parameters SET name_bg = 'Поддържани протоколи за връзка' WHERE id = 5163; -- Data Link Protocol Supports
UPDATE parameters SET name_bg = 'Паралелно' WHERE id = 5164; -- Parallel
UPDATE parameters SET name_bg = 'Тип памет' WHERE id = 5165; -- Memory Type
UPDATE parameters SET name_bg = 'Характеристики на паметта' WHERE id = 5166; -- Memory Features
UPDATE parameters SET name_bg = 'LAN чипсет' WHERE id = 5167; -- LAN Chipset
UPDATE parameters SET name_bg = 'Брой инсталирани процесори' WHERE id = 5169; -- CPU Installed Quantity
UPDATE parameters SET name_bg = 'Макс. инсталируема системна памет' WHERE id = 5170; -- Maximum Installable System Memory Storage Capacity
UPDATE parameters SET name_bg = 'BIOS характеристики' WHERE id = 5171; -- BIOS Features
UPDATE parameters SET name_bg = 'BIOS тип' WHERE id = 5172; -- BIOS Type
UPDATE parameters SET name_bg = 'Интегрирано аудио' WHERE id = 5173; -- Audio Integrated
UPDATE parameters SET name_bg = 'Инсталиран L2 кеш' WHERE id = 5174; -- Installed L2 Cache Memory Storage Capacity
UPDATE parameters SET name_bg = 'Тип видео памет' WHERE id = 5175; -- Video Memory Type
UPDATE parameters SET name_bg = 'Вътрешна тактова честота' WHERE id = 5177; -- Internal Clock Rate
UPDATE parameters SET name_bg = 'Форм фактор на паметта' WHERE id = 5178; -- Memory Form Factor
UPDATE parameters SET name_bg = 'Метод за проверка на грешки' WHERE id = 5179; -- Provides Error Checking Method
UPDATE parameters SET name_bg = 'Тип контролер за съхранение' WHERE id = 5180; -- Storage Controller Type
UPDATE parameters SET name_bg = 'Включени аксесоари' WHERE id = 5181; -- Included Accessories
UPDATE parameters SET name_bg = 'Интегриран LAN' WHERE id = 5182; -- LAN Integrated
UPDATE parameters SET name_bg = 'Поддържани аудио стандарти' WHERE id = 5183; -- Compliant Audio Standards
UPDATE parameters SET name_bg = 'Интегриран видео контролер' WHERE id = 5184; -- Video Controller Integrated
UPDATE parameters SET name_bg = 'Общ брой слотове за памет' WHERE id = 5185; -- Memory Slot Total Quantity
UPDATE parameters SET name_bg = 'Режим на аудио изход' WHERE id = 5186; -- Audio Output Mode
UPDATE parameters SET name_bg = 'Брой CPU ядра' WHERE id = 5187; -- CPU Core Quantity
UPDATE parameters SET name_bg = 'Сертификации' WHERE id = 5188; -- Certifications
UPDATE parameters SET name_bg = 'Тип захранващи конектори' WHERE id = 5189; -- Power Connectors Type
UPDATE parameters SET name_bg = 'Форм фактор' WHERE id = 5190; -- Form Factor
UPDATE parameters SET name_bg = 'Инсталиран процесор' WHERE id = 5191; -- CPU Installed Name
UPDATE parameters SET name_bg = 'Аудио интерфейс' WHERE id = 5193; -- Audio Interface
UPDATE parameters SET name_bg = 'Форм фактор на контролера за съхранение' WHERE id = 5194; -- Storage Controller Form Factor
UPDATE parameters SET name_bg = 'Честота на паметта' WHERE id = 5195; -- Memory Speed
UPDATE parameters SET name_bg = 'Аудио чипсет' WHERE id = 5196; -- Audio Chipset
UPDATE parameters SET name_bg = 'Характеристики на дънната платка' WHERE id = 5198; -- Mainboard Features
UPDATE parameters SET name_bg = 'Поддържани процесори' WHERE id = 5199; -- Supports Central Processor Unit
UPDATE parameters SET name_bg = 'Интегрирано' WHERE id = 5200; -- Integrated
UPDATE parameters SET name_bg = 'Макс. честота на CPU шина' WHERE id = 5201; -- Max. CPU Bus Clock Rate
UPDATE parameters SET name_bg = 'Чипсет' WHERE id = 5202; -- Chipset
UPDATE parameters SET name_bg = 'Макс. поддържан брой процесори' WHERE id = 5203; -- CPU Maximum Quantity Supports
UPDATE parameters SET name_bg = 'Аудио линеен изход' WHERE id = 5215; -- Audio Line-Out
UPDATE parameters SET name_bg = 'Общ капацитет за съхранение' WHERE id = 5222; -- Total Storage Capacity
UPDATE parameters SET name_bg = 'Наименование на контролера' WHERE id = 5231; -- Controller Name
UPDATE parameters SET name_bg = 'Брой кабели в комплекта' WHERE id = 5232; -- Cables Quantity in Kit
UPDATE parameters SET name_bg = 'Твърдотелно устройство (SSD)' WHERE id = 5235; -- Solid State Drive
UPDATE parameters SET name_bg = 'Твърд диск' WHERE id = 5236; -- Hard Disk Drive
UPDATE parameters SET name_bg = 'Макс. работна надморска височина' WHERE id = 5243; -- Max. Altitude Operating
UPDATE parameters SET name_bg = 'Стандартна височина на рак' WHERE id = 5244; -- Standard Rack Height
UPDATE parameters SET name_bg = 'Мин. влажност при съхранение' WHERE id = 5245; -- Minimum Non-Operating Humidity
UPDATE parameters SET name_bg = 'Макс. влажност при съхранение' WHERE id = 5246; -- Maximum Non-Operating Humidity
UPDATE parameters SET name_bg = 'Брой вътрешни гнезда 3.5 инча' WHERE id = 5248; -- Number of 3.5-inch Internal Drive Bays
UPDATE parameters SET name_bg = 'Бележки за гаранционния срок' WHERE id = 5249; -- Warranty Term Notes
UPDATE parameters SET name_bg = 'Капацитет на паметта' WHERE id = 5250; -- Memory Capacity
UPDATE parameters SET name_bg = 'Мин. температура при съхранение' WHERE id = 5251; -- Minimum Non-Operating Ambient Temperature
UPDATE parameters SET name_bg = 'Монтажни възможности' WHERE id = 5252; -- Mounting Capability
UPDATE parameters SET name_bg = 'Макс. надморска височина при съхранение' WHERE id = 5253; -- Max. Altitude Storage
UPDATE parameters SET name_bg = 'Брой поддържани дънни платки' WHERE id = 5254; -- Quantity of Mainboards Supported
UPDATE parameters SET name_bg = 'Брой инсталирани захранвания' WHERE id = 5255; -- Power Device Installed Quantity
UPDATE parameters SET name_bg = 'Форм фактор на системата' WHERE id = 5256; -- System Form Factor
UPDATE parameters SET name_bg = 'Макс. температура при съхранение' WHERE id = 5257; -- Maximum Non-Operating Ambient Temperature
UPDATE parameters SET name_bg = 'Характеристики' WHERE id = 5258; -- Features
UPDATE parameters SET name_bg = 'Тип система' WHERE id = 5259; -- System Type
UPDATE parameters SET name_bg = 'Макс. брой поддържани захранвания' WHERE id = 5260; -- Power Device Maximum Quantity Supports
UPDATE parameters SET name_bg = 'Работен ток' WHERE id = 5262; -- Operating Current
UPDATE parameters SET name_bg = 'Външно захранване' WHERE id = 5266; -- External Power
UPDATE parameters SET name_bg = 'Брой ядра за трасиране на лъчи' WHERE id = 5267; -- Ray-tracing Cores Quantity
UPDATE parameters SET name_bg = 'Макс. поддържани монитори' WHERE id = 5271; -- Max. Monitors Supports
UPDATE parameters SET name_bg = 'Брой текстурни единици (TMU)' WHERE id = 5280; -- Texture Mapping Unit Quantity
UPDATE parameters SET name_bg = 'Брой Tensor ядра' WHERE id = 5283; -- Tensor Core Quantity
UPDATE parameters SET name_bg = 'GPU Boost честота' WHERE id = 5284; -- GPU Core Speed Boost
UPDATE parameters SET name_bg = 'Брой рендериращи единици (ROP)' WHERE id = 5285; -- Render Output Units Quantity (ROP)
UPDATE parameters SET name_bg = 'L3 кеш памет' WHERE id = 5300; -- L3 Cache Memory
UPDATE parameters SET name_bg = 'Ширина на облегалката' WHERE id = 5305; -- Backrest Width
UPDATE parameters SET name_bg = 'Дълбочина на седалката' WHERE id = 5306; -- Seat Depth
UPDATE parameters SET name_bg = 'Височина на седалката' WHERE id = 5309; -- Seat Height
UPDATE parameters SET name_bg = 'Минимална височина на седалката' WHERE id = 5310; -- Minimum Seat Height
UPDATE parameters SET name_bg = 'Ширина на седалката' WHERE id = 5313; -- Seat Width
UPDATE parameters SET name_bg = 'Височина на облегалката' WHERE id = 5315; -- Backrest Height
UPDATE parameters SET name_bg = 'Пълнеж' WHERE id = 5318; -- Filler
UPDATE parameters SET name_bg = 'Максимална височина на седалката' WHERE id = 5319; -- Maximum Seat Height
UPDATE parameters SET name_bg = 'Клавишни капачки' WHERE id = 5322; -- Keycaps
UPDATE parameters SET name_bg = 'Материал на корпуса' WHERE id = 5323; -- Casing Material
UPDATE parameters SET name_bg = 'Регулиране на височината' WHERE id = 5324; -- Height Adjustment
UPDATE parameters SET name_bg = 'Характеристики на подлакътниците' WHERE id = 5325; -- Armrests Features
UPDATE parameters SET name_bg = 'Подлакътници' WHERE id = 5326; -- Armrests
UPDATE parameters SET name_bg = 'Брой колела' WHERE id = 5327; -- Wheels Quantity
UPDATE parameters SET name_bg = 'Характеристики на стола' WHERE id = 5328; -- Chair Features
UPDATE parameters SET name_bg = 'Максимално подходящо тегло' WHERE id = 5329; -- Maximum Suitable Weight
UPDATE parameters SET name_bg = 'Материал на рамката' WHERE id = 5330; -- Frame Material
UPDATE parameters SET name_bg = 'Максимален ъгъл на облегалката' WHERE id = 5331; -- Maximum Backrest Angle
UPDATE parameters SET name_bg = 'Максимална подходяща височина' WHERE id = 5332; -- Maximum Suitable Height
UPDATE parameters SET name_bg = 'Минимален ъгъл на облегалката' WHERE id = 5333; -- Minimum Backrest Angle
UPDATE parameters SET name_bg = 'Механизъм на огъване' WHERE id = 5334; -- Bending Mechanism
UPDATE parameters SET name_bg = 'Дължина на облегалката' WHERE id = 5337; -- Backrest Length
UPDATE parameters SET name_bg = 'Изход на захранващия адаптер' WHERE id = 5341; -- Power Adapter Output
UPDATE parameters SET name_bg = 'Тип щепсел на захранващия кабел' WHERE id = 5342; -- Power cord, Plug Type
UPDATE parameters SET name_bg = 'Изход на безжичното зарядно' WHERE id = 5343; -- Wireless Charger Output
UPDATE parameters SET name_bg = 'RGB подсветка' WHERE id = 5344; -- RGB Backlight
UPDATE parameters SET name_bg = 'Вход на захранващия адаптер' WHERE id = 5345; -- Power Adapter Input
UPDATE parameters SET name_bg = 'Тъчпад' WHERE id = 5347; -- Touchpad
UPDATE parameters SET name_bg = 'Датчик за движение' WHERE id = 5349; -- Motion sensor
UPDATE parameters SET name_bg = 'Непрекъснато използване' WHERE id = 5350; -- Continuous Use
UPDATE parameters SET name_bg = 'Подпора за глава' WHERE id = 5351; -- Headrest Support
UPDATE parameters SET name_bg = 'Възглавница за глава' WHERE id = 5354; -- Headrest Pillow
UPDATE parameters SET name_bg = 'Дистанционно управление' WHERE id = 5356; -- Remote control
UPDATE parameters SET name_bg = 'Програмируеми бутони' WHERE id = 5358; -- Programmable buttons
UPDATE parameters SET name_bg = 'Скорост на работа' WHERE id = 5360; -- Operation rate
UPDATE parameters SET name_bg = 'Безжично слушане' WHERE id = 5361; -- Listening Wirelessly
UPDATE parameters SET name_bg = 'Колела' WHERE id = 5362; -- Wheels
UPDATE parameters SET name_bg = 'Честота на опресняване' WHERE id = 5363; -- Response Rate
UPDATE parameters SET name_bg = 'Съхранение на игрови аксесоари' WHERE id = 5364; -- Game accessories storage
UPDATE parameters SET name_bg = 'Едновременно зареждане' WHERE id = 5365; -- Simultaneous charging
UPDATE parameters SET name_bg = 'USB Type-C порт' WHERE id = 5366; -- Type C port
UPDATE parameters SET name_bg = 'Стойка за слушалки' WHERE id = 5367; -- Headset holder
UPDATE parameters SET name_bg = 'Съхранение на игрови дискове' WHERE id = 5369; -- Gaming disc storage
UPDATE parameters SET name_bg = 'USB 2.0 порт' WHERE id = 5370; -- USB 2.0 port
UPDATE parameters SET name_bg = 'Максимално тегло за работа' WHERE id = 5372; -- Maximum Handling Weight
UPDATE parameters SET name_bg = 'Акустична конструкция' WHERE id = 5373; -- Acoustic Design
UPDATE parameters SET name_bg = 'Диаметър на говорителя' WHERE id = 5374; -- Driver Diameter
UPDATE parameters SET name_bg = 'Чувствителност' WHERE id = 5375; -- Sensitivity
UPDATE parameters SET name_bg = 'Импеданс' WHERE id = 5376; -- Impedance
UPDATE parameters SET name_bg = 'Макс. скорост на движение' WHERE id = 5377; -- Max. Movement Speed
UPDATE parameters SET name_bg = 'Автоматични програми за готвене' WHERE id = 5378; -- Automatic Cooking Programs
UPDATE parameters SET name_bg = 'Максимална температура на готвене' WHERE id = 5379; -- Maximum Cooking Temperature
UPDATE parameters SET name_bg = 'Тип купа' WHERE id = 5381; -- Bowl Type
UPDATE parameters SET name_bg = 'Минимална температура на готвене' WHERE id = 5382; -- Minimum Cooking Temperature
UPDATE parameters SET name_bg = 'Максимално времетраене на готвене' WHERE id = 5383; -- Maximum Cooking Time
UPDATE parameters SET name_bg = 'Мощност на кана' WHERE id = 5384; -- Kettle Power
UPDATE parameters SET name_bg = 'Интелигентна система за контрол на температурата' WHERE id = 5385; -- Intelligent temperature control system
UPDATE parameters SET name_bg = 'Индикации' WHERE id = 5386; -- Indications
UPDATE parameters SET name_bg = 'Модел мотор на вентилатора' WHERE id = 5387; -- Fan motor Model
UPDATE parameters SET name_bg = 'Гласов протокол' WHERE id = 5388; -- Voice Protocol
UPDATE parameters SET name_bg = 'Мощност на нагревателя (<W)' WHERE id = 5389; -- Power of the heater (<W)
UPDATE parameters SET name_bg = 'Вр. за преминаване от топъл към студен въздух' WHERE id = 5390; -- Time to convert hot air to cold air
UPDATE parameters SET name_bg = 'Номинална мощност на мотора, W' WHERE id = 5393; -- Fan motor Rated power, W
UPDATE parameters SET name_bg = 'Модел йонизатор' WHERE id = 5394; -- Ionisator model
UPDATE parameters SET name_bg = 'Температура на термостата, °C' WHERE id = 5396; -- Thermostat Temperature, °C
UPDATE parameters SET name_bg = 'Макс. шум, dB (на 30 см)' WHERE id = 5397; -- Max. Noise, dB (Measured at distance 30 cm)
UPDATE parameters SET name_bg = 'Брой скоростни режими' WHERE id = 5398; -- Speed Modes Quantity
UPDATE parameters SET name_bg = 'Скорост на вятъра в различни режими, m/s' WHERE id = 5400; -- Wind speed in different modes, m/s
UPDATE parameters SET name_bg = 'Сила на изтегляне на аксесоари, N' WHERE id = 5402; -- Accessories pull force, N
UPDATE parameters SET name_bg = 'Макс. температура на изхода, °C' WHERE id = 5403; -- Max. outlet temperature, °C
UPDATE parameters SET name_bg = 'Температура на термичната предпазна бобина, °C' WHERE id = 5404; -- Thermo fuse Temperature, °C
UPDATE parameters SET name_bg = 'Номинална изходна мощност' WHERE id = 5405; -- Nominal Output Power
UPDATE parameters SET name_bg = 'Инфрачервена грижа за косата' WHERE id = 5406; -- Infrared Hair Nursing
UPDATE parameters SET name_bg = 'Йонна мощност, W' WHERE id = 5407; -- ION Power, W
UPDATE parameters SET name_bg = 'Тип аксесоари' WHERE id = 5410; -- Type of the accessories
UPDATE parameters SET name_bg = 'Работен режим' WHERE id = 5411; -- Operating mode
UPDATE parameters SET name_bg = 'Детекция на гравитация' WHERE id = 5412; -- Gravity Detect
UPDATE parameters SET name_bg = 'Система за защита' WHERE id = 5413; -- Protection System
UPDATE parameters SET name_bg = 'Брой режими' WHERE id = 5414; -- Number of modes
UPDATE parameters SET name_bg = 'Макс. скорост на вятъра 200мм, m/s (без дюзи)' WHERE id = 5416; -- Max wind speed, distance 200mm, m/s (test without Nozzles)
UPDATE parameters SET name_bg = 'Мин. шум, dB (на 30 см)' WHERE id = 5417; -- Min. Noise, dB (Measured at distance 30 cm)
UPDATE parameters SET name_bg = 'Мин. шум, dB (на 100 см)' WHERE id = 5418; -- Min. Noise, dB (Measured at distance 100 cm)
UPDATE parameters SET name_bg = 'Макс. шум, dB (на 100 см)' WHERE id = 5419; -- Max. Noise, dB (Measured at distance 100 cm)
UPDATE parameters SET name_bg = 'Брой перки на вентилатора' WHERE id = 5420; -- Quantity of fan blades
UPDATE parameters SET name_bg = 'Модел NTC термистор' WHERE id = 5421; -- NTC Model
UPDATE parameters SET name_bg = 'Скорост на въртене на мотора, RPM' WHERE id = 5422; -- Fan motor rotation speed, RPM
UPDATE parameters SET name_bg = 'Йонизация, йон/cm³' WHERE id = 5423; -- Ionization, ion/cm^3
UPDATE parameters SET name_bg = 'Модел термостат' WHERE id = 5424; -- Thermostat Model
UPDATE parameters SET name_bg = 'Бутони' WHERE id = 5427; -- Buttons
UPDATE parameters SET name_bg = 'Брой температурни режими' WHERE id = 5428; -- Quantity of Temperature Modes
UPDATE parameters SET name_bg = 'Тип нагревателен елемент' WHERE id = 5429; -- Heating element type
UPDATE parameters SET name_bg = 'Тип отваряне на вратата' WHERE id = 5433; -- Door Opening Type
UPDATE parameters SET name_bg = 'Тип модел' WHERE id = 5434; -- Model Type
UPDATE parameters SET name_bg = 'Капацитет (л)' WHERE id = 5437; -- Capacity (L)
UPDATE parameters SET name_bg = 'Разпределение на микровълните' WHERE id = 5439; -- Microwave Distribution
UPDATE parameters SET name_bg = 'Глави за четки' WHERE id = 5440; -- Brush Heads
UPDATE parameters SET name_bg = 'Тип четина' WHERE id = 5441; -- Bristles Type
UPDATE parameters SET name_bg = 'Тип четка за зъби' WHERE id = 5442; -- Toothbrush Type
UPDATE parameters SET name_bg = 'Максимална скорост' WHERE id = 5444; -- Max Speed
UPDATE parameters SET name_bg = 'Материал на нагревателя' WHERE id = 5445; -- Heater Material
UPDATE parameters SET name_bg = 'Размер' WHERE id = 5446; -- Dimension
UPDATE parameters SET name_bg = 'Тип нагревател' WHERE id = 5448; -- Heater Type
UPDATE parameters SET name_bg = 'Резервоар за вода' WHERE id = 5449; -- Water Tank
UPDATE parameters SET name_bg = 'Максимално времетраене на готвене' WHERE id = 5450; -- Max Cooking Time
UPDATE parameters SET name_bg = 'Ниво на шума' WHERE id = 5454; -- Noise level
UPDATE parameters SET name_bg = 'Скорост на въздушния поток' WHERE id = 5458; -- Air flow speed
UPDATE parameters SET name_bg = 'Перки на вентилатора' WHERE id = 5464; -- Fan blades
UPDATE parameters SET name_bg = 'Вакуум (kPa)' WHERE id = 5469; -- Vacuum (kPa)
UPDATE parameters SET name_bg = 'Времетраене за завиране' WHERE id = 5471; -- Boiling Time
UPDATE parameters SET name_bg = 'Площ на покритие до' WHERE id = 5472; -- Coverage Area up to
UPDATE parameters SET name_bg = 'Характеристики на въздушни уреди' WHERE id = 5473; -- Air Appliances Features
UPDATE parameters SET name_bg = 'Режим на четката за зъби' WHERE id = 5474; -- Toothbrush Mode
UPDATE parameters SET name_bg = 'Включва' WHERE id = 5476; -- Includes
UPDATE parameters SET name_bg = 'Състав' WHERE id = 5479; -- Composition
UPDATE parameters SET name_bg = 'Времетраене за нагряване' WHERE id = 5480; -- Heating Time
UPDATE parameters SET name_bg = 'Дебит на парата' WHERE id = 5486; -- Steam Supply Rate
UPDATE parameters SET name_bg = 'Парно налягане' WHERE id = 5487; -- Steam Pressure
UPDATE parameters SET name_bg = 'Температура на парата' WHERE id = 5490; -- Steam Temperature
UPDATE parameters SET name_bg = 'Месингова четка' WHERE id = 5503; -- Brass Brush
UPDATE parameters SET name_bg = 'Брой интерфейси за захранване' WHERE id = 5505; -- Power Interface Quantity
UPDATE parameters SET name_bg = 'Стъргалка' WHERE id = 5507; -- Squeegee
UPDATE parameters SET name_bg = 'Площ на отопление' WHERE id = 5508; -- Heater Coverage Area
UPDATE parameters SET name_bg = 'Покритие на купата/тигана' WHERE id = 5509; -- Bowl / Pan Coating
UPDATE parameters SET name_bg = 'Дължина на парния маркуч' WHERE id = 5510; -- Steam Hose Length
UPDATE parameters SET name_bg = 'Тип умен нагревател' WHERE id = 5511; -- Smart Heater Type
UPDATE parameters SET name_bg = 'Приложение' WHERE id = 5514; -- Application
UPDATE parameters SET name_bg = 'Вграден воден филтър' WHERE id = 5517; -- Built-in Water Filter
UPDATE parameters SET name_bg = 'Брой температурни режими' WHERE id = 5524; -- Number of temperatures
UPDATE parameters SET name_bg = 'Мокро почистване на пода' WHERE id = 5565; -- Floor Mop
UPDATE parameters SET name_bg = 'Брой сменяеми тави' WHERE id = 5566; -- Quantity of Removable Trays
UPDATE parameters SET name_bg = 'Дюзи' WHERE id = 5569; -- Nozzles
UPDATE parameters SET name_bg = 'Скорост на засмукване' WHERE id = 5570; -- Suction Speed
UPDATE parameters SET name_bg = 'Материал на работната повърхност' WHERE id = 5571; -- Working Surface Material
UPDATE parameters SET name_bg = 'Брой работни режими' WHERE id = 5580; -- Quantity of Operating Modes
UPDATE parameters SET name_bg = 'Тип фурна' WHERE id = 5583; -- Oven Type
UPDATE parameters SET name_bg = 'Капацитет на фурната' WHERE id = 5585; -- Oven Capacity
UPDATE parameters SET name_bg = 'Тип мрежа' WHERE id = 5587; -- Network Type
UPDATE parameters SET name_bg = 'Серия и фамилия' WHERE id = 5588; -- Series and Family
UPDATE parameters SET name_bg = 'Артикул на производителя' WHERE id = 5594; -- Manufacturer Article
UPDATE parameters SET name_bg = 'Технология на дисплея' WHERE id = 5595; -- Display Technology
UPDATE parameters SET name_bg = 'Текущ капацитет на батерията' WHERE id = 5597; -- Battery Current Capacity
UPDATE parameters SET name_bg = 'Честотни ленти на мобилната мрежа' WHERE id = 5598; -- Mobile Network Bands
UPDATE parameters SET name_bg = 'Wi-Fi интерфейс' WHERE id = 5600; -- Interface Wi-Fi
UPDATE parameters SET name_bg = 'Flash карта' WHERE id = 5601; -- Flash Card
UPDATE parameters SET name_bg = 'Функция за обаждане' WHERE id = 5602; -- Call Function
UPDATE parameters SET name_bg = 'USB интерфейс' WHERE id = 5603; -- Interface USB
UPDATE parameters SET name_bg = 'Операционна система' WHERE id = 5604; -- Operating System
UPDATE parameters SET name_bg = 'Вградени устройства' WHERE id = 5605; -- Built-in Devices
UPDATE parameters SET name_bg = 'Технология на батерията' WHERE id = 5606; -- Battery Technology
UPDATE parameters SET name_bg = 'Bluetooth интерфейс' WHERE id = 5608; -- Interface Bluetooth
UPDATE parameters SET name_bg = 'Номинално тегло' WHERE id = 5609; -- Nominal Weight
UPDATE parameters SET name_bg = 'Аудио интерфейс' WHERE id = 5610; -- Interface Audio
UPDATE parameters SET name_bg = 'Размер на вътрешната памет' WHERE id = 5611; -- Internal Memory Size
UPDATE parameters SET name_bg = 'Размер на дисплея' WHERE id = 5613; -- Display Size
UPDATE parameters SET name_bg = 'Резолюция на камерата' WHERE id = 5614; -- Camera Resolution
UPDATE parameters SET name_bg = 'Предна камера' WHERE id = 5615; -- Front Camera
UPDATE parameters SET name_bg = 'Вътрешна тактова честота на CPU' WHERE id = 5617; -- CPU Internal Clock Rate
UPDATE parameters SET name_bg = 'Инсталиран обем RAM' WHERE id = 5618; -- Installed RAM Storage Capacity
UPDATE parameters SET name_bg = 'Търговско наименование на модела' WHERE id = 5619; -- Marketing Model Name
UPDATE parameters SET name_bg = 'Времетраене при сърфиране' WHERE id = 5622; -- Battery Web Surfing Time
UPDATE parameters SET name_bg = 'Въртене на екрана' WHERE id = 5623; -- Screen rotation
UPDATE parameters SET name_bg = 'Честота на GPU' WHERE id = 5624; -- GPU Speed
UPDATE parameters SET name_bg = 'Базово зареждане' WHERE id = 5628; -- Base Charge
UPDATE parameters SET name_bg = 'Технология на сменяемата батерия' WHERE id = 5629; -- Removable Battery Technology
UPDATE parameters SET name_bg = 'HDMI интерфейс' WHERE id = 5630; -- Interface HDMI
UPDATE parameters SET name_bg = 'Текущ капацитет на сменяемата батерия' WHERE id = 5631; -- Removable Battery Current Capacity
UPDATE parameters SET name_bg = 'Височина на падане' WHERE id = 5632; -- Drop Height
UPDATE parameters SET name_bg = 'Настройка' WHERE id = 5633; -- Adjustment
UPDATE parameters SET name_bg = 'Опции за свързаност' WHERE id = 5635; -- Connectivity Options
UPDATE parameters SET name_bg = 'Бутон за нулиране/сдвояване' WHERE id = 5636; -- Reset / Pairing Reset Button
UPDATE parameters SET name_bg = 'Общо времетраене с калъф' WHERE id = 5637; -- Total Playtime with case
UPDATE parameters SET name_bg = 'Предназначение / Употреба' WHERE id = 5638; -- Purpose / Use case
UPDATE parameters SET name_bg = 'Бързо зареждане' WHERE id = 5639; -- Fast Charging
UPDATE parameters SET name_bg = 'Режим за игри / Ниска латентност' WHERE id = 5640; -- Gaming Mode / Low Latency
UPDATE parameters SET name_bg = 'Активно шумопотискане (ANC)' WHERE id = 5641; -- Active Noise Cancellation (ANC)
UPDATE parameters SET name_bg = 'Шумопотискане при разговори (ENC)' WHERE id = 5642; -- Call Noise Cancellation (ENC)
UPDATE parameters SET name_bg = 'Безжично зареждане' WHERE id = 5643; -- Wireless Charging
UPDATE parameters SET name_bg = 'Тип аудио система' WHERE id = 5645; -- Audio System Type
UPDATE parameters SET name_bg = 'Предназначение' WHERE id = 5646; -- Destination
UPDATE parameters SET name_bg = 'Форм фактор на микрофона' WHERE id = 5647; -- Microphone Form Factor
UPDATE parameters SET name_bg = 'Управление от кабела' WHERE id = 5648; -- In-Cord Control
UPDATE parameters SET name_bg = 'Честотна характеристика' WHERE id = 5649; -- Frequency Response
UPDATE parameters SET name_bg = 'Технология за свързаност' WHERE id = 5650; -- Connectivity Technology
UPDATE parameters SET name_bg = 'Стандартна консумация (Energy Star)' WHERE id = 5657; -- Standard power consumption (Energy Star)
UPDATE parameters SET name_bg = 'Живот на продукта' WHERE id = 5659; -- Life Time
UPDATE parameters SET name_bg = 'Стандартна консумация (EU Energy Label)' WHERE id = 5660; -- Standard power consumption (EU Energy Label)
UPDATE parameters SET name_bg = 'Тип сензиране' WHERE id = 5662; -- Sensing Type
UPDATE parameters SET name_bg = 'Сензорен порт' WHERE id = 5663; -- Touch Port
UPDATE parameters SET name_bg = 'Сензорна точка' WHERE id = 5665; -- Touch Point
UPDATE parameters SET name_bg = 'Мулти-тъч' WHERE id = 5667; -- Multi-touch
UPDATE parameters SET name_bg = 'eMMC памет' WHERE id = 5668; -- EMMC
UPDATE parameters SET name_bg = 'Wi-Fi стандарт' WHERE id = 5669; -- Wi-Fi Standard
UPDATE parameters SET name_bg = 'Основен Wi-Fi чипсет' WHERE id = 5670; -- Wi-Fi Main Chipset
UPDATE parameters SET name_bg = 'Сензорен' WHERE id = 5671; -- Touch
UPDATE parameters SET name_bg = 'Тип захранващ адаптер' WHERE id = 5672; -- Power Adatper Type
UPDATE parameters SET name_bg = 'Безжичен интерфейс' WHERE id = 5673; -- Wireless Interface
UPDATE parameters SET name_bg = 'Диапазон на цветна температура' WHERE id = 5674; -- Lamp Color Temperature Range
UPDATE parameters SET name_bg = 'Светлинен поток' WHERE id = 5676; -- Luminous Flux
UPDATE parameters SET name_bg = 'Материал на каишката' WHERE id = 5677; -- Strap Material
UPDATE parameters SET name_bg = 'Известия' WHERE id = 5679; -- Notifications
UPDATE parameters SET name_bg = 'Езици на фърмуера' WHERE id = 5681; -- Firmware Languages List
UPDATE parameters SET name_bg = 'Брава на вратата' WHERE id = 5688; -- Door Lock
UPDATE parameters SET name_bg = 'Цифрова клавиатура' WHERE id = 5689; -- Keypad
UPDATE parameters SET name_bg = 'Номер на сензора на изображението' WHERE id = 5691; -- Image Sensor Part number
UPDATE parameters SET name_bg = 'Комуникационен протокол' WHERE id = 5692; -- Communication Protocol
UPDATE parameters SET name_bg = 'Тип сокет' WHERE id = 5693; -- Socket Type
UPDATE parameters SET name_bg = 'Ъгъл на видимост' WHERE id = 5694; -- Viewing Angle
UPDATE parameters SET name_bg = 'Макс. брой свързани устройства' WHERE id = 5696; -- Max Number of Connected Devices
UPDATE parameters SET name_bg = 'Резолюция на основния видеопоток' WHERE id = 5697; -- Main Stream Video Resolution
UPDATE parameters SET name_bg = 'Антена ZigBee' WHERE id = 5698; -- Antenna ZigBee
UPDATE parameters SET name_bg = 'Макс. натоварващ ток' WHERE id = 5700; -- Max. Load Current
UPDATE parameters SET name_bg = 'Тип умно устройство' WHERE id = 5702; -- Smart Device Type
UPDATE parameters SET name_bg = 'Технология за свързване на умно устройство' WHERE id = 5703; -- Smart Device Connection Technology
UPDATE parameters SET name_bg = 'Работна влажност на средата' WHERE id = 5705; -- Operating Ambient Humidity
UPDATE parameters SET name_bg = 'Характеристики на умни устройства' WHERE id = 5706; -- Smart Devices Features
UPDATE parameters SET name_bg = 'LED комплект' WHERE id = 5708; -- LED Set
UPDATE parameters SET name_bg = 'Тип щепсел' WHERE id = 5710; -- Plug Type
UPDATE parameters SET name_bg = 'Предавателна мощност (dBm)' WHERE id = 5711; -- Transmitting Power (dBm)
UPDATE parameters SET name_bg = 'Работна честотна лента' WHERE id = 5712; -- Operating Frequency Band
UPDATE parameters SET name_bg = 'Хоризонтален ъгъл на видимост' WHERE id = 5713; -- Horizontal Viewing Angle
UPDATE parameters SET name_bg = 'Клетъчна мрежа' WHERE id = 5714; -- Cellular
UPDATE parameters SET name_bg = 'Тип интелигентна антена' WHERE id = 5715; -- Smart Antenna Type
UPDATE parameters SET name_bg = 'WEP/WPA2 криптиране' WHERE id = 5716; -- WEP/WPA2 Encryption
UPDATE parameters SET name_bg = 'Ъгъл на въртене' WHERE id = 5717; -- Rotation Angle
UPDATE parameters SET name_bg = 'Светлинна ефективност' WHERE id = 5721; -- Luminous Efficacy
UPDATE parameters SET name_bg = 'Индекс на цветопредаване (CRI)' WHERE id = 5722; -- Color Rendering Index
UPDATE parameters SET name_bg = 'Мощност (W)' WHERE id = 5723; -- Wattage
UPDATE parameters SET name_bg = 'Ъгъл на лъча' WHERE id = 5724; -- Beam Angle
UPDATE parameters SET name_bg = 'Цокъл' WHERE id = 5725; -- Cap (Base)
UPDATE parameters SET name_bg = 'Клас на енергийна ефективност' WHERE id = 5728; -- Energy Efficiency Class
UPDATE parameters SET name_bg = 'Тип форма' WHERE id = 5729; -- Shape Type
UPDATE parameters SET name_bg = 'Измерване на температура (обхват)' WHERE id = 5730; -- Temperature Detection (Range)
UPDATE parameters SET name_bg = 'Работно напрежение' WHERE id = 5732; -- Operating Voltage
UPDATE parameters SET name_bg = 'Активна страна' WHERE id = 5733; -- Active Side
UPDATE parameters SET name_bg = 'Литография' WHERE id = 5736; -- Lithography
UPDATE parameters SET name_bg = 'Капацитет на Optane памет' WHERE id = 5739; -- Optane Memory Capacity
UPDATE parameters SET name_bg = 'Конектор' WHERE id = 5743; -- Connector
UPDATE parameters SET name_bg = 'Скорост на пренос на данни' WHERE id = 5745; -- Data Transfer Rate
UPDATE parameters SET name_bg = 'Интерфейс' WHERE id = 5746; -- Interface
UPDATE parameters SET name_bg = 'Скорост на четене на Flash' WHERE id = 5748; -- Flash Memory Read Data Transfer Rate
UPDATE parameters SET name_bg = 'Скорост на запис на Flash' WHERE id = 5749; -- Flash Memory Write Data Transfer Rate
UPDATE parameters SET name_bg = 'Продължителност на живот' WHERE id = 5750; -- Lifetime
UPDATE parameters SET name_bg = 'Метод на задвижване' WHERE id = 5751; -- Driving Method
UPDATE parameters SET name_bg = 'Препоръчително тегло на детайла' WHERE id = 5752; -- Recommended Workpiece Weight
UPDATE parameters SET name_bg = 'Захватна сила (на захват)' WHERE id = 5753; -- Gripping Force (per jaw)
UPDATE parameters SET name_bg = 'Ход' WHERE id = 5754; -- Stroke
UPDATE parameters SET name_bg = 'Тип захват на роботизираната ръка' WHERE id = 5755; -- Type of Robot Arm Gripper
UPDATE parameters SET name_bg = 'Повторяемост (±)' WHERE id = 5756; -- Repeatability (±)
UPDATE parameters SET name_bg = 'Видове комуникация' WHERE id = 5758; -- Communication Types
UPDATE parameters SET name_bg = 'Операционна система на робота' WHERE id = 5759; -- Robot Operating System
UPDATE parameters SET name_bg = 'Става 1' WHERE id = 5760; -- Joint 1
UPDATE parameters SET name_bg = 'Става 4' WHERE id = 5761; -- Joint 4
UPDATE parameters SET name_bg = 'Става 5' WHERE id = 5762; -- Joint 5
UPDATE parameters SET name_bg = 'Става 2' WHERE id = 5763; -- Joint 2
UPDATE parameters SET name_bg = 'Става 3' WHERE id = 5764; -- Joint 3
UPDATE parameters SET name_bg = 'Максимален полезен товар' WHERE id = 5765; -- Maximum Payload
UPDATE parameters SET name_bg = 'Работен радиус' WHERE id = 5766; -- Working Radius
UPDATE parameters SET name_bg = 'Кобот рамо' WHERE id = 5767; -- Cobot Arm
UPDATE parameters SET name_bg = 'Максимален обхват' WHERE id = 5768; -- Maximum Reach
UPDATE parameters SET name_bg = 'Тип робот' WHERE id = 5769; -- Robot Type
UPDATE parameters SET name_bg = 'Интерфейс на крайния ефектор' WHERE id = 5770; -- End-Effector Interface
UPDATE parameters SET name_bg = 'Контролер' WHERE id = 5771; -- Controller
UPDATE parameters SET name_bg = 'Става 6' WHERE id = 5772; -- Joint 6
UPDATE parameters SET name_bg = 'Брой скорости' WHERE id = 5775; -- Number of Speed
UPDATE parameters SET name_bg = 'Статичен въртящ момент' WHERE id = 5776; -- Static Torque
UPDATE parameters SET name_bg = 'Повторна точност (въртене)(±)' WHERE id = 5777; -- Repeat Accuracy (swiveling)(±)
UPDATE parameters SET name_bg = 'Устойчив въртящ момент' WHERE id = 5778; -- Sustained Torque
UPDATE parameters SET name_bg = 'Обхват на въртене' WHERE id = 5779; -- Rotary Range
UPDATE parameters SET name_bg = 'Повторна точност (позиция)(±)' WHERE id = 5780; -- Repeat Accuracy (position)(±)
UPDATE parameters SET name_bg = 'Скорост на въртене (°/с)' WHERE id = 5781; -- Rotational Speed (deg/s)
UPDATE parameters SET name_bg = 'Линейна скорост' WHERE id = 5782; -- Linear Velocity
UPDATE parameters SET name_bg = 'Кабел за устройството за обучение' WHERE id = 5783; -- Cable Connecting the Teaching Remote Control
UPDATE parameters SET name_bg = 'Степени на свобода' WHERE id = 5784; -- Degrees of Freedom
UPDATE parameters SET name_bg = 'Програмни интерфейси' WHERE id = 5785; -- Programming Interfaces
UPDATE parameters SET name_bg = 'Управляваща кутия' WHERE id = 5786; -- Control Box
UPDATE parameters SET name_bg = 'Диаметър на монтажната повърхност' WHERE id = 5787; -- Mounting Surface Diameter
UPDATE parameters SET name_bg = 'Устройство за обучение' WHERE id = 5788; -- Teaching Remote Controller
UPDATE parameters SET name_bg = 'Роботизирана ръка' WHERE id = 5789; -- Robotic Arm
UPDATE parameters SET name_bg = 'Край на инструмента' WHERE id = 5790; -- Tool End
UPDATE parameters SET name_bg = 'Модел на устройството за обучение' WHERE id = 5791; -- Teaching Remote Controller Model Name
UPDATE parameters SET name_bg = 'Ориентация при монтаж' WHERE id = 5792; -- Installation Orientation
UPDATE parameters SET name_bg = 'Модел на управляващата кутия' WHERE id = 5793; -- Control Box Model Name
UPDATE parameters SET name_bg = 'Кабел за свързване на робота' WHERE id = 5794; -- Cable Connecting the Robot
UPDATE parameters SET name_bg = 'Робот' WHERE id = 5795; -- Robot
UPDATE parameters SET name_bg = 'Вътрешна скорост на данните' WHERE id = 5797; -- Internal Data Bit Rate
UPDATE parameters SET name_bg = 'Вътрешен канал за данни' WHERE id = 5798; -- Data Channel Internal
UPDATE parameters SET name_bg = 'Скорост на въртене' WHERE id = 5799; -- Rotational Speed
UPDATE parameters SET name_bg = 'Интерфейси' WHERE id = 5800; -- Interfaces
UPDATE parameters SET name_bg = 'Номинално захранващо напрежение' WHERE id = 5801; -- Nominal Supply Voltage
UPDATE parameters SET name_bg = 'Брой модули памет' WHERE id = 5802; -- Memory Modules Quantity
UPDATE parameters SET name_bg = 'Технология на паметта' WHERE id = 5803; -- Memory Technology
UPDATE parameters SET name_bg = 'CAS латентност' WHERE id = 5804; -- CAS Latency
UPDATE parameters SET name_bg = 'Минимална входна честота' WHERE id = 5805; -- Minimum Input Frequency
UPDATE parameters SET name_bg = 'Обработка на екрана' WHERE id = 5806; -- Screen Treatment
UPDATE parameters SET name_bg = 'Диагонал' WHERE id = 5807; -- Diagonal Length
UPDATE parameters SET name_bg = 'Съотношение на страните' WHERE id = 5808; -- Image Aspect Ratio
UPDATE parameters SET name_bg = 'Тип дисплей' WHERE id = 5809; -- Display Type
UPDATE parameters SET name_bg = 'Контраст на изображението' WHERE id = 5810; -- Image Contrast
UPDATE parameters SET name_bg = 'Макс. работна влажност' WHERE id = 5811; -- Maximum Operating Humidity
UPDATE parameters SET name_bg = 'Мин. работна влажност' WHERE id = 5812; -- Minimum Operating Humidity
UPDATE parameters SET name_bg = 'Поддържани стандарти за монитор' WHERE id = 5813; -- Monitor Compliant Standards
UPDATE parameters SET name_bg = 'Тип стъпка' WHERE id = 5814; -- Pitch Type
UPDATE parameters SET name_bg = 'Максимално входно напрежение' WHERE id = 5815; -- Maximum Input Voltage
UPDATE parameters SET name_bg = 'Видим диагонал на дисплея' WHERE id = 5816; -- Viewable Display Diagonal Length
UPDATE parameters SET name_bg = 'Макс. вертикален ъгъл на видимост' WHERE id = 5817; -- Image Max. V-View Angle
UPDATE parameters SET name_bg = 'Макс. хоризонтален ъгъл на видимост' WHERE id = 5818; -- Image Max. H-View Angle
UPDATE parameters SET name_bg = 'Консумация в режим на изчакване/заспиване' WHERE id = 5819; -- Power Consumption Stand By/Sleep
UPDATE parameters SET name_bg = 'Поддръжка на цветове' WHERE id = 5820; -- Color Support
UPDATE parameters SET name_bg = 'Време за реакция' WHERE id = 5821; -- Response Time
UPDATE parameters SET name_bg = 'Плътност на пикселите' WHERE id = 5822; -- Pixel Density
UPDATE parameters SET name_bg = 'Максимална входна честота' WHERE id = 5823; -- Maximum Input Frequency
UPDATE parameters SET name_bg = 'Минимално входно напрежение' WHERE id = 5824; -- Minimum Input Voltage
UPDATE parameters SET name_bg = 'Функции за сигурност' WHERE id = 5825; -- Security Features
UPDATE parameters SET name_bg = 'Ширина на стъпката' WHERE id = 5826; -- Pitch Width
UPDATE parameters SET name_bg = 'Максимална консумация на мощност' WHERE id = 5827; -- Maximum Power Consumption
UPDATE parameters SET name_bg = 'Максимална резолюция' WHERE id = 5828; -- Maximum Resolution
UPDATE parameters SET name_bg = 'Тип матрица' WHERE id = 5829; -- Type of Matrix
UPDATE parameters SET name_bg = 'Макс. честота при макс. резолюция' WHERE id = 5830; -- Maximum Video Refresh Rate @ Max. Resolution
UPDATE parameters SET name_bg = 'Интерфейс за монтаж на плосък дисплей' WHERE id = 5831; -- Flat Display Mounting Interface
UPDATE parameters SET name_bg = 'Консумация при работа' WHERE id = 5832; -- Power Consumption Operational
UPDATE parameters SET name_bg = 'Яркост на изображението' WHERE id = 5833; -- Image Brightness
UPDATE parameters SET name_bg = 'Динамично съотношение на контраста' WHERE id = 5834; -- Dynamic Contrast Ratio
UPDATE parameters SET name_bg = 'Многоканален преглед' WHERE id = 5835; -- Multi-Channel Preview

