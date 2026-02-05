package com.peterscode.ecommerce_management_system.service;

import com.peterscode.ecommerce_management_system.model.dto.request.NotificationRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.NotificationResponse;
import com.peterscode.ecommerce_management_system.model.entity.Notification;

import java.util.List;

public interface NotificationService {
    void create(Notification notification); // Internal use
    void sendNotification(NotificationRequest request); // External use via Controller
    List<NotificationResponse> getUserNotifications(Long userId);
    List<NotificationResponse> getUnreadNotifications(Long userId);
    void markAsRead(Long notificationId);
    void markAllAsRead(Long userId);
    long countUnread(Long userId);
}