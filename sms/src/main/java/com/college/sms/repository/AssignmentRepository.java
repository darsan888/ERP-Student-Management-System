package com.college.sms.repository;

import com.college.sms.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Integer> {
    List<Assignment> findBySubject_SubjectId(Integer subjectId);
    List<Assignment> findByFaculty_FacultyId(Integer facultyId);
}
