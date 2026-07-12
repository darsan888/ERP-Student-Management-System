package com.college.sms.repository;

import com.college.sms.entity.Fee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FeeRepository extends JpaRepository<Fee, Integer> {
    List<Fee> findByStudent_StudentId(Integer studentId);
    List<Fee> findByStatus(Fee.Status status);
}
