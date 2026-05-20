-- Fix Asbis category name_bg — replaces English names with Bulgarian translations
-- Run AFTER syncAsbisCategories() has completed.
-- Only updates name_bg; name_en and slug remain in English (slug stability).

UPDATE categories SET name_bg = 'Други'                                        WHERE name_bg = 'Other'                                              AND platform = 'ASBIS';

-- ── Children of "Други" (Other) ───────────────────────────────────────────────
UPDATE categories SET name_bg = 'Мрежово - Cloud Key Enterprise'               WHERE name_bg = 'Networking - Cloud Keys & Gateways - Cloud Key Enterprise' AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Сигурност - Видеорекордер'                    WHERE name_bg = 'Security - Surveillance Video Recorder'              AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Антена'                                        WHERE name_bg = 'Antenna'                                            AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Мрежово - Контрол на достъп'                  WHERE name_bg = 'Networking - Access Control'                        AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Аксесоари за сценична употреба'               WHERE name_bg = 'Accessories for Staged'                             AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Мултимедия - Wi-Fi говорител'                 WHERE name_bg = 'Multimedia - Speaker Wi-Fi'                         AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Аксесоари OTG'                                WHERE name_bg = 'Accessories for OTG'                                AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'AI рекордер'                                  WHERE name_bg = 'AI Recorder'                                        AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Видеоконферентни слушалки'                    WHERE name_bg = 'VC Headsets'                                        AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Реновиран лаптоп'                             WHERE name_bg = '2nd life PC Notebook Commercial'                    AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Робот прахосмукачка'                          WHERE name_bg = 'Vacuum Cleaner Robot'                               AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Гейминг педали за рейсинг'                    WHERE name_bg = 'Gaming Racing Pedals'                               AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Готово за носене'                             WHERE name_bg = 'Ready to wear'                                      AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Слънчев панел'                                WHERE name_bg = 'Solar Panel'                                        AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Гейминг волан за рейсинг'                     WHERE name_bg = 'Gaming Racing Wheel'                                AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Прахосмукачка прътова'                        WHERE name_bg = 'Vacuum Cleaner Stick'                               AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Видеоконферентни докинг станции'              WHERE name_bg = 'VC Docks'                                           AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Гейминг база за волан'                        WHERE name_bg = 'Gaming Wheel Base'                                  AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Производствени материали'                     WHERE name_bg = 'Production Material'                                AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Настолен NAS'                                 WHERE name_bg = 'Desktop NAS'                                        AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Прахосмукачка трансформер'                    WHERE name_bg = 'Vacuum Cleaner Transformer'                         AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Модни аксесоари'                              WHERE name_bg = 'Fashion Accessories'                                AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Мини PC'                                      WHERE name_bg = 'PC Mini'                                            AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Смарт тракер'                                 WHERE name_bg = 'Smart Tracker'                                      AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Гейминг симрейсинг аксесоари'                 WHERE name_bg = 'Gaming Simracing Accessories'                       AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Аксесоари за флексибилни устройства'          WHERE name_bg = 'Accessories for Flexible'                           AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Гейминг монитор'                              WHERE name_bg = 'Gaming Monitor'                                     AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Удължена гаранция'                            WHERE name_bg = 'Extended Warranty'                                  AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Смарт вътрешна сирена'                        WHERE name_bg = 'Smart Indoor Siren'                                 AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Гейминг ръчна спирачка'                       WHERE name_bg = 'Gaming Handbrake'                                   AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Аксесоари за вакуумзатваряне'                 WHERE name_bg = 'Acc - Vacuum sealer'                                AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Гейминг скоростен лост'                       WHERE name_bg = 'Gaming Shifter'                                     AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Видеоконферентни уеб камери'                  WHERE name_bg = 'VC WebCams'                                         AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Иригатори'                                    WHERE name_bg = 'Irrigators'                                         AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Гейминг симрейсинг кокпит'                    WHERE name_bg = 'Gaming Simracing Cockpit'                           AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Видеоконферентни услуги'                      WHERE name_bg = 'VC Services'                                        AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Сигурност - Комплект за видеонаблюдение'      WHERE name_bg = 'Security - Surveillance Kit'                        AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Тенджери под налягане'                        WHERE name_bg = 'Pressure Cookers'                                   AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Блендер'                                      WHERE name_bg = 'Liquidaiser'                                        AND platform = 'ASBIS';

-- ── Children of "Аксесоари" ──────────────────────────────────────────────────
UPDATE categories SET name_bg = 'Аксесоари за прахосмукачки'                   WHERE name_bg = 'Acc - Vacuum Cleaners'                              AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Аксесоари за грижа за зъби'                   WHERE name_bg = 'Acc - Dental care'                                  AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Аксесоари за системи за видеонаблюдение'      WHERE name_bg = 'Security - Surveillance System Accessories'         AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Аксесоари за пречиствателни устройства'       WHERE name_bg = 'Acc - Air Purifiers'                                AND platform = 'ASBIS';

