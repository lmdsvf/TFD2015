package serverSide;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import message.Message;
import message.MessageType;
import network.Network;

public class Server {

	private final int MAXBUFFERMESSAGES = 5;

	private ServerState state;
	private Properties properties;
	private ArrayList<Message> bufferForMessagesWithToHigherOpNumber;
	private int port;
	private KeepingPortOpen k;
	private boolean waittingInViewChange = false;
	private boolean isBackup = true;
	private int timeout;
	private int timeoutViewChange;
	private boolean testStateTransfer;
	private int messagesBetweenCommits;

	public Server(int port, boolean testStateTransfer,
			int messagesBetweenCommits) {
		this.port = port;
		this.testStateTransfer = testStateTransfer;
		this.messagesBetweenCommits = messagesBetweenCommits;
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

		timeout = Integer.parseInt(properties.getProperty("T"));
		timeoutViewChange = Integer.parseInt(properties
				.getProperty("TViewChange"));

		k = new KeepingPortOpen();
		k.start();

	}

	class KeepingPortOpen extends Thread {
		private Network backUpServer;
		private static final int INCIALOPNUMBERVALUEINTUPLE = 0;
		private static final String INCIALRESULTVALUEINTUPLE = "";
		private int numberOfStartViewChangeMessagesReceived = 0;
		private ArrayList<Message> numberOfDoViewChangeMessagesReceived = new ArrayList<Message>();
		private int faultsLimit = (Integer.parseInt(state.getProperties()
				.get("NumberOfIps").toString()) - 1) / 2;
		private int numberOfRecoveryResponseReceived = 0;
		private ArrayList<Message> numberOfRecoveryResponseMessagesReceived = new ArrayList<Message>();
		private double nounce;

		private void recovery() {
			System.out.println("Attempting recovery");
			DatagramPacket data = null;
			nounce = Math.random();
			Message recovery = new Message(MessageType.RECOVERY,
					state.getUsingAddress(), nounce);
			state.setStatus(Status.RECOVERING);
			backUpServer.broadcastToServers(recovery, state.getConfiguration(),
					state.getUsingAddress(), false);
			Message recoveryPrimary = null;
			while (numberOfRecoveryResponseReceived < (faultsLimit + 1)) {
				System.out.print("Waiting for Recovery Responses...");
				data = backUpServer.receive(timeout);
				System.out.println();
				if (data == null) {
					state.setStatus(Status.NORMAL);
					break;
				}

				Message m = Network.networkToMessage(data);
				if (m.getType() == MessageType.RECOVERY_RESPONSE) {
					numberOfRecoveryResponseReceived++;
					if (m.getLog() != null) {
						recoveryPrimary = m;
					}
				}
			}

			if (recoveryPrimary != null) {
				state.setView_number(recoveryPrimary.getView_number());
				state.setLog(recoveryPrimary.getLog());
				state.setOp_number(recoveryPrimary.getLog().size());
				state.setCommit_number(recoveryPrimary.getCommit_Number());
				numberOfRecoveryResponseReceived = 0;
				state.setStatus(Status.NORMAL);
				if ((state.getView_number() % state.getConfiguration().size()) == state
						.getReplica_number()) {
					isBackup = false;
				}
			}

			System.out.println("Recovery complete\n");
		}

