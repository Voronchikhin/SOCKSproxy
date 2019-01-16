import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class ProxyServer extends Server{

    public ProxyServer(int port) throws IOException {
        selector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.socket().bind(new InetSocketAddress(port));
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        serverChannel.keyFor(selector).attach(new HandShakeHandler(selector));
    }

    @Override
    protected void write(SelectionKey key) {
        ((Handler)key.attachment()).onWrite(key.channel());
    }

    @Override
    protected void read(SelectionKey key) {
        ((Handler)key.attachment()).onRead(key.channel());
    }

    @Override
    protected void connect(SelectionKey key) {
        ((Handler)key.attachment()).onConnect(key.channel());
    }

    @Override
    protected void accept(SelectionKey key) {
        ((Handler)key.attachment()).onAccept(key.channel());
    }
}
