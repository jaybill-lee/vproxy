package net.cassite.vproxy.component.app;

import net.cassite.vproxy.component.elgroup.EventLoopGroup;
import net.cassite.vproxy.component.exception.AlreadyExistException;
import net.cassite.vproxy.component.exception.ClosedException;
import net.cassite.vproxy.component.proxy.ConnectorGen;
import net.cassite.vproxy.component.secure.SecurityGroup;
import net.cassite.vproxy.component.svrgroup.ServerGroup;
import net.cassite.vproxy.component.svrgroup.ServerGroups;
import net.cassite.vproxy.connection.Connection;
import net.cassite.vproxy.connection.Connector;
import net.cassite.vproxy.connection.Protocol;
import net.cassite.vproxy.protocol.ProtocolHandler;
import net.cassite.vproxy.socks.AddressType;
import net.cassite.vproxy.socks.Socks5ConnectorProvider;
import net.cassite.vproxy.socks.Socks5ProxyContext;
import net.cassite.vproxy.socks.Socks5ProxyProtocolHandler;
import net.cassite.vproxy.util.Callback;
import net.cassite.vproxy.util.Tuple;
import net.cassite.vproxy.util.Utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.function.Supplier;

public class Socks5Server extends TcpLB {
    // singleton in one Socks5Server object
    class Socks5ConnectorGen implements ConnectorGen<Socks5ProxyContext> {

        @Override
        public Type type() {
            return Type.handler;
        }

        @Override
        public Connector genConnector(Connection accepted) {
            // will not be called
            return null;
        }

        @Override
        public ProtocolHandler<Tuple<Socks5ProxyContext, Callback<Connector, IOException>>> handler() {
            return new Socks5ProxyProtocolHandler(connectorProvider);
        }
    }

    // singleton in one Socks5Server object
    class Socks5ServerConnectorProvider implements Socks5ConnectorProvider {
        @Override
        public Connector provide(Connection accepted, AddressType type, String address, int port) {
            // check whitelist
            InetAddress remoteAddress = accepted.remote.getAddress();
            if (!securityGroup.allow(Protocol.TCP, remoteAddress, bindAddress.getPort()))
                return null; // terminated by securityGroup

            // then let's try to find a connector
            ServerGroups serverGroups = Socks5Server.super.backends;
            if (type == AddressType.domain) {
                String addrport = address + ":" + port;
                // search for a group with name same as the address:port
                for (ServerGroups.ServerGroupHandle gh : serverGroups.getServerGroups()) {
                    if (gh.alias.equals(addrport)) { // matches
                        return gh.group.next();
                    }
                }
            } else {
                // search for the backend in all groups
                for (ServerGroups.ServerGroupHandle gh : serverGroups.getServerGroups()) {
                    for (ServerGroup.ServerHandle sh : gh.group.getServerHandles()) {
                        // match address and port
                        if (Utils.ipStr(sh.server.getAddress().getAddress()).equals(address) && sh.server.getPort() == port) {
                            return sh.makeConnector();
                        }
                    }
                }
            }
            // return null if not found
            // the lib will handle it
            return null;
        }
    }

    private final Socks5ConnectorGen connectorGen = new Socks5ConnectorGen();
    private final Socks5ServerConnectorProvider connectorProvider = new Socks5ServerConnectorProvider();

    public Socks5Server(String alias, EventLoopGroup acceptorGroup, EventLoopGroup workerGroup, InetSocketAddress bindAddress, ServerGroups backends, int inBufferSize, int outBufferSize, SecurityGroup securityGroup) throws IOException, AlreadyExistException, ClosedException {
        super(alias, acceptorGroup, workerGroup, bindAddress, backends, inBufferSize, outBufferSize, securityGroup, 0);
    }

    @Override
    protected Supplier<ConnectorGen> provideConnectorGen() {
        // create a socks5 connector gen
        return () -> connectorGen;
    }
}
