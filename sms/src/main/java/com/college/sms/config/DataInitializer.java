package com.college.sms.config;

import com.college.sms.entity.*;
import com.college.sms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Seeds minimal sample data on first run so the app is immediately usable:
 * - 3 roles (created by schema.sql)
 * - 1 department, 1 course, 1 academic year, 1 semester, 2 subjects
 * - 1 admin user, 1 faculty user, 1 student user
 *
 * Default login credentials (change after first login):
 *   admin    / Admin@123
 *   faculty1 / Faculty@123
 *   student1 / Student@123
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;
    private final AcademicYearRepository academicYearRepository;
    private final SemesterRepository semesterRepository;
    private final SubjectRepository subjectRepository;
    private final FacultyRepository facultyRepository;
    private final StudentRepository studentRepository;
    private final FacultySubjectRepository facultySubjectRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return; // already seeded
        }

        Role adminRole = roleRepository.findByRoleName("ROLE_ADMIN").orElseThrow();
        Role facultyRole = roleRepository.findByRoleName("ROLE_FACULTY").orElseThrow();
        Role studentRole = roleRepository.findByRoleName("ROLE_STUDENT").orElseThrow();

        Department cse = departmentRepository.save(Department.builder()
                .departmentName("Computer Science & Engineering")
                .departmentCode("CSE")
                .hodName("Dr. Rakesh Sharma")
                .build());

        Course btech = courseRepository.save(Course.builder()
                .courseName("B.Tech Computer Science")
                .courseCode("BTECH-CSE")
                .department(cse)
                .durationYears(4)
                .build());

        AcademicYear ay = academicYearRepository.save(AcademicYear.builder()
                .yearLabel("2025-2026")
                .startDate(LocalDate.of(2025, 6, 1))
                .endDate(LocalDate.of(2026, 5, 31))
                .isCurrent(true)
                .build());

        Semester sem3 = semesterRepository.save(Semester.builder()
                .semesterNumber(3)
                .academicYear(ay)
                .course(btech)
                .build());

        Subject dsa = subjectRepository.save(Subject.builder()
                .subjectName("Data Structures & Algorithms")
                .subjectCode("CS301")
                .semester(sem3)
                .credits(4)
                .isLab(false)
                .build());

        Subject dbms = subjectRepository.save(Subject.builder()
                .subjectName("Database Management Systems")
                .subjectCode("CS302")
                .semester(sem3)
                .credits(4)
                .isLab(false)
                .build());

        // --- Admin user ---
        User adminUser = userRepository.save(User.builder()
                .username("admin")
                .password(passwordEncoder.encode("Admin@123"))
                .email("admin@college.edu")
                .role(adminRole)
                .enabled(true)
                .build());

        // --- Faculty user ---
        User facultyUser = userRepository.save(User.builder()
                .username("faculty1")
                .password(passwordEncoder.encode("Faculty@123"))
                .email("faculty1@college.edu")
                .role(facultyRole)
                .enabled(true)
                .build());

        Faculty faculty = facultyRepository.save(Faculty.builder()
                .user(facultyUser)
                .employeeCode("EMP001")
                .fullName("Dr. Anita Verma")
                .department(cse)
                .designation("Associate Professor")
                .phone("9876543210")
                .dateOfJoining(LocalDate.of(2018, 7, 1))
                .build());

        facultySubjectRepository.save(FacultySubject.builder().faculty(faculty).subject(dsa).build());
        facultySubjectRepository.save(FacultySubject.builder().faculty(faculty).subject(dbms).build());

        // --- Student user ---
        User studentUser = userRepository.save(User.builder()
                .username("student1")
                .password(passwordEncoder.encode("Student@123"))
                .email("student1@college.edu")
                .role(studentRole)
                .enabled(true)
                .build());

        studentRepository.save(Student.builder()
                .user(studentUser)
                .registerNumber("CSE2023001")
                .fullName("Rahul Kumar")
                .course(btech)
                .department(cse)
                .currentSemester(sem3)
                .dateOfBirth(LocalDate.of(2005, 4, 12))
                .gender("Male")
                .phone("9998887770")
                .address("Hyderabad, Telangana")
                .admissionYear(2023)
                .guardianName("Suresh Kumar")
                .guardianPhone("9998887771")
                .leaveBalance(12)
                .build());

        System.out.println("=================================================");
        System.out.println(" Sample data seeded successfully.");
        System.out.println(" Login credentials:");
        System.out.println("   Admin    -> admin / Admin@123");
        System.out.println("   Faculty  -> faculty1 / Faculty@123");
        System.out.println("   Student  -> student1 / Student@123");
        System.out.println("=================================================");
    }
}