		@Override
		public void run() {
			System.out.println("Initializing the Server\n");
			backUpServer = new Network(port);
			DatagramPacket data = null;

			recovery();

			int mod = state.getView_number() % state.getConfiguration().size();

			// primario
			if (mod == state.getReplica_number()) {
				isBackup = false;
				new StartServicingClient(state, timeout, testStateTransfer,
						messagesBetweenCommits).start();
				System.out.println("Taking primary functions!");
			} else
				System.out.println("Taking backup functions!");

			while (true) { // espera do contacto do primary
				if (!waittingInViewChange)
					System.out.println("Waiting for primary...");
				else
					System.out.println("Waiting for View Change Messages...");
				if (isBackup) {
					data = backUpServer.receive(timeoutViewChange);
				} else {
					data = backUpServer.receive(0);
				}

				if (data != null) // se nao fez timeout
					new DealWithServers(data).start();
				else {
					/**** Checking ****/
					System.err.println("Current Status:");
					System.err.println("Operation Number: "
							+ state.getOp_number() + "\nCommit Number: "
							+ state.getCommit_number() + " \nView Number: "
							+ state.getView_number() + " \nLog size: "
							+ state.getLog().size() + "\n");
					int u = 0;
					for (Message received : state.getLog()) {
						System.err.println("Message " + u + ": "
								+ received.getType() + " from Client:"
								+ received.getClient_Id());
						u++;
					}

					/**** View Changing! ****/
					initialProcessForViewChange();
				}
			}
		}

