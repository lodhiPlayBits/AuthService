package com.lodhi.notification.contract.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Single wire schema for broadcast announcements on the {@code broadcast-announcements}
 * topic. Produced by the notification-service STOMP gateway (admin action) and consumed
 * by its own fan-out consumer, which publishes to {@code /topic/announcements}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BroadcastAnnouncementEvent implements Serializable {

    private String announcementId;
    private String title;
    private String message;
    private String priority;
    private Long sentBy;
    private Instant timestamp;
}
