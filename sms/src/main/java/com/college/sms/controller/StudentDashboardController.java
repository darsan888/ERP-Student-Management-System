package com.college.sms.controller;

import com.college.sms.entity.Student;
import com.college.sms.repository.*;
import com.college.sms.security.CustomUserDetails;
import com.college.sms.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class StudentDashboardController {

    private final StudentRepository studentRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubjectRepository subjectRepository;
    private final DashboardService dashboardService;

    @GetMapping("/student/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        Student student = studentRepository.findByUser_UserId(principal.getUserId())
                .orElseThrow(() -> new IllegalStateException("Student profile not found for this user"));

        long assignmentsAssigned = subjectRepository
                .findBySemester_SemesterId(student.getCurrentSemester().getSemesterId())
                .stream()
                .mapToLong(subject -> assignmentRepository.findBySubject_SubjectId(subject.getSubjectId()).size())
                .sum();

        model.addAttribute("student", student);
        model.addAttribute("health", dashboardService.buildHealthDashboard(student, assignmentsAssigned));
        return "student/dashboard";
    }
}
