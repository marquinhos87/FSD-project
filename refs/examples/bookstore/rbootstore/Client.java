package rbootstore;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {
    public static void main(String[] args) throws Exception {
        Registry r = LocateRegistry.getRegistry("localhost", 12345);
        Store s = (Store) r.lookup("bookstore");

        //-----------
        Book b = s.search("um livro");

        System.out.println("é o livro "+b.getISBN());

        System.out.println("objeto livro: "+b.getClass().getName());

        s.buy(b);
    }
}
