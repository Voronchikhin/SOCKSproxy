import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.Set;

abstract public class Server implements Runnable {
    protected Logger logger = LogManager.getLogger(Server.class);
    protected Selector selector;



    @Override
    public void run() {
        while (true) {
            try {
                selector.select();
            } catch (IOException e) {
                logger.warn("could not select {}", e);
                return;
            }
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();
            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                if (key.isValid() && key.isAcceptable()) {
                    logger.debug("accept key {}", key);
                    accept(key);
                }
                if (key.isValid() && key.isConnectable()) {
                    logger.debug("connect key {}", key);
                    connect(key);
                }
                if (key.isValid() && key.isReadable()) {
                    logger.debug("read key {}", key);
                    read(key);
                }
                if (key.isValid() && key.isWritable()) {
                    logger.debug("write key {}", key);
                    write(key);
                }

                iter.remove();
            }
        }
    }

    protected abstract void write(SelectionKey key);

    protected abstract void read(SelectionKey key);

    protected abstract void connect(SelectionKey key);

    protected abstract void accept(SelectionKey key);
}

