# ExamFlow — Question Bank & Examination System

ExamFlow is a desktop-based examination management application built with **Java Swing**, **JDBC**, and **MySQL**.

## Features

- Role-based login for Admin, Teacher, and Student
- Admin dashboard for managing accounts, viewing tests, and reviewing results
- Teacher tools to create and edit tests and MCQ questions
- Student interface to attempt tests and view scores/results
- MySQL database storage using JDBC

## Tech Stack

- Java 8 or later
- Java Swing (GUI)
- MySQL 8+ (or compatible MariaDB setup)
- JDBC / MySQL Connector/J
- Maven (dependency and build management)

## Project Structure

```text
ExamFlow/
├── database/
│   └── examflow_database.sql
├── src/
│   └── main/
│       └── java/
│           ├── Main.java
│           └── Database.java
├── .gitignore
├── pom.xml
└── README.md
```

## Requirements

1. Install **JDK 8 or later**.
2. Install and start **MySQL 8+** (or a compatible MariaDB server).
3. Install **Apache Maven**.

Check installations:

```bash
java -version
mvn -version
mysql --version
```

## Database Setup

Run the SQL script from the project root:

```bash
mysql -u root -p < database/examflow_database.sql
```

The script creates the `examflow` database, required tables, demo users, sample tests, and sample questions. It uses `IF NOT EXISTS` / guarded inserts and does not intentionally delete existing records.

## Configure Database Credentials

The application reads database settings from environment variables. Defaults are `localhost`, port `3306`, database `examflow`, and user `root`.

Set your own MySQL username and password before launching the application. **Do not put your real password in Java source code or commit it to GitHub.**

### Windows PowerShell

```powershell
$env:EXAMFLOW_DB_HOST = "localhost"
$env:EXAMFLOW_DB_PORT = "3306"
$env:EXAMFLOW_DB_NAME = "examflow"
$env:EXAMFLOW_DB_USER = "root"
$env:EXAMFLOW_DB_PASSWORD = "your_mysql_password"
```

### macOS / Linux

```bash
export EXAMFLOW_DB_HOST="localhost"
export EXAMFLOW_DB_PORT="3306"
export EXAMFLOW_DB_NAME="examflow"
export EXAMFLOW_DB_USER="root"
export EXAMFLOW_DB_PASSWORD="your_mysql_password"
```

If your local MySQL account has no password, you can leave `EXAMFLOW_DB_PASSWORD` unset.

## Build and Run

From the project root:

```bash
mvn clean package
java -jar target/examflow.jar
```

Maven downloads the MySQL JDBC driver and packages the application into an executable JAR.

## Demo Login Accounts

The SQL script inserts these sample accounts when they do not already exist:

| Role | Username | Password |
|---|---|---|
| Admin | `admin` | `admin123` |
| Teacher | `teacher` | `teacher123` |
| Student | `student` | `student123` |

These are **demo-only credentials**. Change or remove them before using the application beyond a local demonstration.

## Important Security Note

This project is an educational/demo application. The current login implementation stores and compares passwords as plain text. Do not use real or reused passwords, and do not deploy this version for real users without implementing secure password hashing (for example, BCrypt or Argon2), appropriate authorization checks, and production database security.

## GitHub Upload

Create a new empty repository on GitHub (for example, `ExamFlow`). Then run these commands from this folder, replacing the URL with your own repository URL:

```bash
git init
git add .
git commit -m "Initial commit: ExamFlow project"
git branch -M main
git remote add origin https://github.com/YOUR-USERNAME/ExamFlow.git
git push -u origin main
```

Do not upload database backups, real credentials, `.env` files, IDE folders, or the Maven `target/` directory.
