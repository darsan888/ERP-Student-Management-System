package com.college.sms.controller;

import com.college.sms.entity.*;
import com.college.sms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class AdminController {

    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;
    private final SubjectRepository subjectRepository;
    private final FacultyRepository facultyRepository;
    private final StudentRepository studentRepository;
    private final AcademicCalendarEventRepository academicCalendarEventRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final NotificationRepository notificationRepository;
    private final SemesterRepository semesterRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AcademicYearRepository academicYearRepository;
    private final FacultySubjectRepository facultySubjectRepository;
    private final AttendanceRepository attendanceRepository;
    private final MarksRepository marksRepository;

    @GetMapping("/admin/departments")
    public String departments(Model model) {
        model.addAttribute("departments", departmentRepository.findAll());
        return "admin/departments";
    }

    @PostMapping("/admin/departments/add")
    public String addDepartment(@RequestParam String departmentName,
                                 @RequestParam String departmentCode,
                                 @RequestParam(required = false) String hodName) {
        if (!departmentRepository.existsByDepartmentCode(departmentCode)) {
            departmentRepository.save(Department.builder()
                    .departmentName(departmentName)
                    .departmentCode(departmentCode)
                    .hodName(hodName)
                    .build());
        }
        return "redirect:/admin/departments";
    }

    /**
     * The UI no longer exposes "Courses" as something the admin manages directly.
     * Internally we still need a Course row to satisfy the semester/subject/student
     * schema, so we transparently find-or-create one default course per department.
     */
    private Course defaultCourseForDepartment(Department department) {
        List<Course> existing = courseRepository.findByDepartment_DepartmentId(department.getDepartmentId());
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        return courseRepository.save(Course.builder()
                .courseName(department.getDepartmentName())
                .courseCode(department.getDepartmentCode() + "-GEN")
                .department(department)
                .durationYears(4)
                .build());
    }

    @GetMapping("/admin/subjects")
    public String subjects(Model model) {
        model.addAttribute("subjects", subjectRepository.findAll());
        model.addAttribute("departments", departmentRepository.findAll());
        return "admin/subjects";
    }

    @PostMapping("/admin/subjects/add")
    public String addSubject(@RequestParam String subjectName,
                              @RequestParam String subjectCode,
                              @RequestParam Integer departmentId,
                              @RequestParam Integer semesterNumber,
                              @RequestParam Integer credits,
                              @RequestParam(required = false) Boolean isLab,
                              RedirectAttributes redirectAttributes) {
        try {
            Department department = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new IllegalStateException("Department not found"));
            Course course = defaultCourseForDepartment(department);

            Semester semester = semesterRepository.findByCourse_CourseIdAndSemesterNumber(course.getCourseId(), semesterNumber)
                    .orElseGet(() -> semesterRepository.save(Semester.builder()
                            .course(course)
                            .semesterNumber(semesterNumber)
                            .academicYear(currentOrFirstAcademicYear())
                            .build()));

            subjectRepository.save(Subject.builder()
                    .subjectName(subjectName)
                    .subjectCode(subjectCode)
                    .semester(semester)
                    .credits(credits)
                    .isLab(isLab != null && isLab)
                    .build());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not add subject: " + ex.getMessage());
        }
        return "redirect:/admin/subjects";
    }

    @PostMapping("/admin/subjects/{id}/delete")
    public String deleteSubject(@PathVariable("id") Integer subjectId, RedirectAttributes redirectAttributes) {
        try {
            subjectRepository.deleteById(subjectId);
            redirectAttributes.addFlashAttribute("success", "Subject deleted.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error",
                    "Could not delete subject. It may still have attendance, marks, or assignments linked to it.");
        }
        return "redirect:/admin/subjects";
    }

    private AcademicYear currentOrFirstAcademicYear() {
        return academicYearRepository.findByIsCurrentTrue()
                .orElseGet(() -> academicYearRepository.findAll().stream().findFirst()
                        .orElseGet(() -> academicYearRepository.save(AcademicYear.builder()
                                .yearLabel(java.time.Year.now().getValue() + "-" + (java.time.Year.now().getValue() + 1))
                                .startDate(java.time.LocalDate.now())
                                .endDate(java.time.LocalDate.now().plusYears(1))
                                .isCurrent(true)
                                .build())));
    }

    @GetMapping("/admin/faculty")
    public String faculty(Model model) {
        model.addAttribute("facultyList", facultyRepository.findAll());
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("subjects", subjectRepository.findAll());
        model.addAttribute("assignments", facultySubjectRepository.findAll());
        return "admin/faculty";
    }

    @PostMapping("/admin/faculty/assign-subject")
    public String assignSubject(@RequestParam Integer facultyId, @RequestParam Integer subjectId) {
        boolean alreadyAssigned = facultySubjectRepository.findByFaculty_FacultyId(facultyId).stream()
                .anyMatch(fs -> fs.getSubject().getSubjectId().equals(subjectId));
        if (!alreadyAssigned) {
            Faculty faculty = facultyRepository.findById(facultyId)
                    .orElseThrow(() -> new IllegalStateException("Faculty not found"));
            Subject subject = subjectRepository.findById(subjectId)
                    .orElseThrow(() -> new IllegalStateException("Subject not found"));
            facultySubjectRepository.save(FacultySubject.builder().faculty(faculty).subject(subject).build());
        }
        return "redirect:/admin/faculty";
    }

    @PostMapping("/admin/faculty/unassign-subject")
    public String unassignSubject(@RequestParam Integer facultySubjectId) {
        facultySubjectRepository.deleteById(facultySubjectId);
        return "redirect:/admin/faculty";
    }

    @PostMapping("/admin/faculty/add")
    public String addFaculty(@RequestParam String fullName,
                              @RequestParam String employeeCode,
                              @RequestParam Integer departmentId,
                              @RequestParam(required = false) String designation,
                              @RequestParam(required = false) String phone,
                              @RequestParam String username,
                              @RequestParam String email,
                              @RequestParam String password,
                              Model model,
                              RedirectAttributes redirectAttributes) {

        username = username.trim();
        email = email.trim();
        employeeCode = employeeCode.trim();

        if (facultyRepository.existsByEmployeeCode(employeeCode)
                || userRepository.existsByUsername(username)
                || userRepository.existsByEmail(email)) {
            model.addAttribute("facultyList", facultyRepository.findAll());
            model.addAttribute("departments", departmentRepository.findAll());
            model.addAttribute("subjects", subjectRepository.findAll());
            model.addAttribute("assignments", facultySubjectRepository.findAll());
            model.addAttribute("error", "Employee code, username, or email already exists.");
            return "admin/faculty";
        }

        try {
            Department department = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new IllegalStateException("Department not found"));
            Role facultyRole = roleRepository.findByRoleName("ROLE_FACULTY")
                    .orElseThrow(() -> new IllegalStateException("ROLE_FACULTY not seeded"));

            User user = userRepository.save(User.builder()
                    .username(username)
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .role(facultyRole)
                    .enabled(true)
                    .build());

            facultyRepository.save(Faculty.builder()
                    .user(user)
                    .employeeCode(employeeCode)
                    .fullName(fullName)
                    .department(department)
                    .designation(designation)
                    .phone(phone)
                    .build());

            redirectAttributes.addFlashAttribute("success",
                    "Faculty added. They can log in now with username \"" + username + "\" and the password you set.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not add faculty: " + ex.getMessage());
        }

        return "redirect:/admin/faculty";
    }

    @GetMapping("/admin/students")
    public String students(@RequestParam(required = false) String regNo, Model model) {
        List<Student> studentList = (regNo != null && !regNo.isBlank())
                ? studentRepository.advancedSearch(null, null, null, regNo.trim())
                : studentRepository.findAll();
        model.addAttribute("studentList", studentList);
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("regNo", regNo);
        return "admin/students";
    }

    @PostMapping("/admin/students/add")
    public String addStudent(@RequestParam String fullName,
                              @RequestParam String registerNumber,
                              @RequestParam Integer departmentId,
                              @RequestParam Integer semesterNumber,
                              @RequestParam Integer admissionYear,
                              @RequestParam(required = false) String gender,
                              @RequestParam(required = false) String phone,
                              @RequestParam(required = false) String address,
                              @RequestParam(required = false) String dateOfBirth,
                              @RequestParam(required = false) String guardianName,
                              @RequestParam(required = false) String guardianPhone,
                              @RequestParam String username,
                              @RequestParam String email,
                              @RequestParam String password,
                              Model model,
                              RedirectAttributes redirectAttributes) {

        registerNumber = registerNumber.trim();
        username = username.trim();
        email = email.trim();

        if (studentRepository.existsByRegisterNumber(registerNumber)
                || userRepository.existsByUsername(username)
                || userRepository.existsByEmail(email)) {
            model.addAttribute("studentList", studentRepository.findAll());
            model.addAttribute("departments", departmentRepository.findAll());
            model.addAttribute("error", "Register number, username, or email already exists.");
            return "admin/students";
        }

        try {
            Department department = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new IllegalStateException("Department not found"));
            Course course = defaultCourseForDepartment(department);
            Semester semester = semesterRepository.findByCourse_CourseIdAndSemesterNumber(course.getCourseId(), semesterNumber)
                    .orElseGet(() -> semesterRepository.save(Semester.builder()
                            .course(course)
                            .semesterNumber(semesterNumber)
                            .academicYear(currentOrFirstAcademicYear())
                            .build()));
            Role studentRole = roleRepository.findByRoleName("ROLE_STUDENT")
                    .orElseThrow(() -> new IllegalStateException("ROLE_STUDENT not seeded"));

            User user = userRepository.save(User.builder()
                    .username(username)
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .role(studentRole)
                    .enabled(true)
                    .build());

            studentRepository.save(Student.builder()
                    .user(user)
                    .registerNumber(registerNumber)
                    .fullName(fullName)
                    .course(course)
                    .department(department)
                    .currentSemester(semester)
                    .gender(gender)
                    .phone(phone)
                    .address(address)
                    .dateOfBirth(dateOfBirth != null && !dateOfBirth.isBlank() ? java.time.LocalDate.parse(dateOfBirth) : null)
                    .admissionYear(admissionYear)
                    .guardianName(guardianName)
                    .guardianPhone(guardianPhone)
                    .build());

            redirectAttributes.addFlashAttribute("success",
                    "Student added. They can log in to the Student Portal now with username \"" + username
                            + "\" and the password you set.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not add student: " + ex.getMessage());
        }

        return "redirect:/admin/students";
    }

    @PostMapping("/admin/students/{id}/delete")
    public String deleteStudent(@PathVariable("id") Integer studentId, RedirectAttributes redirectAttributes) {
        try {
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new IllegalStateException("Student not found"));
            Integer userId = student.getUser() != null ? student.getUser().getUserId() : null;
            studentRepository.deleteById(studentId);
            if (userId != null) {
                userRepository.deleteById(userId);
            }
            redirectAttributes.addFlashAttribute("success", "Student removed.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not remove student: " + ex.getMessage());
        }
        return "redirect:/admin/students";
    }

    @GetMapping("/admin/students/{id}")
    public String studentDetail(@PathVariable("id") Integer studentId, Model model) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalStateException("Student not found"));

        long totalAttendance = attendanceRepository.countTotalByStudent(studentId);
        long presentAttendance = attendanceRepository.countPresentByStudent(studentId);
        double attendancePct = totalAttendance == 0 ? 0.0 : (presentAttendance * 100.0 / totalAttendance);

        model.addAttribute("student", student);
        model.addAttribute("marksList", marksRepository.findByStudent_StudentId(studentId));
        model.addAttribute("attendanceRecords", attendanceRepository.findByStudent_StudentId(studentId));
        model.addAttribute("totalAttendance", totalAttendance);
        model.addAttribute("presentAttendance", presentAttendance);
        model.addAttribute("absentAttendance", totalAttendance - presentAttendance);
        model.addAttribute("attendancePct", Math.round(attendancePct * 10) / 10.0);
        return "admin/student-detail";
    }

    @GetMapping("/admin/attendance")
    public String attendanceOverview(@RequestParam(required = false) Integer subjectId,
                                      @RequestParam(required = false) String date,
                                      Model model) {
        model.addAttribute("subjects", subjectRepository.findAll());
        java.time.LocalDate selectedDate = (date != null && !date.isBlank())
                ? java.time.LocalDate.parse(date) : java.time.LocalDate.now();
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("selectedSubjectId", subjectId);

        if (subjectId != null) {
            Subject subject = subjectRepository.findById(subjectId)
                    .orElseThrow(() -> new IllegalStateException("Subject not found"));
            java.util.List<Attendance> records = attendanceRepository
                    .findBySubject_SubjectIdAndAttendanceDate(subjectId, selectedDate);
            model.addAttribute("subject", subject);
            model.addAttribute("records", records);
            model.addAttribute("presentCount", records.stream().filter(a -> a.getStatus() == Attendance.Status.PRESENT).count());
            model.addAttribute("absentCount", records.stream().filter(a -> a.getStatus() == Attendance.Status.ABSENT).count());
            model.addAttribute("lateCount", records.stream().filter(a -> a.getStatus() == Attendance.Status.LATE).count());
        }
        return "admin/attendance";
    }

    @GetMapping("/admin/calendar")
    public String calendar(@RequestParam(required = false) Integer year,
                            @RequestParam(required = false) Integer month,
                            Model model) {
        java.time.YearMonth ym = (year != null && month != null)
                ? java.time.YearMonth.of(year, month)
                : java.time.YearMonth.now();

        java.util.List<AcademicCalendarEvent> allEvents = academicCalendarEventRepository.findAll();
        java.util.List<AcademicCalendarEvent> monthEvents = allEvents.stream()
                .filter(e -> java.time.YearMonth.from(e.getEventDate()).equals(ym))
                .sorted(java.util.Comparator.comparing(AcademicCalendarEvent::getEventDate))
                .toList();

        java.time.LocalDate firstOfMonth = ym.atDay(1);
        int leadingBlanks = firstOfMonth.getDayOfWeek().getValue() % 7; // Sunday-start grid
        model.addAttribute("year", ym.getYear());
        model.addAttribute("month", ym.getMonthValue());
        model.addAttribute("monthLabel", ym.getMonth().name() + " " + ym.getYear());
        model.addAttribute("daysInMonth", ym.lengthOfMonth());
        model.addAttribute("leadingBlanks", leadingBlanks);
        model.addAttribute("monthEvents", monthEvents);
        model.addAttribute("allEvents", allEvents);
        model.addAttribute("prevYear", ym.minusMonths(1).getYear());
        model.addAttribute("prevMonth", ym.minusMonths(1).getMonthValue());
        model.addAttribute("nextYear", ym.plusMonths(1).getYear());
        model.addAttribute("nextMonth", ym.plusMonths(1).getMonthValue());
        return "admin/calendar";
    }

    @PostMapping("/admin/calendar/add")
    public String addCalendarEvent(@RequestParam String title,
                                    @RequestParam(required = false) String description,
                                    @RequestParam String eventDate,
                                    @RequestParam AcademicCalendarEvent.EventType eventType) {
        academicCalendarEventRepository.save(AcademicCalendarEvent.builder()
                .title(title)
                .description(description)
                .eventDate(java.time.LocalDate.parse(eventDate))
                .eventType(eventType)
                .academicYear(currentOrFirstAcademicYear())
                .build());
        return "redirect:/admin/calendar";
    }

    @GetMapping("/admin/analytics")
    public String analytics(Model model) {
        model.addAttribute("totalStudents", studentRepository.count());
        model.addAttribute("totalFaculty", facultyRepository.count());
        model.addAttribute("totalDepartments", departmentRepository.count());
        model.addAttribute("totalSubjects", subjectRepository.count());
        return "admin/analytics";
    }

    @GetMapping("/admin/reports")
    public String reports(Model model) {
        model.addAttribute("departments", departmentRepository.findAll());
        return "admin/reports";
    }

    @GetMapping("/admin/reports/export/{departmentId}")
    public String exportDepartmentReport(@PathVariable Integer departmentId, Model model) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new IllegalStateException("Department not found"));
        model.addAttribute("department", department);
        model.addAttribute("students", studentRepository.findByDepartment_DepartmentId(departmentId));
        model.addAttribute("facultyList", facultyRepository.findAll().stream()
                .filter(f -> f.getDepartment().getDepartmentId().equals(departmentId)).toList());
        model.addAttribute("generatedAt", LocalDateTime.now());
        return "admin/report-print";
    }

    @GetMapping("/admin/leaves")
    public String leaves(Model model) {
        model.addAttribute("leaveRequests", leaveRequestRepository.findAll());
        return "admin/leaves";
    }

    @PostMapping("/admin/leaves/{id}/approve")
    public String approveLeave(@PathVariable("id") Integer leaveId,
                                @RequestParam(value = "remarks", required = false) String remarks) {
        decideLeave(leaveId, LeaveRequest.Status.APPROVED, remarks);
        return "redirect:/admin/leaves";
    }

    @PostMapping("/admin/leaves/{id}/reject")
    public String rejectLeave(@PathVariable("id") Integer leaveId,
                               @RequestParam(value = "remarks", required = false) String remarks) {
        decideLeave(leaveId, LeaveRequest.Status.REJECTED, remarks);
        return "redirect:/admin/leaves";
    }

    private void decideLeave(Integer leaveId, LeaveRequest.Status status, String remarks) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> new IllegalStateException("Leave request not found"));
        leaveRequest.setStatus(status);
        leaveRequest.setDecidedAt(LocalDateTime.now());
        leaveRequest.setRemarks(remarks);
        leaveRequestRepository.save(leaveRequest);

        // Notify the applicant (student or faculty) so the decision shows up on their dashboard/notifications
        Notification.NotificationBuilder notificationBuilder = Notification.builder()
                .title("Leave Request " + status.name())
                .message("Your leave request (" + leaveRequest.getFromDate() + " to " + leaveRequest.getToDate()
                        + ") has been " + status.name().toLowerCase()
                        + (remarks != null && !remarks.isBlank() ? (" - " + remarks) : "."))
                .notificationType(Notification.Type.LEAVE)
                .targetRole(Notification.TargetRole.ALL);

        if (leaveRequest.getStudent() != null) {
            notificationBuilder.targetUser(leaveRequest.getStudent().getUser());
        } else if (leaveRequest.getFaculty() != null) {
            notificationBuilder.targetUser(leaveRequest.getFaculty().getUser());
        }
        notificationRepository.save(notificationBuilder.build());
    }

    @GetMapping("/admin/notifications")
    public String notifications(Model model) {
        model.addAttribute("notifications", notificationRepository.findAllByOrderByCreatedAtDesc());
        return "admin/notifications";
    }

    @GetMapping("/admin/settings")
    public String settings(Model model) {
        return "admin/settings";
    }
}
