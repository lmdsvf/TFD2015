package serverSide;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Properties;

import message.Message;
import message.MessageType;
import network.Network;

public class StartServicingClient extends Thread {

	private Network server;
	private ServerState state;

	public StartServicingClient(ServerState state) {
		this.state = state;
	}

	@Override
	public void run() {
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
		server = new Network(Integer.parseInt(properties.getProperty("PClient")));
		System.out.println("ServerSocket activa");
		
		while (true) { // espera q venha clients
			System.out.println("Waiting for clients...");
			DatagramPacket data = server.receive();
			
			if(data != null) // se nao fez timeout
				new DealWithClient(data).start();
			else
				System.out.println("Send keep alive to backups");
			// state.getClientTable().put(server.getIP().toString(), new
			// Tuple());
		}
	}

	private class DealWithClient extends Thread {

		private Message msg;
		private String clientName;
		private InetAddress clientIP;
		private int clientPort;
		
		private DealWithClient(DatagramPacket data) {
			clientIP = data.getAddress();
			this.msg = Network.networkToMessage(data);
		}

		@Override
		public void run() {
			// if (server.getSocket().isConnected()) {
System.out.println("Received: " + msg.getType());
			switch (msg.getType()) {
			case REQUEST:
				// send prepare to all backups
				Message sm = new Message(MessageType.PREPARE, 12548, msg, 1, 0);
				
				// for (DealWithServers ds : backupServers.values()) {
				// ds.getNetwork().send(sm);
				// }

				// wait for half the prepare_ok message
				// int ok = 0;
				// while(ok < (backupServers.size()/2) + 1){
				//
				// ok++;
				// }

				Message reply = new Message(MessageType.REPLY, 0, msg.getRequest_Number(), "result");
				server.send(reply,clientIP);
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
