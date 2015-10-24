package serverSide;

import java.util.HashMap;

import network.Network;

public class Server {
	public static final int PORT = 4567;
	public static final int PORT_S = 4568;
	private Network serversSockets;
	private ServerState state;
	private HashMap<String, DealWithServers> backupServers;

	public Server() {
		state = new ServerState();
		backupServers = new HashMap<String,DealWithServers>();
		//int mod = state.getView_number() % state.getConfiguration().size();
		//if (mod == state.getReplica_number()) {
		//new ConnectToServers().start();
		new StartServicingClient(state).start();
		System.out.println("Primario esta Disponivel");
		//} else {
		//	new KeepingPortOpen().start();
		//}
	}

	class KeepingPortOpen extends Thread {
		@Override
		public void run() {
			serversSockets = new Network(PORT_S);
			System.out.println("ServerSocket activa");
			while (true) { // espera q venha clients
//				Socket socket = serversSockets.accept(); //
//				ObjectOutputStream out = new ObjectOutputStream(
//						socket.getOutputStream());
//				ObjectInputStream in = new ObjectInputStream(
//						socket.getInputStream());
//				new DealWithServers(out, in).start();
//				System.out.println("novo cliente");
			}
		}
	}
/*
	class ConnectToServers extends Thread {

		@Override
		public void run() {

			for (int contador = 0; contador < state.getConfiguration().size(); contador++) {
				if (contador != state.getReplica_number()) {
					
					try {
						Socket s = new Socket(state.getConfiguration().get(
								contador), Server.PORT_S);
						ObjectOutputStream out = new ObjectOutputStream(
								s.getOutputStream());
						ObjectInputStream in = new ObjectInputStream(
								s.getInputStream());
						DealWithServers dealing = new DealWithServers(out, in);
						backupServers.put(state.getConfiguration()
								.get(contador), dealing);
						dealing.start();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}

	}
*/
	

	public static void main(String[] args) {
		new Server();
		System.out.println("New Server Created!");
	}
}
