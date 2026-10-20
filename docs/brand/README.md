# Бренд KitapRadar

<p>
  <img src="logo-full.svg" alt="KitapRadar — светлая версия" width="320">
</p>

**Идея знака:** открытая книга, над корешком расходятся волны сигнала. Книга — каталог изданий, волны — «маячок», который следит за ценой и сообщает о снижении.

## Файлы

| Файл | Назначение |
|---|---|
| `logo-full.svg` | Основной логотип (знак + надпись) для светлого фона |
| `logo-full-dark.svg` | Логотип для тёмного фона |
| `logo-mark.svg` | Только знак, светлый фон |
| `logo-mark-dark.svg` | Только знак, тёмный фон |
| `favicon.svg` | Иконка сайта (вкладка браузера, 32 px и больше) |
| `favicon-16.svg` | Упрощённая иконка для 16 px (страницы залиты, одна волна) |
| `png/favicon-16.png`, `png/icon-32.png` | Растровые favicon |
| `png/apple-touch-icon.png` | Иконка для iOS (180×180) |
| `png/icon-192.png`, `png/icon-512.png` | Иконки PWA / manifest |
| `png/logo-full.png` | Логотип в высоком разрешении для документов и презентаций |
| `social-preview.svg`, `png/social-preview.png` | Превью репозитория на GitHub и для ссылок (1280×640) |

Надпись в SVG переведена в кривые — логотип выглядит одинаково везде, установка шрифта не нужна.

## Цвета

| Роль | Светлый фон | Тёмный фон |
|---|---|---|
| Основной (книга, «Kitap») | `#0E1A2B` | `#FFFFFF` |
| Акцент (волны, «Radar») | `#C2410C` | `#F59E5B` |
| Фон | `#F4F6F8` / `#FFFFFF` | `#0E1A2B` |

## Шрифты
- Надпись логотипа — **Unbounded Bold** (в SVG уже кривые).
- Интерфейс — Unbounded (заголовки) и **Onest** (текст), оба с кириллицей, Google Fonts.

## Правила использования
- Минимальный размер: знак — 16 px (только `favicon-16`), полный логотип — 120 px по ширине.
- Свободное поле вокруг логотипа — не меньше высоты волн знака.
- Не менять цвета частей, не растягивать, не добавлять тени и обводки, не поворачивать.
- На фото и сложном фоне — только на однотонной подложке.

## Подключение во frontend (веха F1)
1. Скопировать `favicon.svg`, `png/favicon-16.png`, `png/icon-32.png`, `png/apple-touch-icon.png`, `png/icon-192.png`, `png/icon-512.png` в `frontend/public/`.
2. В `frontend/index.html`:
   ```html
   <link rel="icon" href="/favicon.svg" type="image/svg+xml">
   <link rel="icon" href="/icon-32.png" sizes="32x32" type="image/png">
   <link rel="icon" href="/favicon-16.png" sizes="16x16" type="image/png">
   <link rel="apple-touch-icon" href="/apple-touch-icon.png">
   <meta name="theme-color" content="#0E1A2B">
   ```
3. Логотип в шапке сайта — `logo-full.svg` как React-компонент или `<img>`, `alt="KitapRadar"`.

## Превью репозитория на GitHub
GitHub → репозиторий → **Settings → General → Social preview → Edit** → загрузить `png/social-preview.png`.
