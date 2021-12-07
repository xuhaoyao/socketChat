package com.scnu.socketChat;

import java.net.ServerSocket;
import java.net.Socket;

public class ChatServer {

	private static final int port = 58888;

	public static void main(String[] args) throws Exception{
		ServerSocket server = new ServerSocket(port);
		System.out.println("等待连接....");
		Socket socket = server.accept();
		System.out.println("连接成功....,对方的端口号是:" + socket.getPort());
		Thread s = new Thread(new ClientWorker(socket));
		Thread c = new Thread(new ServerWorker(socket));
		s.start();
		c.start();
	}
}
