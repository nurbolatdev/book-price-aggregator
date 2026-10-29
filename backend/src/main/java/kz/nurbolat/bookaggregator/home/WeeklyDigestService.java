package kz.nurbolat.bookaggregator.home;

import kz.nurbolat.bookaggregator.config.AppProperties;
import kz.nurbolat.bookaggregator.home.dto.DropItem;
import kz.nurbolat.bookaggregator.notification.EmailPublisher;
import kz.nurbolat.bookaggregator.notification.EmailType;
import kz.nurbolat.bookaggregator.notification.Notification;
import kz.nurbolat.bookaggregator.notification.NotificationDelivery;
import kz.nurbolat.bookaggregator.notification.NotificationRepository;
import kz.nurbolat.bookaggregator.notification.NotificationService;
import kz.nurbolat.bookaggregator.notification.WeeklyDigestEmail;
import kz.nurbolat.bookaggregator.common.text.Emails;
import kz.nurbolat.bookaggregator.store.Store;
import kz.nurbolat.bookaggregator.store.StoreCode;
import kz.nurbolat.bookaggregator.store.StoreRepository;
import kz.nurbolat.bookaggregator.user.User;
import kz.nurbolat.bookaggregator.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Weekly email with the biggest price drops of the week (the same selection as "Подешевели за неделю" on the home
 * page) for every user with a confirmed email and the weekly digest switched on. A user gets at most one per ISO
 * week (Almaty time): checked before queueing and guaranteed by a unique index, so a repeated or concurrent run
 * skips them. Each user is handled in their own transaction; the email leaves after its commit and marks the
 * notification published once the broker confirms it, a missed publication is retried by {@code BeaconEmailRelay}.
 */
@Slf4j
@Service
public class WeeklyDigestService {

    static final int BOOKS = 8;
    private static final ZoneId ALMATY = ZoneId.of("Asia/Almaty");
    private static final String[] MONTHS = {"января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа",
            "сентября", "октября", "ноября", "декабря"};

    private final DropsService dropsService;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final NotificationRepository notifications;
    private final NotificationService notificationService;
    private final EmailPublisher emailPublisher;
    private final AppProperties appProperties;
    private final Clock clock;
    private final TransactionTemplate transaction;

    public WeeklyDigestService(DropsService dropsService, UserRepository userRepository, StoreRepository storeRepository,
                               NotificationRepository notifications, NotificationService notificationService,
                               EmailPublisher emailPublisher, AppProperties appProperties, Clock clock,
                               PlatformTransactionManager transactionManager) {
        this.dropsService = dropsService;
        this.userRepository = userRepository;
        this.storeRepository = storeRepository;
        this.notifications = notifications;
        this.notificationService = notificationService;
        this.emailPublisher = emailPublisher;
        this.appProperties = appProperties;
        this.clock = clock;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * @return how many weekly emails were queued
     */
    public int sendWeeklyDigests() {
        List<DropItem> drops = dropsService.getDrops(BOOKS);
        if (drops.isEmpty()) {
            log.info("Weekly digest: no price drops this week, nothing to send");
            return 0;
        }
        ZonedDateTime now = clock.instant().atZone(ALMATY);
        String isoWeek = "%d-W%02d".formatted(now.get(IsoFields.WEEK_BASED_YEAR), now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
        Map<String, Object> payload = payload(drops, isoWeek, now);
        String subject = WeeklyDigestEmail.subject(payload);

        int queued = 0;
        for (Long userId : userRepository.findWeeklyDigestRecipientIds()) {
            try {
                if (Boolean.TRUE.equals(transaction.execute(status -> sendTo(userId, isoWeek, payload, subject)))) {
                    queued++;
                }
            } catch (DataIntegrityViolationException e) {
                log.info("Weekly digest {} for user {} was queued by another run", isoWeek, userId);
            }
        }
        log.info("Weekly digest {}: {} emails queued", isoWeek, queued);
        return queued;
    }

    private boolean sendTo(Long userId, String isoWeek, Map<String, Object> payload, String subject) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !user.isEmailVerified() || !user.isWeeklyDigest() || notifications.existsWeekly(userId, isoWeek)) {
            return false;
        }
        Notification notification = notificationService.queue(user, null, payload, NotificationDelivery.WEEKLY);
        emailPublisher.publish(EmailType.WEEKLY_DIGEST, user.getEmail(), subject,
                WeeklyDigestEmail.model(payload, appProperties.baseUrl(), user.getName()), List.of(notification.getId()));
        log.info("Weekly digest {} for {}", isoWeek, Emails.mask(user.getEmail()));
        return true;
    }

    private Map<String, Object> payload(List<DropItem> drops, String isoWeek, ZonedDateTime now) {
        Map<StoreCode, String> storeNames = new HashMap<>();
        for (Store store : storeRepository.findAllByOrderByIdAsc()) {
            storeNames.put(store.getCode(), store.getName());
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (DropItem drop : drops) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("editionId", drop.editionId());
            item.put("title", drop.title());
            item.put("author", drop.author());
            item.put("coverUrl", drop.coverUrl());
            item.put("storeName", storeNames.getOrDefault(drop.store(), drop.store().name()));
            item.put("price", drop.price());
            item.put("previousPrice", drop.previousPrice());
            item.put("dropPercent", drop.dropPercent());
            items.add(item);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("isoWeek", isoWeek);
        payload.put("periodText", periodText(now.toLocalDate().minusDays(7), now.toLocalDate()));
        payload.put("pricesCollectedAt", clock.instant().toString());
        payload.put("items", items);
        return payload;
    }

    /** {@code 19–26 октября}, or {@code 28 сентября – 5 октября} across a month border. */
    static String periodText(LocalDate from, LocalDate to) {
        String toMonth = MONTHS[to.getMonthValue() - 1];
        if (from.getMonth() == to.getMonth()) {
            return from.getDayOfMonth() + "–" + to.getDayOfMonth() + " " + toMonth;
        }
        return from.getDayOfMonth() + " " + MONTHS[from.getMonthValue() - 1] + " – " + to.getDayOfMonth() + " " + toMonth;
    }
}
