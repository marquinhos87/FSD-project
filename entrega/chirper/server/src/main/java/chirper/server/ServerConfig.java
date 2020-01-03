/* -------------------------------------------------------------------------- */

package chirper.server;

import chirper.server.network.ServerId;
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
import java.util.Objects;
import java.util.stream.Collectors;

/* -------------------------------------------------------------------------- */

public class ServerConfig
{
    private final ServerId localServerId;
    private final int localServerPort;

    private final Map< ServerId, Address > remoteServerAddressesById;

    public ServerConfig(
        ServerId localServerId,
        int localServerPort,
        Map< ServerId, Address > remoteServerAddressesById
    )
    {
        this.localServerId = Objects.requireNonNull(localServerId);
        this.localServerPort = localServerPort;

        this.remoteServerAddressesById = new HashMap<>(remoteServerAddressesById);
    }

    public ServerId getLocalServerId()
    {
        return this.localServerId;
    }

    public int getLocalServerPort()
    {
        return this.localServerPort;
    }

    public Map< ServerId, Address > getRemoteServerAddressesById()
    {
        return Collections.unmodifiableMap(remoteServerAddressesById);
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
                .collect(Collectors.toMap(
                    s -> new ServerId(s.id),
                    s -> Address.from(s.host, s.port)
                ))
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
