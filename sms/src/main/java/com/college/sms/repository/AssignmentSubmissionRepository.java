package com.college.sms.repository;

import com.college.sms.entity.AssignmentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, Integer> {
    List<AssignmentSubmission> findByAssignment_AssignmentId(Integer assignmentId);
    List<AssignmentSubmission> findByStudent_StudentId(Integer studentId);
    Optional<AssignmentSubmission> findByAssignment_AssignmentIdAndStudent_StudentId(Integer assignmentId, Integer studentId);
    long countByStudent_StudentId(Integer studentId);
}
