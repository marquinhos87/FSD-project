package bookstore;

public class BookImpl implements Book {
    private int isbn;
    private String title;

    public BookImpl(int isbn, String title) {
        this.isbn = isbn;
        this.title = title;
    }

    public int getISBN() {
        return isbn;
    }

    public String getTitle() {
        return title;
    }
}
