package serverSide;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

import message.Message;
import network.Network;

public class Server {
	public static final int PORT = 4567;
	public static final int PORT_S = 4568;
	private Network serversSockets;
	private ServerState state;
	private HashMap<String, DealWithServers> backupServers;

	public Server() {
		state = new ServerState();
		backupServers = new HashMap<String, DealWithServers>();
		int mod = state.getView_number() % state.getConfiguration().size();
		if (mod == state.getReplica_number()) {
			// new ConnecteToServers().start();

			new StartServicingClient(state).start();
			System.out.println("Primario esta Disponivel");
		} else {
			new KeepingPortOpen().start();
		}
	}

	class KeepingPortOpen extends Thread {
		private Network server;

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
			server = new Network(Integer.parseInt(properties
					.getProperty("PServer")));
			System.out.println("ServerSocket activa");
			while (true) { // espera q venha clients
				System.out.println("Waiting for primary...");
				Message message = server.receive();
				// state.getClientTable().put(server.getIP().toString(), new
				// Tuple());
				new DealWithServersTest(message).start();
			}
		}
	}

	/*
	 * class ConnectToServers extends Thread {
	 * 
	 * @Override public void run() {
	 * 
	 * for (int contador = 0; contador < state.getConfiguration().size();
	 * contador++) { if (contador != state.getReplica_number()) {
	 * 
	 * try { Socket s = new Socket(state.getConfiguration().get( contador),
	 * Server.PORT_S); ObjectOutputStream out = new ObjectOutputStream(
	 * s.getOutputStream()); ObjectInputStream in = new ObjectInputStream(
	 * s.getInputStream()); DealWithServers dealing = new DealWithServers(out,
	 * in); backupServers.put(state.getConfiguration() .get(contador), dealing);
	 * dealing.start(); } catch (IOException e) { // TODO Auto-generated catch
	 * block e.printStackTrace(); } } } }
	 * 
	 * }
	 */

	public static void main(String[] args) {
		new Server();
		System.out.println("New Server Created!");
	}
}
