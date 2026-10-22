-- Demo catalog: generated, deterministic. Prices are test values, not real store prices.
-- Used by integration tests and by DevDataLoader (app.dev.seed=true).

WITH book AS (
    INSERT INTO books (title, author, description, title_norm, author_norm)
    VALUES ('Мастер и Маргарита', 'Михаил Булгаков', 'Роман о визите дьявола в Москву 1930-х годов.', 'мастер и маргарита', 'михаил булгаков')
    RETURNING id
)
INSERT INTO editions (book_id, isbn13, publisher, year, binding, language, pages, cover_url)
SELECT book.id, v.isbn13, v.publisher, v.year, v.binding, v.language, v.pages, v.cover_url
FROM book, (VALUES
    ('9785601000017', 'АСТ', 2023, 'HARDCOVER', 'ru', 480, 'https://covers.example.com/9785601000017.jpg'),
    ('9785601000024', 'Азбука', 2022, 'PAPERBACK', 'ru', 512, 'https://covers.example.com/9785601000024.jpg'),
    ('9785601000031', 'Эксмо', 2021, 'HARDCOVER', 'ru', 448, 'https://covers.example.com/9785601000031.jpg')
) AS v (isbn13, publisher, year, binding, language, pages, cover_url);

WITH book AS (
    INSERT INTO books (title, author, description, title_norm, author_norm)
    VALUES ('Собачье сердце', 'Михаил Булгаков', 'Повесть об опыте профессора Преображенского.', 'собачье сердце', 'михаил булгаков')
    RETURNING id
)
INSERT INTO editions (book_id, isbn13, publisher, year, binding, language, pages, cover_url)
SELECT book.id, v.isbn13, v.publisher, v.year, v.binding, v.language, v.pages, v.cover_url
FROM book, (VALUES
    ('9785601000048', 'АСТ', 2022, 'PAPERBACK', 'ru', 160, 'https://covers.example.com/9785601000048.jpg')
) AS v (isbn13, publisher, year, binding, language, pages, cover_url);

WITH book AS (
    INSERT INTO books (title, author, description, title_norm, author_norm)
    VALUES ('Преступление и наказание', 'Фёдор Достоевский', 'Роман о бедном студенте Родионе Раскольникове.', 'преступление и наказание', 'федор достоевский')
    RETURNING id
)
INSERT INTO editions (book_id, isbn13, publisher, year, binding, language, pages, cover_url)
SELECT book.id, v.isbn13, v.publisher, v.year, v.binding, v.language, v.pages, v.cover_url
FROM book, (VALUES
    ('9785601000055', 'Эксмо', 2023, 'HARDCOVER', 'ru', 608, 'https://covers.example.com/9785601000055.jpg'),
    ('9785601000062', 'Азбука', 2021, 'PAPERBACK', 'ru', 672, 'https://covers.example.com/9785601000062.jpg')
) AS v (isbn13, publisher, year, binding, language, pages, cover_url);

WITH book AS (
    INSERT INTO books (title, author, description, title_norm, author_norm)
    VALUES ('Идиот', 'Фёдор Достоевский', 'Роман о князе Мышкине.', 'идиот', 'федор достоевский')
    RETURNING id
)
INSERT INTO editions (book_id, isbn13, publisher, year, binding, language, pages, cover_url)
SELECT book.id, v.isbn13, v.publisher, v.year, v.binding, v.language, v.pages, v.cover_url
FROM book, (VALUES
    ('9785601000079', 'АСТ', 2022, 'HARDCOVER', 'ru', 640, 'https://covers.example.com/9785601000079.jpg'),
    ('9785601000086', 'Азбука', 2023, 'PAPERBACK', 'ru', 704, 'https://covers.example.com/9785601000086.jpg')
) AS v (isbn13, publisher, year, binding, language, pages, cover_url);

WITH book AS (
    INSERT INTO books (title, author, description, title_norm, author_norm)
    VALUES ('Война и мир', 'Лев Толстой', 'Роман-эпопея о русском обществе в эпоху наполеоновских войн.', 'война и мир', 'лев толстой')
    RETURNING id
)
INSERT INTO editions (book_id, isbn13, publisher, year, binding, language, pages, cover_url)
SELECT book.id, v.isbn13, v.publisher, v.year, v.binding, v.language, v.pages, v.cover_url
FROM book, (VALUES
    ('9785601000093', 'Эксмо', 2022, 'HARDCOVER', 'ru', 1360, 'https://covers.example.com/9785601000093.jpg'),
    ('9785601000109', 'АСТ', 2020, 'PAPERBACK', 'ru', 1408, 'https://covers.example.com/9785601000109.jpg')
) AS v (isbn13, publisher, year, binding, language, pages, cover_url);

WITH book AS (
    INSERT INTO books (title, author, description, title_norm, author_norm)
    VALUES ('Анна Каренина', 'Лев Толстой', 'Роман о трагической любви замужней дамы.', 'анна каренина', 'лев толстой')
    RETURNING id
)
INSERT INTO editions (book_id, isbn13, publisher, year, binding, language, pages, cover_url)
SELECT book.id, v.isbn13, v.publisher, v.year, v.binding, v.language, v.pages, v.cover_url
FROM book, (VALUES
    ('9785601000116', 'Азбука', 2023, 'HARDCOVER', 'ru', 864, 'https://covers.example.com/9785601000116.jpg')
) AS v (isbn13, publisher, year, binding, language, pages, cover_url);

WITH book AS (
    INSERT INTO books (title, author, description, title_norm, author_norm)
    VALUES ('Евгений Онегин', 'Александр Пушкин', 'Роман в стихах.', 'евгений онегин', 'александр пушкин')
    RETURNING id
)
INSERT INTO editions (book_id, isbn13, publisher, year, binding, language, pages, cover_url)
SELECT book.id, v.isbn13, v.publisher, v.year, v.binding, v.language, v.pages, v.cover_url
FROM book, (VALUES
    ('9785601000123', 'Махаон', 2022, 'HARDCOVER', 'ru', 288, 'https://covers.example.com/9785601000123.jpg')
) AS v (isbn13, publisher, year, binding, language, pages, cover_url);

WITH book AS (
    INSERT INTO books (title, author, description, title_norm, author_norm)
    VALUES ('Капитанская дочка', 'Александр Пушкин', 'Исторический роман о пугачёвском восстании.', 'капитанская дочка', 'александр пушкин')
    RETURNING id
)
INSERT INTO editions (book_id, isbn13, publisher, year, binding, language, pages, cover_url)
SELECT book.id, v.isbn13, v.publisher, v.year, v.binding, v.language, v.pages, v.cover_url
FROM book, (VALUES
    ('9785601000130', 'АСТ', 2021, 'PAPERBACK', 'ru', 224, 'https://covers.example.com/9785601000130.jpg')
) AS v (isbn13, publisher, year, binding, language, pages, cover_url);

WITH book AS (
    INSERT INTO books (title, author, description, title_norm, author_norm)
    VALUES ('Мёртвые души', 'Николай Гоголь', 'Поэма о похождениях Чичикова.', 'мертвые души', 'николай гоголь')
    RETURNING id
)
INSERT INTO editions (book_id, isbn13, publisher, year, binding, language, pages, cover_url)
SELECT book.id, v.isbn13, v.publisher, v.year, v.binding, v.language, v.pages, v.cover_url
FROM book, (VALUES
    ('9785601000147', 'Эксмо', 2022, 'PAPERBACK', 'ru', 384, 'https://covers.example.com/9785601000147.jpg')
) AS v (isbn13, publisher, year, binding, language, pages, cover_url);

WITH book AS (
    INSERT INTO books (title, author, description, title_norm, author_norm)
    VALUES ('Отцы и дети', 'Иван Тургенев', 'Роман о конфликте поколений.', 'отцы и дети', 'иван тургенев')
    RETURNING id
)
INSERT INTO editions (book_id, isbn13, publisher, year, binding, language, pages, cover_url)
SELECT book.id, v.isbn13, v.publisher, v.year, v.binding, v.language, v.pages, v.cover_url
FROM book, (VALUES
    ('9785601000154', 'Азбука', 2021, 'PAPERBACK', 'ru', 256, 'https://covers.example.com/9785601000154.jpg')
) AS v (isbn13, publisher, year, binding, language, pages, cover_url);

WITH book AS (
    INSERT INTO books (title, author, description, title_norm, author_norm)
    VALUES ('Путь Абая', 'Мухтар Ауэзов', 'Роман-эпопея о жизни Абая Кунанбаева.', 'путь абая', 'мухтар ауэзов')
    RETURNING id
)
INSERT INTO editions (book_id, isbn13, publisher, year, binding, language, pages, cover_url)
SELECT book.id, v.isbn13, v.publisher, v.year, v.binding, v.language, v.pages, v.cover_url
FROM book, (VALUES
    ('9785601000161', 'Фолиант', 2022, 'HARDCOVER', 'ru', 1024, 'https://covers.example.com/9785601000161.jpg'),
    ('9785601000178', 'Жибек жолы', 2020, 'HARDCOVER', 'ru', 960, 'https://covers.example.com/9785601000178.jpg')
) AS v (isbn13, publisher, year, binding, language, pages, cover_url);

WITH book AS (
    INSERT INTO books (title, author, description, title_norm, author_norm)
    VALUES ('Абай жолы', 'Мұхтар Әуезов', 'Абай Құнанбайұлының өмірі туралы роман-эпопея.', 'абай жолы', 'мұхтар әуезов')
    RETURNING id
)
INSERT INTO editions (book_id, isbn13, publisher, year, binding, language, pages, cover_url)
SELECT book.id, v.isbn13, v.publisher, v.year, v.binding, v.language, v.pages, v.cover_url
FROM book, (VALUES
    ('9785601000185', 'Жазушы', 2021, 'HARDCOVER', 'kk', 1100, 'https://covers.example.com/9785601000185.jpg')
) AS v (isbn13, publisher, year, binding, language, pages, cover_url);

WITH book AS (
    INSERT INTO books (title, author, description, title_norm, author_norm)
    VALUES ('Қара сөздер', 'Абай Құнанбайұлы', 'Абайдың философиялық прозалық шығармалары.', 'қара сөздер', 'абай құнанбайұлы')
    RETURNING id
)
INSERT INTO editions (book_id, isbn13, publisher, year, binding, language, pages, cover_url)
SELECT book.id, v.isbn13, v.publisher, v.year, v.binding, v.language, v.pages, v.cover_url
FROM book, (VALUES
    ('9785601000192', 'Фолиант', 2023, 'PAPERBACK', 'kk', 176, 'https://covers.example.com/9785601000192.jpg')
) AS v (isbn13, publisher, year, binding, language, pages, cover_url);

