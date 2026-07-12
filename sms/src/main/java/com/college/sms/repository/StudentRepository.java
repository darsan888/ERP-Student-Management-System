package com.college.sms.repository;

import com.college.sms.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Integer> {
    Optional<Student> findByUser_UserId(Integer userId);
    Optional<Student> findByRegisterNumber(String registerNumber);
    boolean existsByRegisterNumber(String registerNumber);
    List<Student> findByDepartment_DepartmentId(Integer departmentId);
    List<Student> findByCurrentSemester_SemesterId(Integer semesterId);

    // Advanced search
    @org.springframework.data.jpa.repository.Query("""
        SELECT s FROM Student s
        WHERE (:departmentId IS NULL OR s.department.departmentId = :departmentId)
        AND (:semesterId IS NULL OR s.currentSemester.semesterId = :semesterId)
        AND (:name IS NULL OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :name, '%')))
        AND (:registerNumber IS NULL OR LOWER(s.registerNumber) LIKE LOWER(CONCAT('%', :registerNumber, '%')))
        """)
    List<Student> advancedSearch(Integer departmentId, Integer semesterId, String name, String registerNumber);
}
