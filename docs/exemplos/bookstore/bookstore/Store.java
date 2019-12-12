package bookstore;

public interface Store {
    Book search(String title);
    boolean buy(Book book);
}
