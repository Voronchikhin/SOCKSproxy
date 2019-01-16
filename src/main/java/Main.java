import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        ProxyServer server = new ProxyServer(1080);
        server.run();
    }
}
