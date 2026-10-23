package kz.nurbolat.bookaggregator.notification;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Email to send, as it travels through {@code kr.notify.email}.
 *
 * @param locale          language of the templates ({@code ru})
 * @param subject         subject line; {@code null} for the fixed subject of the type
 * @param model           values the template shows (name, absolute links, prices)
 * @param notificationIds rows of {@code notifications} this email delivers, marked sent or failed by the listener;
 *                        empty for account emails
 */
public record EmailMessage(UUID messageId, EmailType type, String to, String locale, String subject,
                           Map<String, Object> model, List<Long> notificationIds) {

    public String subjectOrDefault() {
        return subject != null ? subject : type.getSubject();
    }
}
