package com.college.sms.service;

import com.college.sms.entity.Student;
import com.college.sms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * Powers the "Student Health Dashboard" + Attendance Risk Indicator +
 * simple AI-style performance classification described in the project spec.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AttendanceRepository attendanceRepository;
    private final MarksRepository marksRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final FeeRepository feeRepository;

    public enum RiskLevel { GREEN, YELLOW, RED }
    public enum PerformanceCategory { EXCELLENT, AVERAGE, NEEDS_IMPROVEMENT }

    /** Attendance percentage for a student across all subjects, 0-100. */
    public double attendancePercentage(Integer studentId) {
        long total = attendanceRepository.countTotalByStudent(studentId);
        if (total == 0) return 100.0;
        long present = attendanceRepository.countPresentByStudent(studentId);
        return round((present * 100.0) / total);
    }

    /** Green >= 85%, Yellow 75-84%, Red < 75% (the classic 75% eligibility cutoff). */
    public RiskLevel attendanceRisk(double attendancePercentage) {
        if (attendancePercentage >= 85) return RiskLevel.GREEN;
        if (attendancePercentage >= 75) return RiskLevel.YELLOW;
        return RiskLevel.RED;
    }

    public double averageMarks(Integer studentId) {
        Double avg = marksRepository.studentAverage(studentId);
        return avg == null ? 0.0 : round(avg);
    }

    /**
     * Simple, explainable rule-based classifier (documented as "AI-Based Performance
     * Prediction" in the spec) combining attendance and marks. A heavier ML model
     * can be swapped in later without changing the calling code.
     */
    public PerformanceCategory predictPerformance(double attendancePct, double avgMarks) {
        double score = (attendancePct * 0.4) + (avgMarks * 0.6);
        if (score >= 75) return PerformanceCategory.EXCELLENT;
        if (score >= 50) return PerformanceCategory.AVERAGE;
        return PerformanceCategory.NEEDS_IMPROVEMENT;
    }

    /** Overall academic score out of 100 combining attendance, marks and assignment completion. */
    public double overallAcademicScore(Integer studentId, long assignmentsAssigned) {
        double attendancePct = attendancePercentage(studentId);
        double avgMarks = averageMarks(studentId);
        long submitted = assignmentSubmissionRepository.countByStudent_StudentId(studentId);
        double assignmentCompletion = assignmentsAssigned == 0 ? 100.0
                : round((submitted * 100.0) / assignmentsAssigned);

        double score = (attendancePct * 0.3) + (avgMarks * 0.5) + (assignmentCompletion * 0.2);
        return round(score);
    }

    /** Builds the full "Student Health Dashboard" data map used by the student dashboard view. */
    public Map<String, Object> buildHealthDashboard(Student student, long assignmentsAssigned) {
        Map<String, Object> data = new HashMap<>();
        double attendancePct = attendancePercentage(student.getStudentId());
        double avgMarks = averageMarks(student.getStudentId());
        long submitted = assignmentSubmissionRepository.countByStudent_StudentId(student.getStudentId());

        BigDecimal feesPending = feeRepository.findByStudent_StudentId(student.getStudentId()).stream()
                .map(f -> f.getTotalAmount().subtract(f.getPaidAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        data.put("attendancePercentage", attendancePct);
        data.put("attendanceRisk", attendanceRisk(attendancePct));
        data.put("averageMarks", avgMarks);
        data.put("assignmentsSubmitted", submitted);
        data.put("assignmentsAssigned", assignmentsAssigned);
        data.put("feesPending", feesPending);
        data.put("leaveBalance", student.getLeaveBalance());
        data.put("overallScore", overallAcademicScore(student.getStudentId(), assignmentsAssigned));
        data.put("performanceCategory", predictPerformance(attendancePct, avgMarks));
        return data;
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
