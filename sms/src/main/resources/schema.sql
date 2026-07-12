-- ============================================================
-- ERP Integrated Student Management System
-- Full Relational Schema (Normalized to 3NF)
-- Run manually once for a clean install:
--   mysql -u root -p < schema.sql
-- Then optionally load data.sql for sample data.
--
-- NOTE: This file is NOT run automatically (spring.sql.init.mode=never
-- in application.properties). Spring Boot/Hibernate manages the schema
-- automatically at startup via spring.jpa.hibernate.ddl-auto=update,
-- reading column names straight from the Java entities.
--
-- IMPORTANT: if you already ran an older copy of this file by hand,
-- your database may have leftover columns (e.g. first_name/last_name)
-- that no longer match the entities (which use a single full_name
-- column). That mismatch causes "Add Student"/"Add Faculty" to fail
-- with a database error. If Add Student/Add Faculty is failing, the
-- safest fix is to drop and let Hibernate recreate the schema:
--   DROP DATABASE sms_erp_db;
-- then just restart the app - it will rebuild everything cleanly.
-- ============================================================

CREATE DATABASE IF NOT EXISTS sms_erp_db;
USE sms_erp_db;

SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------
-- 1. ROLES
-- ------------------------------------------------------------
DROP TABLE IF EXISTS roles;
CREATE TABLE roles (
    role_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name   VARCHAR(30) NOT NULL UNIQUE   -- SUPER_ADMIN, FACULTY, STUDENT
);

-- ------------------------------------------------------------
-- 2. USERS  (single auth table for all roles -> avoids duplicate login logic)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS users;
CREATE TABLE users (
    user_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password      VARCHAR(255) NOT NULL,     -- BCrypt hash
    email         VARCHAR(100) NOT NULL UNIQUE,
    role_id       BIGINT NOT NULL,
    enabled       BOOLEAN DEFAULT TRUE,
    account_locked BOOLEAN DEFAULT FALSE,
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(role_id)
);

