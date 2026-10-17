import java.net.ServerSocket;
public class BindTest {
    public static void main(String[] a) throws Exception {
        try (ServerSocket s = new ServerSocket(0)) {
            System.out.println("BIND OK port=" + s.getLocalPort());
        }
    }
}
