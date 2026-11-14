import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * FILE 2 of 3 : MySQL BACKEND (JDBC)
 * Saara database ka kaam (login, users, tests, questions, results) yahin hota hai.
 * Main.java sirf UI dikhata hai aur is class ko call karta hai.
 *
 * Zaruri: mysql-connector-j (JDBC driver) jar project me add karo.
 */
public class Database {

    // Configure these values with environment variables; never commit real credentials.
    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }

    private static final String HOST = env("EXAMFLOW_DB_HOST", "localhost");
    private static final String PORT = env("EXAMFLOW_DB_PORT", "3306");
    private static final String DB_NAME = env("EXAMFLOW_DB_NAME", "examflow");
    private static final String DB_USER = env("EXAMFLOW_DB_USER", "root");
    private static final String DB_PASSWORD = env("EXAMFLOW_DB_PASSWORD", "");

    private static final String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB_NAME
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata";

    private Database() { }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, DB_USER, DB_PASSWORD);
    }

    private static RuntimeException error(SQLException e) {
        return new RuntimeException("Database error: " + e.getMessage(), e);
    }

    /** Startup par check karta hai ki MySQL se connection ho raha hai ya nahi. */
    public static void testConnection() {
        try (Connection c = connect()) {
            // connected
        } catch (SQLException e) {
            throw new RuntimeException("MySQL se connect nahi ho paya.\n" + e.getMessage()
                    + "\n\nCheck karo: MySQL chal raha hai, examflow_database.sql run ho chuka hai,"
                    + "\nEXAMFLOW_DB_USER / EXAMFLOW_DB_PASSWORD environment variables sahi hain.", e);
        }
    }

    // =================================================================
    // USERS
    // =================================================================
    public static Main.AppUser login(String username, String password, String role) {
        String sql = "SELECT id, username, password, role FROM users "
                + "WHERE BINARY username = ? AND BINARY password = ? AND role = ?";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, role);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapUser(rs);
                return null;
            }
        } catch (SQLException e) { throw error(e); }
    }

    public static List<Main.AppUser> getUsers() {
        List<Main.AppUser> list = new ArrayList<>();
        String sql = "SELECT id, username, password, role FROM users ORDER BY id";
        try (Connection c = connect(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapUser(rs));
        } catch (SQLException e) { throw error(e); }
        return list;
    }

    public static int countUsers(String role) {
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM users WHERE role = ?")) {
            ps.setString(1, role);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) { throw error(e); }
    }

    /** excludeId = jis user ko ignore karna hai (edit ke time), naya user ho to 0 do. */
    public static boolean usernameExists(String username, int excludeId) {
        String sql = "SELECT COUNT(*) FROM users WHERE LOWER(username) = LOWER(?) AND id <> ?";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) { throw error(e); }
    }

    public static void addUser(String username, String password, String role) {
        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, role);
            ps.executeUpdate();
        } catch (SQLException e) { throw error(e); }
    }

    public static void updateUser(int id, String username, String password, String role) {
        String sql = "UPDATE users SET username = ?, password = ?, role = ? WHERE id = ?";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, role);
            ps.setInt(4, id);
            ps.executeUpdate();
        } catch (SQLException e) { throw error(e); }
    }

    public static void deleteUser(int id) {
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { throw error(e); }
    }

    private static Main.AppUser mapUser(ResultSet rs) throws SQLException {
        return new Main.AppUser(rs.getInt("id"), rs.getString("username"),
                rs.getString("password"), rs.getString("role"));
    }

    // =================================================================
    // TESTS (questions ke saath load hote hain)
    // =================================================================
    public static List<Main.ExamTest> getTests() {
        return loadTests(null);
    }

    public static Main.ExamTest getTest(int id) {
        List<Main.ExamTest> list = loadTests(id);
        return list.isEmpty() ? null : list.get(0);
    }

    private static List<Main.ExamTest> loadTests(Integer onlyId) {
        List<Main.ExamTest> list = new ArrayList<>();
        Map<Integer, Main.ExamTest> byId = new HashMap<>();
        String testSql = "SELECT id, name, subject, duration_minutes FROM tests"
                + (onlyId == null ? "" : " WHERE id = ?") + " ORDER BY id";
        String questionSql = "SELECT id, test_id, question_text, option_a, option_b, option_c, option_d, "
                + "correct_answer, marks FROM questions"
                + (onlyId == null ? "" : " WHERE test_id = ?") + " ORDER BY id";
        try (Connection c = connect()) {
            try (PreparedStatement ps = c.prepareStatement(testSql)) {
                if (onlyId != null) ps.setInt(1, onlyId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Main.ExamTest t = new Main.ExamTest(rs.getInt("id"), rs.getString("name"),
                                rs.getString("subject"), rs.getInt("duration_minutes"));
                        list.add(t);
                        byId.put(t.id, t);
                    }
                }
            }
            try (PreparedStatement ps = c.prepareStatement(questionSql)) {
                if (onlyId != null) ps.setInt(1, onlyId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Main.ExamTest t = byId.get(rs.getInt("test_id"));
                        if (t == null) continue;
                        String[] options = {rs.getString("option_a"), rs.getString("option_b"),
                                rs.getString("option_c"), rs.getString("option_d")};
                        t.questions.add(new Main.Question(rs.getInt("id"), rs.getString("question_text"),
                                options, rs.getInt("correct_answer"), rs.getInt("marks")));
                    }
                }
            }
        } catch (SQLException e) { throw error(e); }
        return list;
    }

    /** Naya test banata hai aur generated id ke saath ExamTest return karta hai. */
    public static Main.ExamTest addTest(String name, String subject, int duration, int createdBy) {
        String sql = "INSERT INTO tests (name, subject, duration_minutes, created_by) VALUES (?, ?, ?, ?)";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, subject);
            ps.setInt(3, duration);
            if (createdBy > 0) ps.setInt(4, createdBy); else ps.setNull(4, Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new Main.ExamTest(keys.getInt(1), name, subject, duration);
            }
        } catch (SQLException e) { throw error(e); }
    }

    public static void updateTest(int id, String name, String subject, int duration) {
        String sql = "UPDATE tests SET name = ?, subject = ?, duration_minutes = ? WHERE id = ?";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, subject);
            ps.setInt(3, duration);
            ps.setInt(4, id);
            ps.executeUpdate();
        } catch (SQLException e) { throw error(e); }
    }

    public static void deleteTest(int id) {
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement("DELETE FROM tests WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();   // questions ON DELETE CASCADE se khud delete ho jaate hain
        } catch (SQLException e) { throw error(e); }
    }

    // =================================================================
    // QUESTIONS
    // =================================================================
    public static void addQuestion(int testId, String text, String[] options, int correct, int marks) {
        String sql = "INSERT INTO questions (test_id, question_text, option_a, option_b, option_c, option_d, "
                + "correct_answer, marks) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, testId);
            ps.setString(2, text);
            for (int i = 0; i < 4; i++) ps.setString(3 + i, options[i]);
            ps.setInt(7, correct);
            ps.setInt(8, marks);
            ps.executeUpdate();
        } catch (SQLException e) { throw error(e); }
    }

    public static void updateQuestion(int id, String text, String[] options, int correct, int marks) {
        String sql = "UPDATE questions SET question_text = ?, option_a = ?, option_b = ?, option_c = ?, "
                + "option_d = ?, correct_answer = ?, marks = ? WHERE id = ?";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, text);
            for (int i = 0; i < 4; i++) ps.setString(2 + i, options[i]);
            ps.setInt(6, correct);
            ps.setInt(7, marks);
            ps.setInt(8, id);
            ps.executeUpdate();
        } catch (SQLException e) { throw error(e); }
    }

    public static void deleteQuestion(int id) {
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement("DELETE FROM questions WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { throw error(e); }
    }

    // =================================================================
    // RESULTS
    // =================================================================
    public static Main.ExamResult saveResult(int studentId, Main.ExamTest test, int score, int total) {
        String sql = "INSERT INTO results (student_id, test_id, test_name, subject, score, total) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, test.id);
            ps.setString(3, test.name);
            ps.setString(4, test.subject);
            ps.setInt(5, score);
            ps.setInt(6, total);
            ps.executeUpdate();
        } catch (SQLException e) { throw error(e); }
        String date = new SimpleDateFormat("dd MMM yyyy, hh:mm a").format(new java.util.Date());
        return new Main.ExamResult("", test.name, test.subject, score, total, date);
    }

    /** Admin ke liye: sabhi students ke results. */
    public static List<Main.ExamResult> getResults() {
        return loadResults(0);
    }

    /** Student ke liye: sirf us student ke results (naye pehle). */
    public static List<Main.ExamResult> getResultsForStudent(int studentId) {
        return loadResults(studentId);
    }

    private static List<Main.ExamResult> loadResults(int studentId) {
        List<Main.ExamResult> list = new ArrayList<>();
        String sql = "SELECT u.username, r.test_name, r.subject, r.score, r.total, r.submitted_at "
                + "FROM results r JOIN users u ON u.id = r.student_id"
                + (studentId > 0 ? " WHERE r.student_id = ? ORDER BY r.id DESC" : " ORDER BY r.id");
        SimpleDateFormat fmt = new SimpleDateFormat("dd MMM yyyy, hh:mm a");
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (studentId > 0) ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Main.ExamResult(rs.getString("username"), rs.getString("test_name"),
                            rs.getString("subject"), rs.getInt("score"), rs.getInt("total"),
                            fmt.format(rs.getTimestamp("submitted_at"))));
                }
            }
        } catch (SQLException e) { throw error(e); }
        return list;
    }
}
