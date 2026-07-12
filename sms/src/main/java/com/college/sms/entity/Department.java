package com.college.sms.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "departments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer departmentId;

    @Column(nullable = false, unique = true, length = 100)
    private String departmentName;

    @Column(nullable = false, unique = true, length = 10)
    private String departmentCode;

    private String hodName;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
