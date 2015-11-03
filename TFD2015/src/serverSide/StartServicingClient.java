package serverSide;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import message.Message;
import message.MessageType;
import network.Network;

public class StartServicingClient extends Thread {

	private Network server;
	private Network serverToserver;
	private ServerState state;
	private ArrayList<Message> bufferForMessagesWithToHigherOpNumber;

	public StartServicingClient(ServerState state) {
		this.state = state;
	}

	@Override
	public void run() {
		server = new Network(Integer.parseInt(state.getProperties()
				.getProperty("PClient")));
		System.out.println("ServerSocket activa");
		serverToserver = new Network(Integer.parseInt(state.getProperties()
				.getProperty("PServer")));
		bufferForMessagesWithToHigherOpNumber = new ArrayList<Message>();
		while (true) { // espera q venha clients
			System.out.println("Waiting for clients...");
			DatagramPacket data = server.receive();
			if (data != null) // se nao fez timeout
				new DealWithClient(data).start();
			else {
				System.out.println("Send keep alive to backups");
				Message commit = new Message(MessageType.COMMIT,
						state.getView_number(), state.getCommit_number());
				try {
					for (String ip : state.getConfiguration()) {
						server.send(commit, InetAddress.getByName(ip), Integer
								.parseInt(state.getProperties().getProperty(
										"PServer")));
						System.out.println("Commited to: " + ip);
					}
				} catch (NumberFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			// state.getClientTable().put(server.getIP().toString(), new
			// Tuple());
		}
	}

	private class DealWithClient extends Thread {

		private Message msg;
		private InetAddress clientIP;
		private int portDestination;
		private static final int INCIALOPNUMBERVALUEINTUPLE = 0;
		private static final String INCIALRESULTVALUEINTUPLE = "";

		private DealWithClient(DatagramPacket data) {
			clientIP = data.getAddress();
			portDestination = data.getPort();
			this.msg = Network.networkToMessage(data);

			if (!state.getClientTable().containsKey(msg.getClient_Id())) {
				System.out.println("Entrou na criação do tuple!");
				System.out
						.println("ClientIp na Criação: " + msg.getClient_Id());
				state.getClientTable().put(
						msg.getClient_Id(),
						new Tuple(INCIALOPNUMBERVALUEINTUPLE,
								INCIALRESULTVALUEINTUPLE));
			}
		}

		@Override
		public void run() {
			// if (server.getSocket().isConnected()) {po
			System.out.println("Received: " + msg.getType());
			switch (msg.getType()) {
			case REQUEST:
				System.out.println("ClientIp no Request: " + clientIP);
				if (msg.getRequest_Number() == (state.getClientTable()
						.get(clientIP.getHostAddress()).getOp_number() + 1)) {// Temos
					// que
					// ver
					// isto
					// melhor
					// send prepare to all backups
					state.op_number_increment();
					state.getLog().add(this.msg);
					state.getClientTable().get(clientIP.getHostAddress())
							.setOp_number(msg.getRequest_Number());
					Message prepare = new Message(MessageType.PREPARE,
							state.getView_number(), msg, state.getOp_number(),
							state.getCommit_number());// temos que ver o op_n e
														// o
														// commit_n
					try {
						for (String ip : state.getConfiguration()) {
							server.send(prepare, InetAddress.getByName(ip),
									Integer.parseInt(state.getProperties()
											.getProperty("PServer")));
							System.out.println("Sended to: " + ip);
						}
					} catch (NumberFormatException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					// for (DealWithServers ds : backupServers.values()) {
					// ds.getNetwork().send(sm);
					// }
					int i = 0;
					int majority = ((state.getConfiguration().size() / 2) + 1);
					System.err.println("Majority: " + majority);
					while (i < majority) {
						DatagramPacket prepareOk = serverToserver.receive();
						System.out.println("BackUp ip: "
								+ prepareOk.getAddress());
						Message newPrepareOk = Network
								.networkToMessage(prepareOk);
						if (newPrepareOk.getType().equals(
								MessageType.PREPARE_OK)) {
							System.out.println("Recebe AQUI!!!!!!!!");
							i++;
						}
					}
					// wait for half the prepare_ok message
					// int ok = 0;
					// while(ok < (backupServers.size()/2) + 1){
					//
					// ok++;
					// }
					state.commit_number_increment();
					state.getClientTable().get(clientIP.getHostAddress())
							.setOp_number(msg.getRequest_Number());
					state.getClientTable().get(clientIP.getHostAddress())
							.setResult("Result" + msg.getOperation_number());
					Message reply = new Message(MessageType.REPLY,
							state.getView_number(), msg.getRequest_Number(),
							"result" + msg.getRequest_Number());
					server.send(reply, clientIP, portDestination);
				} else if (msg.getRequest_Number() == (state.getClientTable()
						.get(clientIP.getHostAddress()).getOp_number())) {// Se for o mesmo
															// requestNumber, o
															// ultimo
															// requestNumber,
															// envia o resultado
					Message reply = new Message(MessageType.REPLY,
							state.getView_number(), msg.getRequest_Number(),
							state.getClientTable().get(clientIP.getHostAddress()).getResult());
					server.send(reply, clientIP, portDestination);
				}

				break;
			default:
				break;
			}
		}
	}
}
