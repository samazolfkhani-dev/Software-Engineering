package ap.exercises.finalproject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class StudentManager implements Account_Handler<Student>, Ban_Control {

    public void registerStudent(String name, String username, String password) throws InvalidEntrance {
        if (isUsernameTaken(username)) {
            System.out.println("This username already exists. Please choose a different username.");
            return;
        }
        String sql = "INSERT INTO students (username, password, name, student_id, permission) VALUES (?, ?, ?, ?, 'ACTIVE')";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, username.trim());
            ps.setString(2, password.trim());
            ps.setString(3, name.trim());
            ps.setString(4, String.valueOf(nextStudentId()));
            ps.executeUpdate();
            System.out.println("Student registration completed successfully.");
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    public Student authenticateUser(String username, String password) {
        return findStudent("username = ? AND password = ?", username, password);
    }

    public Student getAUser(String username) {
        return findStudent("username = ?", username);
    }

    public Map<String, Student> returnStudent() {
        Map<String, Student> students = new LinkedHashMap<>();
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "SELECT * FROM students ORDER BY CAST(student_id AS INTEGER)")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Student s = studentFrom(rs);
                    students.put(s.getUsername(), s);
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return students;
    }

    public void banStudent(String username) {
        updatePermission(username, "INACTIVE");
        System.out.println("Student Banned Successfully!");
    }

    public void unbanStudent(String username) {
        updatePermission(username, "ACTIVE");
        System.out.println("Student Unbanned Successfully!");
    }

    public void editName(String name, Student s) throws InvalidEntrance {
        s.setName(name); // validates the new name
        updateField("name", name, s.getUsername());
    }

    public void editPassword(String password, Student s) throws InvalidEntrance {
        s.setPassword(password); // validates the new password
        updateField("password", password, s.getUsername());
    }

    public void editStudentId(String id, Student s) throws InvalidEntrance {
        s.setStudentId(id); // validates the new id
        updateField("student_id", id, s.getUsername());
    }

    private boolean isUsernameTaken(String username) {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "SELECT 1 FROM students WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return false;
    }

    private int nextStudentId() {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "SELECT MAX(CAST(student_id AS INTEGER)) FROM students")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt(1) + 1;
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return 1;
    }

    private void updatePermission(String username, String permission) {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "UPDATE students SET permission = ? WHERE username = ?")) {
            ps.setString(1, permission);
            ps.setString(2, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    private void updateField(String column, String value, String username) {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "UPDATE students SET " + column + " = ? WHERE username = ?")) {
            ps.setString(1, value);
            ps.setString(2, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    private Student findStudent(String condition, String... params) {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "SELECT * FROM students WHERE " + condition)) {
            for (int i = 0; i < params.length; i++)
                ps.setString(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return studentFrom(rs);
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return null;
    }

    private Student studentFrom(ResultSet rs) throws SQLException {
        try {
            Student s = new Student(rs.getString("name"), rs.getString("student_id"),
                    rs.getString("username"), rs.getString("password"));
            s.setPermission(rs.getString("permission"));
            return s;
        } catch (InvalidEntrance e) {
            throw new BookException("Corrupted student row: " + e.getMessage());
        }
    }
}
