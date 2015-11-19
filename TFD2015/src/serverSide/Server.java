package serverSide;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
				DatagramPacket data = backUpServer.receiveViewChange();
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
					/**** View Changing! ****/
					System.out.println("View Changing!");
					// Aqui será o ViewChange
					state.setLastest_normal_view_change(state.getView_number());
					state.view_numer_increment();
					state.setStatus(Status.VIEWCHANGE);
					Message startViewChange = new Message(
							MessageType.START_VIEW_CHANGE,
							state.getView_number(), state.getUsingIp());
					try {
						for (String ip : state.getConfiguration()) {
							if (!state.getUsingIp().equals(ip)) {
								backUpServer.send(startViewChange, InetAddress
										.getByName(ip), Integer
										.parseInt(state.getProperties()
												.getProperty("PServer")));
								System.out
										.println("Start View Message sended to: "
												+ ip);
							}
						}
					} catch (NumberFormatException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}

		class DealWithServers extends Thread {
			private static final int UNIQUE = 1;
			private Message msg;
			private InetAddress ipPrimary;

			public DealWithServers(DatagramPacket data) {
				this.ipPrimary = data.getAddress();
				this.msg = Network.networkToMessage(data);
			}

			private void getNewLogAndViewNumber(ArrayList<Message> aux) {
				int biggestViewNumber = 0;
				ArrayList<Message> possibleLogs = new ArrayList<Message>();
				for (Message m : aux) {
					if (biggestViewNumber < m.getView_number()) {
						biggestViewNumber = m.getView_number();
					}
				}
				state.setView_number(biggestViewNumber);// Confirmar
				for (Message m : aux) {
					if (biggestViewNumber == m.getView_number()) {
						possibleLogs.add(m);
					}
				}
				if (possibleLogs.size() == UNIQUE) {
					state.setLog(possibleLogs.get(0).getLog());
					state.setCommit_number(possibleLogs.get(0)
							.getCommit_Number());
				} else {
					int largestN = 0;
					ArrayList<Message> possibleLogsWithOp_number = new ArrayList<Message>();
					for (Message m : possibleLogs) {
						if (m.getOperation_number() > largestN) {
							largestN = m.getOperation_number();
							possibleLogsWithOp_number.add(m);
						}
					}
					state.setLog(possibleLogsWithOp_number.get(
							possibleLogsWithOp_number.size() - 1).getLog());
					state.setCommit_number(possibleLogsWithOp_number.get(
							possibleLogsWithOp_number.size() - 1)
							.getCommit_Number());
				}
				state.setOp_number(state.getLog().size());
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
					break;

				case START_VIEW_CHANGE:
					System.out.println("Start View Change Message Received!");

					state.setLastest_normal_view_change(state.getView_number());
					state.view_numer_increment();
					state.setStatus(Status.VIEWCHANGE);
					int f = (state.getNUMBEROFIPS() - 1) / 2;
					int i = 1;
					while (i != f) {
						DatagramPacket start = backUpServer.receive();// VER
						// ISTO // MELHOR // A // SÉRIO!!
						if (start != null) {
							i++;
						} else
							break;
						System.err.println("StartVIEWCHANGE Loop");
					}
					System.out.println("NUMBER OF FAULTS: " + f + " i: " + i);
					if (i >= f) {
						Message doViewChange = new Message(
								MessageType.DO_VIEW_CHANGE,
								state.getView_number(), state.getLog(),
								state.getLastest_normal_view_change(),
								state.getOp_number(), state.getCommit_number(),
								state.getUsingIp());
						try {
							backUpServer.send(doViewChange, InetAddress
									.getByName(state.getConfiguration().get(
											state.getView_number()
													% state.getConfiguration()
															.size())),
									Integer.parseInt(properties
											.getProperty("PServer")));
							System.out
									.println("DoViewChange Message Sended to the future primary, Witch is: "
											+ state.getConfiguration()
													.get(state.getView_number()
															% state.getConfiguration()
																	.size()));
						} catch (NumberFormatException e) {
							e.printStackTrace();
						} catch (UnknownHostException e) {
							e.printStackTrace();
						}
					}

					break;
				case DO_VIEW_CHANGE:
					System.out.println("Do View Change Message Received!");
					int fdo = (state.getNUMBEROFIPS() - 1) / 2;
					int ido = 1;
					ArrayList<Message> aux = new ArrayList<Message>();
					DatagramPacket start = null;
					while (ido != fdo + 1) {
						start = null;
						start = backUpServer.receive();// VER //ISTO // MELHOR
														// // A // SÉRIO!! E
														// fazer com que as
														// replicas sejam
														// diferentes, //
														// garantir vá.
						if (start != null) {
							aux.add(Network.networkToMessage(start));
							ido++;
						} else
							break;
					}
					if (ido >= fdo + 1) {// Ver ISto melhor
						getNewLogAndViewNumber(aux);
						Message startView = new Message(MessageType.START_VIEW,
								state.getView_number(), state.getLog(),
								state.getOp_number(), state.getCommit_number());
						try {
							for (String ip : state.getConfiguration()) {
								if (!state.getUsingIp().equals(ip)) {
									backUpServer.send(startView, InetAddress
											.getByName(ip), Integer
											.parseInt(state.getProperties()
													.getProperty("PServer")));
									System.out
											.println("Start View Message sended to: "
													+ ip);
								}
							}
						} catch (NumberFormatException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (UnknownHostException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						state.setStatus(Status.NORMAL);
					}

					break;
				case START_VIEW:
					System.out.println("Start View Message Received!");
					state.setView_number(msg.getView_number());
					state.setCommit_number(msg.getCommit_Number());
					state.setLog(msg.getLog());
					state.setOp_number(msg.getOperation_number());
					state.setStatus(Status.NORMAL);
					new StartServicingClient(state);
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
