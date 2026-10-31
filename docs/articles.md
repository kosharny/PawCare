# Локальные статьи PawCare

Добавлены шесть авторских материалов с практическими примерами. Каждый полностью доступен на en/ru/uk/pl, содержит примерно 420–560 слов, обложку, внутреннюю иллюстрацию, 6–7 смысловых разделов, выделения, списки и отдельный совет. Переводы адаптированы к языку; id и изображения общие.

| id | Тема | Обложка | Внутреннее изображение |
|---|---|---|---|
| `sniff-walks` | Прогулка глазами собаки | `article_sniff_walk` | `article_walk_pause` |
| `body-language` | О чём собака говорит без слов | `article_body_language` | `article_give_space` |
| `cooperative-care` | Уход без спешки | `article_cooperative_care` | `article_care_setup` |
| `home-enrichment` | Дом, в котором есть что открыть | `article_home_enrichment` | `article_toy_rotation` |
| `quiet-rest` | Тихий уголок для спокойного дня | `article_quiet_rest` | `article_rest_corner` |
| `car-travel` | В дорогу без суеты | `article_car_travel` | `article_travel_kit` |

Первый материал выбран редакционно для блока рекомендации на «Сегодня». Он открывается через тот же стабильный id, что и карточка во вкладке. Рекомендация не основана на медицинском профилировании или отправке пользовательских данных.

JSON-пакеты лежат в `app/src/main/assets/articles/`. `BundledArticlesRepository` читает их на IO dispatcher, проверяет id и основные поля, кеширует по языку. Неподдерживаемая системная локаль получает английский пакет. Room и пользовательские записи не изменяются. Версии зависимостей сохранены.

Разметка — структурированные блоки `heading`, `paragraph`, `bullet`, `callout`, `image`. В текстовых блоках `**...**` означает полужирное выделение. Composable отображает native Text/AnnotatedString, а не HTML/WebView. Читатель может выделять текст; ссылки на источники открываются внешним браузером. Добавление статьи требует обновить все четыре пакета и ресурсы изображений.

У списка и деталей отдельные screen-scoped ViewModel, loading/error/retry/not-found состояния. Идентификатор открытой статьи сохраняется в SavedStateHandle и типизированном NavKey. Прокрутку сохраняет LazyColumn совместно с Navigation 3 saveable-state decorator. Локаль из Configuration запускает обновление контента.

## Редакционные источники

Проверены 25.10.2026. Это дополнительное чтение и сверка общих принципов; тексты и примеры написаны для PawCare. Исходные фотографии и иллюстрации организаций не используются, одобрение статьи этими организациями не заявляется.

- [Blue Cross — Enrichment for dogs](https://www.bluecross.org.uk/advice/dog/enrichment-for-dogs)
- [Blue Cross — Scent work for dogs](https://www.bluecross.org.uk/advice/dog/behaviour-and-training/scent-work-for-dogs)
- [Dogs Trust — Dog walker guidelines](https://www.dogstrust.org.uk/downloads/dog-walker-guidelines-2022.pdf)
- [Dogs Trust — Body language](https://www.dogstrust.org.uk/dog-advice/understanding-your-dog/body-language)
- [RSPCA — Understanding dog behaviour](https://www.rspca.org.uk/adviceandwelfare/pets/dogs/behaviour/understanding)
- [Dogs Trust — Handling your dog](https://www.dogstrust.org.uk/dog-advice/health-wellbeing/at-home/how-to-handle-your-dog)
- [Canine Partners — Consent-based handling](https://caninepartners.org.uk/blogs/consent-based-handling/)
- [RSPCA — Grooming](https://www.rspca.org.uk/adviceandwelfare/pets/dogs/health/grooming)

## Изображения

Встроены двенадцать сгенерированных изображений в едином стиле тёплой редакционной фотографии: шесть обложек и шесть внутренних сцен. Все WebP находятся в `app/src/main/res/drawable-nodpi/`, имеют размер 1600 × 900 px, профиль sRGB и не содержат прозрачности. Общий объём — около 3,04 МиБ. Имена ресурсов и локализованные описания сохранены. Неизменённые PNG-исходники и прежние XML хранятся в `artwork/articles/sources/` и `artwork/articles/previous-vectors/`; [промпты и параметры экспорта](../artwork/articles/generation.json) позволяют воспроизвести подготовку серии. Подробные исходные сцены описаны в [article-image-briefs-ru.txt](article-image-briefs-ru.txt).

## Расширение библиотеки и порядок вкладок

Добавлены ещё три материала на всех четырёх языках; исходные три сохранены. Новые темы: домашние занятия, место для отдыха и подготовка к автомобильной поездке. У каждого — собственные обложка и встроенная иллюстрация, подписи, выделения и источники. Всего 6 статей, 24 языковые версии и 12 изображений.

Порядок вкладок: «Сегодня» → «Календарь» → «Полезное» → «Питомец». HomeTab определяет порядок независимо от устойчивого id. Старые id 2 для профиля и 3 для статей сохранены, вместе с saveable-state ключами и переходом «Все статьи».

Дополнительные редакционные источники:

- [Dogs Trust — Enrichment activities](https://www.dogstrust.org.uk/dog-advice/life-with-your-dog/enrichment/enrichment-activities-for-dogs)
- [Dogs Trust — Helping your dog settle](https://www.dogstrust.org.uk/dog-advice/training/unwanted-behaviours/hyperactive-and-restlessness)
- [Battersea — Calm and relaxed](https://www.battersea.org.uk/pet-advice/dog-advice/my-dog-wont-settle-teaching-your-dog-be-calm-and-relaxed)
- [Dogs Trust — Living happily together](https://www.dogstrust.org.uk/downloads/living-happily-together-guide-2024.pdf)
- [Dogs Trust — Car travel](https://www.dogstrust.org.uk/dog-advice/training/outdoors/car-travel)
- [Blue Cross — Car travel](https://www.bluecross.org.uk/advice/dog/travelling-with-your-dog-by-car)
- [Blue Cross — Moving house and travelling](https://www.bluecross.org.uk/advice/dog/moving-house-and-travelling-with-dogs)
