package chirper.shared;

import io.atomix.utils.net.Address;
import org.apache.commons.math3.analysis.function.Add;

public class Peer
{
    private final int id;
    private final Address address;

    public Peer(int id, Address address)
    {
        this.id = id;
        this.address = address;
    }

    public int getId()
    {
        return this.id;
    }

    public Address getAddress()
    {
        return this.address;
    }
}
