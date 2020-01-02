package chirper.server;

import io.atomix.utils.net.Address;

public class ServerIdAddress
{
    public final int id;
    public final Address ad;

    public ServerIdAddress(int id, Address ad) {
        this.id = id;
        this.ad = ad;
    }
}
