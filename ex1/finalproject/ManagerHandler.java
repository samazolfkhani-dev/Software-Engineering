package ap.exercises.finalproject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ManagerHandler implements Account_Handler<Manager> {

    public void registerManager(String username, String password) throws InvalidEntrance {
        if (isUsernameTaken(username)) {
            System.out.println("This username already exists. Please choose a different username.");
            return;
        }
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "INSERT INTO managers (username, password) VALUES (?, ?)")) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    public Manager authenticateUser(String username, String password) {
        return findManager("lower(username) = lower(?) AND password = ?", username, password);
    }

    public Manager getAUser(String username) {
        return findManager("username = ?", username);
    }

    private boolean isUsernameTaken(String username) {
        return DatabaseManager.count("SELECT COUNT(*) FROM managers WHERE username = ?", username) > 0;
    }

    private Manager findManager(String condition, String... params) {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "SELECT * FROM managers WHERE " + condition)) {
            for (int i = 0; i < params.length; i++)
                ps.setString(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return new Manager(rs.getString("username"), rs.getString("password"));
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        } catch (InvalidEntrance e) {
            System.out.println(e.getMessage());
        }
        return null;
    }
}
