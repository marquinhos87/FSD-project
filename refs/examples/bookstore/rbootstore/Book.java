package rbootstore;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Book extends Remote {
    int getISBN() throws RemoteException;
    String getTitle() throws RemoteException;
}
