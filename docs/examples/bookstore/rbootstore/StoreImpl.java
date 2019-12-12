package rbootstore;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

public class StoreImpl extends UnicastRemoteObject implements Store {
    private Map<String,Book> books = new HashMap<String, Book>();

    public StoreImpl() throws RemoteException {
        books.put("um livro", new BookImpl(1, "um livro"));
        books.put("outro livro", new BookImpl(2, "outro livro"));
    }

    public Book search(String title) {
        return books.get(title);
    }

    public boolean buy(Book book) throws RemoteException {
        System.out.println("Buying "+book.getTitle());
        return true;
    }
}
