package com.college.sms.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "students")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer studentId;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, unique = true, length = 30)
    private String registerNumber;

    @Column(nullable = false, length = 100)
    private String fullName;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne
    @JoinColumn(name = "current_semester_id", nullable = false)
    private Semester currentSemester;

    private LocalDate dateOfBirth;
    private String gender;
    private String phone;
    private String address;

    @Column(nullable = false)
    private Integer admissionYear;

    private String photoUrl;
    private String guardianName;
    private String guardianPhone;

    @Builder.Default
    private Integer leaveBalance = 12;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
