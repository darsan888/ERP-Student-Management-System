package com.college.sms.repository;

import com.college.sms.entity.FacultySubject;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FacultySubjectRepository extends JpaRepository<FacultySubject, Integer> {
    List<FacultySubject> findByFaculty_FacultyId(Integer facultyId);
    List<FacultySubject> findBySubject_SubjectId(Integer subjectId);
}