WITH book AS (
    INSERT INTO books (title, author, description, title_norm, author_norm)
    VALUES ('Кочевники', 'Ильяс Есенберлин', 'Историческая трилогия о Казахском ханстве.', 'кочевники', 'ильяс есенберлин')
    RETURNING id
)
INSERT INTO editions (book_id, isbn13, publisher, year, binding, language, pages, cover_url)
SELECT book.id, v.isbn13, v.publisher, v.year, v.binding, v.language, v.pages, v.cover_url
FROM book, (VALUES
    ('9785601000208', 'Фолиант', 2021, 'HARDCOVER', 'ru', 896, 'https://covers.example.com/9785601000208.jpg')
) AS v (isbn13, publisher, year, binding, language, pages, cover_url);

WITH book AS (
    INSERT INTO books (title, author, description, title_norm, author_norm)
    VALUES ('1984', 'Джордж Оруэлл', 'Антиутопия о тоталитарном государстве.', '1984', 'джордж оруэлл')
    RETURNING id
)
INSERT INTO editions (book_id, isbn13, publisher, year, binding, language, pages, cover_url)
SELECT book.id, v.isbn13, v.publisher, v.year, v.binding, v.language, v.pages, v.cover_url
FROM book, (VALUES
    ('9785601000215', 'АСТ', 2023, 'PAPERBACK', 'ru', 320, 'https://covers.example.com/9785601000215.jpg'),
    ('9785601000222', 'Эксмо', 2022, 'HARDCOVER', 'ru', 352, 'https://covers.example.com/9785601000222.jpg')
) AS v (isbn13, publisher, year, binding, language, pages, cover_url);

WITH book AS (
    INSERT INTO books (title, author, description, title_norm, author_norm)
    VALUES ('Скотный двор', 'Джордж Оруэлл', 'Сатирическая повесть-притча.', 'скотный двор', 'джордж оруэлл')
    RETURNING id
)
INSERT INTO editions (book_id, isbn13, publisher, year, binding, language, pages, cover_url)
SELECT book.id, v.isbn13, v.publisher, v.year, v.binding, v.language, v.pages, v.cover_url
FROM book, (VALUES
    ('9785601000239', 'АСТ', 2022, 'PAPERBACK', 'ru', 128, 'https://covers.example.com/9785601000239.jpg')
) AS v (isbn13, publisher, year, binding, language, pages, cover_url);

WITH book AS (
    INSERT INTO books (title, author, description, title_norm, author_norm)
    VALUES ('Маленький принц', 'Антуан де Сент-Экзюпери', 'Философская сказка для детей и взрослых.', 'маленький принц', 'антуан де сент экзюпери')
    RETURNING id
)
INSERT INTO editions (book_id, isbn13, publisher, year, binding, language, pages, cover_url)
SELECT book.id, v.isbn13, v.publisher, v.year, v.binding, v.language, v.pages, v.cover_url
FROM book, (VALUES
    ('9785601000246', 'Эксмо', 2023, 'HARDCOVER', 'ru', 112, 'https://covers.example.com/9785601000246.jpg'),
    ('9785601000253', 'Махаон', 2021, 'PAPERBACK', 'ru', 96, 'https://covers.example.com/9785601000253.jpg')
) AS v (isbn13, publisher, year, binding, language, pages, cover_url);

WITH book AS (
    INSERT INTO books (title, author, description, title_norm, author_norm)
    VALUES ('Гарри Поттер и философский камень', 'Джоан Роулинг', 'Первая книга о юном волшебнике.', 'гарри поттер и философский камень', 'джоан роулинг')
    RETURNING id
)
INSERT INTO editions (book_id, isbn13, publisher, year, binding, language, pages, cover_url)
SELECT book.id, v.isbn13, v.publisher, v.year, v.binding, v.language, v.pages, v.cover_url
FROM book, (VALUES
    ('9785601000260', 'Махаон', 2023, 'HARDCOVER', 'ru', 432, 'https://covers.example.com/9785601000260.jpg'),
    ('9785601000277', 'Махаон', 2020, 'PAPERBACK', 'ru', 400, 'https://covers.example.com/9785601000277.jpg')
) AS v (isbn13, publisher, year, binding, language, pages, cover_url);

WITH book AS (
    INSERT INTO books (title, author, description, title_norm, author_norm)
    VALUES ('Три товарища', 'Эрих Мария Ремарк', 'Роман о дружбе и любви в Германии 1920-х.', 'три товарища', 'эрих мария ремарк')
    RETURNING id
)
INSERT INTO editions (book_id, isbn13, publisher, year, binding, language, pages, cover_url)
SELECT book.id, v.isbn13, v.publisher, v.year, v.binding, v.language, v.pages, v.cover_url
FROM book, (VALUES
    ('9785601000284', 'АСТ', 2022, 'PAPERBACK', 'ru', 480, 'https://covers.example.com/9785601000284.jpg')
) AS v (isbn13, publisher, year, binding, language, pages, cover_url);

WITH book AS (
    INSERT INTO books (title, author, description, title_norm, author_norm)
    VALUES ('Цветы для Элджернона', 'Дэниел Киз', 'Роман о человеке, которому повысили интеллект.', 'цветы для элджернона', 'дэниел киз')
    RETURNING id
)
INSERT INTO editions (book_id, isbn13, publisher, year, binding, language, pages, cover_url)
SELECT book.id, v.isbn13, v.publisher, v.year, v.binding, v.language, v.pages, v.cover_url
FROM book, (VALUES
    ('9785601000291', 'Эксмо', 2023, 'PAPERBACK', 'ru', 320, 'https://covers.example.com/9785601000291.jpg')
) AS v (isbn13, publisher, year, binding, language, pages, cover_url);

WITH book AS (
    INSERT INTO books (title, author, description, title_norm, author_norm)
    VALUES ('Над пропастью во ржи', 'Джером Сэлинджер', 'Роман о подростке Холдене Колфилде.', 'над пропастью во ржи', 'джером сэлинджер')
    RETURNING id
)
INSERT INTO editions (book_id, isbn13, publisher, year, binding, language, pages, cover_url)
SELECT book.id, v.isbn13, v.publisher, v.year, v.binding, v.language, v.pages, v.cover_url
FROM book, (VALUES
    ('9785601000307', 'Эксмо', 2021, 'PAPERBACK', 'ru', 272, 'https://covers.example.com/9785601000307.jpg')
) AS v (isbn13, publisher, year, binding, language, pages, cover_url);

WITH book AS (
    INSERT INTO books (title, author, description, title_norm, author_norm)
    VALUES ('Шантарам', 'Грегори Дэвид Робертс', 'Роман о беглом заключённом в Бомбее.', 'шантарам', 'грегори дэвид робертс')
    RETURNING id
)
INSERT INTO editions (book_id, isbn13, publisher, year, binding, language, pages, cover_url)
SELECT book.id, v.isbn13, v.publisher, v.year, v.binding, v.language, v.pages, v.cover_url
FROM book, (VALUES
    ('9785601000314', 'Азбука', 2022, 'HARDCOVER', 'ru', 864, 'https://covers.example.com/9785601000314.jpg')
) AS v (isbn13, publisher, year, binding, language, pages, cover_url);

WITH book AS (
    INSERT INTO books (title, author, description, title_norm, author_norm)
    VALUES ('Sapiens. Краткая история человечества', 'Юваль Ной Харари', 'История вида Homo sapiens.', 'sapiens краткая история человечества', 'юваль ной харари')
    RETURNING id
)
INSERT INTO editions (book_id, isbn13, publisher, year, binding, language, pages, cover_url)
SELECT book.id, v.isbn13, v.publisher, v.year, v.binding, v.language, v.pages, v.cover_url
FROM book, (VALUES
    ('9785601000321', 'Синдбад', 2023, 'HARDCOVER', 'ru', 512, 'https://covers.example.com/9785601000321.jpg')
) AS v (isbn13, publisher, year, binding, language, pages, cover_url);

WITH book AS (
    INSERT INTO books (title, author, description, title_norm, author_norm)
    VALUES ('Atomic Habits', 'James Clear', 'Tiny changes, remarkable results.', 'atomic habits', 'james clear')
    RETURNING id
)
INSERT INTO editions (book_id, isbn13, publisher, year, binding, language, pages, cover_url)
SELECT book.id, v.isbn13, v.publisher, v.year, v.binding, v.language, v.pages, v.cover_url
FROM book, (VALUES
    ('9785601000338', 'Penguin', 2018, 'PAPERBACK', 'en', 320, 'https://covers.example.com/9785601000338.jpg')
) AS v (isbn13, publisher, year, binding, language, pages, cover_url);

INSERT INTO offers (edition_id, store_id, external_id, url, title_raw, price, old_price, in_stock,
                    delivery_text, match_status, first_seen_at, last_seen_at, last_changed_at)
SELECT e.id, s.id, v.external_id, v.url, v.title_raw, v.price, v.old_price::NUMERIC, v.in_stock,
       v.delivery_text, 'MATCHED', NOW() - make_interval(days => v.first_days),
       NOW() - make_interval(hours => v.seen_hours), NOW() - make_interval(days => v.changed_days)
