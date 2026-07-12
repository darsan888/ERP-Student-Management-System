package com.college.sms.repository;

import com.college.sms.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    List<Notification> findByTargetUser_UserIdOrderByCreatedAtDesc(Integer userId);
    List<Notification> findByTargetRoleOrderByCreatedAtDesc(Notification.TargetRole targetRole);
    List<Notification> findAllByOrderByCreatedAtDesc();
}
