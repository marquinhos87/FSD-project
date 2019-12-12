package rbootstore;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class BookImpl extends UnicastRemoteObject implements Book {
    private int isbn;
    private String title;

    public BookImpl(int isbn, String title) throws RemoteException  {
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