		private void initialProcessForViewChange() {
			System.out.println("Initializing View Change!");
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
							.println("PREPARE received with Request message from client: "
									+ msg.getClient_Message().getClient_Id());

					// if this view_number is lower than the primary's then
					// needs state transfer
					if (msg.getView_number() > state.getView_number()) {
						stateTransfer();
					} else {
						if (!state.getClientTable().containsKey(
								msg.getClient_Message().getClient_Id())) {
							state.getClientTable().put(
									msg.getClient_Message().getClient_Id(),
									new Tuple(INCIALOPNUMBERVALUEINTUPLE,
											INCIALRESULTVALUEINTUPLE));
						}
						if (msg.getOperation_number() > (state.getLog().size() + 1)) {
							System.out
									.println("Operation Number received to high! Went to the Buffer!");
							bufferForMessagesWithToHigherOpNumber.add(msg);
							if (bufferForMessagesWithToHigherOpNumber.size() >= MAXBUFFERMESSAGES) {
								stateTransfer();
								bufferForMessagesWithToHigherOpNumber.clear();
							}
						} else if (msg.getOperation_number() == (state.getLog()
								.size() + 1)) {
							state.op_number_increment();
							state.getLog().add(this.msg.getClient_Message());
							System.out.println("Client id na message: "
									+ msg.getClient_Message().getClient_Id());
							state.getClientTable()
									.get(msg.getClient_Message().getClient_Id())
									.setRequest_number(msg.getRequest_Number());
							if (msg.getCommit_Number() == state
									.getCommit_number() + 1) {
								state.setCommit_number(msg.getCommit_Number());
								state.getClientTable()
										.get(msg.getClient_Message()
												.getClient_Id())
										.setResult(
												"result"
														+ msg.getClient_Message()
																.getRequest_Number());
							}
							// else /*
							// * if (msg.getCommit_Number() > state
							// * .getCommit_number() + 1)
							// */{
							// // stateTransfer();
							// }

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
							Message prepareOk = new Message(
									MessageType.PREPARE_OK,
									state.getView_number(),
									state.getOp_number(), state.getUsingIp());
							backUpServer
									.send(prepareOk,
											ipPrimary,
											Integer.parseInt(properties
													.getProperty("PServer"))
													+ (state.getView_number() % Integer
															.parseInt(properties
																	.getProperty("NumberOfIps"))));
							DealingWithBuffer();
						}
					}
					break;
				case COMMIT:
					System.out.println("COMMIT received from: "
							+ msg.getBackUp_Ip());
					if (msg.getCommit_Number() == state.getCommit_number() + 1) {
						state.setCommit_number(msg.getCommit_Number());
						/* E aqui? */
					} else if (msg.getCommit_Number() > state
							.getCommit_number() + 1) {
						stateTransfer();
					}
					/**** Checking ****/
					System.err.println("Current Status:");
					System.err.println("Operation Number: "
							+ state.getOp_number() + "\nCommit Number: "
							+ state.getCommit_number() + " \nView Number: "
							+ state.getView_number() + " \nLog size: "
							+ state.getLog().size() + "\n");
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
							.println("START_VIEW_CHANGE received and was sended by: "
									+ msg.getBackUp_Ip());

					if (state.getStatus().equals(Status.NORMAL)) {
						initialProcessForViewChange();
						numberOfStartViewChangeMessagesReceived++;
					} else if (state.getStatus().equals(Status.VIEWCHANGE)) {

						// verifica se o view number da msg � igual ao view
						// number do server
						if (msg.getView_number() == state.getView_number()) {
							numberOfStartViewChangeMessagesReceived++;
						}
					}

					// se o numero de ViewChangeMessages >= numero de faltas
					if (numberOfStartViewChangeMessagesReceived >= faultsLimit) {
						numberOfStartViewChangeMessagesReceived = 0;

						Message doViewChange = new Message(
								MessageType.DO_VIEW_CHANGE,
								state.getView_number(), state.getLog(),
								state.getLastest_normal_view_change(),
								state.getOp_number(), state.getCommit_number(),
								state.getUsingAddress());

						try {
							backUpServer
									.send(doViewChange,
											InetAddress.getByName(state
													.getConfiguration()
													.get(state.getView_number()
															% state.getConfiguration()
																	.size())
													.split(":")[0]),
											Integer.parseInt((state
													.getConfiguration()
													.get(state.getView_number()
															% state.getConfiguration()
																	.size())
													.split(":")[1])));

						} catch (NumberFormatException e) {
							e.printStackTrace();
						} catch (UnknownHostException e) {
							e.printStackTrace();
						}
					}

					break;

				case DO_VIEW_CHANGE:
					System.out.println("DO_VIEW_CHANGE received by: "
							+ msg.getBackUp_Ip());

					if (state.getStatus().equals(Status.NORMAL)) {
						initialProcessForViewChange();
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
					}

					if (state.getStatus().equals(Status.VIEWCHANGE)) {
						numberOfDoViewChangeMessagesReceived.add(msg);

						if (numberOfDoViewChangeMessagesReceived.size() >= faultsLimit + 1) {
							state.setView_number(msg.getView_number());

							ArrayList<Message> messagesWithBiggestViewNumber = new ArrayList<Message>();
							int biggestLastNormalViewNumber = -1;
							int biggestOpNumber = -1;
							int biggestCommitNumber = -1;
							for (Message m : numberOfDoViewChangeMessagesReceived) {
								if (m.getLastest_Normal_View_Change() > biggestLastNormalViewNumber) {
									messagesWithBiggestViewNumber.clear();
									biggestLastNormalViewNumber = m
											.getLastest_Normal_View_Change();
									messagesWithBiggestViewNumber.add(m);
								} else if (m.getLastest_Normal_View_Change() == biggestLastNormalViewNumber) {
									if (m.getOperation_number() > biggestOpNumber)
										messagesWithBiggestViewNumber.add(0, m);
									else
										messagesWithBiggestViewNumber.add(
												messagesWithBiggestViewNumber
														.size(), m);
								}

								if (m.getCommit_Number() > biggestCommitNumber)
									biggestCommitNumber = m.getCommit_Number();
							}

							state.setLog(messagesWithBiggestViewNumber.get(0)
									.getLog());
							if (state.getLog().size() != 0)
								state.setOp_number(messagesWithBiggestViewNumber
										.get(0)
										.getLog()
										.get(messagesWithBiggestViewNumber
												.get(0).getLog().size() - 1)
										.getOperation_number());
							state.setCommit_number(biggestCommitNumber);

							state.setStatus(Status.NORMAL);

							Message startView = new Message(
									MessageType.START_VIEW,
									state.getView_number(), state.getLog(),
									state.getOp_number(),
									state.getCommit_number());
							backUpServer.broadcastToServers(startView,
									state.getConfiguration(),
									state.getUsingIp(), false);

							isBackup = false;
							new StartServicingClient(state, timeout,
									testStateTransfer, messagesBetweenCommits)
									.start();
							System.out.println("Taking primary functions!");
						}
					}

					break;

				case START_VIEW:
					System.out.println("START_VIEW received from: "
							+ msg.getBackUp_Ip());
					state.setLog(msg.getLog());
					if (msg.getLog().size() != 0) {
						state.setOp_number(msg.getLog().size());
					} else
						state.setOp_number(0);
					state.setView_number(msg.getView_number());
					state.setStatus(Status.NORMAL);
					HashMap<String, Tuple> clientTable = state.getClientTable();
					for (int i = state.getLog().size() - 1; i >= 0; i--) {
						if (clientTable.keySet().contains(
								state.getLog().get(i).getClient_Id())) {
							if (state.getLog().get(i).getRequest_Number() > state
									.getClientTable()
									.get(state.getLog().get(i).getClient_Id())
									.getRequest_number()) {
								clientTable.get(
										state.getLog().get(i).getClient_Id())
										.setRequest_number(
												state.getLog().get(i)
														.getRequest_Number());
								clientTable
										.get(state.getLog().get(i)
												.getClient_Id())
										.setResult(
												"result"
														+ (state.getLog()
																.get(i)
																.getRequest_Number()));
							}
						}
					}
					// UPDATE CLIENT TABLE!!! -> check!
					break;

				case RECOVERY:
					System.out.println("RECOVERY received!");
					if (state.getStatus().equals(Status.NORMAL)) {
						Message recoveryResponse;
						int mod = state.getView_number()
								% state.getConfiguration().size();
						if (mod == state.getReplica_number()) {
							recoveryResponse = new Message(
									MessageType.RECOVERY_RESPONSE,
									state.getView_number(), msg.getNounce(),
									state.getLog(), state.getOp_number(),
									state.getCommit_number(),
									state.getUsingAddress());
						} else {
							recoveryResponse = new Message(
									MessageType.RECOVERY_RESPONSE,
									state.getView_number(), msg.getNounce(),
									null, 0, 0, state.getUsingAddress());
						}
						try {
							backUpServer.send(recoveryResponse,
									InetAddress.getByName(msg.getBackUp_Ip()
											.split(":")[0]), Integer
											.parseInt(msg.getBackUp_Ip().split(
													":")[1]));
						} catch (NumberFormatException e) {
							e.printStackTrace();
						} catch (UnknownHostException e) {
							e.printStackTrace();
						}
					}

					break;
				case RECOVERY_RESPONSE:
					if (msg.getNounce() == nounce) {
						numberOfRecoveryResponseReceived++;
						numberOfRecoveryResponseMessagesReceived.add(msg);
						System.out
								.println("RECOVERY_RESPONSE received! Now exists: "
										+ numberOfRecoveryResponseReceived);

						if (numberOfRecoveryResponseReceived >= faultsLimit + 1) {
							System.out.println("Before: " + "\nView_Number: "
									+ state.getView_number() + "\nOp_number: "
									+ state.getOp_number()
									+ "\nCommit_Number: "
									+ state.getCommit_number() + "\nLog size: "
									+ state.getLog().size());
							for (Message gettingInfo : numberOfRecoveryResponseMessagesReceived) {
								if (gettingInfo.getLog() != null) {
									state.setView_number(gettingInfo
											.getView_number());
									state.setOp_number(gettingInfo
											.getOperation_number());
									state.setCommit_number(gettingInfo
											.getCommit_Number());
									state.setLog(gettingInfo.getLog());
									state.setStatus(Status.NORMAL);
									numberOfRecoveryResponseReceived = 0;
									numberOfRecoveryResponseMessagesReceived
											.clear();
								}
							}
							System.out.println("Recovery process done!");
							System.out.println("After: " + "\nView_Number: "
									+ state.getView_number() + "\nOp_number: "
									+ state.getOp_number()
									+ "\nCommit_Number: "
									+ state.getCommit_number() + "\nLog size: "
									+ state.getLog().size());
						}
					}
					break;

				case GETSTATE:
					System.out.println("GETSTATE received!");

					// only responds if in normal state and if the view_number
					// of the message is equal to this replica's view_number
					if (state.getStatus() == Status.NORMAL
							&& msg.getView_number() == state.getView_number()) {
						ArrayList<Message> partialLog = new ArrayList<Message>(
								state.getLog().subList(
										msg.getOperation_number(),
										state.getLog().size()));
						Message newState = new Message(MessageType.NEWSTATE,
								state.getView_number(), partialLog,
								state.getOp_number(), state.getCommit_number());
						try {
							backUpServer.send(newState,
									InetAddress.getByName(msg.getBackUp_Ip()
											.split(":")[0]), Integer
											.parseInt(msg.getBackUp_Ip().split(
													":")[1]));
						} catch (NumberFormatException e) {
							e.printStackTrace();
						} catch (UnknownHostException e) {
							e.printStackTrace();
						}
					} else
						System.out.println("IGNORED GETSTATE!");
					break;

				case NEWSTATE:
					System.out.println("NEWSTATE received!");
					state.getLog().addAll(msg.getLog());
					state.setView_number(msg.getView_number());
					state.setOp_number(msg.getOperation_number());
					state.setCommit_number(msg.getCommit_Number());
					HashMap<String, Tuple> clientTable2 = state
							.getClientTable();
					for (int i = state.getLog().size() - 1; i >= 0; i--) {
						if (clientTable2.keySet().contains(
								state.getLog().get(i).getClient_Id())) {
							if (state.getLog().get(i).getRequest_Number() > state
									.getClientTable()
									.get(state.getLog().get(i).getClient_Id())
									.getRequest_number()) {
								clientTable2.get(
										state.getLog().get(i).getClient_Id())
										.setRequest_number(
												state.getLog().get(i)
														.getRequest_Number());
								clientTable2
										.get(state.getLog().get(i)
												.getClient_Id())
										.setResult(
												"result"
														+ (state.getLog()
																.get(i)
																.getRequest_Number()));
							}
						}
					}
					System.out.println("STATE TRANSFER complete");
					break;
				default:
					break;
				}

			}

