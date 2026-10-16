package ap.exercises.finalproject;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

// Runs once, only when the database file is created for the first time:
// it reads the old JSON files (if they are still next to the program)
// and copies their rows into the new SQLite tables.
class SeedFromJson {

    static void importAll() {
        importStudents();
        importLibrarians();
        importManagers();
        importBooks();
        importLoans("BorrowRequests.json", "REQUEST");
        importLoans("Loans.json", "ACTIVE");
        importLoans("ReturnRequests.json", "RETURN_REQUEST");
        importLoans("History.json", "RETURNED");
    }

    private static void importStudents() {
        if (!jsonFileHasContent("Students.json"))
            return;
        List<Student> students = new FileHandling<Student>("Students.json").readFromFile(Student.class);
        if (students == null)
            return;
        String sql = "INSERT OR IGNORE INTO students (username, password, name, student_id, permission) VALUES (?, ?, ?, ?, ?)";
        int count = 0;
        for (Student s : students) {
            try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
                ps.setString(1, s.getUsername());
                ps.setString(2, s.getPassword());
                ps.setString(3, s.getName());
                ps.setString(4, s.getStudentId());
                ps.setString(5, s.getPermission() == null ? "ACTIVE" : s.getPermission());
                count += ps.executeUpdate();
            } catch (Exception e) {
                System.out.println("Could not import student " + s.getUsername() + ": " + e.getMessage());
            }
        }
        if (count > 0)
            System.out.println("Imported " + count + " student(s) from Students.json");
    }

    private static void importLibrarians() {
        if (!jsonFileHasContent("Librarians.json"))
            return;
        List<Librarian> librarians = new FileHandling<Librarian>("Librarians.json").readFromFile(Librarian.class);
        if (librarians == null)
            return;
        String sql = "INSERT OR IGNORE INTO librarians (username, password, num_book) VALUES (?, ?, ?)";
        int count = 0;
        for (Librarian l : librarians) {
            try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
                ps.setString(1, l.getUsername());
                ps.setString(2, l.getPassword());
                ps.setInt(3, l.getNumBook());
                count += ps.executeUpdate();
            } catch (Exception e) {
                System.out.println("Could not import librarian " + l.getUsername() + ": " + e.getMessage());
            }
        }
        if (count > 0)
            System.out.println("Imported " + count + " librarian(s) from Librarians.json");
    }

    private static void importManagers() {
        if (!jsonFileHasContent("Manager.json"))
            return;
        List<Manager> managers = new FileHandling<Manager>("Manager.json").readFromFile(Manager.class);
        if (managers == null)
            return;
        String sql = "INSERT OR IGNORE INTO managers (username, password) VALUES (?, ?)";
        int count = 0;
        for (Manager m : managers) {
            try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
                ps.setString(1, m.getUsername());
                ps.setString(2, m.getPassword());
                count += ps.executeUpdate();
            } catch (Exception e) {
                System.out.println("Could not import manager " + m.getUsername() + ": " + e.getMessage());
            }
        }
        if (count > 0)
            System.out.println("Imported " + count + " manager(s) from Manager.json");
    }

    private static void importBooks() {
        if (!jsonFileHasContent("Books.json"))
            return;
        List<Book> books = new FileHandling<Book>("Books.json").readFromFile(Book.class);
        if (books == null)
            return;
        String sql = "INSERT OR IGNORE INTO books (isbn, name, author, published_year, state) VALUES (?, ?, ?, ?, ?)";
        int count = 0;
        for (Book b : books) {
            try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
                ps.setString(1, b.getISBN());
                ps.setString(2, b.getName());
                ps.setString(3, b.getAuthor());
                ps.setInt(4, b.getPublishedYear());
                ps.setString(5, b.getState() == null ? "AVAILABLE" : b.getState());
                count += ps.executeUpdate();
            } catch (Exception e) {
                System.out.println("Could not import book " + b.getName() + ": " + e.getMessage());
            }
        }
        if (count > 0)
            System.out.println("Imported " + count + " book(s) from Books.json");
    }

    private static void importLoans(String file, String status) {
        if (!jsonFileHasContent(file))
            return;
        List<Loan> loans = new FileHandling<Loan>(file).readFromFile(Loan.class);
        if (loans == null)
            return;
        String sql = "INSERT OR IGNORE INTO loans (id, status, book_isbn, student_username, borrow_request_date, " +
                "issue_date, due_date, return_date, issuer_username, receiver_username) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int count = 0;
        for (Loan l : loans) {
            try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
                ps.setInt(1, l.getId());
                ps.setString(2, status);
                ps.setString(3, l.getBook().getISBN());
                ps.setString(4, l.getStudent().getUsername());
                ps.setString(5, l.getBorrowRequestDate().toString());
                ps.setString(6, l.getIssueDate() == null ? null : l.getIssueDate().toString());
                ps.setString(7, l.getDueDate() == null ? null : l.getDueDate().toString());
                ps.setString(8, l.getReturnDate() == null ? null : l.getReturnDate().toString());
                ps.setString(9, l.getIssuer() == null ? null : l.getIssuer().getUsername());
                ps.setString(10, l.getReceiver() == null ? null : l.getReceiver().getUsername());
                count += ps.executeUpdate();
            } catch (Exception e) {
                System.out.println("Could not import a loan from " + file + ": " + e.getMessage());
            }
        }
        if (count > 0)
            System.out.println("Imported " + count + " loan(s) from " + file);
    }

    private static boolean jsonFileHasContent(String file) {
        File f = new File(file);
        return f.exists() && f.length() > 0;
    }
}
