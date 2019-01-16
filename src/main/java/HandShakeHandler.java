import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;

public class HandShakeHandler implements Handler {
    public HandShakeHandler(Selector selector) {
        this.selector=selector;
    }
    private Selector selector;
    private SocketChannel client;
    private final static int maxHsSize = 1+1+255;
    private ByteBuffer buffer = ByteBuffer.allocate(maxHsSize);
    private int readed = 0;
    private int needToBeReaded = 2;
    private Logger logger = LogManager.getLogger(HandShakeHandler.class);
    @Override
    public void onRead(SelectableChannel channel) {
        logger.trace("read hs handler");
        try {
            readed = client.read(buffer);
            logger.info("read {} bytes:{}", readed, buffer.array());
            buffer.flip();
            byte protocol = buffer.get();
            byte nMethods = buffer.get();

            SelectionKey selectionKey = channel.keyFor(selector);
            selectionKey.interestOps(selectionKey.interestOps()&~SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onWrite(SelectableChannel channel) {
        logger.trace("write hs handler");
        buffer.clear();
        buffer.put(ProtocolType.SOCKS5);
        buffer.put((byte) 0x00);
        buffer.flip();
        try {
            client.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        SelectionKey selectionKey = channel.keyFor(selector);
        selectionKey.interestOps(SelectionKey.OP_READ);
        selectionKey.attach(new CommandHandler(selector, client));
    }

    @Override
    public void onAccept(SelectableChannel channel) {
        try {
            client = ((ServerSocketChannel)channel).accept();
            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_READ);
            SelectionKey selectionKey = client.keyFor(selector);
            selectionKey.attach(this);
            channel.keyFor(selector).attach(new HandShakeHandler(selector));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnect(SelectableChannel channel) {
        throw new RuntimeException();
    }
}
