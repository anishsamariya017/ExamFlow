import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * FILE 3 of 3 : GUI (ab MySQL database use karta hai via Database.java)
 */
public class Main extends JFrame {
    private static final Color BLUE = new Color(79, 70, 229);
    private static final Color PURPLE = new Color(124, 58, 237);
    private static final Color CYAN = new Color(6, 182, 212);
    private static final Color GREEN = new Color(16, 185, 129);
    private static final Color ORANGE = new Color(245, 158, 11);
    private static final Color DARK = new Color(30, 41, 59);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color BG = new Color(243, 246, 255);
    private static final Color BORDER = new Color(226, 232, 255);

    private String currentUser = "";
    private String currentRole = "";
    private int currentUserId = 0;
    private int currentQuestionIndex = 0;
    private ExamTest activeTest;
    private int[] selectedAnswers;

    public Main() {
        setTitle("ExamFlow | Question Bank & Examination System");
        setSize(1120, 740);
        setMinimumSize(new Dimension(900, 620));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        showLogin();
    }

    // ===================== MODEL CLASSES =====================
    static class AppUser {
        int id; String username; String password; String role;
        AppUser(int id, String username, String password, String role) {
            this.id = id; this.username = username; this.password = password; this.role = role;
        }
    }

    static class Question {
        int id; String text; String[] options; int correctAnswer; int marks;
        Question(int id, String text, String[] options, int correctAnswer, int marks) {
            this.id = id; this.text = text; this.options = options;
            this.correctAnswer = correctAnswer; this.marks = marks;
        }
    }

    static class ExamTest {
        int id; String name; String subject; int duration;
        List<Question> questions = new ArrayList<>();
        ExamTest(int id, String name, String subject, int duration) {
            this.id = id; this.name = name; this.subject = subject; this.duration = duration;
        }
        int totalMarks() {
            int total = 0;
            for (Question q : questions) total += q.marks;
            return total;
        }
    }

    static class ExamResult {
        String student, testName, subject, date;
        int score, total;
        ExamResult(String student, String testName, String subject, int score, int total, String date) {
            this.student = student; this.testName = testName; this.subject = subject;
            this.score = score; this.total = total; this.date = date;
        }
    }

