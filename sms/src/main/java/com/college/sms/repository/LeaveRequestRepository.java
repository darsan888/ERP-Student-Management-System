package com.college.sms.repository;

import com.college.sms.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Integer> {
    List<LeaveRequest> findByStudent_StudentId(Integer studentId);
    List<LeaveRequest> findByStatus(LeaveRequest.Status status);
    List<LeaveRequest> findByFaculty_FacultyIdAndStatus(Integer facultyId, LeaveRequest.Status status);
    List<LeaveRequest> findByFaculty_FacultyId(Integer facultyId);
}
