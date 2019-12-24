package rbootstore;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Server {
    public static void main(String[] args) throws Exception {
        Store s = new StoreImpl();
        //----------

        Registry r = LocateRegistry.createRegistry(12345);
        r.bind("bookstore", s);
    }

}
