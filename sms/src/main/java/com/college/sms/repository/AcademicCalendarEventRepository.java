package com.college.sms.repository;

import com.college.sms.entity.AcademicCalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AcademicCalendarEventRepository extends JpaRepository<AcademicCalendarEvent, Integer> {
    List<AcademicCalendarEvent> findByAcademicYear_AcademicYearId(Integer academicYearId);
}
