package com.college.sms.repository;

import com.college.sms.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Integer> {
    List<Course> findByDepartment_DepartmentId(Integer departmentId);
}
