import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ClientWorker implements Runnable{

    private final Socket socket;

    public ClientWorker(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        boolean flag = true;
        OutputStream outputStream = null;
        try {
            outputStream = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
            flag = false;
        }
        Scanner sendMsg = new Scanner(System.in);
        while (flag){
            String msg = sendMsg.nextLine();
            try {
                /**
                 * 中文在UTF-8下,一个中文是三个字节
                 * 而由于Unicode编码(定长编码,两个字节)的缘故,中文被编码为2个字节
                 * 因此String底层的char数组能够存下一个中文
                 * 而UTF-8下中文转成的字节流,需要三个byte才能存放
                 * 因此
                 *      这是一个整包!!!
                 * 六个中文,三个ascii,用byte数组存的话一共需要21个byte
                 */
                byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8);
                byte[] transferBytes = new byte[4 + msgBytes.length];
                System.arraycopy(Utils.int2Bytes(msgBytes.length),0,transferBytes,0,4);
                System.arraycopy(msgBytes,0,transferBytes,4,msgBytes.length);
                outputStream.write(transferBytes);
            } catch (IOException e) {
                flag = false;
            }
//				Thread.yield();
        }
    }
}
