package com.college.sms.repository;

import com.college.sms.entity.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FacultyRepository extends JpaRepository<Faculty, Integer> {
    Optional<Faculty> findByUser_UserId(Integer userId);
    List<Faculty> findByDepartment_DepartmentId(Integer departmentId);
    boolean existsByEmployeeCode(String employeeCode);
}
