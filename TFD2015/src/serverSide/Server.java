package serverSide;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Properties;

import message.Message;
import message.MessageType;
import network.Network;

public class Server {
	private ServerState state;
	private Properties properties;
	private ArrayList<Message> bufferForMessagesWithToHigherOpNumber;

	public Server() {
		state = new ServerState();
		properties = state.getProperties();
		bufferForMessagesWithToHigherOpNumber = new ArrayList<Message>();
		try {
			properties.load(new FileReader("Configuration.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		int mod = state.getView_number() % state.getConfiguration().size();
		// primario
		if (mod == state.getReplica_number()) {
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
		private static final int INCIALOPNUMBERVALUEINTUPLE = 0;
		private static final String INCIALRESULTVALUEINTUPLE = "";

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
				else {
					/**** Checking ****/
					System.err.println("Current Operation Number: "
							+ state.getOp_number()
							+ "\n Current Commit Number: "
							+ state.getCommit_number()
							+ " \n Current View Number: "
							+ state.getView_number() + " \n Curent Log size: "
							+ state.getLog().size());
					int u = 0;
					for (Message received : state.getLog()) {
						System.err.println("Message " + u + ": "
								+ received.getType() + " from Client:"
								+ received.getClient_Id());
						u++;
					}
					/********/
					System.out.println("Don't do nothing!");
					// Aqui será o ViewChange
				}

			}
		}

		class DealWithServers extends Thread {
			private Message msg;
			private InetAddress ipPrimary;

			public DealWithServers(DatagramPacket data) {
				this.ipPrimary = data.getAddress();
				this.msg = Network.networkToMessage(data);
			}

			@Override
			public void run() {
				switch (msg.getType()) {
				case PREPARE:
					System.out
							.println("Prepare Messge Received with Request message from client: "
									+ msg.getClient_Message().getClient_Id());
					if (!state.getClientTable().containsKey(
							msg.getClient_Message().getClient_Id())) {
						state.getClientTable().put(
								msg.getClient_Message().getClient_Id(),
								new Tuple(INCIALOPNUMBERVALUEINTUPLE,
										INCIALRESULTVALUEINTUPLE));
					}
					if (msg.getOperation_number() > (state.getLog().size() + 1)) {
						System.out
								.println("Operation Number recived to high! Went to the Buffer!");
						bufferForMessagesWithToHigherOpNumber.add(msg);
					} else if (msg.getOperation_number() == (state.getLog()
							.size() + 1)) {
						System.out.println("Operation Number expected!");
						state.op_number_increment();
						state.getLog().add(this.msg.getClient_Message());
						if (msg.getCommit_Number() == state.getCommit_number() + 1) {
							state.setCommit_number(msg.getCommit_Number());
							/* Ver depois isto */
							state.getClientTable()
									.get(msg.getClient_Message().getClient_Id())
									.setResult(
											"result"
													+ msg.getClient_Message()
															.getRequest_Number());
						} else if (msg.getCommit_Number() > state
								.getCommit_number() + 1) {
							// Transfer State
						}// Falar com o professor
						Message messageOfClient = msg.getClient_Message();
						state.getClientTable()
								.get(messageOfClient.getClient_Id())
								.setRequest_number(
										messageOfClient.getRequest_Number());
						state.getClientTable()
								.get(messageOfClient.getClient_Id())
								.setResult(
										"result"
												+ messageOfClient
														.getRequest_Number());
						Message prepareOk = new Message(MessageType.PREPARE_OK,
								state.getView_number(), state.getOp_number(),
								state.getUsingIp());
						backUpServer.send(prepareOk, ipPrimary, Integer
								.parseInt(properties.getProperty("PServer")));
						DealingWithBuffer();
					}
					break;
				case COMMIT:
					if (msg.getCommit_Number() == state.getCommit_number() + 1) {
						state.setCommit_number(msg.getCommit_Number());
						/* E aqui? */
					} else if (msg.getCommit_Number() > state
							.getCommit_number() + 1) {
						// Transfer State
					}
					break;
				default:
					break;
				}
			}

			private void DealingWithBuffer() {
				ArrayList<Message> aux = new ArrayList<Message>();
				for (Message m : bufferForMessagesWithToHigherOpNumber) {
					if (m.getOperation_number() == state.getLog().size() + 1) {
						Message prepareOk = new Message(MessageType.PREPARE_OK,
								state.getView_number(), state.getOp_number(),
								"");// Temos que ver
									// isto melhor
						backUpServer.send(prepareOk, ipPrimary, Integer
								.parseInt(properties.getProperty("PServer")));// Pode
																				// haver
																				// um
																				// problema
																				// aqui!
																				// ipPrimary
					} else {
						aux.add(m);
					}
					if (bufferForMessagesWithToHigherOpNumber.size() != aux
							.size() && aux.size() != 0) {
						bufferForMessagesWithToHigherOpNumber = aux;
						DealingWithBuffer();
					}
				}
			}
		}
	}

	public static void main(String[] args) {
		new Server();
		System.out.println("New Server Created!");
	}
}
