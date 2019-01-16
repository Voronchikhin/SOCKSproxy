import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

public abstract class Attachment {
    private boolean handShake = true;

    abstract Attachment getOtherAttachment();

    abstract ByteBuffer getBuf();

    SelectableChannel getChannel() {
        return socketChannel;
    }

    boolean isFinishRead() {
        return isFinishRead;
    }



    public void setFinishRead(boolean finishRead) {
        isFinishRead = finishRead;
    }


    public void setOutputShutdown(boolean outputShutdown) {
        this.outputShutdown = outputShutdown;
    }

    private Selector selector;
    private SelectableChannel socketChannel;

    public boolean isOutputShutdown() {
        return outputShutdown;
    }

    private boolean outputShutdown = false;
    private boolean isFinishRead = false;

    Attachment(SelectableChannel socketChannel, Selector selector){
        this.socketChannel = socketChannel;
        this.selector = selector;
    }


    void close() {
        try {
            socketChannel.close();
            //socketChannel.keyFor(selector).cancel();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void addOption(int option){
        SelectionKey currentOption = socketChannel.keyFor(selector);
        currentOption.interestOps(currentOption.interestOps()|option);
    }

    void deleteOption(int option){
        SelectionKey currentOption = socketChannel.keyFor(selector);
        currentOption.interestOps(currentOption.interestOps()&~option);
    }

    public boolean isHandShake() {
        return handShake;
    }

    public void setHandShake(boolean handShake) {
        this.handShake = handShake;
    }
}
