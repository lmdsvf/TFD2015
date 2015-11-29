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

		@Override
		public void run() {
			backUpServer = new Network(port);
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
					firstToNotice = true;
					broacastStartViewMessage = true;
					state.setLastest_normal_view_change(state.getView_number());
					state.view_numer_increment();
					state.setStatus(Status.VIEWCHANGE);
					Message startViewChange = new Message(
							MessageType.START_VIEW_CHANGE,
							state.getView_number(), state.getUsingAddress());
					try {
						int i = 0;
						for (String ip : state.getConfiguration()) {
							if (!state.getUsingAddress().equals(ip)) {
								backUpServer
										.send(startViewChange, InetAddress
												.getByName(ip.split(":")[0]),
												Integer.parseInt(state
														.getProperties()
														.getProperty("P" + i)));
								System.out
										.println("Start View Message sended to: "
												+ ip);
							}
							i++;
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
					if (!firstToNotice && !broacastStartViewMessage) {
						state.setLastest_normal_view_change(state
								.getView_number());
						state.view_numer_increment();
						state.setStatus(Status.VIEWCHANGE);
						Message startViewChange = new Message(
								MessageType.START_VIEW_CHANGE,
								state.getView_number(), state.getUsingAddress());
						try {
							int i = 0;
							for (String ip : state.getConfiguration()) {
								if (!state.getUsingAddress().equals(ip)) {
									backUpServer
											.send(startViewChange,
													InetAddress.getByName(ip
															.split(":")[0]),
													Integer.parseInt(state
															.getProperties()
															.getProperty(
																	"P" + i)));
									System.out
											.println("Start View Message sended to: "
													+ ip);
								}
								i++;
							}
						} catch (NumberFormatException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (UnknownHostException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						broacastStartViewMessage = true;
					}
					int f = (Integer.parseInt(state.getProperties()
							.get("NumberOfIps").toString()) - 1) / 2;
					System.out.println("Number of faults: " + f);
					int i = 1;
					while (i < f) {
						Message startViewMessage = Network
								.networkToMessage(backUpServer.receive());
						if (startViewMessage.getType().equals(
								MessageType.START_VIEW_CHANGE)) {
							i++;
						}
					}
					System.out
							.println("Number of faults and number of messages of startViewChange received: "
									+ f + " - " + i);
					Message doViewChange = new Message(
							MessageType.DO_VIEW_CHANGE, state.getView_number(),
							state.getLog(),
							state.getLastest_normal_view_change(),
							state.getOp_number(), state.getCommit_number(),
							state.getUsingAddress());
					try {
						// if (!state
						// .getConfiguration()
						// .get(state.getView_number()
						// % state.getConfiguration().size())
						// .equals(state.getUsingAddress())) {
						backUpServer.send(doViewChange, InetAddress
								.getByName((state.getConfiguration().get(state
										.getView_number()
										% state.getConfiguration().size()))
										.split(":")[0]), Integer
								.parseInt((state.getConfiguration().get(state
										.getView_number()
										% state.getConfiguration().size()))
										.split(":")[1]));
						// } else
						broadcastDoVIewChange = true;
						System.out
								.println("The doViewChange message sended...");
					} catch (NumberFormatException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					break;
				case DO_VIEW_CHANGE:
					System.out.println("DoViewChange message receveided by: "
							+ msg.getBackUp_Ip());
					System.out.println("Debbuging doviewchange "
							+ msg.getType());
					ArrayList<Message> receivedDoStartViewMessages = new ArrayList<Message>();
					receivedDoStartViewMessages.add(msg);
					// Acho que é um bocado impossivel de isto acontecer, só
					// caso extremamente exceptional
					if (!firstToNotice && !broacastStartViewMessage) {
						System.out.println("Entrou no ! !");
						state.setLastest_normal_view_change(state
								.getView_number());
						state.view_numer_increment();
						state.setStatus(Status.VIEWCHANGE);
					}
					Message startViewChange = new Message(
							MessageType.START_VIEW_CHANGE,
							state.getView_number(), state.getUsingAddress());
					try {
						int i2 = 0;
						for (String ip : state.getConfiguration()) {
							if (!state.getUsingAddress().equals(ip)) {
								backUpServer
										.send(startViewChange, InetAddress
												.getByName(ip.split(":")[0]),
												Integer.parseInt(state
														.getProperties()
														.getProperty("P" + i2)));
								System.out
										.println("Start View Message sended to: "
												+ ip);
							}
							i2++;
						}
					} catch (NumberFormatException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					broacastStartViewMessage = true;

					int fdo = ((Integer.parseInt(state.getProperties()
							.get("NumberOfIps").toString()) - 1) / 2) + 1;
					System.out.println("Number of faults +1: " + fdo);
					int ido = 1;
					while (ido < fdo) {
						Message doViewMessage = Network
								.networkToMessage(backUpServer
										.receiveViewChange());
						if (doViewMessage == null) {
							continue;
						}
						System.out.println("Debbuging doviewchange loop "
								+ doViewMessage.getType());
						if (doViewMessage.getType().equals(
								MessageType.DO_VIEW_CHANGE)) {
							receivedDoStartViewMessages.add(doViewMessage);
							ido++;
						}
					}
					System.out
							.println("Number of faults and number of messages of doViewChange received: "
									+ fdo + " - " + ido);
					// Check if the viewNumbers of the messages are equal to my
					System.out.println("Number of messages: "
							+ receivedDoStartViewMessages.size());
					boolean allSameViewNumber = true;
					for (Message received : receivedDoStartViewMessages) {
						if (received.getView_number() != state.getView_number()) {
							allSameViewNumber = false;
							break;
						}
					}
					if (allSameViewNumber)
						System.out.println("Everyone in the same viewNumber");
					// Selecting the log 1º largest v' after n
					int biggestViewNumber = 0;
					ArrayList<Message> possibleLogs = new ArrayList<Message>();
					for (Message m : receivedDoStartViewMessages) {
						if (biggestViewNumber < m.getView_number()) {
							biggestViewNumber = m.getView_number();
						}
					}
					System.out.println("Biggest ViewNumber is "
							+ biggestViewNumber);
					state.setView_number(biggestViewNumber);// Confirmar
					for (Message m : receivedDoStartViewMessages) {
						if (biggestViewNumber == m.getView_number()) {
							System.out.println("Message added.");
							possibleLogs.add(m);
						}
					}
					if (possibleLogs.size() > 1) {
						System.out
								.println("More than one message with the biggest view number.");
						int biggestOpNumber = 0;
						for (Message futureOpNumber : possibleLogs) {
							if (biggestOpNumber < futureOpNumber
									.getOperation_number()) {
								biggestOpNumber = futureOpNumber
										.getOperation_number();
							}
						}
						for (Message futureOpNumber : possibleLogs) {
							if (biggestOpNumber == futureOpNumber
									.getOperation_number()) {
								if (futureOpNumber.getLog().size() != 0) {
									System.out
											.println("Log with some information.");
									state.setLog(futureOpNumber.getLog());
								} else
									state.setLog(new ArrayList<Message>());
							}
						}
					} else {
						if (possibleLogs.get(0).getLog().size() != 0) {
							System.out
									.println("Log with some information. In this case, only one has been in the possibleLogs array.");
							state.setLog(possibleLogs.get(0).getLog());
						} else
							state.setLog(new ArrayList<Message>());
					}

					// set opNumber with log size
					state.setOp_number(state.getLog().size());
					System.out.println("Final Op_number: "
							+ state.getOp_number());
					// set commit_Number with largest commit_Number received
					// in all messages
					int biggestCommitNumber = 0;
					for (Message m : receivedDoStartViewMessages) {
						if (biggestCommitNumber < m.getCommit_Number()) {
							System.out.println("Message added.");
							biggestCommitNumber = m.getCommit_Number();
						}
					}
					state.setCommit_number(biggestCommitNumber);
					System.out.println("Final CommitNumber: "
							+ state.getCommit_number());
					// Status to Normal
					state.setStatus(Status.NORMAL);

					// Sending startView message
					Message startView = new Message(MessageType.START_VIEW,
							state.getView_number(), state.getLog(),
							state.getOp_number(), state.getCommit_number());
					try {
						int isv = 0;
						for (String ip : state.getConfiguration()) {
							if (!state.getUsingAddress().equals(ip)) {
								backUpServer
										.send(startView,
												InetAddress.getByName(ip
														.split(":")[0]),
												Integer.parseInt(state
														.getProperties()
														.getProperty("P" + isv)));
								System.out
										.println("Start View Message sended to: "
												+ ip);
							}
							isv++;
						}
					} catch (NumberFormatException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					beginClientServer = true;

					break;
				case START_VIEW:
					System.out
							.println("Start View message receveided it was sended by: "
									+ msg.getBackUp_Ip());
					state.setView_number(msg.getView_number());
					state.setLog(msg.getLog());
					state.setOp_number(msg.getOperation_number());
					state.setCommit_number(msg.getCommit_Number());
					// Missing the Updating The Client Table
					break;
				default:
					break;
				}
				if (beginClientServer) {
					k.interrupt();
					new StartServicingClient(state).start();
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