			private void stateTransfer() {
				System.out.println("Initiate STATE TRANSFER!");
				Message getState = new Message(MessageType.GETSTATE,
						state.getView_number(), state.getOp_number(),
						state.getUsingAddress());
				int intReplica = (int) (Math.random() * 10 % state
						.getConfiguration().size());
				System.out.println("SELECTED REPLICA TO SEND GETSTATE: "
						+ state.getConfiguration().get(intReplica));
				String address = state.getConfiguration().get(intReplica);
				try {
					backUpServer.send(getState,
							InetAddress.getByName(address.split(":")[0]),
							Integer.parseInt(address.split(":")[1]));
				} catch (NumberFormatException | UnknownHostException e) {
					e.printStackTrace();
				}
			}

			private void DealingWithBuffer() {
				ArrayList<Message> aux = new ArrayList<Message>();
				for (Message m : bufferForMessagesWithToHigherOpNumber) {
					if (m.getOperation_number() == state.getLog().size() + 1) {
						Message prepareOk = new Message(MessageType.PREPARE_OK,
								state.getView_number(), state.getOp_number(),
								"");
						backUpServer
								.send(prepareOk,
										ipPrimary,
										Integer.parseInt(properties
												.getProperty("PServer"))
												+ (state.getView_number() % Integer.parseInt(properties
														.getProperty("NumberOfIps"))));
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
		boolean testStateTransfer = false;
		int messagesBetweenCommits = 0;
		try {
			port = Integer.parseInt(args[0]);
		} catch (NumberFormatException e) {
			System.out.println("The inserted port is not a valid one");
			System.exit(-1);
		}
		if (args.length == 2) {
			testStateTransfer = true;
			messagesBetweenCommits = Integer.parseInt(args[1]);
		}
		new Server(port, testStateTransfer, messagesBetweenCommits);
	}
}
