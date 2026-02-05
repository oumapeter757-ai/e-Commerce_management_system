package com.peterscode.ecommerce_management_system.service.impl;

import com.peterscode.ecommerce_management_system.exception.ResourceNotFoundException;
import com.peterscode.ecommerce_management_system.mapper.NotificationMapper;
import com.peterscode.ecommerce_management_system.model.dto.request.NotificationRequest;
import com.peterscode.ecommerce_management_system.model.dto.response.NotificationResponse;
import com.peterscode.ecommerce_management_system.model.entity.Notification;
import com.peterscode.ecommerce_management_system.model.entity.User;
import com.peterscode.ecommerce_management_system.repository.NotificationRepository;
import com.peterscode.ecommerce_management_system.repository.UserRepository;
import com.peterscode.ecommerce_management_system.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// Optional: for WebSocket
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImplementation implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;

    // Optional: Inject WebSocket template if using real-time notifications
    // private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Async // Async creation to not block main transaction
    public void create(Notification notification) {
        notificationRepository.save(notification);
        // Optional: Trigger WebSocket push here
        // messagingTemplate.convertAndSendToUser(notification.getUser().getEmail(), "/queue/notifications", notification);
    }

    @Override
    @Transactional
    public void sendNotification(NotificationRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Notification notification = Notification.builder()
                .user(user)
                .type(request.getType())
                .title(request.getTitle())
                .message(request.getMessage())
                .referenceType(request.getReferenceType())
                .referenceId(request.getReferenceId())
                .priority(request.getPriority())
                .build();

        create(notification);
    }

    @Override
    public List<NotificationResponse> getUserNotifications(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return notificationRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(notificationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationResponse> getUnreadNotifications(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user)
                .stream()
                .map(notificationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.markAsRead();
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        List<Notification> unread = notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user);
        unread.forEach(Notification::markAsRead);
        notificationRepository.saveAll(unread);
    }

    @Override
    public long countUnread(Long userId) {
        User user = userRepository.getReferenceById(userId);
        return notificationRepository.countByUserAndIsReadFalse(user);
    }
}