package com.college.sms.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "academic_calendar")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AcademicCalendarEvent {
    public enum EventType { HOLIDAY, EXAM, EVENT, DEADLINE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer eventId;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private LocalDate eventDate;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EventType eventType = EventType.EVENT;

    @ManyToOne
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;
}
