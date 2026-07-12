package com.college.sms.repository;

import com.college.sms.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Integer> {
    List<Subject> findBySemester_SemesterId(Integer semesterId);
}
