package com.college.sms.repository;

import com.college.sms.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Integer> {
    List<Attendance> findByStudent_StudentId(Integer studentId);
    List<Attendance> findByStudent_StudentIdAndSubject_SubjectId(Integer studentId, Integer subjectId);
    List<Attendance> findBySubject_SubjectIdAndAttendanceDate(Integer subjectId, LocalDate date);
    Optional<Attendance> findByStudent_StudentIdAndSubject_SubjectIdAndAttendanceDate(
            Integer studentId, Integer subjectId, LocalDate date);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student.studentId = :studentId AND a.status = 'PRESENT'")
    long countPresentByStudent(Integer studentId);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student.studentId = :studentId")
    long countTotalByStudent(Integer studentId);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.subject.subjectId = :subjectId AND a.status = 'PRESENT'")
    long countPresentBySubject(Integer subjectId);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.subject.subjectId = :subjectId")
    long countTotalBySubject(Integer subjectId);
}
