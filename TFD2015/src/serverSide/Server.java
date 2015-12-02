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
	private int port;
	private boolean firstToNotice = false;
	private boolean broacastStartViewMessage = false;
	private boolean broadcastDoVIewChange = false;
	private KeepingPortOpen k;
	private boolean beginClientServer = false;
	private boolean waittingInViewChange = false;

	public Server(int port) {
		this.port = port;
		state = new ServerState(port);
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
			k = new KeepingPortOpen();
			k.start();
		}
	}

	class KeepingPortOpen extends Thread {
		private Network backUpServer;
		private static final int INCIALOPNUMBERVALUEINTUPLE = 0;
		private static final String INCIALRESULTVALUEINTUPLE = "";
		private int numberOfStartViewChangeMessagesReceived = 0;
		private ArrayList<Message> numberOfDoViewChangeMessagesReceived = new ArrayList<Message>();
		private int faultsLimit = (Integer.parseInt(state.getProperties()
				.get("NumberOfIps").toString()) - 1) / 2;
		private boolean updateAfterStartViewMessageReceived = false;

		@Override
		public void run() {
			backUpServer = new Network(port);
			System.out.println("BackUp activo");
			while (true) { // espera do contacto do primary
				if (!waittingInViewChange)
					System.out.println("Waiting for primary...");
				else
					System.out.println("Waiting for View Change Messages...");
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
							+ state.getView_number() + " \n Current Log size: "
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
					inicialProcessForViewChange();

				}
			}
		}

		private void inicialProcessForViewChange() {
			state.setLastest_normal_view_change(state.getView_number());
			state.view_number_increment();
			state.setStatus(Status.VIEWCHANGE);
			Message startViewChange = new Message(
					MessageType.START_VIEW_CHANGE, state.getView_number(),
					state.getUsingAddress());
			backUpServer.broadcastToServers(startViewChange,
					state.getConfiguration(), state.getUsingAddress(), false);
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
							.println("Prepare Message Received with Request message from client: "
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
								.println("Operation Number recieved to high! Went to the Buffer!");
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
						backUpServer
								.send(prepareOk,
										ipPrimary,
										Integer.parseInt(properties
												.getProperty("PServer")
												+ (state.getView_number() % Integer.parseInt(properties
														.getProperty("NumberOfIps")))));
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
					System.out
							.println("Start View Message received and was sended by: "
									+ msg.getBackUp_Ip());

					if (state.getStatus().equals(Status.NORMAL)) {
						inicialProcessForViewChange();
						System.out.println("De ceretea que não chega aqui!");
						numberOfStartViewChangeMessagesReceived++;
						// TODO Auto-generated catch block
						if (numberOfStartViewChangeMessagesReceived >= faultsLimit) {

							Message doViewChange = new Message(
									MessageType.DO_VIEW_CHANGE,
									state.getView_number(), state.getLog(),
									state.getLastest_normal_view_change(),
									state.getOp_number(),
									state.getCommit_number(),
									state.getUsingAddress());
							try {
								backUpServer
										.send(doViewChange,
												InetAddress.getByName(state
														.getConfiguration()
														.get(state
																.getView_number()
																% state.getConfiguration()
																		.size())
														.split(":")[0]),
												Integer.parseInt((state
														.getConfiguration()
														.get(state
																.getView_number()
																% state.getConfiguration()
																		.size())
														.split(":")[1])));
							} catch (NumberFormatException e) {
								e.printStackTrace();
							} catch (UnknownHostException e) {
								e.printStackTrace();
							}

						}
						/***  **/
					} else if (state.getStatus().equals(Status.VIEWCHANGE)) {
						if (msg.getView_number() == state.getView_number()) {
							numberOfStartViewChangeMessagesReceived++;
							if (numberOfStartViewChangeMessagesReceived >= faultsLimit) {

								Message doViewChange = new Message(
										MessageType.DO_VIEW_CHANGE,
										state.getView_number(), state.getLog(),
										state.getLastest_normal_view_change(),
										state.getOp_number(),
										state.getCommit_number(),
										state.getUsingAddress());
								try {
									backUpServer
											.send(doViewChange,
													InetAddress.getByName(state
															.getConfiguration()
															.get(state
																	.getView_number()
																	% state.getConfiguration()
																			.size())
															.split(":")[0]),
													Integer.parseInt((state
															.getConfiguration()
															.get(state
																	.getView_number()
																	% state.getConfiguration()
																			.size())
															.split(":")[1])));
								} catch (NumberFormatException e) {

									e.printStackTrace();
								} catch (UnknownHostException e) {
									e.printStackTrace();
								}

							}
						} else {
							// 1 million dollars question
						}
					}

					break;
				case DO_VIEW_CHANGE:
					System.out.println("DoVIewChange received by: "
							+ msg.getBackUp_Ip());
					numberOfDoViewChangeMessagesReceived.add(msg);
					if (state.getStatus().equals(Status.NORMAL)) {
						inicialProcessForViewChange();
						System.out.println("De ceretea que não chega aqui!");
						numberOfStartViewChangeMessagesReceived++;
						// TODO Auto-generated catch block
						if (numberOfStartViewChangeMessagesReceived >= faultsLimit) {

							Message doViewChange = new Message(
									MessageType.DO_VIEW_CHANGE,
									state.getView_number(), state.getLog(),
									state.getLastest_normal_view_change(),
									state.getOp_number(),
									state.getCommit_number(),
									state.getUsingAddress());
							try {
								backUpServer
										.send(doViewChange,
												InetAddress.getByName(state
														.getConfiguration()
														.get(state
																.getView_number()
																% state.getConfiguration()
																		.size())
														.split(":")[0]),
												Integer.parseInt((state
														.getConfiguration()
														.get(state
																.getView_number()
																% state.getConfiguration()
																		.size())
														.split(":")[1])));
							} catch (NumberFormatException e) {
								e.printStackTrace();
							} catch (UnknownHostException e) {
								e.printStackTrace();
							}

						}
						/***  **/
					} else {
						if (numberOfDoViewChangeMessagesReceived.size() >= faultsLimit + 1) {
							int biggestViewNumber = 0;
							ArrayList<Message> messagesWithBiggestViewNumber = new ArrayList<Message>();
							Message auxBiggestViewNumberAndBiggestOpNumber = null;
							int biggestOpNumber = 0;
							for (Message message : numberOfDoViewChangeMessagesReceived) {// VAi
																							// buscar
																							// o
																							// valor
																							// maior
																							// do
																							// viewnumber
								if (message.getView_number() > biggestViewNumber) {
									biggestViewNumber = message
											.getView_number();
									messagesWithBiggestViewNumber.clear();
									messagesWithBiggestViewNumber.add(message);
									biggestOpNumber = message
											.getOperation_number();
									auxBiggestViewNumberAndBiggestOpNumber = message;
								} else if (message.getView_number() == biggestViewNumber) {
									messagesWithBiggestViewNumber.add(message);
									if (message.getOperation_number() > biggestOpNumber) {
										biggestOpNumber = message
												.getOperation_number();
										auxBiggestViewNumberAndBiggestOpNumber = message;
									}
								}
							}

							state.setView_number(auxBiggestViewNumberAndBiggestOpNumber
									.getView_number());
							state.setOp_number(auxBiggestViewNumberAndBiggestOpNumber
									.getOperation_number());
							state.setLog(auxBiggestViewNumberAndBiggestOpNumber
									.getLog());

						}

					}

					break;
				case START_VIEW:

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
						backUpServer
								.send(prepareOk,
										ipPrimary,
										Integer.parseInt(properties
												.getProperty("PServer")
												+ (state.getView_number() % Integer.parseInt(properties
														.getProperty("NumberOfIps")))));// Pode
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
		int port = 0;
		try {
			port = Integer.parseInt(args[0]);
		} catch (NumberFormatException e) {
			System.out.println("The inserted port is not a valid one");
			System.exit(-1);
		}
		new Server(port);
		System.out.println("New Server Created!");
	}
}
