package com.college.sms.repository;

import com.college.sms.entity.Timetable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TimetableRepository extends JpaRepository<Timetable, Integer> {
    List<Timetable> findBySemester_SemesterId(Integer semesterId);
    List<Timetable> findByFaculty_FacultyId(Integer facultyId);
}
