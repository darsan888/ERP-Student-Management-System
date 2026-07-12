package com.college.sms.repository;

import com.college.sms.entity.Semester;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SemesterRepository extends JpaRepository<Semester, Integer> {
    List<Semester> findByCourse_CourseId(Integer courseId);
    java.util.Optional<Semester> findByCourse_CourseIdAndSemesterNumber(Integer courseId, Integer semesterNumber);
}
