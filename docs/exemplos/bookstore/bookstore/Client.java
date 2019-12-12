package bookstore;

public class Client {
    public static void main(String[] args) throws Exception {
        Store s = new StoreImpl();

        Book b = s.search("um livro");

        System.out.println("é o livro "+b.getISBN());

        System.out.println("objeto livro: "+b.getClass().getName());

        s.buy(b);
    }
}
