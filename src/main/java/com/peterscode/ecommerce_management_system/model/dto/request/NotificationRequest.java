package com.peterscode.ecommerce_management_system.model.dto.request;

import com.peterscode.ecommerce_management_system.model.enums.NotificationType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationRequest {
    private Long userId;
    private NotificationType type;
    private String title;
    private String message;
    private String referenceType;
    private Long referenceId;
    private Integer priority;
}