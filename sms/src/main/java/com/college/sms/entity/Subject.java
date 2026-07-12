package com.college.sms.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "subjects")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer subjectId;

    @Column(nullable = false, length = 100)
    private String subjectName;

    @Column(nullable = false, unique = true, length = 20)
    private String subjectCode;

    @ManyToOne
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @Builder.Default
    private Integer credits = 3;

    @Builder.Default
    private Boolean isLab = false;
}