-- ------------------------------------------------------------
-- 3. DEPARTMENTS
-- ------------------------------------------------------------
DROP TABLE IF EXISTS departments;
CREATE TABLE departments (
    department_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    department_name VARCHAR(100) NOT NULL UNIQUE,
    department_code VARCHAR(10)  NOT NULL UNIQUE,
    hod_faculty_id  BIGINT NULL,   -- FK added after faculty table is created
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------
-- 4. ACADEMIC_YEAR
-- ------------------------------------------------------------
DROP TABLE IF EXISTS academic_year;
CREATE TABLE academic_year (
    academic_year_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    year_label        VARCHAR(20) NOT NULL UNIQUE,  -- e.g. 2025-2026
    start_date        DATE NOT NULL,
    end_date          DATE NOT NULL,
    is_current        BOOLEAN DEFAULT FALSE
);

-- ------------------------------------------------------------
-- 5. SEMESTER
-- ------------------------------------------------------------
DROP TABLE IF EXISTS semester;
CREATE TABLE semester (
    semester_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    semester_number   INT NOT NULL,             -- 1..8
    academic_year_id  BIGINT NOT NULL,
    start_date        DATE,
    end_date          DATE,
    is_current        BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_semester_year FOREIGN KEY (academic_year_id) REFERENCES academic_year(academic_year_id)
);

-- ------------------------------------------------------------
-- 6. COURSES (a "course" = degree programme, e.g. B.Tech CSE)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS courses;
CREATE TABLE courses (
    course_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_name     VARCHAR(100) NOT NULL,
    course_code     VARCHAR(20)  NOT NULL UNIQUE,
    department_id   BIGINT NOT NULL,
    duration_years  INT NOT NULL DEFAULT 4,
    total_semesters INT NOT NULL DEFAULT 8,
    CONSTRAINT fk_courses_department FOREIGN KEY (department_id) REFERENCES departments(department_id)
);

-- ------------------------------------------------------------
-- 7. SUBJECTS
-- ------------------------------------------------------------
DROP TABLE IF EXISTS subjects;
CREATE TABLE subjects (
    subject_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject_name   VARCHAR(100) NOT NULL,
    subject_code   VARCHAR(20)  NOT NULL UNIQUE,
    course_id      BIGINT NOT NULL,
    semester_id    BIGINT NOT NULL,
    credits        INT NOT NULL DEFAULT 3,
    is_lab         BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_subjects_course   FOREIGN KEY (course_id)   REFERENCES courses(course_id),
    CONSTRAINT fk_subjects_semester FOREIGN KEY (semester_id) REFERENCES semester(semester_id)
);

-- ------------------------------------------------------------
-- 8. FACULTY
-- ------------------------------------------------------------
DROP TABLE IF EXISTS faculty;
CREATE TABLE faculty (
    faculty_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL UNIQUE,
    employee_code   VARCHAR(20) NOT NULL UNIQUE,
    full_name       VARCHAR(100) NOT NULL,
    department_id   BIGINT NOT NULL,
    designation     VARCHAR(50),               -- Professor / Asst. Professor
    phone           VARCHAR(15),
    date_of_joining DATE,
    photo_path      VARCHAR(255),
    CONSTRAINT fk_faculty_user       FOREIGN KEY (user_id)       REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_faculty_department FOREIGN KEY (department_id) REFERENCES departments(department_id)
);

-- Now that faculty exists, wire the HOD FK on departments
ALTER TABLE departments
    ADD CONSTRAINT fk_department_hod FOREIGN KEY (hod_faculty_id) REFERENCES faculty(faculty_id);

-- ------------------------------------------------------------
-- 9. FACULTY_SUBJECT_MAP (assign faculty to subjects, many-to-many with a section)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS faculty_subject_map;
CREATE TABLE faculty_subject_map (
    map_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    faculty_id   BIGINT NOT NULL,
    subject_id   BIGINT NOT NULL,
    section      VARCHAR(5) DEFAULT 'A',
    academic_year_id BIGINT NOT NULL,
    CONSTRAINT fk_fsm_faculty FOREIGN KEY (faculty_id) REFERENCES faculty(faculty_id) ON DELETE CASCADE,
    CONSTRAINT fk_fsm_subject FOREIGN KEY (subject_id) REFERENCES subjects(subject_id) ON DELETE CASCADE,
    CONSTRAINT fk_fsm_year    FOREIGN KEY (academic_year_id) REFERENCES academic_year(academic_year_id),
    UNIQUE KEY uq_faculty_subject_section_year (faculty_id, subject_id, section, academic_year_id)
);

-- ------------------------------------------------------------
-- 10. STUDENTS  (register_number is the unique, human-facing key -> prevents duplicate records)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS students;
CREATE TABLE students (
    student_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT NOT NULL UNIQUE,
    register_number  VARCHAR(20) NOT NULL UNIQUE,   -- e.g. 21CSE045 - unique constraint prevents duplicates
    full_name        VARCHAR(100) NOT NULL,
    course_id        BIGINT NOT NULL,
    department_id    BIGINT NOT NULL,
    current_semester_id BIGINT NOT NULL,
    section          VARCHAR(5) DEFAULT 'A',
    gender           VARCHAR(10),
    date_of_birth    DATE,
    phone            VARCHAR(15),
    address          VARCHAR(255),
    admission_year   INT,
    photo_url        VARCHAR(255),
    guardian_name    VARCHAR(100),
    guardian_phone   VARCHAR(15),
    leave_balance    INT DEFAULT 12,
    status           VARCHAR(20) DEFAULT 'ACTIVE',  -- ACTIVE, GRADUATED, SUSPENDED
    CONSTRAINT fk_students_user     FOREIGN KEY (user_id)     REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_students_course   FOREIGN KEY (course_id)   REFERENCES courses(course_id),
    CONSTRAINT fk_students_dept     FOREIGN KEY (department_id) REFERENCES departments(department_id),
    CONSTRAINT fk_students_semester FOREIGN KEY (current_semester_id) REFERENCES semester(semester_id)
);

-- ------------------------------------------------------------
-- 11. TIMETABLE
-- ------------------------------------------------------------
DROP TABLE IF EXISTS timetable;
CREATE TABLE timetable (
    timetable_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject_id    BIGINT NOT NULL,
    faculty_id    BIGINT NOT NULL,
    section       VARCHAR(5) DEFAULT 'A',
    day_of_week   VARCHAR(10) NOT NULL,   -- MONDAY..SATURDAY
    period_number INT NOT NULL,
    start_time    TIME NOT NULL,
    end_time      TIME NOT NULL,
    room_number   VARCHAR(20),
    CONSTRAINT fk_timetable_subject FOREIGN KEY (subject_id) REFERENCES subjects(subject_id) ON DELETE CASCADE,
    CONSTRAINT fk_timetable_faculty FOREIGN KEY (faculty_id) REFERENCES faculty(faculty_id)
);

-- ------------------------------------------------------------
-- 12. ATTENDANCE  (one row per student per subject per date)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS attendance;
CREATE TABLE attendance (
    attendance_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id    BIGINT NOT NULL,
    subject_id    BIGINT NOT NULL,
    faculty_id    BIGINT NOT NULL,
    attendance_date DATE NOT NULL,
    status        VARCHAR(10) NOT NULL,   -- PRESENT, ABSENT, LEAVE
    marked_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_attendance_student FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,
    CONSTRAINT fk_attendance_subject FOREIGN KEY (subject_id) REFERENCES subjects(subject_id) ON DELETE CASCADE,
    CONSTRAINT fk_attendance_faculty FOREIGN KEY (faculty_id) REFERENCES faculty(faculty_id),
    UNIQUE KEY uq_attendance_once (student_id, subject_id, attendance_date)
);

-- ------------------------------------------------------------
-- 13. MARKS  (covers both internal and semester/external exams via exam_type)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS marks;
CREATE TABLE marks (
    mark_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id    BIGINT NOT NULL,
    subject_id    BIGINT NOT NULL,
    faculty_id    BIGINT NOT NULL,
    exam_type     VARCHAR(20) NOT NULL,     -- INTERNAL1, INTERNAL2, ASSIGNMENT, SEMESTER
    marks_obtained DECIMAL(5,2) NOT NULL,
    max_marks      DECIMAL(5,2) NOT NULL DEFAULT 100,
    uploaded_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_marks_student FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,
    CONSTRAINT fk_marks_subject FOREIGN KEY (subject_id) REFERENCES subjects(subject_id) ON DELETE CASCADE,
    CONSTRAINT fk_marks_faculty FOREIGN KEY (faculty_id) REFERENCES faculty(faculty_id),
    UNIQUE KEY uq_marks_once (student_id, subject_id, exam_type)
);

-- ------------------------------------------------------------
-- 14. ASSIGNMENTS  (created by faculty)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS assignments;
CREATE TABLE assignments (
    assignment_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject_id     BIGINT NOT NULL,
    faculty_id     BIGINT NOT NULL,
    title          VARCHAR(150) NOT NULL,
    description    TEXT,
    due_date       DATETIME NOT NULL,
    max_marks      DECIMAL(5,2) DEFAULT 10,
    created_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_assignment_subject FOREIGN KEY (subject_id) REFERENCES subjects(subject_id) ON DELETE CASCADE,
    CONSTRAINT fk_assignment_faculty FOREIGN KEY (faculty_id) REFERENCES faculty(faculty_id)
);

-- ------------------------------------------------------------
-- 14b. ASSIGNMENT_SUBMISSIONS  (student submissions against assignments)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS assignment_submissions;
CREATE TABLE assignment_submissions (
    submission_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    assignment_id  BIGINT NOT NULL,
    student_id     BIGINT NOT NULL,
    file_path      VARCHAR(255),
    submitted_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    marks_awarded  DECIMAL(5,2),
    status         VARCHAR(20) DEFAULT 'SUBMITTED',  -- SUBMITTED, LATE, GRADED
    CONSTRAINT fk_submission_assignment FOREIGN KEY (assignment_id) REFERENCES assignments(assignment_id) ON DELETE CASCADE,
    CONSTRAINT fk_submission_student    FOREIGN KEY (student_id)    REFERENCES students(student_id) ON DELETE CASCADE,
    UNIQUE KEY uq_submission_once (assignment_id, student_id)
);

-- ------------------------------------------------------------
-- 15. FEES
-- ------------------------------------------------------------
DROP TABLE IF EXISTS fees;
CREATE TABLE fees (
    fee_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id     BIGINT NOT NULL,
    semester_id    BIGINT NOT NULL,
    fee_type       VARCHAR(30) NOT NULL,   -- TUITION, HOSTEL, EXAM, LIBRARY
    amount_due     DECIMAL(10,2) NOT NULL,
    amount_paid    DECIMAL(10,2) DEFAULT 0,
    due_date       DATE,
    payment_date   DATE,
    status         VARCHAR(20) DEFAULT 'PENDING',  -- PENDING, PARTIAL, PAID
    receipt_number VARCHAR(30) UNIQUE,
    CONSTRAINT fk_fees_student  FOREIGN KEY (student_id)  REFERENCES students(student_id) ON DELETE CASCADE,
    CONSTRAINT fk_fees_semester FOREIGN KEY (semester_id) REFERENCES semester(semester_id)
);

-- ------------------------------------------------------------
-- 16. LEAVE_REQUESTS
-- ------------------------------------------------------------
DROP TABLE IF EXISTS leave_requests;
CREATE TABLE leave_requests (
    leave_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id    BIGINT NOT NULL,
    approver_faculty_id BIGINT,
    from_date     DATE NOT NULL,
    to_date       DATE NOT NULL,
    reason        VARCHAR(255) NOT NULL,
    status        VARCHAR(20) DEFAULT 'PENDING',   -- PENDING, APPROVED, REJECTED
    applied_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    decided_at    DATETIME,
    remarks       VARCHAR(255),
    CONSTRAINT fk_leave_student  FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,
    CONSTRAINT fk_leave_approver FOREIGN KEY (approver_faculty_id) REFERENCES faculty(faculty_id)
);

-- ------------------------------------------------------------
-- 17. NOTIFICATIONS  (generic notification/event log, drives the Smart Notifications feature)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS notifications;
CREATE TABLE notifications (
    notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_user_id BIGINT NOT NULL,
    sender_user_id     BIGINT,
    title             VARCHAR(150) NOT NULL,
    message           VARCHAR(500) NOT NULL,
    type              VARCHAR(30) NOT NULL,  -- ATTENDANCE_LOW, FEE_DUE, MARKS_UPLOADED, LEAVE_STATUS, ASSIGNMENT_DUE, GENERAL
    is_read           BOOLEAN DEFAULT FALSE,
    created_at        DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_sender    FOREIGN KEY (sender_user_id)    REFERENCES users(user_id)
);

SET FOREIGN_KEY_CHECKS = 1;

-- ------------------------------------------------------------
-- Helpful indexes for the analytics / dashboard queries
-- ------------------------------------------------------------
CREATE INDEX idx_attendance_student_subject ON attendance(student_id, subject_id);
CREATE INDEX idx_marks_student_subject ON marks(student_id, subject_id);
CREATE INDEX idx_notifications_recipient ON notifications(recipient_user_id, is_read);
CREATE INDEX idx_students_department ON students(department_id, current_semester_id);
