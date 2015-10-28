package clientSide;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import network.Network;
import message.Message;
import message.MessageType;

public class Client {
	private ClientState state;
	private String serverAddress;
	private int port;

	public Client(int id) {
		readConfiguration();
		state = new ClientState(id);
		execute();
		// TODO Auto-generated constructor stub
		// connection();
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
	
	public void execute(){
		System.out.println(state.toString());
		Network net = new Network(serverAddress,port);
		Message msg = new Message(MessageType.REQUEST,"operation",state.getId()+"",0);
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

	public static void main(String[] args) {
		int nClients = 10;
		for (int i = 0;i<nClients;i++){
			new Client(i);
			System.out.println("New Client Created!");
		}
	}

}
