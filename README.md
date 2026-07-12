# SMS (Student Management System)

> **ERP-Based Integrated Student Management System**

SMS (Student Management System) is an ERP-based web application developed using **Java Spring Boot**. It provides a centralized platform for managing academic and administrative activities through dedicated portals for **Admin**, **Faculty**, and **Students**. The system follows a role-based architecture with secure authentication and is designed to simplify student information management, faculty operations, and institutional workflows.

---

# Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Setup & Installation](#setup--installation)
- [Project Structure](#project-structure)
- [Usage](#usage)
- [Database Setup](#database-setup)
- [Contributing](#contributing)
- [License](#license)

---

# Features

## Authentication & Authorization
- Secure user authentication
- Role-based access control
- Separate portals for:
  - Admin
  - Faculty
  - Student
- Session-based login/logout
- Thymeleaf Security integration using `sec:authorize`

## Admin Module
- Manage student records
- Manage faculty records
- Manage user accounts
- Administrative dashboard
- Monitor system data

## Faculty Module
- Faculty dashboard
- View assigned students
- Update student academic information
- Manage student-related records

## Student Module
- Student dashboard
- View personal profile
- Access academic information
- View institutional updates

## General Features
- Responsive web interface
- MVC architecture
- Database persistence using Spring Data JPA (Hibernate)
- MySQL integration
- Server-side rendering with Thymeleaf
- Modular project structure
- Maven dependency management

---

# Tech Stack

| Technology | Version |
|------------|---------|
| Java | 21 |
| Spring Boot | 3.3.4 |
| Spring Web (MVC) | Included |
| Spring Data JPA (Hibernate) | Included |
| Thymeleaf | Included |
| Spring Security Extras | Included |
| MySQL | Database |
| Maven | Build Tool |
| Packaging | JAR |

---

# Prerequisites

Before running the project, install:

- Java Development Kit (JDK) 21
- Apache Maven
- MySQL Server
- Git (optional)

Verify installation:

```bash
java -version
mvn -version
mysql --version
```

---

# Setup & Installation

## 1. Clone the Repository

```bash
git clone https://github.com/your-username/SMS.git
cd SMS
```

## 2. Configure Database

Open:

```text
src/main/resources/application.properties
```

Update the database configuration:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/student_management_system
spring.datasource.username=your_username
spring.datasource.password=your_password

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

## 3. Create the Database

```sql
CREATE DATABASE student_management_system;
```

## 4. Initialize the Schema

The project includes:

```text
src/main/resources/schema.sql
```

Run this SQL file manually or configure Spring Boot to execute it automatically during application startup.

## 5. Build the Project

```bash
mvn clean install
```

## 6. Run the Application

Using Maven:

```bash
mvn spring-boot:run
```

Or using the generated JAR:

```bash
java -jar target/SMS-1.0.0.jar
```

---

# Project Structure

```text
SMS
│
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── college
│   │   │           └── ... application source code
│   │   │
│   │   └── resources
│   │       ├── application.properties
│   │       ├── schema.sql
│   │       ├── templates
│   │       │   ├── admin
│   │       │   ├── faculty
│   │       │   ├── student
│   │       │   ├── auth
│   │       │   └── fragments
│   │       └── static
│   │           ├── css
│   │           ├── js
│   │           └── images
│   │
│   └── test
│       └── java
│           └── com
│               └── college
│
├── pom.xml
├── README.md
└── target/
```

---

# Usage

Start the application and open:

```text
http://localhost:8080
```

Users can log in through the authentication module and access their respective dashboards:

- Admin Portal
- Faculty Portal
- Student Portal

---

# Database Setup

The database schema is available at:

```text
src/main/resources/schema.sql
```

Ensure:

- MySQL is running.
- The database has been created.
- Database credentials in `application.properties` are correct.
- The schema is executed before using the application if not automatically initialized.

---

# Contributing

Contributions are welcome.

1. Fork the repository.
2. Create a new branch:

```bash
git checkout -b feature/your-feature
```

3. Commit your changes:

```bash
git commit -m "Add your feature"
```

4. Push the branch:

```bash
git push origin feature/your-feature
```

5. Open a Pull Request.

---

# License

This project is licensed under the **MIT License**.

See the `LICENSE` file for more information.