    // ===================== UI HELPERS =====================
    private JPanel page() {
        JPanel p = new JPanel(new BorderLayout(20, 20));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(25, 28, 25, 28));
        return p;
    }

    private Font f(int style, int size) { return new Font("SansSerif", style, size); }

    private Component gap(int size) { return Box.createVerticalStrut(size); }

    private JPanel transparentPanel() { JPanel p = new JPanel(); p.setOpaque(false); return p; }

    private JPanel transparentPanel(LayoutManager layout) { JPanel p = new JPanel(layout); p.setOpaque(false); return p; }

    private JLabel heading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(f(Font.BOLD, 27));
        label.setForeground(DARK);
        return label;
    }

    private JLabel subtitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(f(Font.PLAIN, 14));
        label.setForeground(MUTED);
        return label;
    }

    private JButton button(String text) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fill = getModel().isPressed() ? PURPLE
                        : getModel().isRollover() ? new Color(99, 102, 241) : BLUE;
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(f(Font.BOLD, 13));
        b.setForeground(Color.WHITE);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(11, 18, 11, 18));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton secondaryButton(String text) {
        JButton b = new JButton(text);
        b.setFont(f(Font.BOLD, 13));
        b.setForeground(DARK);
        b.setBackground(new Color(229, 231, 255));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setBorder(new EmptyBorder(10, 16, 10, 16));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1), new EmptyBorder(20, 20, 20, 20)));
        return p;
    }

    private void setPage(JPanel panel) {
        setContentPane(panel);
        revalidate();
        repaint();
    }

    private void addHeader(JPanel panel, String title, String description) {
        JPanel header = transparentPanel(new BorderLayout(10, 6));
        JPanel labels = transparentPanel();
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        labels.add(heading(title));
        labels.add(gap(4));
        labels.add(subtitle(description));
        header.add(labels, BorderLayout.CENTER);
        JButton logout = secondaryButton("↪  Logout");
        logout.addActionListener(e -> {
            currentUser = "";
            currentRole = "";
            currentUserId = 0;
            activeTest = null;
            selectedAnswers = null;
            showLogin();
        });
        header.add(logout, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);
    }

    private void showMessage(String text) {
        JOptionPane.showMessageDialog(this, text, "ExamFlow", JOptionPane.INFORMATION_MESSAGE);
    }

    private JPanel statCard(String label, String value, Color accent, String caption) {
        JPanel p = card();
        p.setLayout(new BorderLayout(12, 8));
        JPanel strip = new JPanel();
        strip.setBackground(accent);
        strip.setPreferredSize(new Dimension(6, 10));
        p.add(strip, BorderLayout.WEST);
        JPanel textPanel = transparentPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        JLabel l = new JLabel(label);
        l.setForeground(MUTED);
        l.setFont(f(Font.BOLD, 13));
        JLabel v = new JLabel(value);
        v.setForeground(accent);
        v.setFont(f(Font.BOLD, 32));
        JLabel c = new JLabel(caption);
        c.setForeground(MUTED);
        c.setFont(f(Font.PLAIN, 12));
        textPanel.add(l); textPanel.add(gap(6));
        textPanel.add(v); textPanel.add(gap(4));
        textPanel.add(c);
        p.add(textPanel, BorderLayout.CENTER);
        return p;
    }

    private JPanel sectionTitle(String title, String description) {
        JPanel p = transparentPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel t = heading(title);
        t.setFont(f(Font.BOLD, 20));
        p.add(t);
        p.add(gap(4));
        p.add(subtitle(description));
        return p;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(f(Font.BOLD, 13));
        l.setForeground(DARK);
        return l;
    }

    private void styleField(JTextField field) {
        field.setFont(f(Font.PLAIN, 14));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        field.setPreferredSize(new Dimension(300, 42));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(8, 10, 8, 10)));
    }

    private void styleCombo(JComboBox<?> combo) {
        combo.setFont(f(Font.PLAIN, 14));
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        combo.setPreferredSize(new Dimension(300, 42));
        combo.setBackground(Color.WHITE);
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private JPanel emptyState(String title, String message) {
        JPanel p = card();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel t = heading(title);
        t.setFont(f(Font.BOLD, 20));
        t.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel m = subtitle(message);
        m.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(gap(15)); p.add(t); p.add(gap(8)); p.add(m); p.add(gap(15));
        return p;
    }

    // ===================== LOGIN =====================
    private void showLogin() {
        JPanel root = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, new Color(49, 46, 129),
                        getWidth(), getHeight(), new Color(124, 58, 237)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        JPanel layout = new JPanel(new GridLayout(1, 2, 0, 0));
        layout.setPreferredSize(new Dimension(850, 510));
        layout.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 90), 1));

        JPanel brand = new JPanel();
        brand.setBackground(new Color(49, 46, 129));
        brand.setBorder(new EmptyBorder(38, 35, 38, 35));
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        JLabel logo = new JLabel("EXAMFLOW");
        logo.setForeground(Color.WHITE);
        logo.setFont(f(Font.BOLD, 28));
        JLabel tagline = new JLabel("<html><div style='width:280px'>Your learning journey,<br>one test at a time.</div></html>");
        tagline.setForeground(new Color(224, 231, 255));
        tagline.setFont(f(Font.BOLD, 24));
        JLabel details = new JLabel("<html><div style='width:280px;line-height:1.7'>A simple and organized space for teachers to create tests and students to measure their progress.</div></html>");
        details.setForeground(new Color(203, 213, 255));
        details.setFont(f(Font.PLAIN, 14));
        brand.add(logo);
        brand.add(gap(55)); brand.add(tagline);
        brand.add(gap(18)); brand.add(details);
        brand.add(Box.createVerticalGlue());
        JLabel foot = new JLabel("QUESTION BANK  •  ONLINE EXAMS");
        foot.setForeground(new Color(199, 210, 254));
        foot.setFont(f(Font.BOLD, 10));
        brand.add(foot);

        JPanel form = new JPanel();
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(38, 36, 38, 36));
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        JLabel welcome = heading("Welcome back");
        welcome.setFont(f(Font.BOLD, 25));
        JLabel hint = subtitle("Sign in to access your dashboard");
        form.add(welcome); form.add(gap(8));
        form.add(hint); form.add(gap(32));

        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();
        JComboBox<String> role = new JComboBox<>(new String[]{"Admin", "Teacher", "Student"});
        styleField(username);
        styleField(password);
        styleCombo(role);
        form.add(fieldLabel("Username")); form.add(gap(7));
        form.add(username); form.add(gap(18));
        form.add(fieldLabel("Password")); form.add(gap(7));
        form.add(password); form.add(gap(18));
        form.add(fieldLabel("Login as")); form.add(gap(7));
        form.add(role); form.add(gap(25));

        JButton login = button("Sign In  →");
        login.setAlignmentX(Component.LEFT_ALIGNMENT);
        login.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        login.setPreferredSize(new Dimension(300, 45));
        form.add(login);

        login.addActionListener(e -> {
            String user = username.getText().trim();
            String pass = new String(password.getPassword());
            String selectedRole = ((String) role.getSelectedItem()).toUpperCase();
            AppUser matched = Database.login(user, pass, selectedRole);   // <-- DATABASE
            if (matched == null) {
                JOptionPane.showMessageDialog(this, "Username, password or role is incorrect.",
                        "Login failed", JOptionPane.ERROR_MESSAGE);
                return;
            }
            currentUserId = matched.id;
            currentUser = matched.username;
            currentRole = matched.role;
            if ("ADMIN".equals(currentRole)) showAdminDashboard();
            else if ("TEACHER".equals(currentRole)) showTeacherDashboard();
            else showStudentDashboard();
        });
        password.addActionListener(e -> login.doClick());
        layout.add(brand);
        layout.add(form);
        root.add(layout);
        setPage(root);
    }

    // ===================== ADMIN =====================
    private void showAdminDashboard() {
        JPanel root = page();
        addHeader(root, "Admin Dashboard", "Manage accounts, monitor tests and review student results.");
        int studentCount = Database.countUsers("STUDENT");
        int teacherCount = Database.countUsers("TEACHER");
        List<ExamTest> tests = Database.getTests();
        int resultCount = Database.getResults().size();

        JPanel body = transparentPanel(new BorderLayout(18, 18));
        JPanel stats = transparentPanel(new GridLayout(1, 4, 12, 0));
        stats.add(statCard("STUDENTS", String.valueOf(studentCount), BLUE, "Registered accounts"));
        stats.add(statCard("TEACHERS", String.valueOf(teacherCount), PURPLE, "Teaching accounts"));
        stats.add(statCard("TOTAL TESTS", String.valueOf(tests.size()), CYAN, "Available assessments"));
        stats.add(statCard("RESULTS", String.valueOf(resultCount), GREEN, "Submissions recorded"));
        body.add(stats, BorderLayout.NORTH);

        JPanel actions = transparentPanel(new GridLayout(0, 2, 15, 15));
        actions.add(adminActionCard("Manage Users", "Create, edit or remove teacher and student accounts.", "Open User Management", this::showUserManagement));
        actions.add(adminActionCard("All Tests", "Review all tests and their question counts.", "View All Tests", this::showAdminTests));
        actions.add(adminActionCard("Student Results", "Review scores submitted by students.", "View Results", this::showAdminResults));
        actions.add(adminActionCard("Question Overview", "See question totals for each assessment.", "View Questions Summary", this::showAdminTests));
        body.add(actions, BorderLayout.CENTER);
        root.add(body, BorderLayout.CENTER);
        setPage(root);
    }

    private JPanel adminActionCard(String title, String description, String buttonText, Runnable action) {
        JPanel p = card();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel t = heading(title);
        t.setFont(f(Font.BOLD, 20));
        JLabel d = new JLabel("<html><div style='width:300px'>" + escapeHtml(description) + "</div></html>");
        d.setForeground(MUTED);
        d.setFont(f(Font.PLAIN, 13));
        JButton b = button(buttonText);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.addActionListener(e -> action.run());
        p.add(t); p.add(gap(10)); p.add(d);
        p.add(Box.createVerticalGlue()); p.add(gap(18));
        p.add(b);
        return p;
    }

    private void showUserManagement() {
        JPanel root = page();
        addHeader(root, "User Management", "Add, edit and remove teacher or student accounts.");
        List<AppUser> users = Database.getUsers();
        JPanel content = transparentPanel(new BorderLayout(12, 12));
        JPanel top = transparentPanel(new BorderLayout());
        top.add(sectionTitle("Registered Accounts", "Admin accounts are protected from deletion here."), BorderLayout.WEST);
        JButton add = button("+ Add User");
        add.addActionListener(e -> userDialog(null));
        top.add(add, BorderLayout.EAST);
        content.add(top, BorderLayout.NORTH);

        String[] columns = {"Username", "Role", "Password", "Actions"};
        Object[][] data = new Object[users.size()][4];
        for (int i = 0; i < users.size(); i++) {
            AppUser u = users.get(i);
            data[i][0] = u.username;
            data[i][1] = u.role;
            data[i][2] = "••••••••";
            data[i][3] = "Select row";
        }
        JTable table = new JTable(data, columns);
        table.setRowHeight(34);
        table.setFont(f(Font.PLAIN, 13));
        table.getTableHeader().setFont(f(Font.BOLD, 13));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        content.add(scroll, BorderLayout.CENTER);

        JPanel controls = transparentPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JButton edit = secondaryButton("Edit Selected");
        JButton delete = secondaryButton("Delete Selected");
        JButton back = secondaryButton("← Back to Admin Dashboard");
        edit.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { showMessage("Select a user first."); return; }
            userDialog(users.get(row));
        });
        delete.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { showMessage("Select a user first."); return; }
            AppUser u = users.get(row);
            if ("ADMIN".equals(u.role)) { showMessage("Admin accounts cannot be deleted here."); return; }
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Delete account '" + u.username + "'?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                Database.deleteUser(u.id);                    // <-- DATABASE
                showUserManagement();
            }
        });
        back.addActionListener(e -> showAdminDashboard());
        controls.add(edit); controls.add(delete); controls.add(back);
        content.add(controls, BorderLayout.SOUTH);
        root.add(content, BorderLayout.CENTER);
        setPage(root);
    }

    private void userDialog(AppUser existing) {
        boolean isAdminAccount = existing != null && "ADMIN".equals(existing.role);
        JTextField username = new JTextField(existing == null ? "" : existing.username);
        JPasswordField password = new JPasswordField(existing == null ? "" : existing.password);
        JComboBox<String> role = new JComboBox<>(isAdminAccount
                ? new String[]{"ADMIN"} : new String[]{"TEACHER", "STUDENT"});
        if (existing != null) role.setSelectedItem(existing.role);
        if (isAdminAccount) role.setEnabled(false);
        styleField(username); styleField(password); styleCombo(role);
        JPanel form = new JPanel(new GridLayout(0, 1, 6, 6));
        form.setBorder(new EmptyBorder(8, 8, 8, 8));
        form.add(fieldLabel("Username")); form.add(username);
        form.add(fieldLabel("Password")); form.add(password);
        form.add(fieldLabel("Role")); form.add(role);
        int choice = JOptionPane.showConfirmDialog(this, form,
                existing == null ? "Create User" : "Edit User", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;
        String newName = username.getText().trim();
        String newPass = new String(password.getPassword());
        if (newName.isEmpty() || newPass.isEmpty()) { showMessage("Username and password are required."); return; }
        if (Database.usernameExists(newName, existing == null ? 0 : existing.id)) {
            showMessage("That username already exists.");
            return;
        }
        if (existing == null) Database.addUser(newName, newPass, (String) role.getSelectedItem());
        else Database.updateUser(existing.id, newName, newPass, (String) role.getSelectedItem());
        showUserManagement();
    }

    private void showAdminTests() {
        JPanel root = page();
        addHeader(root, "All Tests", "Overview of every test and its questions.");
        List<ExamTest> tests = Database.getTests();
        JPanel list = transparentPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        if (tests.isEmpty()) list.add(emptyState("No tests available", "Teachers have not created any tests yet."));
        for (ExamTest t : tests) {
            JPanel p = card();
            p.setLayout(new BorderLayout(12, 8));
            JLabel title = new JLabel(t.name + " — " + t.subject);
            title.setFont(f(Font.BOLD, 17));
            JLabel detail = subtitle(t.questions.size() + " questions  •  " + t.duration + " minutes  •  " + t.totalMarks() + " marks");
            p.add(title, BorderLayout.NORTH);
            p.add(detail, BorderLayout.CENTER);
            JButton view = secondaryButton("View Questions");
            view.addActionListener(e -> showAdminQuestionView(t));
            p.add(view, BorderLayout.EAST);
            list.add(p);
            list.add(gap(10));
        }
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        JButton back = secondaryButton("← Back to Admin Dashboard");
        back.addActionListener(e -> showAdminDashboard());
        JPanel bottom = transparentPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(back);
        root.add(scroll, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);
        setPage(root);
    }

    private void showAdminQuestionView(ExamTest test) {
        JPanel root = page();
        addHeader(root, test.name + " Questions", "Read-only question overview for administrators.");
        JPanel list = transparentPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        if (test.questions.isEmpty()) list.add(emptyState("No questions", "This test has no questions yet."));
        for (int i = 0; i < test.questions.size(); i++) {
            Question q = test.questions.get(i);
            JPanel p = card();
            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
            JLabel question = new JLabel("Q" + (i + 1) + ". " + q.text);
            question.setFont(f(Font.BOLD, 15));
            p.add(question);
            p.add(gap(8));
            for (int j = 0; j < 4; j++) {
                JLabel option = new JLabel((char) ('A' + j) + ". " + q.options[j] + (j == q.correctAnswer ? "  ✓ Correct" : ""));
                option.setForeground(j == q.correctAnswer ? GREEN : DARK);
                p.add(option);
            }
            list.add(p);
            list.add(gap(10));
        }
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        JButton back = secondaryButton("← Back to All Tests");
        back.addActionListener(e -> showAdminTests());
        JPanel bottom = transparentPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(back);
        root.add(scroll, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);
        setPage(root);
    }

    private void showAdminResults() {
        JPanel root = page();
        addHeader(root, "Student Results", "All submitted test results across student accounts.");
        List<ExamResult> results = Database.getResults();
        String[] columns = {"Student", "Test", "Subject", "Score", "Percentage", "Submitted"};
        Object[][] data = new Object[results.size()][6];
        for (int i = 0; i < results.size(); i++) {
            ExamResult r = results.get(i);
            double pct = r.total == 0 ? 0 : r.score * 100.0 / r.total;
            data[i][0] = r.student;
            data[i][1] = r.testName;
            data[i][2] = r.subject;
            data[i][3] = r.score + " / " + r.total;
            data[i][4] = String.format("%.1f%%", pct);
            data[i][5] = r.date;
        }
        JTable table = new JTable(data, columns);
        table.setRowHeight(32);
        table.setFont(f(Font.PLAIN, 13));
        table.getTableHeader().setFont(f(Font.BOLD, 12));
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        JButton back = secondaryButton("← Back to Admin Dashboard");
        back.addActionListener(e -> showAdminDashboard());
        JPanel bottom = transparentPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(back);
        root.add(scroll, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);
        setPage(root);
    }

    // ===================== TEACHER =====================
    private void showTeacherDashboard() {
        JPanel root = page();
        addHeader(root, "Teacher Dashboard", "Create assessments, manage questions and track your test bank.");
        List<ExamTest> tests = Database.getTests();
        JPanel body = transparentPanel(new BorderLayout(18, 18));
        int totalQuestions = 0;
        for (ExamTest t : tests) totalQuestions += t.questions.size();
        JPanel stats = transparentPanel(new GridLayout(1, 3, 15, 0));
        stats.add(statCard("TOTAL TESTS", String.valueOf(tests.size()), BLUE, "Assessments created"));
        stats.add(statCard("TOTAL QUESTIONS", String.valueOf(totalQuestions), PURPLE, "Across all tests"));
        stats.add(statCard("TOTAL SUBJECTS",
                String.valueOf(tests.stream().map(t -> t.subject.toLowerCase()).distinct().count()), CYAN, "Subjects covered"));

        JPanel listPanel = transparentPanel(new BorderLayout(0, 14));
        JPanel listHeader = transparentPanel(new BorderLayout());
        listHeader.add(sectionTitle("Your Tests", "Open a test to manage its questions."), BorderLayout.WEST);
        JButton create = button("+  Create New Test");
        create.addActionListener(e -> createTestDialog());
        listHeader.add(create, BorderLayout.EAST);
        listPanel.add(listHeader, BorderLayout.NORTH);

        JPanel list = transparentPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        if (tests.isEmpty()) {
            list.add(emptyState("No tests created yet", "Create your first test to start adding questions."));
        } else {
            for (ExamTest test : tests) {
                list.add(teacherTestCard(test));
                list.add(gap(12));
            }
        }
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        listPanel.add(scroll, BorderLayout.CENTER);
        body.add(stats, BorderLayout.NORTH);
        body.add(listPanel, BorderLayout.CENTER);
        root.add(body, BorderLayout.CENTER);
        setPage(root);
    }

    private JPanel teacherTestCard(ExamTest test) {
        JPanel p = card();
        p.setLayout(new BorderLayout(15, 10));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        JPanel icon = new JPanel(new GridBagLayout());
        icon.setBackground(new Color(238, 242, 255));
        icon.setPreferredSize(new Dimension(62, 62));
        JLabel iconText = new JLabel("T" + test.id);
        iconText.setFont(f(Font.BOLD, 18));
        iconText.setForeground(BLUE);
        icon.add(iconText);
        JPanel details = transparentPanel();
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(test.name);
        name.setFont(f(Font.BOLD, 18));
        JLabel subject = new JLabel(test.subject);
        subject.setForeground(BLUE);
        subject.setFont(f(Font.BOLD, 13));
        JLabel info = subtitle(test.questions.size() + " questions   •   " + test.duration + " min   •   " + test.totalMarks() + " marks");
        details.add(name); details.add(gap(5));
        details.add(subject); details.add(gap(7));
        details.add(info);
        JPanel actions = transparentPanel(new FlowLayout(FlowLayout.RIGHT, 7, 8));
        JButton manage = button("Manage Questions");
        JButton edit = secondaryButton("Edit");
        JButton delete = secondaryButton("Delete");
        manage.addActionListener(e -> showQuestionManager(test));
        edit.addActionListener(e -> editTestDialog(test));
        delete.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Delete " + test.name + " and all its questions?",
                    "Delete test", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                Database.deleteTest(test.id);                  // <-- DATABASE
                showTeacherDashboard();
            }
        });
        actions.add(manage); actions.add(edit); actions.add(delete);
        p.add(icon, BorderLayout.WEST);
        p.add(details, BorderLayout.CENTER);
        p.add(actions, BorderLayout.SOUTH);
        return p;
    }

    private void createTestDialog() {
        JTextField name = new JTextField();
        JTextField subject = new JTextField();
        JSpinner duration = new JSpinner(new SpinnerNumberModel(15, 1, 300, 5));
        styleField(name); styleField(subject);
        JPanel form = new JPanel(new GridLayout(0, 1, 6, 6));
        form.setBorder(new EmptyBorder(8, 8, 8, 8));
        form.add(fieldLabel("Test name")); form.add(name);
        form.add(fieldLabel("Subject")); form.add(subject);
        form.add(fieldLabel("Duration (minutes)")); form.add(duration);
        int choice = JOptionPane.showConfirmDialog(this, form, "Create a new test", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;
        if (name.getText().trim().isEmpty() || subject.getText().trim().isEmpty()) {
            showMessage("Please enter both test name and subject.");
            return;
        }
        ExamTest test = Database.addTest(name.getText().trim(), subject.getText().trim(),
                (Integer) duration.getValue(), currentUserId);   // <-- DATABASE
        showTeacherDashboard();
        int add = JOptionPane.showConfirmDialog(this, "Test created successfully. Add questions now?",
                "Test created", JOptionPane.YES_NO_OPTION);
        if (add == JOptionPane.YES_OPTION) showQuestionManager(test);
    }

    private void editTestDialog(ExamTest test) {
        JTextField name = new JTextField(test.name);
        JTextField subject = new JTextField(test.subject);
        JSpinner duration = new JSpinner(new SpinnerNumberModel(test.duration, 1, 300, 5));
        styleField(name); styleField(subject);
        JPanel form = new JPanel(new GridLayout(0, 1, 6, 6));
        form.setBorder(new EmptyBorder(8, 8, 8, 8));
        form.add(fieldLabel("Test name")); form.add(name);
        form.add(fieldLabel("Subject")); form.add(subject);
        form.add(fieldLabel("Duration (minutes)")); form.add(duration);
        int choice = JOptionPane.showConfirmDialog(this, form, "Edit test", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;
        if (name.getText().trim().isEmpty() || subject.getText().trim().isEmpty()) {
            showMessage("Test name and subject cannot be empty.");
            return;
        }
        Database.updateTest(test.id, name.getText().trim(), subject.getText().trim(), (Integer) duration.getValue());
        showTeacherDashboard();
    }

    private void showQuestionManager(ExamTest t) {
        ExamTest test = Database.getTest(t.id);                // fresh data from DB
        if (test == null) { showTeacherDashboard(); return; }
        JPanel root = page();
        addHeader(root, "Question Manager", test.name + "  •  " + test.subject + "  •  " + test.questions.size() + " questions");
        JPanel content = transparentPanel(new BorderLayout(15, 15));
        JPanel top = transparentPanel(new BorderLayout());
        top.add(sectionTitle("Questions", "Add, edit or remove MCQs for this test."), BorderLayout.WEST);
        JButton add = button("+  Add Question");
        add.addActionListener(e -> questionDialog(test, null));
        top.add(add, BorderLayout.EAST);
        content.add(top, BorderLayout.NORTH);

        JPanel list = transparentPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        if (test.questions.isEmpty()) {
            list.add(emptyState("No questions yet", "Use Add Question to build this test."));
        } else {
            for (int i = 0; i < test.questions.size(); i++) {
                final Question q = test.questions.get(i);
                final int number = i + 1;
                JPanel qCard = card();
                qCard.setLayout(new BorderLayout(10, 10));
                JPanel qTop = transparentPanel(new BorderLayout(10, 5));
                JLabel numberLabel = new JLabel("QUESTION " + number);
                numberLabel.setFont(f(Font.BOLD, 11));
                numberLabel.setForeground(BLUE);
                JLabel marksLabel = new JLabel(q.marks + " mark(s)");
                marksLabel.setForeground(GREEN);
                qTop.add(numberLabel, BorderLayout.WEST);
                qTop.add(marksLabel, BorderLayout.EAST);
                JPanel details = transparentPanel();
                details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
                JLabel questionText = new JLabel("<html><div style='width:700px'>" + escapeHtml(q.text) + "</div></html>");
                questionText.setFont(f(Font.BOLD, 16));
                details.add(questionText);
                details.add(gap(12));
                for (int j = 0; j < 4; j++) {
                    JLabel option = new JLabel((char) ('A' + j) + ". " + q.options[j] + (j == q.correctAnswer ? "   ✓ Correct answer" : ""));
                    option.setFont(f(Font.PLAIN, 13));
                    option.setForeground(j == q.correctAnswer ? new Color(5, 150, 105) : DARK);
                    details.add(option);
                    details.add(gap(5));
                }
                JPanel buttons = transparentPanel(new FlowLayout(FlowLayout.RIGHT));
                JButton edit = secondaryButton("Edit Question");
                JButton delete = secondaryButton("Delete");
                edit.addActionListener(e -> questionDialog(test, q));
                delete.addActionListener(e -> {
                    int confirm = JOptionPane.showConfirmDialog(this,
                            "Delete question " + number + "?", "Delete question", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        Database.deleteQuestion(q.id);          // <-- DATABASE
                        showQuestionManager(test);
                    }
                });
                buttons.add(edit);
                buttons.add(delete);
                JPanel middle = transparentPanel(new BorderLayout(0, 10));
                middle.add(qTop, BorderLayout.NORTH);
                middle.add(details, BorderLayout.CENTER);
                qCard.add(middle, BorderLayout.CENTER);
                qCard.add(buttons, BorderLayout.SOUTH);
                list.add(qCard);
                list.add(gap(12));
            }
        }
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        content.add(scroll, BorderLayout.CENTER);
        JButton back = secondaryButton("←  Back to Dashboard");
        back.addActionListener(e -> showTeacherDashboard());
        JPanel bottom = transparentPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(back);
        content.add(bottom, BorderLayout.SOUTH);
        root.add(content, BorderLayout.CENTER);
        setPage(root);
    }

    private void questionDialog(ExamTest test, Question existing) {
        JTextField questionText = new JTextField();
        JTextField[] optionFields = new JTextField[4];
        for (int i = 0; i < 4; i++) optionFields[i] = new JTextField();
        JComboBox<String> correct = new JComboBox<>(new String[]{"A", "B", "C", "D"});
        JSpinner marks = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        if (existing != null) {
            questionText.setText(existing.text);
            for (int i = 0; i < 4; i++) optionFields[i].setText(existing.options[i]);
            correct.setSelectedIndex(existing.correctAnswer);
            marks.setValue(existing.marks);
        }
        styleField(questionText);
        for (JTextField field : optionFields) styleField(field);
        JPanel form = new JPanel(new GridLayout(0, 1, 6, 6));
        form.setBorder(new EmptyBorder(8, 8, 8, 8));
        form.add(fieldLabel("Question text"));
        form.add(questionText);
        for (int i = 0; i < 4; i++) {
            form.add(fieldLabel("Option " + (char) ('A' + i)));
            form.add(optionFields[i]);
        }
        form.add(fieldLabel("Correct answer"));
        form.add(correct);
        form.add(fieldLabel("Marks"));
        form.add(marks);
        JScrollPane scroll = new JScrollPane(form);
        scroll.setPreferredSize(new Dimension(470, 480));
        int choice = JOptionPane.showConfirmDialog(this, scroll,
                existing == null ? "Add MCQ Question" : "Edit MCQ Question", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;
        if (questionText.getText().trim().isEmpty()) {
            showMessage("Please enter the question text.");
            return;
        }
        String[] options = new String[4];
        for (int i = 0; i < 4; i++) {
            options[i] = optionFields[i].getText().trim();
            if (options[i].isEmpty()) {
                showMessage("Please fill in all four options.");
                return;
            }
        }
        String text = questionText.getText().trim();
        int mk = (Integer) marks.getValue();
        if (existing == null) Database.addQuestion(test.id, text, options, correct.getSelectedIndex(), mk);
        else Database.updateQuestion(existing.id, text, options, correct.getSelectedIndex(), mk);
        showQuestionManager(test);
    }

    // ===================== STUDENT =====================
    private void showStudentDashboard() {
        JPanel root = page();
        addHeader(root, "Student Dashboard", "Explore your available tests and check your results.");
        List<ExamTest> tests = Database.getTests();
        int attempted = Database.getResultsForStudent(currentUserId).size();
        JPanel body = transparentPanel(new BorderLayout(18, 18));
        JPanel stats = transparentPanel(new GridLayout(1, 2, 15, 0));
        stats.add(statCard("AVAILABLE TESTS", String.valueOf(tests.size()), BLUE, "Ready for you to attempt"));
        stats.add(statCard("TESTS ATTEMPTED", String.valueOf(attempted), GREEN, "Your completed tests"));
        JPanel testPanel = transparentPanel(new BorderLayout(0, 14));
        JPanel top = transparentPanel(new BorderLayout());
        top.add(sectionTitle("Available Tests", "Choose a subject to view test instructions."), BorderLayout.WEST);
        JButton myResults = secondaryButton("▤  My Results");
        myResults.addActionListener(e -> showStudentResults());
        top.add(myResults, BorderLayout.EAST);
        testPanel.add(top, BorderLayout.NORTH);
        JPanel grid = transparentPanel(new GridLayout(0, 2, 15, 15));
        if (tests.isEmpty()) {
            grid.setLayout(new BorderLayout());
            grid.add(emptyState("No tests available", "Please check again later."));
        } else {
            for (ExamTest test : tests) grid.add(studentTestCard(test));
        }
        JScrollPane scroll = new JScrollPane(grid);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        testPanel.add(scroll, BorderLayout.CENTER);
        body.add(stats, BorderLayout.NORTH);
        body.add(testPanel, BorderLayout.CENTER);
        root.add(body, BorderLayout.CENTER);
        setPage(root);
    }

    private JPanel studentTestCard(ExamTest test) {
        JPanel p = card();
        p.setLayout(new BorderLayout(8, 12));
        JPanel top = transparentPanel(new BorderLayout(8, 8));
        JPanel badge = new JPanel(new GridBagLayout());
        badge.setBackground(new Color(238, 242, 255));
        badge.setPreferredSize(new Dimension(50, 50));
        JLabel badgeLabel = new JLabel("✎");
        badgeLabel.setFont(f(Font.BOLD, 24));
        badgeLabel.setForeground(BLUE);
        badge.add(badgeLabel);
        JPanel labels = transparentPanel();
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(test.name);
        name.setFont(f(Font.BOLD, 19));
        JLabel subject = new JLabel(test.subject);
        subject.setForeground(BLUE);
        subject.setFont(f(Font.BOLD, 13));
        labels.add(name); labels.add(gap(5)); labels.add(subject);
        top.add(badge, BorderLayout.WEST);
        top.add(labels, BorderLayout.CENTER);
        JPanel details = transparentPanel(new GridLayout(0, 1, 5, 5));
        details.add(subtitle("Questions       " + test.questions.size()));
        details.add(subtitle("Duration          " + test.duration + " minutes"));
        details.add(subtitle("Total marks      " + test.totalMarks()));
        JButton view = button("View Test  →");
        view.addActionListener(e -> showTestDetails(test));
        p.add(top, BorderLayout.NORTH);
        p.add(details, BorderLayout.CENTER);
        p.add(view, BorderLayout.SOUTH);
        return p;
    }

    private void showTestDetails(ExamTest test) {
        JPanel root = page();
        addHeader(root, "Test Overview", "Review the details before you begin.");
        JPanel center = transparentPanel(new GridBagLayout());
        JPanel details = card();
        details.setPreferredSize(new Dimension(620, 500));
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        JLabel chip = new JLabel("  AVAILABLE TEST  ");
        chip.setOpaque(true);
        chip.setBackground(new Color(220, 252, 231));
        chip.setForeground(new Color(21, 128, 61));
        chip.setFont(f(Font.BOLD, 11));
        chip.setBorder(new EmptyBorder(7, 8, 7, 8));
        chip.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel title = heading(test.name);
        title.setFont(f(Font.BOLD, 29));
        JLabel subject = new JLabel(test.subject);
        subject.setFont(f(Font.BOLD, 17));
        subject.setForeground(BLUE);
        JPanel stats = transparentPanel(new GridLayout(1, 3, 10, 0));
        stats.add(miniStat("QUESTIONS", String.valueOf(test.questions.size()), BLUE));
        stats.add(miniStat("DURATION", test.duration + " min", PURPLE));
        stats.add(miniStat("TOTAL MARKS", String.valueOf(test.totalMarks()), GREEN));
        JLabel instTitle = heading("Instructions");
        instTitle.setFont(f(Font.BOLD, 18));
        JLabel instructions = new JLabel("<html><div style='width:520px;line-height:1.7'>" + "1. Read every question carefully.<br>"
                + "2. Choose one answer for each multiple-choice question.<br>" + "3. Use Previous and Next to move between questions.<br>"
                + "4. You can submit with unanswered questions, but they will receive zero marks.<br>"
                + "5. Your score will be displayed immediately after submission.</div></html>");
        instructions.setFont(f(Font.PLAIN, 13));
        JButton start = button("Start Test  →");
        start.setAlignmentX(Component.LEFT_ALIGNMENT);
        start.addActionListener(e -> {
            if (test.questions.isEmpty()) {
                showMessage("This test does not have any questions yet.");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this, "Ready to start " + test.name + "?\nDuration: "
                    + test.duration + " minutes", "Start test", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) startTest(test);
        });
        JButton back = secondaryButton("←  Back to Tests");
        back.setAlignmentX(Component.LEFT_ALIGNMENT);
        back.addActionListener(e -> showStudentDashboard());
        details.add(chip);
        details.add(gap(18)); details.add(title);
        details.add(gap(6)); details.add(subject);
        details.add(gap(22)); details.add(stats);
        details.add(gap(25)); details.add(instTitle);
        details.add(gap(12)); details.add(instructions);
        details.add(Box.createVerticalGlue()); details.add(start);
        details.add(gap(10)); details.add(back);
        center.add(details);
        root.add(center, BorderLayout.CENTER);
        setPage(root);
    }

    private JPanel miniStat(String label, String value, Color color) {
        JPanel p = new JPanel();
        p.setBackground(new Color(248, 250, 252));
        p.setBorder(new EmptyBorder(12, 10, 12, 10));
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel l = new JLabel(label);
        l.setFont(f(Font.BOLD, 10));
        l.setForeground(MUTED);
        JLabel v = new JLabel(value);
        v.setFont(f(Font.BOLD, 18));
        v.setForeground(color);
        p.add(l); p.add(gap(8)); p.add(v);
        return p;
    }

    private void startTest(ExamTest test) {
        ExamTest fresh = Database.getTest(test.id);            // latest questions from DB
        if (fresh == null || fresh.questions.isEmpty()) {
            showMessage("This test is no longer available.");
            showStudentDashboard();
            return;
        }
        activeTest = fresh;
        currentQuestionIndex = 0;
        selectedAnswers = new int[fresh.questions.size()];
        Arrays.fill(selectedAnswers, -1);
        showQuestionScreen();
    }

    private void showQuestionScreen() {
        if (activeTest == null || activeTest.questions.isEmpty()) {
            showStudentDashboard();
            return;
        }
        JPanel root = page();
        addHeader(root, activeTest.name + "  •  Exam", activeTest.subject + " — select an answer for each question.");
        Question question = activeTest.questions.get(currentQuestionIndex);
        JPanel content = transparentPanel(new BorderLayout(15, 15));
        JPanel progressCard = card();
        progressCard.setLayout(new BorderLayout(12, 10));
        JLabel progressLabel = new JLabel("QUESTION " + (currentQuestionIndex + 1) + " OF " + activeTest.questions.size());
        progressLabel.setFont(f(Font.BOLD, 12));
        progressLabel.setForeground(BLUE);
        JLabel marksLabel = new JLabel(question.marks + " mark(s)");
        marksLabel.setForeground(GREEN);
        progressCard.add(progressLabel, BorderLayout.WEST);
        progressCard.add(marksLabel, BorderLayout.EAST);
        JProgressBar progress = new JProgressBar(0, activeTest.questions.size());
        progress.setValue(currentQuestionIndex + 1);
        progress.setStringPainted(true);
        progress.setString((currentQuestionIndex + 1) + " / " + activeTest.questions.size());
        progress.setForeground(BLUE);
        progress.setBackground(new Color(224, 231, 255));
        progress.setBorderPainted(false);
        progress.setPreferredSize(new Dimension(100, 12));
        progressCard.add(progress, BorderLayout.SOUTH);
        content.add(progressCard, BorderLayout.NORTH);

        JPanel questionCard = card();
        questionCard.setLayout(new BoxLayout(questionCard, BoxLayout.Y_AXIS));
        JLabel qText = new JLabel("<html><div style='width:750px;line-height:1.5'>" + (currentQuestionIndex + 1) + ". " + escapeHtml(question.text) + "</div></html>");
        qText.setFont(f(Font.BOLD, 21));
        qText.setAlignmentX(Component.LEFT_ALIGNMENT);
        questionCard.add(qText);
        questionCard.add(gap(25));
        ButtonGroup group = new ButtonGroup();
        JRadioButton[] optionButtons = new JRadioButton[4];
        for (int i = 0; i < 4; i++) {
            final int optionIndex = i;
            optionButtons[i] = new JRadioButton((char) ('A' + i) + "     " + question.options[i]);
            optionButtons[i].setFont(f(Font.PLAIN, 16));
            optionButtons[i].setForeground(DARK);
            optionButtons[i].setBackground(Color.WHITE);
            optionButtons[i].setFocusPainted(false);
            optionButtons[i].setBorder(new EmptyBorder(13, 12, 13, 12));
            optionButtons[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            group.add(optionButtons[i]);
            questionCard.add(optionButtons[i]);
            questionCard.add(gap(9));
            optionButtons[i].addActionListener(e -> selectedAnswers[currentQuestionIndex] = optionIndex);
        }
        if (selectedAnswers[currentQuestionIndex] >= 0) {
            optionButtons[selectedAnswers[currentQuestionIndex]].setSelected(true);
        }
        JScrollPane qScroll = new JScrollPane(questionCard);
        qScroll.setBorder(null);
        qScroll.getViewport().setBackground(BG);
        content.add(qScroll, BorderLayout.CENTER);

        JPanel nav = transparentPanel(new BorderLayout());
        JLabel answered = new JLabel(answeredCount() + " of " + activeTest.questions.size() + " questions answered");
        answered.setForeground(MUTED);
        nav.add(answered, BorderLayout.WEST);
        JPanel buttons = transparentPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton previous = secondaryButton("←  Previous");
        JButton next = button("Next  →");
        JButton submit = button("Submit Test");
        previous.setEnabled(currentQuestionIndex > 0);
        next.setEnabled(currentQuestionIndex < activeTest.questions.size() - 1);
        previous.addActionListener(e -> {
            saveSelectedOption(optionButtons);
            currentQuestionIndex--;
            showQuestionScreen();
        });
        next.addActionListener(e -> {
            saveSelectedOption(optionButtons);
            currentQuestionIndex++;
            showQuestionScreen();
        });
        submit.addActionListener(e -> {
            saveSelectedOption(optionButtons);
            int unanswered = 0;
            for (int answer : selectedAnswers) if (answer == -1) unanswered++;
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Submit this test now?\nUnanswered questions: " + unanswered, "Confirm submission", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) calculateResult();
        });
        buttons.add(previous); buttons.add(next); buttons.add(submit);
        nav.add(buttons, BorderLayout.EAST);
        content.add(nav, BorderLayout.SOUTH);
        root.add(content, BorderLayout.CENTER);
        setPage(root);
    }

    private int answeredCount() {
        int count = 0;
        if (selectedAnswers != null) {
            for (int answer : selectedAnswers) if (answer >= 0) count++;
        }
        return count;
    }

    private void saveSelectedOption(JRadioButton[] buttons) {
        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i].isSelected()) {
                selectedAnswers[currentQuestionIndex] = i;
                return;
            }
        }
    }

    private void calculateResult() {
        int score = 0, total = 0;
        for (int i = 0; i < activeTest.questions.size(); i++) {
            Question q = activeTest.questions.get(i);
            total += q.marks;
            if (selectedAnswers[i] == q.correctAnswer) score += q.marks;
        }
        ExamResult result = Database.saveResult(currentUserId, activeTest, score, total);   // <-- DATABASE
        showResultScreen(result);
    }

    private void showResultScreen(ExamResult result) {
        JPanel root = page();
        addHeader(root, "Test Completed", "Here is your result summary.");
        JPanel center = transparentPanel(new GridBagLayout());
        JPanel resultCard = card();
        resultCard.setPreferredSize(new Dimension(570, 470));
        resultCard.setLayout(new BoxLayout(resultCard, BoxLayout.Y_AXIS));
        JLabel success = new JLabel("✓");
        success.setFont(f(Font.BOLD, 42));
        success.setForeground(GREEN);
        success.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel title = heading("Well done!");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setFont(f(Font.BOLD, 28));
        JLabel subtitle = subtitle("You have submitted your test successfully.");
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel testName = new JLabel(result.testName + "  •  " + result.subject);
        testName.setFont(f(Font.BOLD, 15));
        testName.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel score = new JLabel(result.score + " / " + result.total);
        score.setFont(f(Font.BOLD, 48));
        score.setForeground(BLUE);
        score.setAlignmentX(Component.CENTER_ALIGNMENT);
        double percentage = result.total == 0 ? 0 : result.score * 100.0 / result.total;
        JLabel percent = new JLabel(String.format("Score: %.1f%%", percentage));
        percent.setFont(f(Font.BOLD, 17));
        percent.setForeground(GREEN);
        percent.setAlignmentX(Component.CENTER_ALIGNMENT);
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue((int) Math.round(percentage));
        bar.setStringPainted(true);
        bar.setString(String.format("%.1f%%", percentage));
        bar.setForeground(GREEN);
        bar.setBackground(new Color(220, 252, 231));
        bar.setMaximumSize(new Dimension(420, 18));
        bar.setAlignmentX(Component.CENTER_ALIGNMENT);
        JButton dashboard = button("Back to Dashboard");
        dashboard.setAlignmentX(Component.CENTER_ALIGNMENT);
        dashboard.addActionListener(e -> {
            activeTest = null;
            selectedAnswers = null;
            showStudentDashboard();
        });
        JButton history = secondaryButton("View My Results");
        history.setAlignmentX(Component.CENTER_ALIGNMENT);
        history.addActionListener(e -> {
            activeTest = null;
            selectedAnswers = null;
            showStudentResults();
        });
        resultCard.add(gap(10)); resultCard.add(success);
        resultCard.add(gap(4)); resultCard.add(title);
        resultCard.add(gap(8)); resultCard.add(subtitle);
        resultCard.add(gap(22)); resultCard.add(testName);
        resultCard.add(gap(14)); resultCard.add(score);
        resultCard.add(gap(4)); resultCard.add(percent);
        resultCard.add(gap(14)); resultCard.add(bar);
        resultCard.add(gap(25)); resultCard.add(dashboard);
        resultCard.add(gap(10)); resultCard.add(history);
        center.add(resultCard);
        root.add(center, BorderLayout.CENTER);
        setPage(root);
    }

    private void showStudentResults() {
        JPanel root = page();
        addHeader(root, "My Results", "Review the tests you have completed.");
        List<ExamResult> myResults = Database.getResultsForStudent(currentUserId);   // newest first
        JPanel list = transparentPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        for (ExamResult r : myResults) {
            JPanel p = card();
            p.setLayout(new BorderLayout(12, 8));
            p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 115));
            JPanel left = transparentPanel();
            left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
            JLabel name = new JLabel(r.testName + "  •  " + r.subject);
            name.setFont(f(Font.BOLD, 17));
            JLabel date = subtitle("Submitted: " + r.date);
            left.add(name); left.add(gap(8)); left.add(date);
            double percent = r.total == 0 ? 0 : r.score * 100.0 / r.total;
            JLabel score = new JLabel(r.score + " / " + r.total + String.format("  (%.1f%%)", percent));
            score.setFont(f(Font.BOLD, 19));
            score.setForeground(percent >= 50 ? GREEN : ORANGE);
            p.add(left, BorderLayout.CENTER);
            p.add(score, BorderLayout.EAST);
            list.add(p);
            list.add(gap(12));
        }
        if (myResults.isEmpty()) {
            list.add(emptyState("No results yet", "Complete a test and your score will appear here."));
        }
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        JButton back = secondaryButton("←  Back to Dashboard");
        back.addActionListener(e -> showStudentDashboard());
        JPanel bottom = transparentPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(back);
        root.add(scroll, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);
        setPage(root);
    }

    // ===================== MAIN =====================
    public static void main(String[] args) {
        // Koi bhi database error aaye to popup me dikhega (app crash nahi hoga)
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, ex.getMessage(), "ExamFlow - Error", JOptionPane.ERROR_MESSAGE);
        });
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) { }
            try {
                Database.testConnection();
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage(), "ExamFlow - Database Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
            new Main().setVisible(true);
        });
    }
}
