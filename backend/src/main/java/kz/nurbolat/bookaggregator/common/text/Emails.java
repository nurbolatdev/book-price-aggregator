package kz.nurbolat.bookaggregator.common.text;

/**
 * Email addresses in logs: only the first letter of the name stays ({@code n***@example.com}).
 */
public final class Emails {

    private Emails() {
    }

    public static String mask(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
