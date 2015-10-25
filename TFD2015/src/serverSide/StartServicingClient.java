package serverSide;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.SocketException;
import java.util.Properties;

import network.Network;
import message.Message;
import message.MessageType;

public class StartServicingClient extends Thread {

	private Network server;
	private ServerState state;

	public StartServicingClient(ServerState state) {
		this.state = state;
	}

	@Override
	public void run() {
		// new Network(Server.PORT);
		Properties properties = new Properties();
		try {
			properties.load(new FileReader("Configuration.txt"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		server = new Network(
				Integer.parseInt(properties.getProperty("PClient")));
		System.out.println("ServerSocket activa");
		while (true) { // espera q venha clients
			System.out.println("Waiting for clients...");
			Message message = server.receive();
			// state.getClientTable().put(server.getIP().toString(), new
			// Tuple());
			new DealWithClient(message).start();
		}
	}

	private class DealWithClient extends Thread {

		private Message msg;
		private String clientName;

		private DealWithClient(Message msg) {
			this.msg = msg;
		}

		@Override
		public void run() {
			// if (server.getSocket().isConnected()) {

			switch (msg.getType()) {
			case REQUEST:
				// send prepare to all backups
				// Message sm = new Message(MessageType.PREPARE, 1, msg, 1,
				// 0);
				// for (DealWithServers ds : backupServers.values()) {
				// ds.getNetwork().send(sm);
				// }

				// wait for half the prepare_ok message
				// int ok = 0;
				// while(ok < (backupServers.size()/2) + 1){
				//
				// ok++;
				// }
				Message reply = new Message(MessageType.REPLY, 0,
						msg.getRequest_Number(), "result");
				server.send(reply);
				break;
			default:
				break;
			}
			/*
			 * ServerMessage message = (ServerMessage) in.readObject();
			 * 
			 * switch (message.getType()) { case CONNECTED: if
			 * (!maps.contains(message.getSenderNickname())) { clientName =
			 * message.getSenderNickname();
			 * 
			 * maps.addEntry(message.getSenderNickname(), out); q.add(message);
			 * } else { ClientMessage msg = new ClientMessage(
			 * ClientSideMessage.Type.ERROR, "ja existe esse nome");
			 * out.writeObject(msg); } break; case NEW_ORDER: q.add(message);
			 * break; default: break; }
			 */
			// } else {
			// System.out.println("Fixe, resultou!");
			//
			// }
		}
	}
}
