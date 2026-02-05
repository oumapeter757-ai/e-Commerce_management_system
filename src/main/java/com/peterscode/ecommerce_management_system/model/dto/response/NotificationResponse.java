package com.peterscode.ecommerce_management_system.model.dto.response;

import com.peterscode.ecommerce_management_system.model.enums.NotificationType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificationResponse {
    private Long id;
    private String title;
    private String message;
    private NotificationType type;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private String actionUrl;
    private String referenceType;
    private Long referenceId;
}