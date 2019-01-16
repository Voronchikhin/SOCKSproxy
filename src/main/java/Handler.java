import java.nio.channels.SelectableChannel;

public interface Handler {
    void onRead(SelectableChannel channel);
    void onWrite(SelectableChannel channel);
    void onAccept(SelectableChannel channel);
    void onConnect(SelectableChannel channel);
}
