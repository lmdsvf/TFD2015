package clientSide;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;

import javax.swing.JOptionPane;

import message.ClientMessage;
import message.MessageType;
import message.Operation;
import serverSide.Server;

public class Client {
	private ClientState state;
	private Socket socket;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private String serverAddress;

	public Client() {
		readConfiguration();
		// TODO Auto-generated constructor stub
		serverAddress = JOptionPane.showInputDialog("IP").toString();
		// connection();
	}

	private void readConfiguration() {
		Properties properties = new Properties();
		try {
			properties.load(new FileReader("Configuration.txt"));
			System.out.println(properties.getProperty("IP0"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void connection() {
		try {
			socket = new Socket(serverAddress, Server.PORT);
			in = new ObjectInputStream(socket.getInputStream());
			out = new ObjectOutputStream(socket.getOutputStream());
			/*
			 * ServerMessage msg = new ServerMessage(
			 * ServerSideMessage.Type.CONNECTED, nickname, null);
			 */
			ClientMessage msg = new ClientMessage(MessageType.REQUEST,
					new Operation(2, 3, OperationType.SUM), 1, 1);
			out.writeObject(msg);
			new Receive(in, out).start();
			// isConnected = true;
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	class Receive extends Thread {

		private ObjectInputStream in;
		private ObjectOutputStream out;

		public Receive(ObjectInputStream in, ObjectOutputStream out) {
			this.in = in;
			this.out = out;
		}

		@Override
		public void run() {
			try {
				while (true) {
					ClientMessage msg = (ClientMessage) in.readObject();
					System.out.println("Resultado: " + msg.getX());
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public static void main(String[] args) {
		new Client();
		System.out.println("New Client Created!");
	}

}
