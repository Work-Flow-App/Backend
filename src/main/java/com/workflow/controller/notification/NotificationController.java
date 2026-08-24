package com.workflow.controller.notification;

import com.workflow.dto.notification.CursorPagedResponse;
import com.workflow.dto.notification.NotificationResponse;
import com.workflow.entity.auth.User;
import com.workflow.service.notification.INotificationManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Notifications")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final INotificationManagementService managementService;

    private Long getUserId(Authentication auth) {
        return ((User) auth.getPrincipal()).getId();
    }

    @Operation(summary = "Get user notifications via Cursor Pagination")
    @GetMapping
    public ResponseEntity<CursorPagedResponse<NotificationResponse>> getNotifications(
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        return ResponseEntity.ok(managementService.getUserNotifications(getUserId(auth), unreadOnly, cursor, size));
    }

    @Operation(summary = "Get unread notification count")
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication auth) {
        long count = managementService.getUnreadCount(getUserId(auth));
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @Operation(summary = "Mark single notification as read")
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, Authentication auth) {
        managementService.markAsRead(id, getUserId(auth));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Mark all notifications as read")
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Authentication auth) {
        managementService.markAllAsRead(getUserId(auth));
        return ResponseEntity.noContent().build();
    }
}