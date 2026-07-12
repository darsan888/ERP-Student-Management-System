package com.college.sms.repository;

import com.college.sms.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Integer> {
    boolean existsByDepartmentCode(String departmentCode);
}
