package com.college.sms.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "marks", uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "subject_id", "exam_type"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Marks {
    public enum ExamType { INTERNAL1, INTERNAL2, INTERNAL3, SEMESTER, ASSIGNMENT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer marksId;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExamType examType;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal marksObtained;

    @Builder.Default
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal maxMarks = new BigDecimal("100");

    private String grade;
    private LocalDate examDate;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
