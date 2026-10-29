package kz.nurbolat.bookaggregator.notification;

import kz.nurbolat.bookaggregator.common.text.Money;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WeeklyDigestEmailTemplateTest {

    private static final String BASE_URL = "https://kitapradar.kz";
    private static final String PRICE = Money.format(new BigDecimal("3150"));
    private static final SpringTemplateEngine ENGINE = engine();

    @Test
    void shouldShowEveryBookWithAbsoluteLinks() {
        Map<String, Object> model = WeeklyDigestEmail.model(payload(2), BASE_URL, "Нурболат");

        String html = render("email/weekly-digest", model);

        assertThat(html).contains(
                "Подборка недели · 19–26 октября", "Нурболат, эти книги сильнее всего подешевели за неделю",
                "Книга 1", "Автор 1 · Flip.kz", PRICE, "−18%",
                "Книга 2", "href=\"" + BASE_URL + "/editions/41\"", "href=\"" + BASE_URL + "/editions/42\"",
                "Все скидки недели", "href=\"" + BASE_URL + "/deals\"",
                "Отключить подборку", "Настройки уведомлений", "href=\"" + BASE_URL + "/profile\"",
                "Цены собраны 26.10.2026 10:00");
        assertThat(html).doesNotContain("href=\"/", "href=\"#\"", "null");
    }

    @Test
    void shouldRenderTextVersionWithTheSameFacts() {
        String text = render("email/weekly-digest.txt", WeeklyDigestEmail.model(payload(2), BASE_URL, null));

        assertThat(text).contains("Здравствуйте!", "«Книга 1», Автор 1", "Flip.kz: " + PRICE + " (−18%)",
                BASE_URL + "/editions/41", "Все скидки недели: " + BASE_URL + "/deals",
                "Отключить подборку и настройки уведомлений: " + BASE_URL + "/profile");
        assertThat(text).doesNotContain("[(", "null");
    }

    @Test
    void shouldPutCountAndBiggestDropIntoSubject() {
        assertThat(WeeklyDigestEmail.subject(payload(1))).isEqualTo("Подборка недели: 1 книга подешевела до −18%");
        assertThat(WeeklyDigestEmail.subject(payload(3))).isEqualTo("Подборка недели: 3 книги подешевели до −20%");
        assertThat(WeeklyDigestEmail.subject(payload(8))).isEqualTo("Подборка недели: 8 книг подешевели до −25%");
        assertThat(WeeklyDigestEmail.subject(payload(11))).isEqualTo("Подборка недели: 11 книг подешевели до −28%");
    }

    /** Book {@code i} dropped {@code 17 + i}%. */
    private static Map<String, Object> payload(int books) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 1; i <= books; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("editionId", 40 + i);
            item.put("title", "Книга " + i);
            item.put("author", "Автор " + i);
            item.put("coverUrl", null);
            item.put("storeName", "Flip.kz");
            // As it comes back from JSON
            item.put("price", i % 2 == 1 ? 3150.0 : new BigDecimal("3150.00"));
            item.put("previousPrice", 3850);
            item.put("dropPercent", 17 + i);
            items.add(item);
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("isoWeek", "2026-W44");
        payload.put("periodText", "19–26 октября");
        payload.put("pricesCollectedAt", "2026-10-26T05:00:00Z");
        payload.put("items", items);
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
