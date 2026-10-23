package kz.nurbolat.bookaggregator.notification;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Kinds of emails, each with its templates ({@code templates/email/<template>.html} and {@code .txt}).
 */
@Getter
@RequiredArgsConstructor
public enum EmailType {

    VERIFY_EMAIL("verify-email", "Подтвердите email — KitapRadar"),
    RESET_PASSWORD("reset-password", "Сброс пароля — KitapRadar"),
    // Templates arrive with beacons (M19)
    PRICE_DROP("price-drop", "Цена снизилась — KitapRadar"),
    DIGEST("digest", "Сводка цен — KitapRadar");

    private final String template;
    private final String subject;
}
