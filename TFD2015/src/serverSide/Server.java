package serverSide;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import message.ClientMessage;
import message.MessageType;

public class Server {
	public static final int PORT = 4567;
	public static final int PORTS = 4567;
	private ServerSocket serverSocket;

	public Server() {
		// TODO Auto-generated constructor stub
		new StartServicing().start();
	}

	class StartServicing extends Thread {

		@Override
		public void run() {
			try {
				serverSocket = new ServerSocket(PORT);
				System.out.println("ServerSocket activa");
				while (true) { // espera q venha clients
					Socket socket = serverSocket.accept(); //
					ObjectOutputStream out = new ObjectOutputStream(
							socket.getOutputStream());
					ObjectInputStream in = new ObjectInputStream(
							socket.getInputStream());
					new DealWithClient(in, out).start();
					System.out.println("novo cliente");
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		class DealWithClient extends Thread {

			private ObjectInputStream in;
			private ObjectOutputStream out;
			private String clientName;

			public DealWithClient(ObjectInputStream in, ObjectOutputStream out) {
				// TODO Auto-generated constructor stub
				this.in = in;
				this.out = out;
			}

			public ObjectOutputStream getOut() {
				return out;
			}

			@Override
			public void run() {
				try {
					while (true) {
						ClientMessage msg = (ClientMessage) in.readObject();
						switch (msg.getType()) {
						case REQUEST:
							ClientMessage newMsg = new ClientMessage(
									MessageType.REPLY, 1, 1, 5);
							out.writeObject(newMsg);
							break;
						default:
							break;
						}
						/*
						 * ServerMessage message = (ServerMessage)
						 * in.readObject();
						 * 
						 * switch (message.getType()) { case CONNECTED: if
						 * (!maps.contains(message.getSenderNickname())) {
						 * clientName = message.getSenderNickname();
						 * 
						 * maps.addEntry(message.getSenderNickname(), out);
						 * q.add(message); } else { ClientMessage msg = new
						 * ClientMessage( ClientSideMessage.Type.ERROR,
						 * "ja existe esse nome"); out.writeObject(msg); }
						 * break; case NEW_ORDER: q.add(message); break;
						 * default: break; }
						 */

					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					/*
					 * ServerMessage msg = new ServerMessage(
					 * ServerSideMessage.Type.DISCONNECTED, clientName, null);
					 * q.clear(); q.add(msg); e.printStackTrace();
					 */
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

		}

	}

	public static void main(String[] args) {
		new Server();
		System.out.println("New Server Created!");
	}
}
