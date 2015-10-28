package serverSide;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Properties;

import message.Message;
import message.MessageType;
import network.Network;

public class Server {
	// public static final int PORT = 4567;
	// public static final int PORT_S = 4568;
	private Network serversSockets;
	private ServerState state;
	private HashMap<String, DealWithServers> backupServers;
	private Properties properties;

	public Server() {
		state = new ServerState();
		backupServers = new HashMap<String, DealWithServers>();
		properties = new Properties();
		try {
			properties.load(new FileReader("Configuration.txt"));

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int mod = state.getView_number() % state.getConfiguration().size();
		// primario
		if (mod == state.getReplica_number()) {
			// new ConnecteToServers().start();
			new StartServicingClient(state).start();
			System.out.println("Primario esta Disponivel");
			// replica
		} else {
			// Instancia network para fazer recovery
			new KeepingPortOpen().start();
		}
	}

	class KeepingPortOpen extends Thread {
		private Network backUpServer;

		@Override
		public void run() {

			backUpServer = new Network(Integer.parseInt(properties
					.getProperty("PServer")));
			System.out.println("BackUp activo");
			while (true) { // espera do contacto do primary
				System.out.println("Waiting for primary...");

				DatagramPacket data = backUpServer.receive();

				if (data != null) // se nao fez timeout
					new DealWithServers(data).start();
				else
					System.out.println("Don't do nothing!");
				// Message message = server.receive();
				// state.getClientTable().put(server.getIP().toString(), new
				// Tuple());
				// new DealWithServersTest(message).start();
			}
		}

		class DealWithServers extends Thread {
			private DatagramPacket rawData;
			private Message msg;
			private InetAddress ipPrimary;

			public DealWithServers(DatagramPacket data) {
				this.rawData = data;
				this.ipPrimary = data.getAddress();
				this.msg = Network.networkToMessage(data);
			}

			@Override
			public void run() {
				switch (msg.getType()) {
				case PREPARE:
					// verificar se o op number
					System.err.println("Recebeu Pah!");
					Message prepareOk = new Message(MessageType.PREPARE_OK, 1,
							1, "Menham");
					backUpServer
							.send(prepareOk, ipPrimary,
									Integer.parseInt(properties
											.getProperty("PServer")));
					break;
				default:
					break;
				}
			}

		}
	}

	public static void main(String[] args) {
		new Server();
		System.out.println("New Server Created!");
	}
}
