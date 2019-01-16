import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class ForwarderHandler implements Handler {
    private Selector selector;

    private SocketChannel client;
    private SocketChannel host;

    private Attachment clientAttachment;
    private Attachment hostAttachment;

    private Attachment current;



    private Logger logger = LogManager.getLogger(ForwarderHandler.class);


    public ForwarderHandler(Selector selector, SocketChannel client, SocketChannel host) {
        this.selector = selector;
        this.client = client;
        this.host = host;
        Buffer buffer = new Buffer(selector);
        buffer.setHost(host);
        buffer.setClient(client);
        this.clientAttachment = buffer.getClientAttachment();
        this.hostAttachment = buffer.getHostAttachment();
    }

    @Override
    public void onRead(SelectableChannel channel) {
        logger.debug("read" );
        setOtherBuffer(channel);
        Attachment attachment = current;
        try {
            int byteRead = ((SocketChannel)attachment.getChannel()).read(attachment.getBuf());
            SocketChannel otherChannel = (SocketChannel) attachment.getOtherAttachment().getChannel();
            if ( byteRead > 0 && otherChannel.isConnected()){
                attachment.getOtherAttachment().addOption(SelectionKey.OP_WRITE);
            }
            if(byteRead == -1){
                attachment.deleteOption(SelectionKey.OP_READ);
                attachment.setFinishRead(true);
                if(attachment.getBuf().position() == 0){
                    otherChannel.shutdownOutput();
                    attachment.getOtherAttachment().setOutputShutdown(true);
                    if(attachment.isOutputShutdown() || attachment.getOtherAttachment().getBuf().position() == 0){
                        attachment.close();
                        attachment.getOtherAttachment().close();
                    }
                }
            }

            if (!attachment.getBuf().hasRemaining()) {
                attachment.deleteOption(SelectionKey.OP_READ);
            }
        } catch (IOException e) {
            e.printStackTrace();
            attachment.close();
        }

    }

    private void setOtherBuffer(SelectableChannel channel) {
        if(channel.equals(client)){
            logger.debug("read client");
            current = clientAttachment;
        }

        if(channel.equals(host)){
            logger.debug("read host");
            current = hostAttachment;
        }
    }

    @Override
    public void onWrite(SelectableChannel channel) {
        setOtherBuffer(channel);

        Attachment attachment = current;
        Attachment otherAttachment = attachment.getOtherAttachment();
        otherAttachment.getBuf().flip();
        try {
            SocketChannel socketChannel = (SocketChannel)attachment.getChannel();
            int byteWrite = socketChannel.write(otherAttachment.getBuf());
            if (byteWrite > 0 ){
                otherAttachment.getBuf().compact();
                otherAttachment.addOption(SelectionKey.OP_READ);
            }
            if(otherAttachment.getBuf().position() == 0){
                attachment.deleteOption(SelectionKey.OP_WRITE);
                if (otherAttachment.isFinishRead()) {
                    socketChannel.shutdownOutput();
                    attachment.setOutputShutdown(true);
                    if (otherAttachment.isOutputShutdown()) {
                        attachment.close();
                        otherAttachment.close();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onAccept(SelectableChannel channel) {
        logger.debug("accept");
        throw new RuntimeException();

    }

    @Override
    public void onConnect(SelectableChannel channel) {
        throw new RuntimeException();
    }
}
