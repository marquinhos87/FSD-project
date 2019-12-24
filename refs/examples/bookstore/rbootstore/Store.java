package rbootstore;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Store extends Remote {
    Book search(String title) throws RemoteException;
    boolean buy(Book book) throws RemoteException;
}
