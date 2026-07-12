package com.college.sms.controller;

import com.college.sms.entity.*;
import com.college.sms.repository.*;
import com.college.sms.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class FacultyDashboardController {

    private final FacultyRepository facultyRepository;
    private final FacultySubjectRepository facultySubjectRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final MarksRepository marksRepository;
    private final NotificationRepository notificationRepository;
    private final SubjectRepository subjectRepository;
    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;

    private Faculty currentFaculty(CustomUserDetails principal) {
        return facultyRepository.findByUser_UserId(principal.getUserId())
                .orElseThrow(() -> new IllegalStateException("Faculty profile not found for this user"));
    }

    @GetMapping("/faculty/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails principal, Model model) {

        Faculty faculty = facultyRepository.findByUser_UserId(principal.getUserId())
                .orElseThrow(() -> new IllegalStateException("Faculty profile not found for this user"));

        model.addAttribute("faculty", faculty);
        model.addAttribute("assignedSubjects",
                facultySubjectRepository.findByFaculty_FacultyId(faculty.getFacultyId()));
        model.addAttribute("pendingLeaves",
                leaveRequestRepository.findByFaculty_FacultyIdAndStatus(
                        faculty.getFacultyId(),
                        com.college.sms.entity.LeaveRequest.Status.PENDING));
        model.addAttribute("myAssignments",
                assignmentRepository.findByFaculty_FacultyId(faculty.getFacultyId()));

        return "faculty/dashboard";
    }

    @GetMapping("/faculty/attendance")
    public String attendance(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        Faculty faculty = currentFaculty(principal);
        model.addAttribute("faculty", faculty);
        model.addAttribute("assignedSubjects", facultySubjectRepository.findByFaculty_FacultyId(faculty.getFacultyId()));
        return "faculty/attendance";
    }

    @GetMapping("/faculty/attendance/{subjectId}")
    public String attendanceEntry(@AuthenticationPrincipal CustomUserDetails principal,
                                   @PathVariable Integer subjectId,
                                   @RequestParam(value = "date", required = false) String date,
                                   Model model) {
        Faculty faculty = currentFaculty(principal);
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalStateException("Subject not found"));
        java.time.LocalDate attendanceDate = (date != null && !date.isBlank())
                ? java.time.LocalDate.parse(date) : java.time.LocalDate.now();

        List<Student> students = studentRepository.findByCurrentSemester_SemesterId(subject.getSemester().getSemesterId());
        List<Object[]> rows = students.stream()
                .map(s -> new Object[]{s, attendanceRepository
                        .findByStudent_StudentIdAndSubject_SubjectIdAndAttendanceDate(s.getStudentId(), subjectId, attendanceDate)
                        .orElse(null)})
                .toList();

        model.addAttribute("faculty", faculty);
        model.addAttribute("subject", subject);
        model.addAttribute("attendanceDate", attendanceDate);
        model.addAttribute("rows", rows);
        model.addAttribute("statuses", Attendance.Status.values());
        return "faculty/attendance-entry";
    }

    @PostMapping("/faculty/attendance/{subjectId}/save")
    public String saveAttendance(@AuthenticationPrincipal CustomUserDetails principal,
                                  @PathVariable Integer subjectId,
                                  @RequestParam("attendanceDate") String date,
                                  @RequestParam Map<String, String> allParams) {
        Faculty faculty = currentFaculty(principal);
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalStateException("Subject not found"));
        java.time.LocalDate attendanceDate = java.time.LocalDate.parse(date);

        allParams.forEach((key, value) -> {
            if (!key.startsWith("status_") || value == null || value.isBlank()) return;
            Integer studentId = Integer.valueOf(key.substring("status_".length()));
            Student student = studentRepository.findById(studentId).orElse(null);
            if (student == null) return;

            Attendance attendance = attendanceRepository
                    .findByStudent_StudentIdAndSubject_SubjectIdAndAttendanceDate(studentId, subjectId, attendanceDate)
                    .orElse(Attendance.builder()
                            .student(student)
                            .subject(subject)
                            .attendanceDate(attendanceDate)
                            .build());
            attendance.setFaculty(faculty);
            attendance.setStatus(Attendance.Status.valueOf(value));
            attendanceRepository.save(attendance);
        });

        return "redirect:/faculty/attendance/" + subjectId + "?date=" + date;
    }

    @GetMapping("/faculty/marks")
    public String marks(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        Faculty faculty = currentFaculty(principal);
        model.addAttribute("faculty", faculty);
        model.addAttribute("assignedSubjects", facultySubjectRepository.findByFaculty_FacultyId(faculty.getFacultyId()));
        return "faculty/marks";
    }

    @GetMapping("/faculty/marks/{subjectId}")
    public String marksEntry(@AuthenticationPrincipal CustomUserDetails principal,
                              @PathVariable Integer subjectId,
                              @RequestParam(value = "examType", required = false, defaultValue = "INTERNAL1") Marks.ExamType examType,
                              Model model) {
        Faculty faculty = currentFaculty(principal);
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalStateException("Subject not found"));

        List<Student> students = studentRepository.findByCurrentSemester_SemesterId(subject.getSemester().getSemesterId());
        List<Object[]> rows = students.stream()
                .map(s -> new Object[]{s, marksRepository
                        .findByStudent_StudentIdAndSubject_SubjectIdAndExamType(s.getStudentId(), subjectId, examType)
                        .orElse(null)})
                .toList();

        model.addAttribute("faculty", faculty);
        model.addAttribute("subject", subject);
        model.addAttribute("examType", examType);
        model.addAttribute("examTypes", Marks.ExamType.values());
        model.addAttribute("rows", rows);
        return "faculty/marks-entry";
    }

    @PostMapping("/faculty/marks/{subjectId}/save")
    public String saveMarks(@AuthenticationPrincipal CustomUserDetails principal,
                             @PathVariable Integer subjectId,
                             @RequestParam("examType") Marks.ExamType examType,
                             @RequestParam("maxMarks") BigDecimal maxMarks,
                             @RequestParam Map<String, String> allParams) {
        Faculty faculty = currentFaculty(principal);
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalStateException("Subject not found"));

        allParams.forEach((key, value) -> {
            if (!key.startsWith("marks_") || value == null || value.isBlank()) return;
            Integer studentId = Integer.valueOf(key.substring("marks_".length()));
            Student student = studentRepository.findById(studentId).orElse(null);
            if (student == null) return;

            Marks marksRow = marksRepository
                    .findByStudent_StudentIdAndSubject_SubjectIdAndExamType(studentId, subjectId, examType)
                    .orElse(Marks.builder()
                            .student(student)
                            .subject(subject)
                            .faculty(faculty)
                            .examType(examType)
                            .build());
            marksRow.setFaculty(faculty);
            marksRow.setMarksObtained(new BigDecimal(value));
            marksRow.setMaxMarks(maxMarks);
            marksRow.setExamDate(java.time.LocalDate.now());
            marksRepository.save(marksRow);
        });

        return "redirect:/faculty/marks/" + subjectId + "?examType=" + examType;
    }

    @GetMapping("/faculty/assignments")
    public String assignments(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        Faculty faculty = currentFaculty(principal);
        model.addAttribute("faculty", faculty);
        model.addAttribute("myAssignments", assignmentRepository.findByFaculty_FacultyId(faculty.getFacultyId()));
        model.addAttribute("assignedSubjects", facultySubjectRepository.findByFaculty_FacultyId(faculty.getFacultyId()));
        return "faculty/assignments";
    }

    @PostMapping("/faculty/assignments/add")
    public String addAssignment(@AuthenticationPrincipal CustomUserDetails principal,
                                 @RequestParam Integer subjectId,
                                 @RequestParam String title,
                                 @RequestParam(required = false) String description,
                                 @RequestParam("dueDate") String dueDate,
                                 @RequestParam BigDecimal maxMarks) {
        Faculty faculty = currentFaculty(principal);
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalStateException("Subject not found"));

        assignmentRepository.save(Assignment.builder()
                .subject(subject)
                .faculty(faculty)
                .title(title)
                .description(description)
                .dueDate(java.time.LocalDateTime.parse(dueDate))
                .maxMarks(maxMarks)
                .build());

        return "redirect:/faculty/assignments";
    }

    @GetMapping("/faculty/assignments/{id}/submissions")
    public String assignmentSubmissions(@AuthenticationPrincipal CustomUserDetails principal,
                                         @PathVariable("id") Integer assignmentId,
                                         Model model) {
        Faculty faculty = currentFaculty(principal);
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalStateException("Assignment not found"));
        if (!assignment.getFaculty().getFacultyId().equals(faculty.getFacultyId())) {
            throw new IllegalStateException("This assignment was not posted by you");
        }

        List<Student> classStudents = studentRepository
                .findByCurrentSemester_SemesterId(assignment.getSubject().getSemester().getSemesterId());
        List<AssignmentSubmission> submissions = assignmentSubmissionRepository
                .findByAssignment_AssignmentId(assignmentId);

        java.util.Set<Integer> submittedStudentIds = submissions.stream()
                .map(sub -> sub.getStudent().getStudentId())
                .collect(java.util.stream.Collectors.toSet());
        List<Student> notSubmitted = classStudents.stream()
                .filter(s -> !submittedStudentIds.contains(s.getStudentId()))
                .toList();

        model.addAttribute("faculty", faculty);
        model.addAttribute("assignment", assignment);
        model.addAttribute("submissions", submissions);
        model.addAttribute("notSubmitted", notSubmitted);
        model.addAttribute("submittedCount", submissions.size());
        model.addAttribute("totalCount", classStudents.size());
        return "faculty/assignment-submissions";
    }

    @PostMapping("/faculty/assignments/{id}/delete")
    public String deleteAssignment(@AuthenticationPrincipal CustomUserDetails principal,
                                    @PathVariable("id") Integer assignmentId,
                                    RedirectAttributes redirectAttributes) {
        Faculty faculty = currentFaculty(principal);
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalStateException("Assignment not found"));
        if (!assignment.getFaculty().getFacultyId().equals(faculty.getFacultyId())) {
            throw new IllegalStateException("This assignment was not posted by you");
        }
        assignmentSubmissionRepository.findByAssignment_AssignmentId(assignmentId)
                .forEach(sub -> assignmentSubmissionRepository.deleteById(sub.getSubmissionId()));
        assignmentRepository.deleteById(assignmentId);
        redirectAttributes.addFlashAttribute("success", "Assignment deleted.");
        return "redirect:/faculty/assignments";
    }

    @GetMapping("/faculty/leaves")
    public String leaves(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        Faculty faculty = currentFaculty(principal);
        model.addAttribute("faculty", faculty);
        model.addAttribute("leaveRequests", leaveRequestRepository.findByFaculty_FacultyId(faculty.getFacultyId()));
        return "faculty/leaves";
    }

    @PostMapping("/faculty/leaves/{id}/approve")
    public String approveLeave(@AuthenticationPrincipal CustomUserDetails principal,
                                @PathVariable("id") Integer leaveId,
                                @RequestParam(value = "remarks", required = false) String remarks) {
        decideLeave(principal, leaveId, LeaveRequest.Status.APPROVED, remarks);
        return "redirect:/faculty/leaves";
    }

    @PostMapping("/faculty/leaves/{id}/reject")
    public String rejectLeave(@AuthenticationPrincipal CustomUserDetails principal,
                               @PathVariable("id") Integer leaveId,
                               @RequestParam(value = "remarks", required = false) String remarks) {
        decideLeave(principal, leaveId, LeaveRequest.Status.REJECTED, remarks);
        return "redirect:/faculty/leaves";
    }

    private void decideLeave(CustomUserDetails principal, Integer leaveId, LeaveRequest.Status status, String remarks) {
        Faculty faculty = currentFaculty(principal);
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> new IllegalStateException("Leave request not found"));
        if (leaveRequest.getFaculty() == null || !leaveRequest.getFaculty().getFacultyId().equals(faculty.getFacultyId())) {
            throw new IllegalStateException("This leave request is not assigned to you");
        }
        leaveRequest.setStatus(status);
        leaveRequest.setDecidedAt(LocalDateTime.now());
        leaveRequest.setRemarks(remarks);
        leaveRequestRepository.save(leaveRequest);

        if (leaveRequest.getStudent() != null) {
            notificationRepository.save(Notification.builder()
                    .title("Leave Request " + status.name())
                    .message("Your leave request (" + leaveRequest.getFromDate() + " to " + leaveRequest.getToDate()
                            + ") has been " + status.name().toLowerCase()
                            + (remarks != null && !remarks.isBlank() ? (" - " + remarks) : "."))
                    .notificationType(Notification.Type.LEAVE)
                    .targetRole(Notification.TargetRole.ALL)
                    .targetUser(leaveRequest.getStudent().getUser())
                    .build());
        }
    }

    @GetMapping("/faculty/notifications")
    public String notifications(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        Faculty faculty = currentFaculty(principal);
        List<com.college.sms.entity.Notification> byUser = notificationRepository
                .findByTargetUser_UserIdOrderByCreatedAtDesc(faculty.getUser().getUserId());
        List<com.college.sms.entity.Notification> byRole = notificationRepository
                .findByTargetRoleOrderByCreatedAtDesc(com.college.sms.entity.Notification.TargetRole.FACULTY);
        List<com.college.sms.entity.Notification> all = notificationRepository
                .findByTargetRoleOrderByCreatedAtDesc(com.college.sms.entity.Notification.TargetRole.ALL);
        List<com.college.sms.entity.Notification> combined = new java.util.ArrayList<>();
        combined.addAll(byUser);
        combined.addAll(byRole);
        combined.addAll(all);
        combined.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        model.addAttribute("notifications", combined);
        return "faculty/notifications";
    }

    @GetMapping("/faculty/reports")
    public String reports(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        Faculty faculty = currentFaculty(principal);
        model.addAttribute("faculty", faculty);
        model.addAttribute("assignedSubjects", facultySubjectRepository.findByFaculty_FacultyId(faculty.getFacultyId()));
        return "faculty/reports";
    }

    @GetMapping("/faculty/reports/export/{subjectId}")
    public String exportSubjectReport(@AuthenticationPrincipal CustomUserDetails principal,
                                       @PathVariable Integer subjectId, Model model) {
        Faculty faculty = currentFaculty(principal);
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalStateException("Subject not found"));
        List<Student> students = studentRepository.findByCurrentSemester_SemesterId(subject.getSemester().getSemesterId());

        List<Object[]> rows = students.stream()
                .map(s -> {
                    List<Marks> studentMarks = marksRepository.findByStudent_StudentIdAndSubject_SubjectId(s.getStudentId(), subjectId);
                    java.util.Map<String, String> display = new java.util.LinkedHashMap<>();
                    for (Marks.ExamType type : Marks.ExamType.values()) {
                        String cell = studentMarks.stream()
                                .filter(m -> m.getExamType() == type)
                                .findFirst()
                                .map(m -> m.getMarksObtained() + "/" + m.getMaxMarks())
                                .orElse("-");
                        display.put(type.name(), cell);
                    }
                    return new Object[]{s, display};
                }).toList();

        model.addAttribute("faculty", faculty);
        model.addAttribute("subject", subject);
        model.addAttribute("rows", rows);
        model.addAttribute("examTypes", Marks.ExamType.values());
        model.addAttribute("generatedAt", LocalDateTime.now());
        return "faculty/report-print";
    }
}