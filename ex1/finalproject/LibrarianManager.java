package ap.exercises.finalproject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class LibrarianManager implements Account_Handler<Librarian> {

    public Librarian authenticateUser(String username, String password) {
        return findLibrarian("username = ? AND password = ?", username, password);
    }

    public void editLibrarianInformation(Librarian librarian, String password) throws InvalidEntrance {
        String oldPassword = librarian.getPassword();
        librarian.setPassword(password); // validates the new password
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "UPDATE librarians SET password = ? WHERE username = ? AND password = ?")) {
            ps.setString(1, password);
            ps.setString(2, librarian.getUsername());
            ps.setString(3, oldPassword);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    public void increaseLibrarianBook(String username) {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "UPDATE librarians SET num_book = num_book + 1 WHERE username = ?")) {
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    public Librarian getAUser(String username) {
        return findLibrarian("username = ?", username);
    }

    public Map<String, Librarian> returnLibrarian() {
        Map<String, Librarian> librarians = new LinkedHashMap<>();
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "SELECT * FROM librarians ORDER BY username")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Librarian l = librarianFrom(rs);
                    librarians.put(l.getUsername(), l);
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return librarians;
    }

    public void librarianHistory(String username) {
        Librarian l = findLibrarian("username = ?", username);
        if (l != null)
            System.out.println("Number of Book Registered: " + l.getNumBook());
    }

    public boolean isUsernameTaken(String username) {
        return DatabaseManager.count("SELECT COUNT(*) FROM librarians WHERE lower(username) = lower(?)", username) > 0;
    }

    public void addLibrarian(String username, String id) throws InvalidEntrance {
        Librarian lib = new Librarian(username.trim(), id); // validates username/password
        if (isUsernameTaken(lib.getUsername())) {
            System.out.println("This username already exists. Please choose a different username.");
            return;
        }
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "INSERT INTO librarians (username, password, num_book) VALUES (?, ?, 0)")) {
            ps.setString(1, lib.getUsername());
            ps.setString(2, lib.getPassword());
            ps.executeUpdate();
            System.out.println("Librarian Added Successfully!");
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    private Librarian findLibrarian(String condition, String... params) {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "SELECT * FROM librarians WHERE " + condition)) {
            for (int i = 0; i < params.length; i++)
                ps.setString(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return librarianFrom(rs);
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return null;
    }

    private Librarian librarianFrom(ResultSet rs) throws SQLException {
        try {
            Librarian l = new Librarian(rs.getString("username"), rs.getString("password"));
            l.setNumBook(rs.getInt("num_book"));
            return l;
        } catch (InvalidEntrance e) {
            throw new BookException("Corrupted librarian row: " + e.getMessage());
        }
    }
}
