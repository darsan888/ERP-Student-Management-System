package com.college.sms.controller;

import com.college.sms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class AdminDashboardController {

    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final DepartmentRepository departmentRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final NotificationRepository notificationRepository;

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalStudents", studentRepository.count());
        model.addAttribute("totalFaculty", facultyRepository.count());
        model.addAttribute("totalDepartments", departmentRepository.count());
        model.addAttribute("pendingLeaves", leaveRequestRepository.findByStatus(
                com.college.sms.entity.LeaveRequest.Status.PENDING).size());
        model.addAttribute("recentNotifications", notificationRepository.findAllByOrderByCreatedAtDesc()
                .stream().limit(5).toList());
        return "admin/dashboard";
    }
}
