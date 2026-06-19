package com.rentsphere.controller;

import com.rentsphere.dto.response.ApiResponse;
import com.rentsphere.dto.response.NotificationResponse;
import com.rentsphere.dto.response.PagedResponse;
import com.rentsphere.entity.Notification;
import com.rentsphere.service.NotificationService;
import com.rentsphere.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notification management")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get my notifications")
    public ResponseEntity<PagedResponse<NotificationResponse>> getMyNotifications(
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Long userId  = SecurityUtils.getCurrentUserId();
        var  result  = notificationService.getForUser(
            userId, PageRequest.of(page, size, Sort.by("createdAt").descending())
        );
        PagedResponse<NotificationResponse> response = PagedResponse.of(
            result.map(NotificationResponse::from)
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count")
    public ResponseEntity<Long> unreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(notificationService.countUnread(userId));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark a single notification as read")
    public ResponseEntity<ApiResponse> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read."));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<ApiResponse> markAllRead() {
        Long userId = SecurityUtils.getCurrentUserId();
        int  count  = notificationService.markAllRead(userId);
        return ResponseEntity.ok(ApiResponse.success(count + " notifications marked as read."));
    }
}