FROM (VALUES
    ('9785601000017', 'OZON', 'seed-9785601000017-ozon', 'https://ozon.demo.example/product/9785601000017', 'Мастер и Маргарита, АСТ', 4030, 4750, TRUE, '3–5 дней', 40, 1, 4),
    ('9785601000017', 'KASPI', 'seed-9785601000017-kaspi', 'https://kaspi.demo.example/product/9785601000017', 'Мастер и Маргарита, АСТ', 4670, NULL, TRUE, 'Завтра', 91, 2, 3),
    ('9785601000017', 'FLIP', 'seed-9785601000017-flip', 'https://flip.demo.example/product/9785601000017', 'Мастер и Маргарита, АСТ', 4980, NULL, TRUE, '1–2 дня', 75, 2, 5),
    ('9785601000017', 'MELOMAN', 'seed-9785601000017-meloman', 'https://meloman.demo.example/product/9785601000017', 'Мастер и Маргарита, АСТ', 5250, NULL, TRUE, 'Самовывоз сегодня', 64, 5, 5),
    ('9785601000017', 'WILDBERRIES', 'seed-9785601000017-wildberries', 'https://wildberries.demo.example/product/9785601000017', 'Мастер и Маргарита, АСТ', 4570, 5270, TRUE, '2–4 дня', 79, 5, 1),
    ('9785601000024', 'OZON', 'seed-9785601000024-ozon', 'https://ozon.demo.example/product/9785601000024', 'Мастер и Маргарита, Азбука', 2100, 2330, TRUE, '3–5 дней', 40, 4, 8),
    ('9785601000024', 'FLIP', 'seed-9785601000024-flip', 'https://flip.demo.example/product/9785601000024', 'Мастер и Маргарита, Азбука', 2220, 2580, TRUE, '1–2 дня', 95, 4, 8),
    ('9785601000031', 'WILDBERRIES', 'seed-9785601000031-wildberries', 'https://wildberries.demo.example/product/9785601000031', 'Мастер и Маргарита, Эксмо', 2950, NULL, TRUE, '2–4 дня', 80, 3, 2),
    ('9785601000031', 'FLIP', 'seed-9785601000031-flip', 'https://flip.demo.example/product/9785601000031', 'Мастер и Маргарита, Эксмо', 2770, NULL, TRUE, '1–2 дня', 81, 1, 2),
    ('9785601000031', 'OZON', 'seed-9785601000031-ozon', 'https://ozon.demo.example/product/9785601000031', 'Мастер и Маргарита, Эксмо', 2940, NULL, TRUE, '3–5 дней', 92, 5, 5),
    ('9785601000048', 'MELOMAN', 'seed-9785601000048-meloman', 'https://meloman.demo.example/product/9785601000048', 'Собачье сердце, АСТ', 2270, 2510, TRUE, 'Самовывоз сегодня', 64, 3, 8),
    ('9785601000048', 'OZON', 'seed-9785601000048-ozon', 'https://ozon.demo.example/product/9785601000048', 'Собачье сердце, АСТ', 2160, NULL, FALSE, '3–5 дней', 48, 3, 8),
    ('9785601000048', 'FLIP', 'seed-9785601000048-flip', 'https://flip.demo.example/product/9785601000048', 'Собачье сердце, АСТ', 2060, 2640, TRUE, '1–2 дня', 67, 2, 8),
    ('9785601000048', 'WILDBERRIES', 'seed-9785601000048-wildberries', 'https://wildberries.demo.example/product/9785601000048', 'Собачье сердце, АСТ', 2070, 2510, TRUE, '2–4 дня', 81, 2, 8),
    ('9785601000048', 'KASPI', 'seed-9785601000048-kaspi', 'https://kaspi.demo.example/product/9785601000048', 'Собачье сердце, АСТ', 2180, NULL, TRUE, 'Завтра', 49, 2, 8),
    ('9785601000055', 'MELOMAN', 'seed-9785601000055-meloman', 'https://meloman.demo.example/product/9785601000055', 'Преступление и наказание, Эксмо', 4370, NULL, TRUE, 'Самовывоз сегодня', 64, 5, 2),
    ('9785601000055', 'KASPI', 'seed-9785601000055-kaspi', 'https://kaspi.demo.example/product/9785601000055', 'Преступление и наказание, Эксмо', 5600, NULL, TRUE, 'Завтра', 41, 5, 1),
    ('9785601000062', 'OZON', 'seed-9785601000062-ozon', 'https://ozon.demo.example/product/9785601000062', 'Преступление и наказание, Азбука', 2230, 2580, TRUE, '3–5 дней', 57, 5, 8),
    ('9785601000062', 'MELOMAN', 'seed-9785601000062-meloman', 'https://meloman.demo.example/product/9785601000062', 'Преступление и наказание, Азбука', 2490, NULL, TRUE, 'Самовывоз сегодня', 65, 3, 8),
    ('9785601000062', 'WILDBERRIES', 'seed-9785601000062-wildberries', 'https://wildberries.demo.example/product/9785601000062', 'Преступление и наказание, Азбука', 2150, NULL, TRUE, '2–4 дня', 43, 4, 8),
    ('9785601000062', 'KASPI', 'seed-9785601000062-kaspi', 'https://kaspi.demo.example/product/9785601000062', 'Преступление и наказание, Азбука', 2290, NULL, TRUE, 'Завтра', 56, 5, 8),
    ('9785601000079', 'KASPI', 'seed-9785601000079-kaspi', 'https://kaspi.demo.example/product/9785601000079', 'Идиот, АСТ', 3210, 3910, TRUE, 'Завтра', 75, 2, 8),
    ('9785601000079', 'OZON', 'seed-9785601000079-ozon', 'https://ozon.demo.example/product/9785601000079', 'Идиот, АСТ', 2770, NULL, TRUE, '3–5 дней', 71, 3, 8),
    ('9785601000079', 'WILDBERRIES', 'seed-9785601000079-wildberries', 'https://wildberries.demo.example/product/9785601000079', 'Идиот, АСТ', 2810, NULL, TRUE, '2–4 дня', 65, 2, 8),
    ('9785601000086', 'KASPI', 'seed-9785601000086-kaspi', 'https://kaspi.demo.example/product/9785601000086', 'Идиот, Азбука', 1810, 2150, FALSE, 'Завтра', 41, 2, 8),
    ('9785601000086', 'WILDBERRIES', 'seed-9785601000086-wildberries', 'https://wildberries.demo.example/product/9785601000086', 'Идиот, Азбука', 1570, NULL, FALSE, '2–4 дня', 89, 1, 8),
    ('9785601000086', 'OZON', 'seed-9785601000086-ozon', 'https://ozon.demo.example/product/9785601000086', 'Идиот, Азбука', 1750, NULL, FALSE, '3–5 дней', 56, 3, 8),
    ('9785601000086', 'MELOMAN', 'seed-9785601000086-meloman', 'https://meloman.demo.example/product/9785601000086', 'Идиот, Азбука', 1920, NULL, FALSE, 'Самовывоз сегодня', 63, 5, 8),
    ('9785601000093', 'WILDBERRIES', 'seed-9785601000093-wildberries', 'https://wildberries.demo.example/product/9785601000093', 'Война и мир, Эксмо', 3720, NULL, TRUE, '2–4 дня', 45, 2, 8),
    ('9785601000093', 'KASPI', 'seed-9785601000093-kaspi', 'https://kaspi.demo.example/product/9785601000093', 'Война и мир, Эксмо', 4110, NULL, TRUE, 'Завтра', 64, 5, 8),
    ('9785601000093', 'OZON', 'seed-9785601000093-ozon', 'https://ozon.demo.example/product/9785601000093', 'Война и мир, Эксмо', 4000, NULL, TRUE, '3–5 дней', 56, 1, 8),
    ('9785601000093', 'FLIP', 'seed-9785601000093-flip', 'https://flip.demo.example/product/9785601000093', 'Война и мир, Эксмо', 3680, NULL, TRUE, '1–2 дня', 53, 4, 8),
    ('9785601000109', 'KASPI', 'seed-9785601000109-kaspi', 'https://kaspi.demo.example/product/9785601000109', 'Война и мир, АСТ', 2210, 2640, TRUE, 'Завтра', 42, 4, 4),
    ('9785601000109', 'OZON', 'seed-9785601000109-ozon', 'https://ozon.demo.example/product/9785601000109', 'Война и мир, АСТ', 2700, NULL, TRUE, '3–5 дней', 48, 2, 4),
    ('9785601000109', 'WILDBERRIES', 'seed-9785601000109-wildberries', 'https://wildberries.demo.example/product/9785601000109', 'Война и мир, АСТ', 2650, 3010, TRUE, '2–4 дня', 95, 5, 2),
    ('9785601000116', 'MELOMAN', 'seed-9785601000116-meloman', 'https://meloman.demo.example/product/9785601000116', 'Анна Каренина, Азбука', 4110, 4790, TRUE, 'Самовывоз сегодня', 68, 1, 1),
    ('9785601000116', 'FLIP', 'seed-9785601000116-flip', 'https://flip.demo.example/product/9785601000116', 'Анна Каренина, Азбука', 5830, 7500, TRUE, '1–2 дня', 92, 2, 5),
    ('9785601000123', 'WILDBERRIES', 'seed-9785601000123-wildberries', 'https://wildberries.demo.example/product/9785601000123', 'Евгений Онегин, Махаон', 3230, 4200, TRUE, '2–4 дня', 50, 4, 8),
    ('9785601000123', 'KASPI', 'seed-9785601000123-kaspi', 'https://kaspi.demo.example/product/9785601000123', 'Евгений Онегин, Махаон', 3440, NULL, TRUE, 'Завтра', 41, 1, 8),
    ('9785601000123', 'MELOMAN', 'seed-9785601000123-meloman', 'https://meloman.demo.example/product/9785601000123', 'Евгений Онегин, Махаон', 3530, NULL, TRUE, 'Самовывоз сегодня', 53, 2, 8),
    ('9785601000130', 'OZON', 'seed-9785601000130-ozon', 'https://ozon.demo.example/product/9785601000130', 'Капитанская дочка, АСТ', 1450, NULL, TRUE, '3–5 дней', 76, 1, 8),
    ('9785601000130', 'WILDBERRIES', 'seed-9785601000130-wildberries', 'https://wildberries.demo.example/product/9785601000130', 'Капитанская дочка, АСТ', 1550, 1910, TRUE, '2–4 дня', 92, 1, 8),
    ('9785601000154', 'MELOMAN', 'seed-9785601000154-meloman', 'https://meloman.demo.example/product/9785601000154', 'Отцы и дети, Азбука', 1820, NULL, TRUE, 'Самовывоз сегодня', 43, 4, 8),
    ('9785601000154', 'WILDBERRIES', 'seed-9785601000154-wildberries', 'https://wildberries.demo.example/product/9785601000154', 'Отцы и дети, Азбука', 2150, 2610, TRUE, '2–4 дня', 67, 4, 8),
    ('9785601000154', 'OZON', 'seed-9785601000154-ozon', 'https://ozon.demo.example/product/9785601000154', 'Отцы и дети, Азбука', 1960, 2180, FALSE, '3–5 дней', 48, 5, 8),
    ('9785601000161', 'KASPI', 'seed-9785601000161-kaspi', 'https://kaspi.demo.example/product/9785601000161', 'Путь Абая, Фолиант', 6010, 7730, TRUE, 'Завтра', 73, 3, 8),
    ('9785601000161', 'WILDBERRIES', 'seed-9785601000161-wildberries', 'https://wildberries.demo.example/product/9785601000161', 'Путь Абая, Фолиант', 5770, NULL, TRUE, '2–4 дня', 63, 4, 8),
    ('9785601000161', 'OZON', 'seed-9785601000161-ozon', 'https://ozon.demo.example/product/9785601000161', 'Путь Абая, Фолиант', 5800, NULL, TRUE, '3–5 дней', 78, 5, 8),
    ('9785601000161', 'MELOMAN', 'seed-9785601000161-meloman', 'https://meloman.demo.example/product/9785601000161', 'Путь Абая, Фолиант', 5150, NULL, TRUE, 'Самовывоз сегодня', 60, 5, 8),
    ('9785601000161', 'FLIP', 'seed-9785601000161-flip', 'https://flip.demo.example/product/9785601000161', 'Путь Абая, Фолиант', 5630, NULL, TRUE, '1–2 дня', 72, 5, 8),
    ('9785601000178', 'MELOMAN', 'seed-9785601000178-meloman', 'https://meloman.demo.example/product/9785601000178', 'Путь Абая, Жибек жолы', 5860, 7250, TRUE, 'Самовывоз сегодня', 10, 1, 2),
    ('9785601000178', 'FLIP', 'seed-9785601000178-flip', 'https://flip.demo.example/product/9785601000178', 'Путь Абая, Жибек жолы', 6000, 7400, FALSE, '1–2 дня', 6, 3, 2),
    ('9785601000178', 'WILDBERRIES', 'seed-9785601000178-wildberries', 'https://wildberries.demo.example/product/9785601000178', 'Путь Абая, Жибек жолы', 5560, NULL, TRUE, '2–4 дня', 5, 4, 2),
    ('9785601000185', 'OZON', 'seed-9785601000185-ozon', 'https://ozon.demo.example/product/9785601000185', 'Абай жолы, Жазушы', 3610, NULL, TRUE, '3–5 дней', 75, 4, 8),
    ('9785601000185', 'KASPI', 'seed-9785601000185-kaspi', 'https://kaspi.demo.example/product/9785601000185', 'Абай жолы, Жазушы', 4050, 4640, TRUE, 'Завтра', 72, 5, 8),
    ('9785601000185', 'MELOMAN', 'seed-9785601000185-meloman', 'https://meloman.demo.example/product/9785601000185', 'Абай жолы, Жазушы', 3880, NULL, TRUE, 'Самовывоз сегодня', 59, 2, 8),
    ('9785601000185', 'WILDBERRIES', 'seed-9785601000185-wildberries', 'https://wildberries.demo.example/product/9785601000185', 'Абай жолы, Жазушы', 4150, NULL, TRUE, '2–4 дня', 43, 2, 8),
    ('9785601000185', 'FLIP', 'seed-9785601000185-flip', 'https://flip.demo.example/product/9785601000185', 'Абай жолы, Жазушы', 3830, NULL, TRUE, '1–2 дня', 85, 1, 8),
    ('9785601000192', 'OZON', 'seed-9785601000192-ozon', 'https://ozon.demo.example/product/9785601000192', 'Қара сөздер, Фолиант', 1460, NULL, TRUE, '3–5 дней', 41, 3, 4),
    ('9785601000192', 'KASPI', 'seed-9785601000192-kaspi', 'https://kaspi.demo.example/product/9785601000192', 'Қара сөздер, Фолиант', 1990, NULL, FALSE, 'Завтра', 58, 5, 1),
    ('9785601000192', 'WILDBERRIES', 'seed-9785601000192-wildberries', 'https://wildberries.demo.example/product/9785601000192', 'Қара сөздер, Фолиант', 1850, 2050, TRUE, '2–4 дня', 90, 4, 3),
    ('9785601000192', 'MELOMAN', 'seed-9785601000192-meloman', 'https://meloman.demo.example/product/9785601000192', 'Қара сөздер, Фолиант', 1950, NULL, TRUE, 'Самовывоз сегодня', 60, 2, 3),
    ('9785601000192', 'FLIP', 'seed-9785601000192-flip', 'https://flip.demo.example/product/9785601000192', 'Қара сөздер, Фолиант', 1950, 2440, TRUE, '1–2 дня', 50, 3, 4),
    ('9785601000208', 'FLIP', 'seed-9785601000208-flip', 'https://flip.demo.example/product/9785601000208', 'Кочевники, Фолиант', 6340, NULL, TRUE, '1–2 дня', 74, 1, 8),
    ('9785601000208', 'WILDBERRIES', 'seed-9785601000208-wildberries', 'https://wildberries.demo.example/product/9785601000208', 'Кочевники, Фолиант', 6600, NULL, TRUE, '2–4 дня', 87, 3, 8),
    ('9785601000208', 'KASPI', 'seed-9785601000208-kaspi', 'https://kaspi.demo.example/product/9785601000208', 'Кочевники, Фолиант', 5750, NULL, TRUE, 'Завтра', 65, 2, 8),
    ('9785601000215', 'OZON', 'seed-9785601000215-ozon', 'https://ozon.demo.example/product/9785601000215', '1984, АСТ', 2810, 3650, TRUE, '3–5 дней', 45, 4, 8),
    ('9785601000215', 'FLIP', 'seed-9785601000215-flip', 'https://flip.demo.example/product/9785601000215', '1984, АСТ', 2540, NULL, TRUE, '1–2 дня', 83, 1, 8),
    ('9785601000215', 'WILDBERRIES', 'seed-9785601000215-wildberries', 'https://wildberries.demo.example/product/9785601000215', '1984, АСТ', 2260, NULL, TRUE, '2–4 дня', 68, 2, 8),
    ('9785601000215', 'KASPI', 'seed-9785601000215-kaspi', 'https://kaspi.demo.example/product/9785601000215', '1984, АСТ', 2440, 2830, TRUE, 'Завтра', 64, 4, 8),
    ('9785601000215', 'MELOMAN', 'seed-9785601000215-meloman', 'https://meloman.demo.example/product/9785601000215', '1984, АСТ', 2790, 3590, TRUE, 'Самовывоз сегодня', 63, 2, 8),
    ('9785601000222', 'MELOMAN', 'seed-9785601000222-meloman', 'https://meloman.demo.example/product/9785601000222', '1984, Эксмо', 4450, 5260, TRUE, 'Самовывоз сегодня', 61, 1, 4),
    ('9785601000222', 'FLIP', 'seed-9785601000222-flip', 'https://flip.demo.example/product/9785601000222', '1984, Эксмо', 5750, NULL, TRUE, '1–2 дня', 95, 4, 2),
    ('9785601000222', 'WILDBERRIES', 'seed-9785601000222-wildberries', 'https://wildberries.demo.example/product/9785601000222', '1984, Эксмо', 5470, NULL, TRUE, '2–4 дня', 52, 2, 5),
    ('9785601000222', 'KASPI', 'seed-9785601000222-kaspi', 'https://kaspi.demo.example/product/9785601000222', '1984, Эксмо', 5240, NULL, TRUE, 'Завтра', 94, 5, 1),
    ('9785601000222', 'OZON', 'seed-9785601000222-ozon', 'https://ozon.demo.example/product/9785601000222', '1984, Эксмо', 6010, NULL, TRUE, '3–5 дней', 54, 1, 5),
    ('9785601000239', 'WILDBERRIES', 'seed-9785601000239-wildberries', 'https://wildberries.demo.example/product/9785601000239', 'Скотный двор, АСТ', 2340, NULL, TRUE, '2–4 дня', 86, 5, 8),
    ('9785601000239', 'OZON', 'seed-9785601000239-ozon', 'https://ozon.demo.example/product/9785601000239', 'Скотный двор, АСТ', 2350, 2800, TRUE, '3–5 дней', 89, 2, 8),
    ('9785601000239', 'KASPI', 'seed-9785601000239-kaspi', 'https://kaspi.demo.example/product/9785601000239', 'Скотный двор, АСТ', 2390, NULL, TRUE, 'Завтра', 57, 3, 8),
    ('9785601000246', 'KASPI', 'seed-9785601000246-kaspi', 'https://kaspi.demo.example/product/9785601000246', 'Маленький принц, Эксмо', 4970, 5590, TRUE, 'Завтра', 52, 5, 8),
    ('9785601000246', 'MELOMAN', 'seed-9785601000246-meloman', 'https://meloman.demo.example/product/9785601000246', 'Маленький принц, Эксмо', 6170, 7770, FALSE, 'Самовывоз сегодня', 72, 4, 8),
    ('9785601000246', 'WILDBERRIES', 'seed-9785601000246-wildberries', 'https://wildberries.demo.example/product/9785601000246', 'Маленький принц, Эксмо', 5420, 6680, TRUE, '2–4 дня', 64, 5, 8),
    ('9785601000246', 'OZON', 'seed-9785601000246-ozon', 'https://ozon.demo.example/product/9785601000246', 'Маленький принц, Эксмо', 4910, NULL, TRUE, '3–5 дней', 50, 1, 8),
    ('9785601000246', 'FLIP', 'seed-9785601000246-flip', 'https://flip.demo.example/product/9785601000246', 'Маленький принц, Эксмо', 5140, 6060, TRUE, '1–2 дня', 66, 5, 8),
    ('9785601000260', 'KASPI', 'seed-9785601000260-kaspi', 'https://kaspi.demo.example/product/9785601000260', 'Гарри Поттер и философский камень, Махаон', 3690, 4240, TRUE, 'Завтра', 57, 3, 4),
    ('9785601000260', 'MELOMAN', 'seed-9785601000260-meloman', 'https://meloman.demo.example/product/9785601000260', 'Гарри Поттер и философский камень, Махаон', 4630, NULL, TRUE, 'Самовывоз сегодня', 81, 2, 5),
    ('9785601000260', 'OZON', 'seed-9785601000260-ozon', 'https://ozon.demo.example/product/9785601000260', 'Гарри Поттер и философский камень, Махаон', 4710, 5820, TRUE, '3–5 дней', 87, 5, 2),
    ('9785601000260', 'FLIP', 'seed-9785601000260-flip', 'https://flip.demo.example/product/9785601000260', 'Гарри Поттер и философский камень, Махаон', 4960, 6450, TRUE, '1–2 дня', 67, 4, 4),
    ('9785601000260', 'WILDBERRIES', 'seed-9785601000260-wildberries', 'https://wildberries.demo.example/product/9785601000260', 'Гарри Поттер и философский камень, Махаон', 4450, 5050, FALSE, '2–4 дня', 62, 2, 4),
    ('9785601000277', 'FLIP', 'seed-9785601000277-flip', 'https://flip.demo.example/product/9785601000277', 'Гарри Поттер и философский камень, Махаон', 2880, NULL, TRUE, '1–2 дня', 69, 5, 8),
    ('9785601000277', 'OZON', 'seed-9785601000277-ozon', 'https://ozon.demo.example/product/9785601000277', 'Гарри Поттер и философский камень, Махаон', 3310, NULL, TRUE, '3–5 дней', 70, 5, 8),
    ('9785601000277', 'MELOMAN', 'seed-9785601000277-meloman', 'https://meloman.demo.example/product/9785601000277', 'Гарри Поттер и философский камень, Махаон', 2770, NULL, TRUE, 'Самовывоз сегодня', 78, 3, 8),
    ('9785601000277', 'KASPI', 'seed-9785601000277-kaspi', 'https://kaspi.demo.example/product/9785601000277', 'Гарри Поттер и философский камень, Махаон', 3090, NULL, TRUE, 'Завтра', 89, 2, 8),
    ('9785601000277', 'WILDBERRIES', 'seed-9785601000277-wildberries', 'https://wildberries.demo.example/product/9785601000277', 'Гарри Поттер и философский камень, Махаон', 3020, 3680, TRUE, '2–4 дня', 74, 1, 8),
    ('9785601000284', 'WILDBERRIES', 'seed-9785601000284-wildberries', 'https://wildberries.demo.example/product/9785601000284', 'Три товарища, АСТ', 2210, NULL, TRUE, '2–4 дня', 72, 2, 4),
    ('9785601000284', 'OZON', 'seed-9785601000284-ozon', 'https://ozon.demo.example/product/9785601000284', 'Три товарища, АСТ', 1980, NULL, TRUE, '3–5 дней', 48, 4, 3),
    ('9785601000291', 'MELOMAN', 'seed-9785601000291-meloman', 'https://meloman.demo.example/product/9785601000291', 'Цветы для Элджернона, Эксмо', 2100, 2440, TRUE, 'Самовывоз сегодня', 91, 5, 8),
    ('9785601000291', 'WILDBERRIES', 'seed-9785601000291-wildberries', 'https://wildberries.demo.example/product/9785601000291', 'Цветы для Элджернона, Эксмо', 2160, NULL, TRUE, '2–4 дня', 65, 4, 8),
    ('9785601000291', 'FLIP', 'seed-9785601000291-flip', 'https://flip.demo.example/product/9785601000291', 'Цветы для Элджернона, Эксмо', 2270, NULL, TRUE, '1–2 дня', 81, 4, 8),
    ('9785601000291', 'OZON', 'seed-9785601000291-ozon', 'https://ozon.demo.example/product/9785601000291', 'Цветы для Элджернона, Эксмо', 2150, 2560, TRUE, '3–5 дней', 73, 2, 8),
    ('9785601000291', 'KASPI', 'seed-9785601000291-kaspi', 'https://kaspi.demo.example/product/9785601000291', 'Цветы для Элджернона, Эксмо', 2000, NULL, TRUE, 'Завтра', 72, 2, 8),
    ('9785601000307', 'WILDBERRIES', 'seed-9785601000307-wildberries', 'https://wildberries.demo.example/product/9785601000307', 'Над пропастью во ржи, Эксмо', 3230, NULL, TRUE, '2–4 дня', 41, 3, 8),
    ('9785601000307', 'OZON', 'seed-9785601000307-ozon', 'https://ozon.demo.example/product/9785601000307', 'Над пропастью во ржи, Эксмо', 2860, NULL, TRUE, '3–5 дней', 79, 3, 8),
    ('9785601000307', 'MELOMAN', 'seed-9785601000307-meloman', 'https://meloman.demo.example/product/9785601000307', 'Над пропастью во ржи, Эксмо', 2960, 3490, TRUE, 'Самовывоз сегодня', 91, 3, 8),
    ('9785601000307', 'KASPI', 'seed-9785601000307-kaspi', 'https://kaspi.demo.example/product/9785601000307', 'Над пропастью во ржи, Эксмо', 3590, 4050, TRUE, 'Завтра', 84, 4, 8),
    ('9785601000314', 'WILDBERRIES', 'seed-9785601000314-wildberries', 'https://wildberries.demo.example/product/9785601000314', 'Шантарам, Азбука', 4860, NULL, TRUE, '2–4 дня', 41, 3, 8),
    ('9785601000314', 'KASPI', 'seed-9785601000314-kaspi', 'https://kaspi.demo.example/product/9785601000314', 'Шантарам, Азбука', 4830, 5520, TRUE, 'Завтра', 72, 2, 8),
    ('9785601000314', 'OZON', 'seed-9785601000314-ozon', 'https://ozon.demo.example/product/9785601000314', 'Шантарам, Азбука', 5210, NULL, TRUE, '3–5 дней', 94, 2, 8),
    ('9785601000321', 'MELOMAN', 'seed-9785601000321-meloman', 'https://meloman.demo.example/product/9785601000321', 'Sapiens. Краткая история человечества, Синдбад', 2960, 3450, TRUE, 'Самовывоз сегодня', 49, 5, 8),
    ('9785601000321', 'KASPI', 'seed-9785601000321-kaspi', 'https://kaspi.demo.example/product/9785601000321', 'Sapiens. Краткая история человечества, Синдбад', 3680, NULL, TRUE, 'Завтра', 66, 4, 8),
    ('9785601000321', 'FLIP', 'seed-9785601000321-flip', 'https://flip.demo.example/product/9785601000321', 'Sapiens. Краткая история человечества, Синдбад', 3310, NULL, TRUE, '1–2 дня', 67, 5, 8),
    ('9785601000321', 'WILDBERRIES', 'seed-9785601000321-wildberries', 'https://wildberries.demo.example/product/9785601000321', 'Sapiens. Краткая история человечества, Синдбад', 3250, NULL, TRUE, '2–4 дня', 52, 4, 8),
    ('9785601000338', 'OZON', 'seed-9785601000338-ozon', 'https://ozon.demo.example/product/9785601000338', 'Atomic Habits, Penguin', 1850, NULL, TRUE, '3–5 дней', 54, 1, 8),
    ('9785601000338', 'MELOMAN', 'seed-9785601000338-meloman', 'https://meloman.demo.example/product/9785601000338', 'Atomic Habits, Penguin', 1510, NULL, FALSE, 'Самовывоз сегодня', 58, 4, 8),
    ('9785601000338', 'KASPI', 'seed-9785601000338-kaspi', 'https://kaspi.demo.example/product/9785601000338', 'Atomic Habits, Penguin', 1590, 1750, TRUE, 'Завтра', 69, 2, 8),
    ('9785601000338', 'FLIP', 'seed-9785601000338-flip', 'https://flip.demo.example/product/9785601000338', 'Atomic Habits, Penguin', 1700, NULL, TRUE, '1–2 дня', 93, 1, 8)
) AS v (isbn13, store, external_id, url, title_raw, price, old_price, in_stock, delivery_text,
         first_days, seen_hours, changed_days)
