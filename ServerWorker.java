import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServerWorker implements Runnable{

    private final Socket socket;

    public ServerWorker(Socket socket) {
        this.socket = socket;
    }

    /**
     * 传过来的数据包根据粘包和拆包分为以下几种情况
     * 1.独立完整的一个数据包(没有粘包和拆包)
     * 2.一个粘包,没有拆包现象
     * 3.一个粘包,也附带了别的数据包的一部分(拆包)
     * 4.仅仅是一个拆包
     * 传输过来的就是一个字节流,怎么判断?
     *
     * 建立协议：数据包前面带数据包的长度
     *
     * 1.每次处理一个完整的数据包
     * 2.当出现拆包的时候,保存上次传过来的部分数据,当拆包传过来的时候再拼接上
     * 3.利用头部的数据包长度字段(4字节),就可以每次只处理一个数据包
     * 4.若数据包的长度小于头部数据包长度字段,说明就是拆包了,那我们保存此次的数据包,等下次字节流再传输过来之后,再拼接上,直到
     *      拼接到的数据包长度≥头部数据包长度字段
     */
    @Override
    public void run() {
        byte[] bytes = new byte[1 << 16];
        int len = 0;
        boolean flag = true;
        InputStream inputStream = null;
        try {
            inputStream = socket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
            flag = false;
        }
        int prevLength = 0,totalLength = 0;
        byte[] totalBytes = new byte[]{};
        while (flag){
            try {
                if((len = inputStream.read(bytes)) != -1){
                    prevLength = totalLength;
                    totalLength += len;
                    byte[] prevBytes = totalBytes;
                    totalBytes = new byte[totalLength];
                    System.arraycopy(prevBytes,0,totalBytes,0,prevLength); //上次没有处理完的包,拆包情况
                    System.arraycopy(bytes,0,totalBytes,prevLength,len);    //拼接
                    while (totalLength > 4){
                        int dataLength = Utils.bytes2Int(totalBytes);
                        if(dataLength + 4 > totalLength){
                            //拆包情况,需要现在收到的字节流,等待下一次的字节流拼接,组成完成的包才解析
                            break;
                        }
                        //解析数据
                        String data = new String(totalBytes,4,dataLength,StandardCharsets.UTF_8);
                        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        System.out.println(time + ":" + data);

                        //解析完成后,去除解析完的字节流
                        totalLength -= (4 + dataLength);
                        byte[] remainingBytes = new byte[totalLength];
                        System.arraycopy(totalBytes,4 + dataLength,remainingBytes,0,totalLength);
                        totalBytes = remainingBytes;
                    }

                }
            } catch (IOException e) {
                flag = false;
            }
//				Thread.yield();
        }
    }
}
