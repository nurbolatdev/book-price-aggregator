package kz.nurbolat.bookaggregator.notification;

import java.util.Map;
import java.util.UUID;

/**
 * Email to send, as it travels through {@code kr.notify.email}.
 *
 * @param locale language of the templates ({@code ru})
 * @param model  values the template shows (name, absolute links, prices)
 */
public record EmailMessage(UUID messageId, EmailType type, String to, String locale, Map<String, Object> model) {
}
