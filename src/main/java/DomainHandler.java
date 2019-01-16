import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xbill.DNS.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;

public class DomainHandler implements Handler {
    private SocketChannel client;
    private SocketChannel host;
    private final String hostname;
    private final Selector selector;
    private DatagramChannel dns;
    private InetSocketAddress hostAddress;
    private int port;
    private boolean isDnsAsked = false;
    private Logger logger = LogManager.getLogger(DomainHandler.class);

    public DomainHandler(SocketChannel client, String hostname, int port, Selector selector) throws IOException {
        this.client = client;
        this.hostname = hostname + ".";
        this.selector = selector;
        this.port = port;
        String[] dnsServers = ResolverConfig.getCurrentConfig().servers();
        dns = DatagramChannel.open();
        dns.configureBlocking(false);
        dns.connect(new InetSocketAddress(dnsServers[0], 53));
        dns.register(selector, SelectionKey.OP_WRITE, this);
        client.register(selector, 0, this);
    }

    @Override
    public void onRead(SelectableChannel channel) {
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        try {
            dns.read(buffer);
            logger.debug("read dns response {}", buffer.array());
            Message msg = new Message(buffer.array());
            Record[] recs = msg.getSectionArray(1);
            for (Record rec : recs) {
                if (rec instanceof ARecord) {
                    ARecord arec = (ARecord) rec;
                    InetAddress adr = arec.getAddress();
                    hostAddress = new InetSocketAddress(adr, port);
                    dns.register(selector, 0);
                    host = SocketChannel.open();
                    host.configureBlocking(false);
                    host.connect(this.hostAddress);

                    host.register(selector, SelectionKey.OP_CONNECT, this);
                    return;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            client.keyFor(selector).cancel();

        }

    }

    @Override
    public void onWrite(SelectableChannel channel) {

        if (!isDnsAsked) {
            try {
                Name name = new Name(hostname);
                Record record = Record.newRecord(name, Type.A, DClass.IN);
                Message message = Message.newQuery(record);
                dns.write(ByteBuffer.wrap(message.toWire()));
                dns.register(selector, SelectionKey.OP_READ, this);
            } catch (Exception ignored) {
                ignored.printStackTrace();

            }
            isDnsAsked = true;
            return;
        }
        ByteBuffer clientReply = ByteBuffer.allocate(1024);
        clientReply.put(ProtocolType.SOCKS5);
        clientReply.put((byte) 0);
        clientReply.put((byte) 0);

        clientReply.put(AddressType.IP4);
        clientReply.put(hostAddress.getAddress().getAddress());
        clientReply.putShort((short) port);
        clientReply.flip();
        try {
            client.write(clientReply);
            logger.debug("write to client {}", clientReply.array());
            ForwarderHandler handler = new ForwarderHandler(selector, client, host);
            client.register(selector, SelectionKey.OP_READ, handler);
            host.register(selector, SelectionKey.OP_READ, handler);
        } catch (Exception e) {
            client.keyFor(selector).cancel();
            e.printStackTrace();
        }
    }

    @Override
    public void onAccept(SelectableChannel channel) {

    }

    @Override
    public void onConnect(SelectableChannel channel) {
        try {
            logger.debug("connect to host {}", host.getRemoteAddress());

            SocketChannel socketChannel = (SocketChannel) channel;

            socketChannel.finishConnect();
            client.keyFor(selector).interestOps(SelectionKey.OP_WRITE);
            host.keyFor(selector).interestOps(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
