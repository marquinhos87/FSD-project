/* -------------------------------------------------------------------------- */

package chirper.server;

import io.atomix.utils.net.Address;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/* -------------------------------------------------------------------------- */

public class Config
{
    private final PeerId localPeerId;
    private final int localPeerPort;

    private final Map< Address, PeerId > remotePeerIds;

    public Config(
        PeerId localPeerId,
        int localPeerPort,
        Map< Address, PeerId > remotePeerIds
    )
    {
        this.localPeerId = localPeerId;
        this.localPeerPort = localPeerPort;

        this.remotePeerIds = new HashMap<>(remotePeerIds);
    }

    public PeerId getLocalPeerId()
    {
        return this.localPeerId;
    }

    public int getLocalPeerPort()
    {
        return this.localPeerPort;
    }

    public Map< Address, PeerId > getRemotePeerIds()
    {
        return Collections.unmodifiableMap(this.remotePeerIds);
    }

    public static Config parseYamlFile(Path filePath) throws IOException
    {
        return parseYaml(Files.readString(filePath));
    }

    public static Config parseYaml(String yaml)
    {
        // parse yaml

        final var parser = new Yaml(new Constructor(YamlRoot.class));
        final var root = parser.< YamlRoot >load(yaml);

        // convert to config

        return new Config(
            new PeerId(root.localPeer.id),
            root.localPeer.port,
            root.remotePeers.stream().collect(Collectors.toMap(
                p -> new Address(p.host, p.port),
                p -> new PeerId(p.id)
            ))
        );
    }

    private static class YamlRoot
    {
        public YamlLocalPeer localPeer;
        public List< YamlRemotePeer > remotePeers;
    }

    private static class YamlLocalPeer
    {
        public int id;
        public int port;
    }

    private static class YamlRemotePeer
    {
        public int id;
        public String host;
        public int port;
    }
}

/* -------------------------------------------------------------------------- */
