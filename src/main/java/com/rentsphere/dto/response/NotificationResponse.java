package com.rentsphere.dto.response;

import com.rentsphere.entity.Notification;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class NotificationResponse {
    private Long   id;
    private String type;
    private String subject;
    private String body;
    private boolean isRead;
    private Instant createdAt;
    private Instant sentAt;

    public static NotificationResponse from(Notification n) {
        return NotificationResponse.builder()
            .id(n.getId())
            .type(n.getType().name())
            .subject(n.getSubject())
            .body(n.getBody())
            .isRead(Boolean.TRUE.equals(n.getIsRead()))
            .createdAt(n.getCreatedAt())
            .sentAt(n.getSentAt())
            .build();
    }
}
