package com.college.sms.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "semester")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Semester {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer semesterId;

    @Column(nullable = false)
    private Integer semesterNumber;

    @ManyToOne
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
}