-- ── Children of "Мултимедиен хардуер" ────────────────────────────────────────
UPDATE categories SET name_bg = 'Мултимедия - Саундбар'                        WHERE name_bg = 'Multimedia - Soundbar'                              AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Мултимедия - Субуфер'                         WHERE name_bg = 'Multimedia - Subwoofer'                             AND platform = 'ASBIS';

-- ── Children of "Мрежови продукти" ───────────────────────────────────────────
UPDATE categories SET name_bg = 'Разширител на обхват'                         WHERE name_bg = 'Range Extender'                                     AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Мрежов хардуер'                               WHERE name_bg = 'Networking - Хардуеър'                              AND platform = 'ASBIS';

-- ── Children of "Софтуеър" ────────────────────────────────────────────────────
UPDATE categories SET name_bg = 'Софт OEM - MS Server медия CD'                WHERE name_bg = 'Soft OEM. MS Server media CD'                       AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Приложен лиценз (Сървърен)'                   WHERE name_bg = 'Application ( Server ) License'                     AND platform = 'ASBIS';

-- ── Children of "Настолни РС" ─────────────────────────────────────────────────
UPDATE categories SET name_bg = 'Компютър всичко в едно'                       WHERE name_bg = 'PC all in one'                                      AND platform = 'ASBIS';

-- ── Children of "Памет" ───────────────────────────────────────────────────────
UPDATE categories SET name_bg = 'Памет гейминг настолна'                       WHERE name_bg = 'Memory Gaming Desktop'                              AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Памет гейминг мобилна'                        WHERE name_bg = 'Memory Gaming Mobile'                               AND platform = 'ASBIS';

-- ── Children of "Игри и мултимедия" ──────────────────────────────────────────
UPDATE categories SET name_bg = 'Гейминг подложки за мишка'                    WHERE name_bg = 'Gaming Mousepads'                                   AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Гейминг бюро'                                 WHERE name_bg = 'Gaming Desk'                                        AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Гейминг микрофон'                             WHERE name_bg = 'Gaming Microphone'                                  AND platform = 'ASBIS';

-- ── Children of "Дребни домакински уреди" ────────────────────────────────────
UPDATE categories SET name_bg = 'Съдове за напитки'                            WHERE name_bg = 'Drinkware'                                          AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Готварски съдове от чугун'                    WHERE name_bg = 'Cast Iron Cookware'                                 AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Контейнери за съхранение на храна'            WHERE name_bg = 'Food Storage Container'                             AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Смарт сешоар'                                 WHERE name_bg = 'Smart Hairdryer'                                    AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Готварски съдове от алуминий'                 WHERE name_bg = 'Cast Aluminum Cookware'                             AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Стилери за коса'                              WHERE name_bg = 'Hair Stylers'                                       AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Аксесоари за мултипекари/грилове'             WHERE name_bg = 'Acc - Multibakers/Grills'                           AND platform = 'ASBIS';

-- ── Children of "Аидио устройства" ───────────────────────────────────────────
UPDATE categories SET name_bg = 'Мултимедия - PC слушалки'                     WHERE name_bg = 'Multimedia - PC Headsets'                           AND platform = 'ASBIS';

-- ── Children of "Смарт устройства" ───────────────────────────────────────────
UPDATE categories SET name_bg = 'Детски часовник'                              WHERE name_bg = 'Kids Watch'                                         AND platform = 'ASBIS';

-- ── Digital signage ───────────────────────────────────────────────────────────
UPDATE categories SET name_bg = 'Дигитална сигнализация IDS'                   WHERE name_bg = 'Digital signage IDS'                                AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Digital signage за вътрешна употреба'         WHERE name_bg = 'Digital signage за вътрешна употреба'               AND platform = 'ASBIS';

-- ── Optane ────────────────────────────────────────────────────────────────────
UPDATE categories SET name_bg = 'SSD сървърен Optane'                          WHERE name_bg = 'SSD Server Optane'                                  AND platform = 'ASBIS';

-- ── Robotic Solutions ─────────────────────────────────────────────────────────
UPDATE categories SET name_bg = 'Роботизирани решения'                         WHERE name_bg = 'Robotic Solutions'                                  AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Роботизирани манипулатори'                    WHERE name_bg = 'Robot Arm Grippers'                                 AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Роботизирани ръце'                            WHERE name_bg = 'Robotic Arms'                                       AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Робот почистващ'                              WHERE name_bg = 'Robot-Cleaner'                                      AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Кухненски робот'                              WHERE name_bg = 'Robot Kitchen'                                      AND platform = 'ASBIS';
UPDATE categories SET name_bg = 'Робот за доставки'                            WHERE name_bg = 'Robot for Deliveries'                               AND platform = 'ASBIS';
