package ap.exercises.finalproject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LoanManager
{
    private final BookHandler bookHandler;

    // Loads a loan together with its book, its student and the two librarians
    // connected to it, so a full Loan object can be rebuilt from a single row.
    private static final String LOAN_JOIN =
            "SELECT l.id AS loan_id, l.borrow_request_date, l.issue_date, l.due_date, l.return_date, " +
            "b.isbn AS book_isbn, b.name AS book_name, b.author AS book_author, " +
            "b.published_year AS book_year, b.state AS book_state, " +
            "s.name AS student_name, s.student_id, s.username AS student_username, " +
            "s.password AS student_password, s.permission AS student_permission, " +
            "li.username AS issuer_name, li.password AS issuer_password, li.num_book AS issuer_num_book, " +
            "lr.username AS receiver_name, lr.password AS receiver_password, lr.num_book AS receiver_num_book " +
            "FROM loans l " +
            "JOIN books b ON b.isbn = l.book_isbn " +
            "JOIN students s ON s.username = l.student_username " +
            "LEFT JOIN librarians li ON li.username = l.issuer_username " +
            "LEFT JOIN librarians lr ON lr.username = l.receiver_username ";

    public LoanManager() {
        this.bookHandler = new BookHandler();
    }

    public Map<Integer, Loan> getBorrowRequest() {
        return loansByStatus("REQUEST");
    }

    public void borrowRequest(Book book, Student student) throws InvalidEntrance {
        if (!student.getPermission().equalsIgnoreCase("Active")) {
            System.out.println("Sorry! You Are Banned.");
            return;
        }
        if (hasPendingRequest(book.getISBN(), student.getUsername())) {
            System.out.println("Request Already Added!");
            return;
        }
        String sql = "INSERT INTO loans (status, book_isbn, student_username, borrow_request_date) VALUES ('REQUEST', ?, ?, ?)";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, book.getISBN());
            ps.setString(2, student.getUsername());
            ps.setString(3, LocalDate.now().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return;
        }
        bookHandler.editBookState(book, "RESERVED");
        System.out.println("Request Was Added Successfully!");
    }

    public void addToLoanList(int id, Librarian librarian) {
        Loan loan = getLoanById(id, "REQUEST");
        if (loan == null)
            return;
        LocalDate today = LocalDate.now();
        String sql = "UPDATE loans SET status = 'ACTIVE', issue_date = ?, due_date = ?, issuer_username = ? WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, today.toString());
            ps.setString(2, today.plusDays(30).toString());
            ps.setString(3, librarian.getUsername());
            ps.setInt(4, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return;
        }
        bookHandler.editBookState(loan.getBook(), "BORROWED");
        System.out.println("Loan Was Added Successfully!");
    }

    public void returnRequest(Student s, Book b) {
        if (b == null)
            return;
        List<Loan> matches = queryLoans(
                "l.status = 'ACTIVE' AND l.student_username = ? AND lower(b.name) = lower(?) AND lower(b.author) = lower(?)",
                s.getUsername(), b.getName(), b.getAuthor());
        if (matches.isEmpty())
            return;
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "UPDATE loans SET status = 'RETURN_REQUEST' WHERE id = ?")) {
            ps.setInt(1, matches.get(0).getId());
            ps.executeUpdate();
            System.out.println("Request Added Successfully!");
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    public boolean getReturnRequest()
    {
        Map<Integer, Loan> requests = loansByStatus("RETURN_REQUEST");
        if (!requests.isEmpty()) {
            System.out.println(requests.values().iterator().next());
            return true;
        }
        return false;
    }

    public void addToHistory(int id, Librarian librarian) {
        Loan loan = getLoanById(id, "RETURN_REQUEST");
        if (loan == null)
            return;
        String sql = "UPDATE loans SET status = 'RETURNED', return_date = ?, receiver_username = ? WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, LocalDate.now().toString());
            ps.setString(2, librarian.getUsername());
            ps.setInt(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return;
        }
        bookHandler.editBookState(loan.getBook(), "AVAILABLE");
        System.out.println("Book Returned Successfully!");
    }

    public int totalLoanPerStudent(String username) {
        return DatabaseManager.count(
                "SELECT COUNT(*) FROM loans WHERE student_username = ? AND status IN ('ACTIVE', 'RETURNED')", username);
    }

    public void historyPerStudent(String username) {
        for (Loan l : queryLoans("l.student_username = ? AND l.status IN ('ACTIVE', 'RETURNED')", username))
            System.out.println(l);
    }

    public int allNotReturnedBookPerStudent(String username) {
        return DatabaseManager.count(
                "SELECT COUNT(*) FROM loans WHERE student_username = ? AND status = 'ACTIVE'", username);
    }

    public int totalOverdueLoansPerStudent(String username) {
        return DatabaseManager.count(
                "SELECT COUNT(*) FROM loans WHERE student_username = ? AND status = 'RETURNED' AND return_date > due_date",
                username);
    }

    public void removeBanStudent(String username) {
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "DELETE FROM loans WHERE status = 'REQUEST' AND student_username = ?")) {
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    public void librarianHistory(String username) {
        int issued = DatabaseManager.count(
                "SELECT COUNT(*) FROM loans WHERE status = 'RETURNED' AND issuer_username = ?", username);
        int received = DatabaseManager.count(
                "SELECT COUNT(*) FROM loans WHERE status = 'RETURNED' AND receiver_username = ?", username);
        System.out.println("Number Of Issues: " + issued);
        System.out.println("Number of ReceiveProcess: " + received);
    }

    public void bookHistory(String name, String author) {
        int requests = DatabaseManager.count(
                "SELECT COUNT(*) FROM loans l JOIN books b ON b.isbn = l.book_isbn " +
                "WHERE l.status = 'REQUEST' AND lower(b.name) = lower(?) AND lower(b.author) = lower(?)", name, author);
        int borrowed = 0;
        long totalDays = 0;
        String sql = "SELECT COUNT(*) AS borrowed, COALESCE(SUM(julianday(return_date) - julianday(issue_date)), 0) AS days " +
                "FROM loans l JOIN books b ON b.isbn = l.book_isbn " +
                "WHERE l.status = 'RETURNED' AND lower(b.name) = lower(?) AND lower(b.author) = lower(?)";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, author);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    borrowed = rs.getInt("borrowed");
                    totalDays = (long) rs.getDouble("days");
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        System.out.println("Number Of Request for This Book: " + requests);
        System.out.println("Number Of Borrowed Time: " + borrowed);
        System.out.println("Average Days Of Borrowing Book: " + (borrowed == 0 ? 0 : totalDays / borrowed));
    }

    public List<Loan> getTop10LateReturns() {
        return queryLoans("l.status = 'RETURNED' AND l.return_date > l.due_date " +
                "ORDER BY julianday(l.return_date) - julianday(l.due_date) DESC LIMIT 10");
    }

    private Map<Integer, Loan> loansByStatus(String status) {
        Map<Integer, Loan> result = new LinkedHashMap<>();
        for (Loan loan : queryLoans("l.status = ?", status))
            result.put(loan.getId(), loan);
        return result;
    }

    private Loan getLoanById(int id, String status) {
        List<Loan> result = queryLoans("l.id = ? AND l.status = ?", String.valueOf(id), status);
        return result.isEmpty() ? null : result.get(0);
    }

    private boolean hasPendingRequest(String isbn, String username) {
        return DatabaseManager.count(
                "SELECT COUNT(*) FROM loans WHERE status = 'REQUEST' AND book_isbn = ? AND student_username = ?",
                isbn, username) > 0;
    }

    private List<Loan> queryLoans(String condition, String... params) {
        List<Loan> result = new ArrayList<>();
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(LOAN_JOIN + "WHERE " + condition)) {
            for (int i = 0; i < params.length; i++)
                ps.setString(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    result.add(loanFrom(rs));
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return result;
    }

    private Loan loanFrom(ResultSet rs) throws SQLException {
        try {
            Book book = new Book(rs.getString("book_name"), rs.getString("book_author"),
                    rs.getInt("book_year"), rs.getString("book_isbn"));
            book.setState(rs.getString("book_state"));
            Student student = new Student(rs.getString("student_name"), rs.getString("student_id"),
                    rs.getString("student_username"), rs.getString("student_password"));
            student.setPermission(rs.getString("student_permission"));
            return new Loan(book, student, rs.getInt("loan_id"),
                    dateValue(rs, "borrow_request_date"), dateValue(rs, "issue_date"),
                    dateValue(rs, "due_date"), dateValue(rs, "return_date"),
                    librarianFrom(rs, "issuer"), librarianFrom(rs, "receiver"));
        } catch (InvalidEntrance e) {
            throw new BookException("Corrupted loan row: " + e.getMessage());
        }
    }

    private Librarian librarianFrom(ResultSet rs, String prefix) throws SQLException {
        String username = rs.getString(prefix + "_name");
        if (username == null)
            return null;
        try {
            Librarian librarian = new Librarian(username, rs.getString(prefix + "_password"));
            librarian.setNumBook(rs.getInt(prefix + "_num_book"));
            return librarian;
        } catch (InvalidEntrance e) {
            throw new BookException("Corrupted librarian row: " + e.getMessage());
        }
    }

    private LocalDate dateValue(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        return value == null ? null : LocalDate.parse(value);
    }
}
