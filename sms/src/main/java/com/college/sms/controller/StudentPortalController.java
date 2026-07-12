package com.college.sms.controller;

import com.college.sms.entity.*;
import com.college.sms.repository.*;
import com.college.sms.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class StudentPortalController {

    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final MarksRepository marksRepository;
    private final TimetableRepository timetableRepository;
    private final SubjectRepository subjectRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final FeeRepository feeRepository;
    private final NotificationRepository notificationRepository;
    private final FacultyRepository facultyRepository;

    private Student currentStudent(CustomUserDetails principal) {
        return studentRepository.findByUser_UserId(principal.getUserId())
                .orElseThrow(() -> new IllegalStateException("Student profile not found for this user"));
    }

    @GetMapping("/student/attendance")
    public String attendance(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        Student student = currentStudent(principal);
        List<Subject> subjects = subjectRepository.findBySemester_SemesterId(student.getCurrentSemester().getSemesterId());

        model.addAttribute("student", student);
        model.addAttribute("subjects", subjects);
        model.addAttribute("subjectStats", subjects.stream().map(subject -> {
            long total = attendanceRepository.findByStudent_StudentIdAndSubject_SubjectId(student.getStudentId(), subject.getSubjectId()).size();
            long present = attendanceRepository.findByStudent_StudentIdAndSubject_SubjectId(student.getStudentId(), subject.getSubjectId())
                    .stream().filter(a -> a.getStatus() == Attendance.Status.PRESENT).count();
            double pct = total == 0 ? 100.0 : (present * 100.0 / total);
            return new Object[]{subject, total, present, Math.round(pct * 10) / 10.0};
        }).toList());

        long overallTotal = attendanceRepository.countTotalByStudent(student.getStudentId());
        long overallPresent = attendanceRepository.countPresentByStudent(student.getStudentId());
        double overallPct = overallTotal == 0 ? 100.0 : (overallPresent * 100.0 / overallTotal);
        model.addAttribute("overallPercentage", Math.round(overallPct * 10) / 10.0);

        return "student/attendance";
    }

    @GetMapping("/student/marks")
    public String marks(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        Student student = currentStudent(principal);
        List<Marks> marksList = marksRepository.findByStudent_StudentId(student.getStudentId());
        Double average = marksRepository.studentAverage(student.getStudentId());

        model.addAttribute("student", student);
        model.addAttribute("marksList", marksList);
        model.addAttribute("average", average == null ? 0 : Math.round(average * 10) / 10.0);
        return "student/marks";
    }

    @GetMapping("/student/timetable")
    public String timetable(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        Student student = currentStudent(principal);
        List<Timetable> entries = timetableRepository.findBySemester_SemesterId(student.getCurrentSemester().getSemesterId());
        model.addAttribute("student", student);
        model.addAttribute("entries", entries);
        model.addAttribute("days", Timetable.DayOfWeek.values());
        return "student/timetable";
    }

    @GetMapping("/student/assignments")
    public String assignments(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        Student student = currentStudent(principal);
        List<Subject> subjects = subjectRepository.findBySemester_SemesterId(student.getCurrentSemester().getSemesterId());

        List<Object[]> rows = subjects.stream()
                .flatMap(subject -> assignmentRepository.findBySubject_SubjectId(subject.getSubjectId()).stream())
                .map(assignment -> {
                    var submission = assignmentSubmissionRepository
                            .findByAssignment_AssignmentIdAndStudent_StudentId(assignment.getAssignmentId(), student.getStudentId());
                    return new Object[]{assignment, submission.orElse(null)};
                }).toList();

        model.addAttribute("student", student);
        model.addAttribute("rows", rows);
        return "student/assignments";
    }

    @PostMapping("/student/assignments/{id}/submit")
    public String submitAssignment(@PathVariable("id") Integer assignmentId,
                                    @RequestParam(value = "fileUrl", required = false) String fileUrl,
                                    @AuthenticationPrincipal CustomUserDetails principal) {
        Student student = currentStudent(principal);
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalStateException("Assignment not found"));

        AssignmentSubmission submission = assignmentSubmissionRepository
                .findByAssignment_AssignmentIdAndStudent_StudentId(assignmentId, student.getStudentId())
                .orElse(AssignmentSubmission.builder()
                        .assignment(assignment)
                        .student(student)
                        .build());

        submission.setFileUrl(fileUrl);
        submission.setSubmittedAt(LocalDateTime.now());
        boolean late = assignment.getDueDate() != null && LocalDateTime.now().isAfter(assignment.getDueDate());
        submission.setStatus(late ? AssignmentSubmission.Status.LATE : AssignmentSubmission.Status.SUBMITTED);
        assignmentSubmissionRepository.save(submission);

        return "redirect:/student/assignments";
    }

    @GetMapping("/student/leaves")
    public String leaves(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        Student student = currentStudent(principal);
        model.addAttribute("student", student);
        model.addAttribute("leaveRequests", leaveRequestRepository.findByStudent_StudentId(student.getStudentId()));
        model.addAttribute("facultyList", facultyRepository.findByDepartment_DepartmentId(student.getDepartment().getDepartmentId()));
        return "student/leaves";
    }

    @PostMapping("/student/leaves/apply")
    public String applyLeave(@AuthenticationPrincipal CustomUserDetails principal,
                              @RequestParam String reason,
                              @RequestParam("fromDate") String fromDate,
                              @RequestParam("toDate") String toDate,
                              @RequestParam(value = "facultyId", required = false) Integer facultyId) {
        Student student = currentStudent(principal);
        LeaveRequest.LeaveRequestBuilder builder = LeaveRequest.builder()
                .student(student)
                .reason(reason)
                .fromDate(java.time.LocalDate.parse(fromDate))
                .toDate(java.time.LocalDate.parse(toDate))
                .status(LeaveRequest.Status.PENDING);
        if (facultyId != null) {
            facultyRepository.findById(facultyId).ifPresent(builder::faculty);
        }
        leaveRequestRepository.save(builder.build());
        return "redirect:/student/leaves";
    }

    @GetMapping("/student/fees")
    public String fees(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        Student student = currentStudent(principal);
        List<Fee> fees = feeRepository.findByStudent_StudentId(student.getStudentId());

        java.math.BigDecimal totalDue = fees.stream()
                .map(f -> f.getTotalAmount().subtract(f.getPaidAmount()))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        java.math.BigDecimal totalPaid = fees.stream()
                .map(Fee::getPaidAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        model.addAttribute("student", student);
        model.addAttribute("fees", fees);
        model.addAttribute("pendingFees", fees.stream().filter(f -> f.getStatus() != Fee.Status.PAID).toList());
        model.addAttribute("paidFees", fees.stream().filter(f -> f.getStatus() == Fee.Status.PAID).toList());
        model.addAttribute("totalDue", totalDue);
        model.addAttribute("totalPaid", totalPaid);
        return "student/fees";
    }

    @PostMapping("/student/fees/{feeId}/pay")
    public String payFee(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Integer feeId) {
        Student student = currentStudent(principal);
        Fee fee = feeRepository.findById(feeId)
                .orElseThrow(() -> new IllegalStateException("Fee record not found"));
        if (!fee.getStudent().getStudentId().equals(student.getStudentId())) {
            throw new IllegalStateException("This fee record does not belong to you");
        }
        fee.setPaidAmount(fee.getTotalAmount());
        fee.setStatus(Fee.Status.PAID);
        fee.setPaidDate(java.time.LocalDate.now());
        fee.setReceiptNumber("RCPT-" + student.getRegisterNumber() + "-" + fee.getFeeId());
        feeRepository.save(fee);
        return "redirect:/student/fees";
    }

    @GetMapping("/student/fees/receipt/{feeId}")
    public String feeReceipt(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Integer feeId, Model model) {
        Student student = currentStudent(principal);
        Fee fee = feeRepository.findById(feeId)
                .orElseThrow(() -> new IllegalStateException("Fee record not found"));
        if (!fee.getStudent().getStudentId().equals(student.getStudentId())) {
            throw new IllegalStateException("This fee record does not belong to you");
        }
        model.addAttribute("student", student);
        model.addAttribute("fee", fee);
        return "student/fee-receipt";
    }

    @GetMapping("/student/notifications")
    public String notifications(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        Student student = currentStudent(principal);
        List<Notification> byUser = notificationRepository.findByTargetUser_UserIdOrderByCreatedAtDesc(student.getUser().getUserId());
        List<Notification> byRole = notificationRepository.findByTargetRoleOrderByCreatedAtDesc(Notification.TargetRole.STUDENT);
        List<Notification> all = notificationRepository.findByTargetRoleOrderByCreatedAtDesc(Notification.TargetRole.ALL);

        List<Notification> combined = new java.util.ArrayList<>();
        combined.addAll(byUser);
        combined.addAll(byRole);
        combined.addAll(all);
        combined.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        model.addAttribute("notifications", combined);
        return "student/notifications";
    }

    @GetMapping("/student/profile")
    public String profile(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        Student student = currentStudent(principal);
        model.addAttribute("student", student);
        return "student/profile";
    }
}
