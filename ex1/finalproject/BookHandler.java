package ap.exercises.finalproject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BookHandler {

    public void addBook(String name, String author, int publishedYear, String ISBN) throws InvalidEntrance {
        Book b = new Book(name.trim(), author.trim(), publishedYear, ISBN.trim()); // validates the input
        if (findByIsbn(b.getISBN()) != null)
            throw new InvalidEntrance("Book Has Already Added!<500>");
        String sql = "INSERT INTO books (isbn, name, author, published_year, state) VALUES (?, ?, ?, ?, 'AVAILABLE')";
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, b.getISBN());
            ps.setString(2, b.getName());
            ps.setString(3, b.getAuthor());
            ps.setInt(4, b.getPublishedYear());
            ps.executeUpdate();
            System.out.println("Book Added Successfully!");
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    public void displayAvailableBooks() {
        List<Book> available = findBooks("state = 'AVAILABLE' ORDER BY name");
        if (available.isEmpty())
            throw new BookException("No Book In List!");
        for (Book b : available)
            System.out.println(b);
    }

    public Book isBookAvailable(String name, String author, int publishedYear) {
        Book b = searchBook(name, author, publishedYear);
        if (b != null && b.getState().equalsIgnoreCase("AVAILABLE"))
            return b;
        return null;
    }

    public Book searchBook(String name, String author, int publishedYear) {
        List<Book> result = findBooks("lower(name) = lower(?) AND lower(author) = lower(?) AND published_year = ?",
                name, author, String.valueOf(publishedYear));
        return result.isEmpty() ? null : result.get(0);
    }

    public void searchBookByGuest(String name) {
        List<Book> result = findBooks("lower(name) = lower(?) ORDER BY name", name);
        if (result.isEmpty())
            System.out.println("Book Not Found!");
        else
            System.out.println(result.get(0));
    }

    public void editBookName(Book book, String name) throws InvalidEntrance {
        if (name == null || name.isEmpty())
            throw new InvalidEntrance("Invalid Name!<101>");
        updateBook(book, "name", name);
    }

    public void editBookAuthor(Book book, String author) throws InvalidEntrance {
        if (author == null || author.isEmpty())
            throw new InvalidEntrance("Invalid Author Name!<102>");
        updateBook(book, "author", author);
    }

    public void editBookYear(Book book, int year) throws InvalidEntrance {
        if (year <= 0)
            throw new InvalidEntrance("Invalid Input!<103>");
        if (book == null)
            return;
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "UPDATE books SET published_year = ? WHERE isbn = ?")) {
            ps.setInt(1, year);
            ps.setString(2, book.getISBN());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    public void editBookState(Book book, String state) {
        if (book == null || BookState.search(state) == null)
            return;
        updateBook(book, "state", BookState.search(state).name());
    }

    public int getBookCount() {
        return DatabaseManager.count("SELECT COUNT(*) FROM books");
    }

    private void updateBook(Book book, String column, String value) {
        if (book == null)
            return;
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "UPDATE books SET " + column + " = ? WHERE isbn = ?")) {
            ps.setString(1, value);
            ps.setString(2, book.getISBN());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    private Book findByIsbn(String isbn) {
        List<Book> result = findBooks("isbn = ?", isbn);
        return result.isEmpty() ? null : result.get(0);
    }

    private List<Book> findBooks(String condition, String... params) {
        List<Book> result = new ArrayList<>();
        try (PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(
                "SELECT * FROM books WHERE " + condition)) {
            for (int i = 0; i < params.length; i++)
                ps.setString(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    result.add(bookFrom(rs));
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return result;
    }

    private Book bookFrom(ResultSet rs) throws SQLException {
        try {
            Book b = new Book(rs.getString("name"), rs.getString("author"),
                    rs.getInt("published_year"), rs.getString("isbn"));
            b.setState(rs.getString("state"));
            return b;
        } catch (InvalidEntrance e) {
            throw new BookException("Corrupted book row: " + e.getMessage());
        }
    }
}
