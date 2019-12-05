/* -------------------------------------------------------------------------- */

package chirper.server;

import io.atomix.utils.net.Address;

import java.util.ArrayList;
import java.util.List;

/* -------------------------------------------------------------------------- */

public class Client {
    private List<String> topicos;
    private Address address;

    public Client(Address address) {
        this.topicos = new ArrayList<>();
        this.address = address;
    }

    public void setTopicos(List<String> topics)
    {
        this.topicos.clear();
        this.topicos.addAll(topics);
    }

    public List<String> getTopicos()
    {
        return topicos;
    }
}

/* -------------------------------------------------------------------------- */
