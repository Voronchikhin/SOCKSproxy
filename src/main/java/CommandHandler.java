import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class CommandHandler implements Handler {
    private Selector selector;
    private SocketChannel client;
    private SocketChannel host;
    private ByteBuffer buffer = ByteBuffer.allocate(255 + 6);
    private Logger logger = LogManager.getLogger(CommandHandler.class);
    private int readed = 0;
    private InetAddress inet4Address;
    public CommandHandler(Selector selector, SocketChannel client) {
        this.client = client;
        this.selector = selector;
    }

    @Override
    public void onRead(SelectableChannel channel) {
        try {
            readed = client.read(buffer);
            buffer.flip();
            byte protocol = buffer.get();
            byte cmd = buffer.get();
            buffer.get();//rsv
            byte atype = buffer.get();
            switch (atype) {
                case AddressType.DOMAIN:
                    logger.debug("domain addressing");
                    connectDomain();
                    break;
                case AddressType.IP4:
                    logger.debug("ipv4 addressing");
                    connectIPv4(client, selector);
                    break;
                case AddressType.IP6:
                    logger.debug("ipv6 addressing");
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
            client.keyFor(selector).cancel();
            try {
                client.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    private void connectDomain() throws IOException {
        logger.info("start dns connection");
        byte hostLength = buffer.get();
        byte[] address = new byte[(int) hostLength & 0xFF];
        buffer.get(address);
        String hostname = new String(address);
        short port = buffer.getShort();

        client.register(selector,0,new DomainHandler(client, hostname, port, selector));
    }

    private void connectIPv4(SocketChannel client, Selector selector) throws IOException {
        byte[] ipAddress = new byte[4];
        buffer.get(ipAddress);
        buffer.order(ByteOrder.BIG_ENDIAN);
        int port = buffer.getShort();
        inet4Address = Inet4Address.getByAddress(ipAddress);

        host = SocketChannel.open();
        host.configureBlocking(false);
        host.connect(new InetSocketAddress(inet4Address, port));

        client.register(selector, 0, this);
        host.register(selector, SelectionKey.OP_CONNECT,this);
    }

    @Override
    public void onWrite(SelectableChannel channel) {
        buffer.array();
        buffer.flip();
        buffer.put(1, (byte) 0);
        try {
            client.write(buffer);
            logger.debug("write to client{}",buffer.array());
            ForwarderHandler handler = new ForwarderHandler(selector, client, host);
            client.register(selector,SelectionKey.OP_READ, handler);
            host.register(selector,SelectionKey.OP_READ, handler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAccept(SelectableChannel channel) {

    }

    @Override
    public void onConnect(SelectableChannel channel) {
        logger.debug("connect");
        SocketChannel socketChannel = (SocketChannel) channel;
        try {
            socketChannel.finishConnect();
            client.keyFor(selector).interestOps(SelectionKey.OP_WRITE);
            host.keyFor(selector).interestOps(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
