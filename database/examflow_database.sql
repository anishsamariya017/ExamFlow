-- =====================================================================
-- ExamFlow | Question Bank & Examination System
-- MySQL 8+ / MariaDB
-- Run: mysql -u root -p < database/examflow_database.sql
-- Safe to re-run: it does NOT delete existing data.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS examflow
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE examflow;

-- USERS
CREATE TABLE IF NOT EXISTS users (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    role       ENUM('ADMIN','TEACHER','STUDENT') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- TESTS
CREATE TABLE IF NOT EXISTS tests (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    name             VARCHAR(100) NOT NULL,
    subject          VARCHAR(100) NOT NULL,
    duration_minutes INT NOT NULL,
    created_by       INT NULL,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_duration CHECK (duration_minutes BETWEEN 1 AND 300),
    CONSTRAINT fk_tests_user FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- QUESTIONS (correct_answer: 0=A, 1=B, 2=C, 3=D)
CREATE TABLE IF NOT EXISTS questions (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    test_id        INT NOT NULL,
    question_text  VARCHAR(500) NOT NULL,
    option_a       VARCHAR(255) NOT NULL,
    option_b       VARCHAR(255) NOT NULL,
    option_c       VARCHAR(255) NOT NULL,
    option_d       VARCHAR(255) NOT NULL,
    correct_answer TINYINT NOT NULL,
    marks          INT NOT NULL DEFAULT 1,
    CONSTRAINT chk_correct CHECK (correct_answer BETWEEN 0 AND 3),
    CONSTRAINT chk_marks   CHECK (marks BETWEEN 1 AND 100),
    CONSTRAINT fk_questions_test FOREIGN KEY (test_id) REFERENCES tests(id) ON DELETE CASCADE,
    INDEX idx_questions_test (test_id)
) ENGINE=InnoDB;

-- RESULTS (test_name / subject stored as snapshot)
CREATE TABLE IF NOT EXISTS results (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    student_id   INT NOT NULL,
    test_id      INT NULL,
    test_name    VARCHAR(100) NOT NULL,
    subject      VARCHAR(100) NOT NULL,
    score        INT NOT NULL,
    total        INT NOT NULL,
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_results_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_results_test    FOREIGN KEY (test_id)    REFERENCES tests(id) ON DELETE SET NULL,
    INDEX idx_results_student (student_id)
) ENGINE=InnoDB;

-- Demo accounts (created only if the usernames do not already exist)
INSERT IGNORE INTO users (username, password, role) VALUES
    ('admin',   'admin123',   'ADMIN'),
    ('teacher', 'teacher123', 'TEACHER'),
    ('student', 'student123', 'STUDENT');

INSERT INTO tests (name, subject, duration_minutes, created_by)
SELECT 'Test 1', 'Java Programming', 15, (SELECT id FROM users WHERE username = 'teacher')
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM tests WHERE name = 'Test 1' AND subject = 'Java Programming');

INSERT INTO tests (name, subject, duration_minutes, created_by)
SELECT 'Test 2', 'Data Structures', 20, (SELECT id FROM users WHERE username = 'teacher')
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM tests WHERE name = 'Test 2' AND subject = 'Data Structures');

INSERT INTO questions (test_id, question_text, option_a, option_b, option_c, option_d, correct_answer, marks)
SELECT t.id, s.q, s.a, s.b, s.c, s.d, s.ans, 1
FROM tests t
JOIN (
    SELECT 'Test 1' AS tn, 'Which keyword is used to inherit a class in Java?' AS q, 'implements' AS a, 'extends' AS b, 'inherits' AS c, 'super' AS d, 1 AS ans
    UNION ALL SELECT 'Test 1', 'Which method is the entry point of a Java program?', 'run()', 'start()', 'main()', 'init()', 2
    UNION ALL SELECT 'Test 1', 'Which data type stores true or false?', 'int', 'char', 'boolean', 'String', 2
    UNION ALL SELECT 'Test 2', 'Which data structure follows LIFO?', 'Queue', 'Stack', 'Tree', 'Graph', 1
    UNION ALL SELECT 'Test 2', 'What is the time complexity of binary search?', 'O(n)', 'O(n²)', 'O(log n)', 'O(1) always', 2
) s ON s.tn = t.name
WHERE NOT EXISTS (SELECT 1 FROM questions x WHERE x.test_id = t.id);

-- Quick check
SELECT 'users' AS tbl, COUNT(*) AS rows_count FROM users
UNION ALL SELECT 'tests', COUNT(*) FROM tests
UNION ALL SELECT 'questions', COUNT(*) FROM questions
UNION ALL SELECT 'results', COUNT(*) FROM results;
