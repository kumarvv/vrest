import com.kumarvv.vrest.AbstractRestServer;

/**
 * Sample REST Server listens at 4001
 */
public class MyServer extends AbstractRestServer {
	public static void main(String[] args) {
		MyServer myServer = new MyServer();
		myServer.start(4001, new Class<?>[] { MyResource.class });
	}
}
