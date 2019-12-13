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

public class ServerConfig
{
    private final ServerId localServerId;
    private final int localServerPort;

    private final Map< Address, ServerId > remoteServerIds;

    public ServerConfig(
        ServerId localServerId,
        int localServerPort,
        Map< Address, ServerId > remoteServerIds
    )
    {
        this.localServerId = localServerId;
        this.localServerPort = localServerPort;

        this.remoteServerIds = new HashMap<>(remoteServerIds);
    }

    public ServerId getLocalServerId()
    {
        return this.localServerId;
    }

    public int getLocalServerPort()
    {
        return this.localServerPort;
    }

    public Map< Address, ServerId > getRemoteServerIds()
    {
        return Collections.unmodifiableMap(this.remoteServerIds);
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
            root.remoteServers.stream().collect(Collectors.toMap(
                p -> new Address(p.host, p.port),
                p -> new ServerId(p.id)
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
        public int id;
        public String host;
        public int port;
    }
}

/* -------------------------------------------------------------------------- */
