import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.Selector;

public class Buffer {
    private boolean isEndedreading = false;
    private boolean isEndedWriting = false;
    private ByteBuffer hostBuffer;

    public ByteBuffer getClientBuffer() {
        return clientBuffer;
    }


    private ByteBuffer clientBuffer;
    private SelectableChannel client;
    private SelectableChannel host;
    private Selector selector;
    private int capacity = 4096;
    public SelectableChannel getClient() {
        return client;
    }

    public void setClient(SelectableChannel client) {
        this.client = client;
    }

    public SelectableChannel getHost() {
        return host;
    }

    public void setHost(SelectableChannel host) {
        this.host = host;
    }

    public Buffer(Selector selector) {
        this.selector = selector;
        hostBuffer = ByteBuffer.allocate(capacity);
        clientBuffer = ByteBuffer.allocate(capacity);
    }

    public ByteBuffer getHostBuffer() {
        return hostBuffer;
    }

    public boolean isEndedWriting() {
        return isEndedWriting;
    }

    public void setEndedWriting(boolean endedWriting) {
        isEndedWriting = endedWriting;
    }

    public boolean isEndedreading() {
        return isEndedreading;
    }
    public void setEndedreading(boolean endedreading) {
        isEndedreading = endedreading;
    }

    public Attachment getClientAttachment() {
        assert selector!=null;
        if(clientAttachment == null) {
            clientAttachment = new Attachment(client, selector) {
                @Override
                Attachment getOtherAttachment() {
                    return getHostAttachment();
                }

                @Override
                ByteBuffer getBuf() {
                    return clientBuffer;
                }
            };
        }
        return clientAttachment;
    }
    Attachment clientAttachment;
    Attachment serverAttachment;
    public Attachment getHostAttachment() {
        if(serverAttachment == null) {
            serverAttachment = new Attachment(host, selector) {
                @Override
                Attachment getOtherAttachment() {
                    return getClientAttachment();
                }

                @Override
                ByteBuffer getBuf() {
                    return hostBuffer;
                }
            };
        }
        return serverAttachment;
    }
}
