# CampusERP — Integrated Student Management System (Phase 1)

A production-style ERP for a college mini project, covering Super Admin, Faculty,
and Student roles with role-based dashboards, attendance/marks/fees data model,
and rule-based analytics (attendance risk, performance prediction, student health
dashboard).

## ⚠️ About the tech stack

The brief asked for **Spring Boot 4**. As of this build, Spring Boot 4 has not
been publicly released — the current stable line is **Spring Boot 3.3.x**, which
is what this project uses, paired with **Java 21**. Everything else matches the
brief: Spring MVC, Spring Data JPA, Hibernate, MySQL, embedded Tomcat, Maven.
When Spring Boot 4 ships, bumping the parent version in `pom.xml` is normally a
drop-in change.

## What's in Phase 1

- Full Maven project (`pom.xml`) — Spring Web, Data JPA, Security, Thymeleaf,
  Validation, MySQL driver, iText (for PDF reports later), Lombok.
- Complete normalized MySQL schema (`src/main/resources/db/schema.sql`) with all
  15+ tables from the spec: users, roles, students, faculty, departments, courses,
  subjects, semester, academic_year, attendance, marks, assignments +
  assignment_submissions, fees, leave_requests, timetable, notifications,
  academic_calendar, faculty_subject (junction table).
- 19 JPA entities matching the schema exactly, with proper `@ManyToOne`/`@OneToOne`
  relationships and unique constraints (e.g. `register_number` is unique, so the
  duplicate-student-record problem from the brief is solved at the DB level).
- 19 Spring Data repositories, including an advanced search query on `Student`
  (department, semester, name, register number).
- Spring Security config with **BCrypt** password hashing, a custom
  `UserDetailsService`, and role-based URL authorization (`/admin/**`,
  `/faculty/**`, `/student/**`), plus a post-login handler that routes each role
  to its own dashboard automatically.
- `DashboardService` — the rule-based engine behind the "innovative features":
  attendance percentage, Green/Yellow/Red risk indicator, a simple explainable
  performance classifier (Excellent / Average / Needs Improvement) driven by
  attendance + marks, and the combined Student Health Dashboard score.
- Three working, styled dashboards (Bootstrap 5 + Chart.js, dark sidebar,
  gradient stat cards) for Admin, Faculty, and Student, wired to real data.
- `DataInitializer` seeds one sample department/course/semester/subjects and one
  user per role so you can log in immediately.

## What's NOT built yet (next phases)

This is a large system — attendance-taking UI, marks/upload forms, leave
workflow (apply → approve → notify → auto-update attendance), assignment
submission + grading, fee receipt/PDF generation, hall ticket PDF, notification
center, advanced search UI, faculty analytics charts, and the UML/ER diagrams
and project report documents are still to come. Tell me which module to build
next (or say "continue" and I'll proceed in the same order as your spec:
attendance → marks → leave workflow → fees → notifications → reports → diagrams).

## Setup Instructions

### 1. Prerequisites
- JDK 21
- Maven 3.9+ (or use IntelliJ's bundled Maven)
- MySQL 8+ running locally
- IntelliJ IDEA (Community or Ultimate)

### 2. Create the database
```bash
mysql -u root -p < src/main/resources/db/schema.sql
```
This creates the `erp_sms` database, all tables, and the 3 roles.

### 3. Configure credentials
Edit `src/main/resources/application.properties` and set your local MySQL
username/password:
```properties
spring.datasource.username=root
spring.datasource.password=root
```

### 4. Run
Open the project folder in IntelliJ (`File → Open`, select the `sms` folder —
IntelliJ will detect the `pom.xml` and import it as a Maven project), then run
`StudentManagementSystemApplication`.

Or from the command line:
```bash
mvn spring-boot:run
```

The app starts at **http://localhost:8080** and redirects to `/login`.

### 5. Log in
On first run, `DataInitializer` seeds one login per role:

| Role    | Username  | Password     |
|---------|-----------|--------------|
| Admin   | admin     | Admin@123    |
| Faculty | faculty1  | Faculty@123  |
| Student | student1  | Student@123  |

## Project Structure
```
sms/
├── pom.xml
├── README.md
└── src/main/
    ├── java/com/college/sms/
    │   ├── StudentManagementSystemApplication.java
    │   ├── config/          # SecurityConfig, DataInitializer
    │   ├── controller/      # Auth + role dashboards
    │   ├── entity/          # 19 JPA entities
    │   ├── repository/      # 19 Spring Data repositories
    │   ├── security/        # CustomUserDetails(Service), success handler
    │   └── service/         # DashboardService (analytics)
    └── resources/
        ├── application.properties
        ├── db/schema.sql
        ├── static/css/style.css
        └── templates/
            ├── auth/login.html, access-denied.html
            ├── admin/dashboard.html
            ├── faculty/dashboard.html
            └── student/dashboard.html
```
