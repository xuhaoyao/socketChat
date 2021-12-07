### 功能

通过socket实现了简单的双向聊天功能，双方互发消息不阻塞。

- `ClientWorker`:客户端线程，用来发送消息
- `ServerWorker`:服务断线程，用来接收消息
- `ChatServer`:    聊天服务器，等待一个客户端通过socket连接
- `ChatClient`:    聊天客户端，连接聊天服务器，实现双方互发消息



### 出现的问题

双方互发消息的时候由于有延迟，以及消息的数据不会很大，基本不会出现粘包和拆包的情况

可以通过下面的代码测试粘包和拆包

#### 粘包

![image](https://user-images.githubusercontent.com/56396192/144971830-c88ba8ca-dbad-458d-9fcf-66209977cc4e.png)

```java
public class SocketServer {
    public static void main(String[] args) throws Exception {
        // 监听指定的端口
        int port = 55533;
        ServerSocket server = new ServerSocket(port);
        
        System.out.println("server等待一个客户端连接");
        Socket socket = server.accept();
        // 建立好连接后，从socket中获取输入流，并建立缓冲区进行读取
        InputStream inputStream = socket.getInputStream();
        byte[] bytes = new byte[1 << 16];   //互联网的MTU是1500,本地的MTU是65536
        int len;
        while ((len = inputStream.read(bytes)) != -1) {
            //注意指定编码格式，发送方和接收方一定要统一，建议使用UTF-8
            String content = new String(bytes, 0, len, StandardCharsets.UTF_8);
            System.out.println("len = " + len + ", content: " + content);
        }
        inputStream.close();
        socket.close();
        server.close();
    }
}
```

```java
public class SocketClient {
    public static void main(String[] args) throws Exception {
        // 要连接的服务端IP地址和端口
        String host = "127.0.0.1";
        int port = 55533;
        // 与服务端建立连接
        Socket socket = new Socket(host, port);
        // 建立连接后获得输出流
        OutputStream outputStream = socket.getOutputStream();
        String message = "这是一个整包!!!";
        for (int i = 0; i < 20; i++) {
            //若出现一点延迟,就不会有粘包的情况
            try { TimeUnit.SECONDS.sleep(1); } catch (InterruptedException e) { e.printStackTrace(); }
            outputStream.write(message.getBytes(StandardCharsets.UTF_8));
        }
        outputStream.close();
        socket.close();
    }
}
```

**测试结果如下：**

本来应该打印20行的,只打印了两行，这就是粘包的效果

```
server等待一个客户端连接
len = 21, content: 这是一个整包!!!
len = 399, content: 这是一个整包!!!这是一个整包!!!这是一个整包!!!这是一个整包!!!这是一个整包!!!这是一个整包!!!这是一个整包!!!这是一个整包!!!这是一个整包!!!这是一个整包!!!这是一个整包!!!这是一个整包!!!这是一个整包!!!这是一个整包!!!这是一个整包!!!这是一个整包!!!这是一个整包!!!这是一个整包!!!这是一个整包!!!
```



**总结出现粘包的原因**：

- 要发送的数据小于TCP发送缓冲区的大小，TCP将多次写入缓冲区的数据一次发送出去；
- 接收数据端的应用层没有及时读取接收缓冲区中的数据；
- 数据发送过快，数据包堆积导致缓冲区积压多个数据后才一次性发送出去(如果客户端每发送一条数据就睡眠一段时间就不会发生粘包)；



#### 拆包

![image](https://user-images.githubusercontent.com/56396192/144971850-577fe12b-c4bf-49c1-ad60-2b6cd3bae34d.png)

如果数据包过大，超过了MSS，就会被拆包分成多个TCP报文段传输。因此要演示拆包的话只需要发送一个大于MSS的数据

`MSS = MTU - (ip头部) - (tcp头部)`

互联网上的MTU一般是1500字节

本地网络的MTU一般是2^16字节

linux下可以用`ifconfig`查看

```bash
[root@VarerLeet2 testSocket]# ifconfig
ens33: flags=4163<UP,BROADCAST,RUNNING,MULTICAST>  mtu 1500
        inet 192.168.200.132  netmask 255.255.255.0  broadcast 192.168.200.255
        inet6 fe80::c4b5:fa80:8cd9:24f  prefixlen 64  scopeid 0x20<link>
        ether 00:0c:29:37:fa:90  txqueuelen 1000  (Ethernet)
        RX packets 968  bytes 114670 (111.9 KiB)
        RX errors 0  dropped 0  overruns 0  frame 0
        TX packets 813  bytes 338002 (330.0 KiB)
        TX errors 0  dropped 0 overruns 0  carrier 0  collisions 0

lo: flags=73<UP,LOOPBACK,RUNNING>  mtu 65536
        inet 127.0.0.1  netmask 255.0.0.0
        inet6 ::1  prefixlen 128  scopeid 0x10<host>
        loop  txqueuelen 1000  (Local Loopback)
        RX packets 58  bytes 137176 (133.9 KiB)
        RX errors 0  dropped 0  overruns 0  frame 0
        TX packets 58  bytes 137176 (133.9 KiB)
        TX errors 0  dropped 0 overruns 0  carrier 0  collisions 0
```



下面演示拆包的代码

将content发送出去就可以看到结果了

- idea下可以直接观察到拆包现象
- 如果在linux下运行的话,看不到拆包现象，但是可以通过tcpdump抓包查看数据传输的细节
- `这是一个很长很长很大的包!`12个中文一个英文字符,共37个字节
- 37 * 1800 = 66600  > 65536 ,可以出现拆包现象了
- 看下面的`tcpdump`命令,观察一下length字段 21845 + 21845 + 21845 + 1065 = 66600

```java
    private static final String content;

    static {
        StringBuilder sb = new StringBuilder();
        for(int i = 0;i < 1800;i++){
            sb.append("这是一个很长很长很大的包!");
        }
        content = sb.toString();
    }
```

```bash
[root@VarerLeet2 testSocket]# sudo tcpdump -i lo 'port 55533'
tcpdump: verbose output suppressed, use -v or -vv for full protocol decode
listening on lo, link-type EN10MB (Ethernet), capture size 262144 bytes
22:38:58.048736 IP localhost.55842 > localhost.55533: Flags [S], seq 1143447607, win 43690, options [mss 65495,sackOK,TS val 580558 ecr 0,nop,wscale 7], length 0
22:38:58.048752 IP localhost.55533 > localhost.55842: Flags [S.], seq 3458445544, ack 1143447608, win 43690, options [mss 65495,sackOK,TS val 580559 ecr 580558,nop,wscale 7], length 0
22:38:58.048762 IP localhost.55842 > localhost.55533: Flags [.], ack 1, win 342, options [nop,nop,TS val 580559 ecr 580559], length 0
22:38:58.054292 IP localhost.55842 > localhost.55533: Flags [.], seq 1:21846, ack 1, win 342, options [nop,nop,TS val 580564 ecr 580559], length 21845
22:38:58.054311 IP localhost.55842 > localhost.55533: Flags [P.], seq 21846:43691, ack 1, win 342, options [nop,nop,TS val 580564 ecr 580559], length 21845
22:38:58.054645 IP localhost.55533 > localhost.55842: Flags [.], ack 21846, win 1365, options [nop,nop,TS val 580564 ecr 580564], length 0
22:38:58.054656 IP localhost.55842 > localhost.55533: Flags [.], seq 43691:65536, ack 1, win 342, options [nop,nop,TS val 580564 ecr 580564], length 21845
22:38:58.054659 IP localhost.55842 > localhost.55533: Flags [FP.], seq 65536:66601, ack 1, win 342, options [nop,nop,TS val 580564 ecr 580564], length 1065
22:38:58.054674 IP localhost.55533 > localhost.55842: Flags [.], ack 43691, win 2388, options [nop,nop,TS val 580564 ecr 580564], length 0
22:38:58.054687 IP localhost.55533 > localhost.55842: Flags [.], ack 65536, win 3411, options [nop,nop,TS val 580564 ecr 580564], length 0
22:38:58.069672 IP localhost.55533 > localhost.55842: Flags [F.], seq 1, ack 66602, win 3635, options [nop,nop,TS val 580579 ecr 580564], length 0
22:38:58.069683 IP localhost.55842 > localhost.55533: Flags [.], ack 2, win 342, options [nop,nop,TS val 580579 ecr 580579], length 0

```



### 解决

由于我们传输的是字节流，字节流连在一起无法辨认哪些字节是属于谁的，因此我们可以定义一个协议

在每个数据发出去之前，在它前面加上这个数据的长度字段，指明这个数据包的真实长度

解决方案的代码在`ServerWorker`和`ClientWorker`中写到了。
![image](https://user-images.githubusercontent.com/56396192/144971883-38dd5ee8-47cd-4f5d-8f11-ede235721c84.png)