JOIN editions e ON e.isbn13 = v.isbn13
JOIN stores s ON s.code = v.store;

INSERT INTO price_history (offer_id, price, old_price, recorded_at)
SELECT o.id, v.price, v.old_price::NUMERIC, NOW() - make_interval(days => v.days_ago)
FROM (VALUES
    ('seed-9785601000017-ozon', 4560, NULL, 40),
    ('seed-9785601000017-ozon', 4800, NULL, 17),
    ('seed-9785601000017-ozon', 4300, NULL, 10),
    ('seed-9785601000017-ozon', 4230, NULL, 9),
    ('seed-9785601000017-ozon', 4030, 4750, 4),
    ('seed-9785601000017-kaspi', 4600, NULL, 91),
    ('seed-9785601000017-kaspi', 5210, NULL, 28),
    ('seed-9785601000017-kaspi', 4950, NULL, 24),
    ('seed-9785601000017-kaspi', 5600, NULL, 10),
    ('seed-9785601000017-kaspi', 4670, NULL, 3),
    ('seed-9785601000017-flip', 5100, NULL, 75),
    ('seed-9785601000017-flip', 4820, NULL, 28),
    ('seed-9785601000017-flip', 5410, NULL, 22),
    ('seed-9785601000017-flip', 5950, NULL, 11),
    ('seed-9785601000017-flip', 4980, NULL, 5),
    ('seed-9785601000017-meloman', 5430, NULL, 64),
    ('seed-9785601000017-meloman', 5000, NULL, 50),
    ('seed-9785601000017-meloman', 5280, NULL, 22),
    ('seed-9785601000017-meloman', 6790, NULL, 14),
    ('seed-9785601000017-meloman', 5250, NULL, 5),
    ('seed-9785601000017-wildberries', 4640, NULL, 79),
    ('seed-9785601000017-wildberries', 4890, NULL, 49),
    ('seed-9785601000017-wildberries', 4380, NULL, 41),
    ('seed-9785601000017-wildberries', 5600, NULL, 14),
    ('seed-9785601000017-wildberries', 4570, 5270, 1),
    ('seed-9785601000024-ozon', 2230, NULL, 40),
    ('seed-9785601000024-ozon', 2050, NULL, 15),
    ('seed-9785601000024-ozon', 2250, NULL, 14),
    ('seed-9785601000024-ozon', 2100, 2330, 8),
    ('seed-9785601000024-flip', 2480, NULL, 95),
    ('seed-9785601000024-flip', 2300, NULL, 85),
    ('seed-9785601000024-flip', 2470, NULL, 70),
    ('seed-9785601000024-flip', 2220, 2580, 8),
    ('seed-9785601000031-wildberries', 3220, NULL, 80),
    ('seed-9785601000031-wildberries', 3240, NULL, 67),
    ('seed-9785601000031-wildberries', 2970, NULL, 44),
    ('seed-9785601000031-wildberries', 2950, NULL, 2),
    ('seed-9785601000031-flip', 2770, NULL, 81),
    ('seed-9785601000031-flip', 3000, NULL, 79),
    ('seed-9785601000031-flip', 2930, NULL, 9),
    ('seed-9785601000031-flip', 2770, NULL, 2),
    ('seed-9785601000031-ozon', 3130, NULL, 92),
    ('seed-9785601000031-ozon', 3090, NULL, 61),
    ('seed-9785601000031-ozon', 3190, NULL, 22),
    ('seed-9785601000031-ozon', 2940, NULL, 5),
    ('seed-9785601000048-meloman', 2240, NULL, 64),
    ('seed-9785601000048-meloman', 2540, NULL, 55),
    ('seed-9785601000048-meloman', 2250, NULL, 27),
    ('seed-9785601000048-meloman', 2270, 2510, 8),
    ('seed-9785601000048-ozon', 2100, NULL, 48),
    ('seed-9785601000048-ozon', 2370, NULL, 43),
    ('seed-9785601000048-ozon', 2100, NULL, 18),
    ('seed-9785601000048-ozon', 2160, NULL, 8),
    ('seed-9785601000048-flip', 2050, NULL, 67),
    ('seed-9785601000048-flip', 2220, NULL, 49),
    ('seed-9785601000048-flip', 2110, NULL, 19),
    ('seed-9785601000048-flip', 2060, 2640, 8),
    ('seed-9785601000048-wildberries', 2030, NULL, 81),
    ('seed-9785601000048-wildberries', 2000, NULL, 64),
    ('seed-9785601000048-wildberries', 2280, NULL, 50),
    ('seed-9785601000048-wildberries', 2070, 2510, 8),
    ('seed-9785601000048-kaspi', 2390, NULL, 49),
    ('seed-9785601000048-kaspi', 2200, NULL, 36),
    ('seed-9785601000048-kaspi', 2420, NULL, 26),
    ('seed-9785601000048-kaspi', 2180, NULL, 8),
    ('seed-9785601000055-meloman', 4640, NULL, 64),
    ('seed-9785601000055-meloman', 4760, NULL, 44),
    ('seed-9785601000055-meloman', 4580, NULL, 27),
    ('seed-9785601000055-meloman', 5320, NULL, 18),
    ('seed-9785601000055-meloman', 4370, NULL, 2),
    ('seed-9785601000055-kaspi', 6290, NULL, 41),
    ('seed-9785601000055-kaspi', 5450, NULL, 36),
    ('seed-9785601000055-kaspi', 5350, NULL, 16),
    ('seed-9785601000055-kaspi', 6990, NULL, 15),
    ('seed-9785601000055-kaspi', 5600, NULL, 1),
    ('seed-9785601000062-ozon', 2150, NULL, 57),
    ('seed-9785601000062-ozon', 2240, NULL, 40),
    ('seed-9785601000062-ozon', 2440, NULL, 22),
    ('seed-9785601000062-ozon', 2230, 2580, 8),
    ('seed-9785601000062-meloman', 2820, NULL, 65),
    ('seed-9785601000062-meloman', 2540, NULL, 46),
    ('seed-9785601000062-meloman', 2400, NULL, 26),
    ('seed-9785601000062-meloman', 2490, NULL, 8),
    ('seed-9785601000062-wildberries', 2280, NULL, 43),
    ('seed-9785601000062-wildberries', 2190, NULL, 29),
    ('seed-9785601000062-wildberries', 2200, NULL, 19),
    ('seed-9785601000062-wildberries', 2150, NULL, 8),
    ('seed-9785601000062-kaspi', 2540, NULL, 56),
    ('seed-9785601000062-kaspi', 2240, NULL, 50),
    ('seed-9785601000062-kaspi', 2410, NULL, 24),
    ('seed-9785601000062-kaspi', 2290, NULL, 8),
    ('seed-9785601000079-kaspi', 3470, NULL, 75),
    ('seed-9785601000079-kaspi', 3310, NULL, 57),
    ('seed-9785601000079-kaspi', 3110, NULL, 53),
    ('seed-9785601000079-kaspi', 3210, 3910, 8),
    ('seed-9785601000079-ozon', 3150, NULL, 71),
    ('seed-9785601000079-ozon', 2990, NULL, 39),
    ('seed-9785601000079-ozon', 3080, NULL, 20),
    ('seed-9785601000079-ozon', 2770, NULL, 8),
    ('seed-9785601000079-wildberries', 3220, NULL, 65),
    ('seed-9785601000079-wildberries', 3080, NULL, 62),
    ('seed-9785601000079-wildberries', 2890, NULL, 43),
    ('seed-9785601000079-wildberries', 2810, NULL, 8),
    ('seed-9785601000086-kaspi', 1960, NULL, 41),
    ('seed-9785601000086-kaspi', 1760, NULL, 33),
    ('seed-9785601000086-kaspi', 1750, NULL, 20),
    ('seed-9785601000086-kaspi', 1810, 2150, 8),
    ('seed-9785601000086-wildberries', 1590, NULL, 89),
    ('seed-9785601000086-wildberries', 1550, NULL, 57),
    ('seed-9785601000086-wildberries', 1740, NULL, 10),
    ('seed-9785601000086-wildberries', 1570, NULL, 8),
    ('seed-9785601000086-ozon', 1660, NULL, 56),
    ('seed-9785601000086-ozon', 1710, NULL, 48),
    ('seed-9785601000086-ozon', 1810, NULL, 24),
    ('seed-9785601000086-ozon', 1750, NULL, 8),
    ('seed-9785601000086-meloman', 2000, NULL, 63),
    ('seed-9785601000086-meloman', 2040, NULL, 56),
    ('seed-9785601000086-meloman', 2000, NULL, 44),
    ('seed-9785601000086-meloman', 1920, NULL, 8),
    ('seed-9785601000093-wildberries', 3780, NULL, 45),
    ('seed-9785601000093-wildberries', 4100, NULL, 42),
    ('seed-9785601000093-wildberries', 3560, NULL, 24),
    ('seed-9785601000093-wildberries', 3720, NULL, 8),
    ('seed-9785601000093-kaspi', 4680, NULL, 64),
    ('seed-9785601000093-kaspi', 4250, NULL, 56),
    ('seed-9785601000093-kaspi', 4340, NULL, 50),
    ('seed-9785601000093-kaspi', 4110, NULL, 8),
    ('seed-9785601000093-ozon', 4510, NULL, 56),
    ('seed-9785601000093-ozon', 3950, NULL, 54),
    ('seed-9785601000093-ozon', 4130, NULL, 18),
    ('seed-9785601000093-ozon', 4000, NULL, 8),
    ('seed-9785601000093-flip', 3640, NULL, 53),
    ('seed-9785601000093-flip', 3520, NULL, 45),
    ('seed-9785601000093-flip', 3770, NULL, 32),
    ('seed-9785601000093-flip', 3680, NULL, 8),
    ('seed-9785601000109-kaspi', 2350, NULL, 42),
    ('seed-9785601000109-kaspi', 2470, NULL, 26),
    ('seed-9785601000109-kaspi', 2220, NULL, 11),
    ('seed-9785601000109-kaspi', 2210, 2640, 4),
    ('seed-9785601000109-ozon', 2800, NULL, 48),
    ('seed-9785601000109-ozon', 2910, NULL, 42),
    ('seed-9785601000109-ozon', 2970, NULL, 10),
    ('seed-9785601000109-ozon', 2700, NULL, 4),
    ('seed-9785601000109-wildberries', 2520, NULL, 95),
    ('seed-9785601000109-wildberries', 2920, NULL, 65),
    ('seed-9785601000109-wildberries', 2550, NULL, 54),
    ('seed-9785601000109-wildberries', 2650, 3010, 2),
    ('seed-9785601000116-meloman', 4480, NULL, 68),
    ('seed-9785601000116-meloman', 4530, NULL, 63),
    ('seed-9785601000116-meloman', 4420, NULL, 51),
    ('seed-9785601000116-meloman', 5480, NULL, 11),
    ('seed-9785601000116-meloman', 4110, 4790, 1),
    ('seed-9785601000116-flip', 6560, NULL, 92),
    ('seed-9785601000116-flip', 6010, NULL, 75),
    ('seed-9785601000116-flip', 5940, NULL, 23),
    ('seed-9785601000116-flip', 7390, NULL, 14),
    ('seed-9785601000116-flip', 5830, 7500, 5),
    ('seed-9785601000123-wildberries', 3510, NULL, 50),
    ('seed-9785601000123-wildberries', 3470, NULL, 46),
    ('seed-9785601000123-wildberries', 3610, NULL, 27),
    ('seed-9785601000123-wildberries', 3230, 4200, 8),
    ('seed-9785601000123-kaspi', 3290, NULL, 41),
    ('seed-9785601000123-kaspi', 3410, NULL, 27),
    ('seed-9785601000123-kaspi', 3800, NULL, 17),
    ('seed-9785601000123-kaspi', 3440, NULL, 8),
    ('seed-9785601000123-meloman', 3420, NULL, 53),
    ('seed-9785601000123-meloman', 3410, NULL, 33),
    ('seed-9785601000123-meloman', 3940, NULL, 28),
    ('seed-9785601000123-meloman', 3530, NULL, 8),
    ('seed-9785601000130-ozon', 1660, NULL, 76),
    ('seed-9785601000130-ozon', 1500, NULL, 71),
    ('seed-9785601000130-ozon', 1590, NULL, 31),
    ('seed-9785601000130-ozon', 1450, NULL, 8),
    ('seed-9785601000130-wildberries', 1580, NULL, 92),
    ('seed-9785601000130-wildberries', 1710, NULL, 82),
    ('seed-9785601000130-wildberries', 1670, NULL, 43),
    ('seed-9785601000130-wildberries', 1550, 1910, 8),
    ('seed-9785601000154-meloman', 2030, NULL, 43),
    ('seed-9785601000154-meloman', 1980, NULL, 42),
    ('seed-9785601000154-meloman', 1920, NULL, 27),
    ('seed-9785601000154-meloman', 1820, NULL, 8),
    ('seed-9785601000154-wildberries', 2290, NULL, 67),
    ('seed-9785601000154-wildberries', 2050, NULL, 26),
    ('seed-9785601000154-wildberries', 2130, NULL, 15),
    ('seed-9785601000154-wildberries', 2150, 2610, 8),
    ('seed-9785601000154-ozon', 2180, NULL, 48),
    ('seed-9785601000154-ozon', 2170, NULL, 25),
    ('seed-9785601000154-ozon', 1950, NULL, 17),
    ('seed-9785601000154-ozon', 1960, 2180, 8),
    ('seed-9785601000161-kaspi', 6510, NULL, 73),
    ('seed-9785601000161-kaspi', 5750, NULL, 29),
    ('seed-9785601000161-kaspi', 6440, NULL, 11),
    ('seed-9785601000161-kaspi', 6010, 7730, 8),
    ('seed-9785601000161-wildberries', 5920, NULL, 63),
    ('seed-9785601000161-wildberries', 5930, NULL, 49),
    ('seed-9785601000161-wildberries', 5780, NULL, 29),
    ('seed-9785601000161-wildberries', 5770, NULL, 8),
    ('seed-9785601000161-ozon', 6110, NULL, 78),
    ('seed-9785601000161-ozon', 6430, NULL, 62),
    ('seed-9785601000161-ozon', 5580, NULL, 35),
    ('seed-9785601000161-ozon', 5800, NULL, 8),
    ('seed-9785601000161-meloman', 5770, NULL, 60),
    ('seed-9785601000161-meloman', 5180, NULL, 19),
    ('seed-9785601000161-meloman', 5130, NULL, 10),
    ('seed-9785601000161-meloman', 5150, NULL, 8),
    ('seed-9785601000161-flip', 5940, NULL, 72),
    ('seed-9785601000161-flip', 5420, NULL, 70),
    ('seed-9785601000161-flip', 5690, NULL, 10),
    ('seed-9785601000161-flip', 5630, NULL, 8),
    ('seed-9785601000178-meloman', 6150, NULL, 10),
    ('seed-9785601000178-meloman', 5860, 7250, 2),
    ('seed-9785601000178-flip', 6300, NULL, 6),
    ('seed-9785601000178-flip', 6000, 7400, 2),
    ('seed-9785601000178-wildberries', 5840, NULL, 5),
    ('seed-9785601000178-wildberries', 5560, NULL, 2),
    ('seed-9785601000185-ozon', 3580, NULL, 75),
    ('seed-9785601000185-ozon', 3890, NULL, 48),
    ('seed-9785601000185-ozon', 3630, NULL, 21),
    ('seed-9785601000185-ozon', 3610, NULL, 8),
    ('seed-9785601000185-kaspi', 3960, NULL, 72),
    ('seed-9785601000185-kaspi', 4380, NULL, 64),
    ('seed-9785601000185-kaspi', 4530, NULL, 61),
    ('seed-9785601000185-kaspi', 4050, 4640, 8),
    ('seed-9785601000185-meloman', 4080, NULL, 59),
    ('seed-9785601000185-meloman', 4150, NULL, 28),
    ('seed-9785601000185-meloman', 4330, NULL, 14),
    ('seed-9785601000185-meloman', 3880, NULL, 8),
    ('seed-9785601000185-wildberries', 4220, NULL, 43),
    ('seed-9785601000185-wildberries', 4560, NULL, 41),
    ('seed-9785601000185-wildberries', 4460, NULL, 40),
    ('seed-9785601000185-wildberries', 4150, NULL, 8),
    ('seed-9785601000185-flip', 4080, NULL, 85),
    ('seed-9785601000185-flip', 4000, NULL, 68),
    ('seed-9785601000185-flip', 4090, NULL, 15),
    ('seed-9785601000185-flip', 3830, NULL, 8),
    ('seed-9785601000192-ozon', 1470, NULL, 41),
    ('seed-9785601000192-ozon', 1460, NULL, 32),
    ('seed-9785601000192-ozon', 1630, NULL, 14),
    ('seed-9785601000192-ozon', 1840, NULL, 13),
    ('seed-9785601000192-ozon', 1460, NULL, 4),
    ('seed-9785601000192-kaspi', 2010, NULL, 58),
    ('seed-9785601000192-kaspi', 2000, NULL, 55),
    ('seed-9785601000192-kaspi', 2610, NULL, 20),
    ('seed-9785601000192-kaspi', 1980, NULL, 17),
    ('seed-9785601000192-kaspi', 1990, NULL, 1),
    ('seed-9785601000192-wildberries', 1780, NULL, 90),
    ('seed-9785601000192-wildberries', 1980, NULL, 73),
    ('seed-9785601000192-wildberries', 1880, NULL, 59),
    ('seed-9785601000192-wildberries', 2300, NULL, 15),
    ('seed-9785601000192-wildberries', 1850, 2050, 3),
    ('seed-9785601000192-meloman', 1930, NULL, 60),
    ('seed-9785601000192-meloman', 2050, NULL, 50),
    ('seed-9785601000192-meloman', 1970, NULL, 23),
    ('seed-9785601000192-meloman', 2490, NULL, 13),
    ('seed-9785601000192-meloman', 1950, NULL, 3),
    ('seed-9785601000192-flip', 2210, NULL, 50),
    ('seed-9785601000192-flip', 2070, NULL, 31),
    ('seed-9785601000192-flip', 1940, NULL, 27),
    ('seed-9785601000192-flip', 2460, NULL, 17),
    ('seed-9785601000192-flip', 1950, 2440, 4),
    ('seed-9785601000208-flip', 7050, NULL, 74),
    ('seed-9785601000208-flip', 6470, NULL, 71),
    ('seed-9785601000208-flip', 6430, NULL, 31),
    ('seed-9785601000208-flip', 6340, NULL, 8),
    ('seed-9785601000208-wildberries', 6450, NULL, 87),
    ('seed-9785601000208-wildberries', 6830, NULL, 68),
    ('seed-9785601000208-wildberries', 6970, NULL, 58),
    ('seed-9785601000208-wildberries', 6600, NULL, 8),
    ('seed-9785601000208-kaspi', 5500, NULL, 65),
    ('seed-9785601000208-kaspi', 6240, NULL, 20),
    ('seed-9785601000208-kaspi', 6280, NULL, 16),
    ('seed-9785601000208-kaspi', 5750, NULL, 8),
    ('seed-9785601000215-ozon', 3010, NULL, 45),
    ('seed-9785601000215-ozon', 2910, NULL, 43),
    ('seed-9785601000215-ozon', 2960, NULL, 14),
    ('seed-9785601000215-ozon', 2810, 3650, 8),
    ('seed-9785601000215-flip', 2660, NULL, 83),
    ('seed-9785601000215-flip', 2800, NULL, 76),
    ('seed-9785601000215-flip', 2450, NULL, 61),
    ('seed-9785601000215-flip', 2540, NULL, 8),
    ('seed-9785601000215-wildberries', 2510, NULL, 68),
    ('seed-9785601000215-wildberries', 2370, NULL, 40),
    ('seed-9785601000215-wildberries', 2410, NULL, 9),
    ('seed-9785601000215-wildberries', 2260, NULL, 8),
    ('seed-9785601000215-kaspi', 2720, NULL, 64),
    ('seed-9785601000215-kaspi', 2430, NULL, 60),
    ('seed-9785601000215-kaspi', 2360, NULL, 54),
    ('seed-9785601000215-kaspi', 2440, 2830, 8),
    ('seed-9785601000215-meloman', 2890, NULL, 63),
    ('seed-9785601000215-meloman', 2760, NULL, 40),
    ('seed-9785601000215-meloman', 3060, NULL, 13),
    ('seed-9785601000215-meloman', 2790, 3590, 8),
    ('seed-9785601000222-meloman', 4300, NULL, 61),
    ('seed-9785601000222-meloman', 4940, NULL, 56),
    ('seed-9785601000222-meloman', 4830, NULL, 37),
    ('seed-9785601000222-meloman', 5820, NULL, 19),
    ('seed-9785601000222-meloman', 4450, 5260, 4),
    ('seed-9785601000222-flip', 5840, NULL, 95),
    ('seed-9785601000222-flip', 5560, NULL, 75),
    ('seed-9785601000222-flip', 5850, NULL, 35),
    ('seed-9785601000222-flip', 7290, NULL, 18),
    ('seed-9785601000222-flip', 5750, NULL, 2),
    ('seed-9785601000222-wildberries', 5960, NULL, 52),
    ('seed-9785601000222-wildberries', 6040, NULL, 32),
    ('seed-9785601000222-wildberries', 5680, NULL, 19),
    ('seed-9785601000222-wildberries', 7220, NULL, 19),
    ('seed-9785601000222-wildberries', 5470, NULL, 5),
    ('seed-9785601000222-kaspi', 4980, NULL, 94),
    ('seed-9785601000222-kaspi', 5860, NULL, 91),
    ('seed-9785601000222-kaspi', 5330, NULL, 41),
    ('seed-9785601000222-kaspi', 6230, NULL, 18),
    ('seed-9785601000222-kaspi', 5240, NULL, 1),
    ('seed-9785601000222-ozon', 5980, NULL, 54),
    ('seed-9785601000222-ozon', 6670, NULL, 31),
    ('seed-9785601000222-ozon', 7830, NULL, 15),
    ('seed-9785601000222-ozon', 6340, NULL, 11),
    ('seed-9785601000222-ozon', 6010, NULL, 5),
    ('seed-9785601000239-wildberries', 2300, NULL, 86),
    ('seed-9785601000239-wildberries', 2280, NULL, 54),
    ('seed-9785601000239-wildberries', 2610, NULL, 45),
    ('seed-9785601000239-wildberries', 2340, NULL, 8),
    ('seed-9785601000239-ozon', 2460, NULL, 89),
    ('seed-9785601000239-ozon', 2270, NULL, 60),
    ('seed-9785601000239-ozon', 2400, NULL, 23),
    ('seed-9785601000239-ozon', 2350, 2800, 8),
    ('seed-9785601000239-kaspi', 2290, NULL, 57),
    ('seed-9785601000239-kaspi', 2480, NULL, 55),
    ('seed-9785601000239-kaspi', 2440, NULL, 34),
    ('seed-9785601000239-kaspi', 2390, NULL, 8),
    ('seed-9785601000246-kaspi', 5630, NULL, 52),
    ('seed-9785601000246-kaspi', 5300, NULL, 47),
    ('seed-9785601000246-kaspi', 5350, NULL, 44),
    ('seed-9785601000246-kaspi', 4970, 5590, 8),
    ('seed-9785601000246-meloman', 6970, NULL, 72),
    ('seed-9785601000246-meloman', 6850, NULL, 71),
    ('seed-9785601000246-meloman', 6060, NULL, 10),
    ('seed-9785601000246-meloman', 6170, 7770, 8),
    ('seed-9785601000246-wildberries', 5240, NULL, 64),
    ('seed-9785601000246-wildberries', 5490, NULL, 34),
    ('seed-9785601000246-wildberries', 5380, NULL, 17),
    ('seed-9785601000246-wildberries', 5420, 6680, 8),
    ('seed-9785601000246-ozon', 4690, NULL, 50),
    ('seed-9785601000246-ozon', 5450, NULL, 37),
    ('seed-9785601000246-ozon', 5180, NULL, 26),
    ('seed-9785601000246-ozon', 4910, NULL, 8),
    ('seed-9785601000246-flip', 5520, NULL, 66),
    ('seed-9785601000246-flip', 5480, NULL, 51),
    ('seed-9785601000246-flip', 5300, NULL, 47),
    ('seed-9785601000246-flip', 5140, 6060, 8),
    ('seed-9785601000260-kaspi', 4020, NULL, 57),
    ('seed-9785601000260-kaspi', 3880, NULL, 37),
    ('seed-9785601000260-kaspi', 4120, NULL, 14),
    ('seed-9785601000260-kaspi', 4440, NULL, 10),
    ('seed-9785601000260-kaspi', 3690, 4240, 4),
    ('seed-9785601000260-meloman', 4810, NULL, 81),
    ('seed-9785601000260-meloman', 4830, NULL, 53),
    ('seed-9785601000260-meloman', 5790, NULL, 15),
    ('seed-9785601000260-meloman', 5020, NULL, 12),
    ('seed-9785601000260-meloman', 4630, NULL, 5),
    ('seed-9785601000260-ozon', 4660, NULL, 87),
    ('seed-9785601000260-ozon', 5220, NULL, 46),
    ('seed-9785601000260-ozon', 5140, NULL, 22),
    ('seed-9785601000260-ozon', 6100, NULL, 10),
    ('seed-9785601000260-ozon', 4710, 5820, 2),
    ('seed-9785601000260-flip', 5370, NULL, 67),
    ('seed-9785601000260-flip', 5070, NULL, 45),
    ('seed-9785601000260-flip', 5160, NULL, 33),
    ('seed-9785601000260-flip', 6640, NULL, 18),
    ('seed-9785601000260-flip', 4960, 6450, 4),
    ('seed-9785601000260-wildberries', 4620, NULL, 62),
    ('seed-9785601000260-wildberries', 4930, NULL, 54),
    ('seed-9785601000260-wildberries', 4360, NULL, 42),
    ('seed-9785601000260-wildberries', 5490, NULL, 15),
    ('seed-9785601000260-wildberries', 4450, 5050, 4),
    ('seed-9785601000277-flip', 3240, NULL, 69),
    ('seed-9785601000277-flip', 2900, NULL, 36),
    ('seed-9785601000277-flip', 3070, NULL, 22),
    ('seed-9785601000277-flip', 2880, NULL, 8),
    ('seed-9785601000277-ozon', 3470, NULL, 70),
    ('seed-9785601000277-ozon', 3200, NULL, 52),
    ('seed-9785601000277-ozon', 3660, NULL, 17),
    ('seed-9785601000277-ozon', 3310, NULL, 8),
    ('seed-9785601000277-meloman', 2970, NULL, 78),
    ('seed-9785601000277-meloman', 2750, NULL, 59),
    ('seed-9785601000277-meloman', 2930, NULL, 38),
    ('seed-9785601000277-meloman', 2770, NULL, 8),
    ('seed-9785601000277-kaspi', 3500, NULL, 89),
    ('seed-9785601000277-kaspi', 3290, NULL, 79),
    ('seed-9785601000277-kaspi', 3430, NULL, 54),
    ('seed-9785601000277-kaspi', 3090, NULL, 8),
    ('seed-9785601000277-wildberries', 3080, NULL, 74),
    ('seed-9785601000277-wildberries', 3170, NULL, 63),
    ('seed-9785601000277-wildberries', 2870, NULL, 59),
    ('seed-9785601000277-wildberries', 3020, 3680, 8),
    ('seed-9785601000284-wildberries', 2330, NULL, 72),
    ('seed-9785601000284-wildberries', 2400, NULL, 37),
    ('seed-9785601000284-wildberries', 2210, NULL, 4),
    ('seed-9785601000284-ozon', 2240, NULL, 48),
    ('seed-9785601000284-ozon', 1940, NULL, 33),
    ('seed-9785601000284-ozon', 2080, NULL, 31),
    ('seed-9785601000284-ozon', 1980, NULL, 3),
    ('seed-9785601000291-meloman', 2010, NULL, 91),
    ('seed-9785601000291-meloman', 2050, NULL, 76),
    ('seed-9785601000291-meloman', 2260, NULL, 42),
    ('seed-9785601000291-meloman', 2100, 2440, 8),
    ('seed-9785601000291-wildberries', 2460, NULL, 65),
    ('seed-9785601000291-wildberries', 2180, NULL, 48),
    ('seed-9785601000291-wildberries', 2390, NULL, 15),
    ('seed-9785601000291-wildberries', 2160, NULL, 8),
    ('seed-9785601000291-flip', 2340, NULL, 81),
    ('seed-9785601000291-flip', 2440, NULL, 69),
    ('seed-9785601000291-flip', 2200, NULL, 20),
    ('seed-9785601000291-flip', 2270, NULL, 8),
    ('seed-9785601000291-ozon', 2330, NULL, 73),
    ('seed-9785601000291-ozon', 2240, NULL, 71),
    ('seed-9785601000291-ozon', 2370, NULL, 60),
    ('seed-9785601000291-ozon', 2150, 2560, 8),
    ('seed-9785601000291-kaspi', 2270, NULL, 72),
    ('seed-9785601000291-kaspi', 2190, NULL, 53),
    ('seed-9785601000291-kaspi', 1960, NULL, 18),
    ('seed-9785601000291-kaspi', 2000, NULL, 8),
    ('seed-9785601000307-wildberries', 3430, NULL, 41),
    ('seed-9785601000307-wildberries', 3390, NULL, 19),
    ('seed-9785601000307-wildberries', 3090, NULL, 15),
    ('seed-9785601000307-wildberries', 3230, NULL, 8),
    ('seed-9785601000307-ozon', 3280, NULL, 79),
    ('seed-9785601000307-ozon', 2980, NULL, 14),
    ('seed-9785601000307-ozon', 3020, NULL, 9),
    ('seed-9785601000307-ozon', 2860, NULL, 8),
    ('seed-9785601000307-meloman', 3070, NULL, 91),
    ('seed-9785601000307-meloman', 2920, NULL, 88),
    ('seed-9785601000307-meloman', 3240, NULL, 63),
    ('seed-9785601000307-meloman', 2960, 3490, 8),
    ('seed-9785601000307-kaspi', 3660, NULL, 84),
    ('seed-9785601000307-kaspi', 4000, NULL, 61),
    ('seed-9785601000307-kaspi', 3780, NULL, 34),
    ('seed-9785601000307-kaspi', 3590, 4050, 8),
    ('seed-9785601000314-wildberries', 4930, NULL, 41),
    ('seed-9785601000314-wildberries', 5000, NULL, 23),
    ('seed-9785601000314-wildberries', 5040, NULL, 18),
    ('seed-9785601000314-wildberries', 4860, NULL, 8),
    ('seed-9785601000314-kaspi', 5340, NULL, 72),
    ('seed-9785601000314-kaspi', 5090, NULL, 57),
    ('seed-9785601000314-kaspi', 5010, NULL, 33),
    ('seed-9785601000314-kaspi', 4830, 5520, 8),
    ('seed-9785601000314-ozon', 5590, NULL, 94),
    ('seed-9785601000314-ozon', 5190, NULL, 62),
    ('seed-9785601000314-ozon', 5200, NULL, 15),
    ('seed-9785601000314-ozon', 5210, NULL, 8),
    ('seed-9785601000321-meloman', 3160, NULL, 49),
    ('seed-9785601000321-meloman', 3310, NULL, 39),
    ('seed-9785601000321-meloman', 3250, NULL, 38),
    ('seed-9785601000321-meloman', 2960, 3450, 8),
    ('seed-9785601000321-kaspi', 3870, NULL, 66),
    ('seed-9785601000321-kaspi', 3960, NULL, 62),
    ('seed-9785601000321-kaspi', 3900, NULL, 58),
    ('seed-9785601000321-kaspi', 3680, NULL, 8),
    ('seed-9785601000321-flip', 3180, NULL, 67),
    ('seed-9785601000321-flip', 3530, NULL, 11),
    ('seed-9785601000321-flip', 3300, NULL, 9),
    ('seed-9785601000321-flip', 3310, NULL, 8),
    ('seed-9785601000321-wildberries', 3280, NULL, 52),
    ('seed-9785601000321-wildberries', 3370, NULL, 49),
    ('seed-9785601000321-wildberries', 3090, NULL, 10),
    ('seed-9785601000321-wildberries', 3250, NULL, 8),
    ('seed-9785601000338-ozon', 2030, NULL, 54),
    ('seed-9785601000338-ozon', 1950, NULL, 12),
    ('seed-9785601000338-ozon', 1830, NULL, 11),
    ('seed-9785601000338-ozon', 1850, NULL, 8),
    ('seed-9785601000338-meloman', 1570, NULL, 58),
    ('seed-9785601000338-meloman', 1560, NULL, 44),
    ('seed-9785601000338-meloman', 1470, NULL, 40),
    ('seed-9785601000338-meloman', 1510, NULL, 8),
    ('seed-9785601000338-kaspi', 1520, NULL, 69),
    ('seed-9785601000338-kaspi', 1740, NULL, 46),
    ('seed-9785601000338-kaspi', 1710, NULL, 33),
    ('seed-9785601000338-kaspi', 1590, 1750, 8),
    ('seed-9785601000338-flip', 1860, NULL, 93),
    ('seed-9785601000338-flip', 1730, NULL, 84),
    ('seed-9785601000338-flip', 1800, NULL, 58),
    ('seed-9785601000338-flip', 1700, NULL, 8)
) AS v (external_id, price, old_price, days_ago)
JOIN offers o ON o.external_id = v.external_id;
