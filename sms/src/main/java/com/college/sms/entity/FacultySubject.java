package com.college.sms.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "faculty_subject", uniqueConstraints = @UniqueConstraint(columnNames = {"faculty_id", "subject_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FacultySubject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer facultySubjectId;

    @ManyToOne
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    @ManyToOne
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;
}
