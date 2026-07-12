package com.college.sms.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {
    public enum Type { ATTENDANCE, ASSIGNMENT, FEES, MARKS, LEAVE, GENERAL }
    public enum TargetRole { ALL, ADMIN, FACULTY, STUDENT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer notificationId;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type notificationType;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TargetRole targetRole = TargetRole.ALL;

    @ManyToOne
    @JoinColumn(name = "target_user_id")
    private User targetUser;

    @Builder.Default
    private Boolean isRead = false;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
