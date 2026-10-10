package com.lodhi.notification_service.websocket;

/**
 * Single source of truth for the STOMP destinations the notification-service exposes.
 *
 * <p>Subscription destinations (clients subscribe, server publishes):
 * <ul>
 *   <li>{@link #ANNOUNCEMENTS_TOPIC} — broadcast announcements consumed from the
 *       Kafka {@code broadcast-announcements} topic</li>
 *   <li>{@link #ADMIN_RESPONSES_TOPIC} — admin-only feed of user ACK/DISMISS responses</li>
 *   <li>{@link #CONFIRMATIONS_QUEUE} / {@link #ERRORS_QUEUE} — per-user queues,
 *       used with the user destination prefix as {@code /user/queue/**}</li>
 * </ul>
 *
 * <p>Application destinations (clients send, server handles):
 * {@code /app/announcement.send} (admin), {@code /app/announcement.ack},
 * {@code /app/announcement.dismiss}.
 */
public final class StompDestinations {

    public static final String APPLICATION_PREFIX = "/app";
    public static final String TOPIC_PREFIX = "/topic";
    public static final String QUEUE_PREFIX = "/queue";
    public static final String USER_PREFIX = "/user";

    public static final String ANNOUNCEMENTS_TOPIC = TOPIC_PREFIX + "/announcements";
    public static final String ADMIN_TOPIC_PREFIX = TOPIC_PREFIX + "/admin";
    public static final String ADMIN_RESPONSES_TOPIC = ADMIN_TOPIC_PREFIX + ".responses";

    public static final String CONFIRMATIONS_QUEUE = QUEUE_PREFIX + "/confirmations";
    public static final String ERRORS_QUEUE = QUEUE_PREFIX + "/errors";

    private StompDestinations() {
    }
}
