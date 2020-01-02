/* -------------------------------------------------------------------------- */

package chirper.server;

import io.atomix.utils.net.Address;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/* -------------------------------------------------------------------------- */

public class ServerConfig
{
    private final ServerId localServerId;
    private final int localServerPort;

    private final List< Address > remoteServerAddresses;

    private final List< ServerIdAddress> remoteServerIdsAddresses;

    public ServerConfig(
        ServerId localServerId,
        int localServerPort,
        Collection< Address > remoteServerAddresses,
        Collection<ServerIdAddress> remoteServerIdsAddresses
    )
    {
        this.localServerId = Objects.requireNonNull(localServerId);
        this.localServerPort = localServerPort;

        this.remoteServerAddresses = new ArrayList<>(remoteServerAddresses);

        this.remoteServerIdsAddresses = new ArrayList<>(remoteServerIdsAddresses);
    }

    public ServerId getLocalServerId()
    {
        return this.localServerId;
    }

    public int getLocalServerPort()
    {
        return this.localServerPort;
    }

    public List< Address > getRemoteServerAddresses()
    {
        return Collections.unmodifiableList(this.remoteServerAddresses);
    }

    public List< ServerIdAddress > getRemoteServerIdsAddresses()
    {
        return Collections.unmodifiableList(this.remoteServerIdsAddresses);
    }

    public static ServerConfig parseYamlFile(Path filePath) throws IOException
    {
        return parseYaml(Files.readString(filePath));
    }

    public static ServerConfig parseYaml(String yaml)
    {
        // parse yaml

        final var parser = new Yaml(new Constructor(YamlRoot.class));
        final var root = parser.< YamlRoot >load(yaml);

        // convert to config

        return new ServerConfig(
            new ServerId(root.localServer.id),
            root.localServer.port,
            root.remoteServers
                .stream()
                .map(s -> new Address(s.host, s.port))
                .collect(Collectors.toList()),
            root.remoteServers
                .stream()
                .map(s -> new ServerIdAddress(s.id,new Address(s.host, s.port)))
                .collect(Collectors.toList())
        );
    }

    private static class YamlRoot
    {
        public YamlLocalServer localServer;
        public List< YamlRemoteServer > remoteServers;
    }

    private static class YamlLocalServer
    {
        public int id;
        public int port;
    }

    private static class YamlRemoteServer
    {
        public String host;
        public int port;
        public int id;
    }
}

/* -------------------------------------------------------------------------- */
