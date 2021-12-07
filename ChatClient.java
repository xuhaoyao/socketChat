import java.net.Socket;

public class ChatClient {
	public static void main(String[] args) throws Exception {
		Socket socket = new Socket("localhost",58888);
		Thread s = new Thread(new ClientWorker(socket));
		Thread c = new Thread(new ServerWorker(socket));
		s.start();
		c.start();
	}
}
