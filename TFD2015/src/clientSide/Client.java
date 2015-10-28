package clientSide;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;

import network.Network;
import message.Message;
import message.MessageType;

public class Client {
	private ClientState state;
	private String serverAddress;
	private int port;

	public Client(String op) {
		readConfiguration();
		state = new ClientState();
		execute(op);
	}

	private void readConfiguration() {
		Properties properties = new Properties();
		try {
			properties.load(new FileReader("Configuration.txt"));
			serverAddress = properties.getProperty("IP0");
			port = Integer.parseInt(properties.getProperty("PClient"));
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void execute(String op){
		try {
			System.out.println(InetAddress.getLocalHost().getHostAddress().toString());
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		Network net = new Network(serverAddress,port);
		state.setId(net.getSocket().getLocalAddress()+":"+net.getSocket().getLocalPort());
		System.out.println(state.toString());
		Message msg = new Message(MessageType.REQUEST,op ,state.getIpAddress()+"",0);
		try {
			net.send(msg, InetAddress.getByName(serverAddress), port);
			DatagramPacket data = net.receive();
			Message reply = Network.networkToMessage(data);
			System.out.println(reply.getResult());
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

//	public void connection() {
//		try {
//			socket = new Socket(serverAddress, Server.PORT);
//			in = new ObjectInputStream(socket.getInputStream());
//			out = new ObjectOutputStream(socket.getOutputStream());
//			/*
//			 * ServerMessage msg = new ServerMessage(
//			 * ServerSideMessage.Type.CONNECTED, nickname, null);
//			 */
//			ClientMessage msg = new ClientMessage(MessageType.REQUEST,
//					new Operation(2, 3, OperationType.SUM), 1, 1);
//			out.writeObject(msg);
//			new Receive(in, out).start();
//			// isConnected = true;
//		} catch (UnknownHostException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}

//	class Receive extends Thread {
//
//		private ObjectInputStream in;
//		private ObjectOutputStream out;
//
//		public Receive(ObjectInputStream in, ObjectOutputStream out) {
//			this.in = in;
//			this.out = out;
//		}
//
//		@Override
//		public void run() {
//			try {
//				while (true) {
//					ClientMessage msg = (ClientMessage) in.readObject();
//					System.out.println("Resultado: " + msg.getX());
//				}
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (ClassNotFoundException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//
//	}

	public static void main(String[] args) throws SocketException {
		
		int nClients = 1;
		String operation = "echo Ola Mundo!";
		for (int i = 0;i<nClients;i++){
			new Client(operation);
			System.out.println("New Client Created!");
		}
		
	}

}
