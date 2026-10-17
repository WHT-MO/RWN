import java.net.ServerSocket;
import java.net.InetSocketAddress;
public class BindTest2 {
    public static void main(String[] a) throws Exception {
        // 测试1: 指定端口绑定
        try (ServerSocket s = new ServerSocket()) {
            s.bind(new InetSocketAddress("127.0.0.1", 58231));
            System.out.println("SPECIFIC PORT OK: 58231");
        } catch (Exception e) { System.out.println("SPECIFIC PORT FAIL: " + e.getMessage()); }
        // 测试2: port 0 环回
        try (ServerSocket s = new ServerSocket()) {
            s.bind(new InetSocketAddress("127.0.0.1", 0));
            System.out.println("EPHEMERAL ON LOOPBACK OK: " + s.getLocalPort());
        } catch (Exception e) { System.out.println("EPHEMERAL ON LOOPBACK FAIL: " + e.getMessage()); }
        // 测试3: port 0 通配
        try (ServerSocket s = new ServerSocket()) {
            s.bind(new InetSocketAddress(0));
            System.out.println("EPHEMERAL WILDCARD OK: " + s.getLocalPort());
        } catch (Exception e) { System.out.println("EPHEMERAL WILDCARD FAIL: " + e.getMessage()); }
    }
}
