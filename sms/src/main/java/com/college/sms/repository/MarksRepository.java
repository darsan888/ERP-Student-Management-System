package com.college.sms.repository;

import com.college.sms.entity.Marks;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface MarksRepository extends JpaRepository<Marks, Integer> {
    List<Marks> findByStudent_StudentId(Integer studentId);
    List<Marks> findBySubject_SubjectId(Integer subjectId);
    List<Marks> findByStudent_StudentIdAndSubject_SubjectId(Integer studentId, Integer subjectId);
    java.util.Optional<Marks> findByStudent_StudentIdAndSubject_SubjectIdAndExamType(Integer studentId, Integer subjectId, Marks.ExamType examType);

    @Query("SELECT AVG(m.marksObtained) FROM Marks m WHERE m.subject.subjectId = :subjectId")
    Double classAverage(Integer subjectId);

    @Query("SELECT AVG(m.marksObtained) FROM Marks m WHERE m.student.studentId = :studentId")
    Double studentAverage(Integer studentId);
}
