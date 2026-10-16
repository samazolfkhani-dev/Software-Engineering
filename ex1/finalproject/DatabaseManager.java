package ap.exercises.finalproject;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

// Holds the single SQLite connection used by the whole program.
// On the first run it creates library.db, builds the tables and
// imports the old JSON files (see SeedFromJson).
public class DatabaseManager {
    private static final String DB_FILE = "library.db";
    private static Connection connection;

    public static synchronized Connection getConnection() {
        if (connection == null) {
            boolean freshDatabase = !new File(DB_FILE).exists();
            try {
                connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);
                try (Statement st = connection.createStatement()) {
                    st.execute("PRAGMA foreign_keys = ON");
                }
                createTables();
                System.out.println("Database ready: " + DB_FILE);
                if (freshDatabase)
                    SeedFromJson.importAll();
            } catch (SQLException e) {
                throw new RuntimeException("Cannot open the database: " + e.getMessage());
            }
        }
        return connection;
    }

    private static void createTables() throws SQLException {
        String[] schema = {
                "CREATE TABLE IF NOT EXISTS students (" +
                        "username TEXT PRIMARY KEY, " +
                        "password TEXT NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "student_id TEXT NOT NULL, " +
                        "permission TEXT NOT NULL DEFAULT 'ACTIVE')",

                "CREATE TABLE IF NOT EXISTS librarians (" +
                        "username TEXT PRIMARY KEY, " +
                        "password TEXT NOT NULL, " +
                        "num_book INTEGER NOT NULL DEFAULT 0)",

                "CREATE TABLE IF NOT EXISTS managers (" +
                        "username TEXT PRIMARY KEY, " +
                        "password TEXT NOT NULL)",

                "CREATE TABLE IF NOT EXISTS books (" +
                        "isbn TEXT PRIMARY KEY, " +
                        "name TEXT NOT NULL, " +
                        "author TEXT NOT NULL, " +
                        "published_year INTEGER NOT NULL, " +
                        "state TEXT NOT NULL DEFAULT 'AVAILABLE')",

                // One table for the whole life of a loan. The old project used four
                // files; the status column replaces them:
                // REQUEST -> ACTIVE -> RETURN_REQUEST -> RETURNED
                "CREATE TABLE IF NOT EXISTS loans (" +
                        "id INTEGER PRIMARY KEY, " +
                        "status TEXT NOT NULL, " +
                        "book_isbn TEXT NOT NULL REFERENCES books(isbn), " +
                        "student_username TEXT NOT NULL REFERENCES students(username), " +
                        "borrow_request_date TEXT NOT NULL, " + // dates are ISO text: yyyy-MM-dd
                        "issue_date TEXT, " +
                        "due_date TEXT, " +
                        "return_date TEXT, " +
                        "issuer_username TEXT REFERENCES librarians(username), " +
                        "receiver_username TEXT REFERENCES librarians(username))"
        };
        try (Statement st = connection.createStatement()) {
            for (String sql : schema)
                st.execute(sql);
        }
    }

    // Small helper for the many "how many rows match?" queries.
    public static int count(String sql, String... params) {
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++)
                ps.setString(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return 0;
    }
}
