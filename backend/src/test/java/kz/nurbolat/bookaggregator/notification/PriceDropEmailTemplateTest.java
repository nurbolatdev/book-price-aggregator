package kz.nurbolat.bookaggregator.notification;

import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PriceDropEmailTemplateTest {

    private static final String BASE_URL = "https://kitapradar.kz";
    private static final SpringTemplateEngine ENGINE = engine();

    @Test
    void shouldShowEveryFieldOfTheMockupWithAbsoluteLinks() {
        Map<String, Object> model = PriceDropEmail.model(payload(), BASE_URL);
        model.put("unsubscribeUrl", BASE_URL + "/unsubscribe?token=abc");

        String html = render("email/price-drop", model);

        assertThat(html).contains(
                "https://img.example/cover.jpg",
                "Мастер и Маргарита", "Михаил Булгаков",
                "3 610 ₸", "3 150 ₸", "Дешевле на 460 ₸ (−12%)",
                "в магазине Flip.kz", "Открыть в Flip.kz", "href=\"https://flip.kz/book/1\"",
                "Сравнить все цены", "href=\"" + BASE_URL + "/editions/41\"",
                "Meloman", "3 380 ₸", "Ozon", "нет в наличии",
                "Цены собраны 20.10.2026 14:12",
                "Отключить этот маячок", "href=\"" + BASE_URL + "/unsubscribe?token=abc\"",
                "Настройки уведомлений", "href=\"" + BASE_URL + "/profile\"");
        assertThat(html).doesNotContain("href=\"/", "href=\"#\"", "null");
    }

    @Test
    void shouldRenderTextVersionWithTheSameFacts() {
        Map<String, Object> model = PriceDropEmail.model(payload(), BASE_URL);

        String text = render("email/price-drop.txt", model);

        assertThat(text).contains("«Мастер и Маргарита», Михаил Булгаков",
                "Было 3 610 ₸ → стало 3 150 ₸", "Открыть в магазине: https://flip.kz/book/1",
                "Сравнить все цены: " + BASE_URL + "/editions/41", "- Meloman: 3 380 ₸",
                "- Ozon: 3 900 ₸ (нет в наличии)");
        // No unsubscribe link in the model — no line for it
        assertThat(text).doesNotContain("Отключить", "[(", "null");
    }

    @Test
    void shouldHideOldPriceWhenThereWasNoDropFromIt() {
        Map<String, Object> payload = payload();
        payload.put("oldPrice", payload.get("newPrice"));

        String html = render("email/price-drop", PriceDropEmail.model(payload, BASE_URL));

        assertThat(html).doesNotContain("Дешевле", "line-through");
    }

    @Test
    void shouldPutTitleAndPriceIntoSubject() {
        assertThat(PriceDropEmail.subject(payload())).isEqualTo("Цена упала: «Мастер и Маргарита» — 3 150 ₸");
    }

    private static Map<String, Object> payload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("editionId", 41);
        payload.put("title", "Мастер и Маргарита");
        payload.put("author", "Михаил Булгаков");
        payload.put("coverUrl", "https://img.example/cover.jpg");
        payload.put("storeName", "Flip.kz");
        payload.put("offerUrl", "https://flip.kz/book/1");
        payload.put("oldPrice", new BigDecimal("3610.00"));
        // As it comes back from JSON
        payload.put("newPrice", 3150.0);
        payload.put("pricesCollectedAt", "2026-10-20T09:12:00Z");
        payload.put("otherOffers", List.of(
                Map.of("storeName", "Meloman", "price", 3380, "inStock", true, "url", "https://meloman.kz/1"),
                Map.of("storeName", "Ozon", "price", new BigDecimal("3900.00"), "inStock", false, "url", "https://ozon.kz/1")));
        return payload;
    }

    private static String render(String template, Map<String, Object> model) {
        Context context = new Context(Locale.forLanguageTag("ru"));
        context.setVariables(model);
        context.setVariable("baseUrl", BASE_URL);
        return ENGINE.process(template, context);
    }

    private static SpringTemplateEngine engine() {
        ClassLoaderTemplateResolver html = new ClassLoaderTemplateResolver();
        html.setPrefix("templates/");
        html.setSuffix(".html");
        html.setTemplateMode(TemplateMode.HTML);
        html.setCharacterEncoding("UTF-8");
        html.setOrder(1);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.addTemplateResolver(new MailTemplateConfig().textEmailTemplateResolver());
        engine.addTemplateResolver(html);
        return engine;
    }
}
