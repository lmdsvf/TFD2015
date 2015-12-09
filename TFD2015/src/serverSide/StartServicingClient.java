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
	private int timeout;
	private boolean testStateTransfer;
	private int messagesBetweenCommits;

	public StartServicingClient(ServerState state, int timeout,
			boolean testStateTransfer, int messagesBetweenCommits) {
		this.state = state;
		this.timeout = timeout;
		this.testStateTransfer = testStateTransfer;
		this.messagesBetweenCommits = messagesBetweenCommits;
	}

	@Override
	public void run() {
		server = new Network(Integer.parseInt(state.getProperties()
				.getProperty("PClient")) + state.getReplica_number());
		serverToserver = new Network(Integer.parseInt(state.getProperties()
				.getProperty("PServer") + state.getReplica_number()));

		while (true) { // espera q venha clients
			System.out.println("Waiting for clients...");
			DatagramPacket data = server.receive(timeout);
			if (data != null) { // se nao fez timeout
				new DealWithClient(data).start();
			} else {
				/**** Checking ****/
				System.err.println("Current Status:");
				System.err.println("Operation Number: " + state.getOp_number()
						+ "\nCommit Number: " + state.getCommit_number()
						+ " \nView Number: " + state.getView_number()
						+ " \nLog size: " + state.getLog().size());
				int u = 0;
				for (Message received : state.getLog()) {
					System.err.println("Message " + u + ": "
							+ received.getType() + " from Client:"
							+ received.getClient_Id());
					u++;
				}
				/********/
				System.out.println("Sending COMMIT Messages to backups!");
				Message commit = new Message(MessageType.COMMIT,
						state.getView_number(), state.getCommit_number());
				server.broadcastToServers(commit, state.getConfiguration(),
						state.getUsingAddress(), false);

			}
		}
	}

	private class DealWithClient extends Thread {

		private Message msg;
		private InetAddress clientIP;
		private String clientId;
		private int portDestination;
		private static final int INCIALOPNUMBERVALUEINTUPLE = 0;
		private static final String INCIALRESULTVALUEINTUPLE = "";

		private DealWithClient(DatagramPacket data) {
			clientIP = data.getAddress();
			portDestination = data.getPort();
			clientId = clientIP.getHostAddress() + ":" + portDestination;
			this.msg = Network.networkToMessage(data);
			this.msg.setClient_Id(clientId);
			if (!state.getClientTable().containsKey(msg.getClient_Id())) {
				System.out
						.println("Putting new Client on Client Table! The clientId is: "
								+ msg.getClient_Id());
				state.getClientTable().put(
						msg.getClient_Id(),
						new Tuple(INCIALOPNUMBERVALUEINTUPLE,
								INCIALRESULTVALUEINTUPLE));
			}
		}

		@Override
		public void run() {

			switch (msg.getType()) {
			case REQUEST:
				System.out.println("REQUEST Message received from client: "
						+ msg.getClient_Id());
				if (msg.getRequest_Number() == (state.getClientTable()
						.get(clientId).getRequest_number() + 1)) {
					int operationNumberOfTheMsg = msg.getOperation_number();
					state.op_number_increment();
					state.getLog().add(this.msg);
					state.getClientTable().get(clientId)
							.setRequest_number(msg.getRequest_Number());
					state.getClientTable().get(clientId)
							.setResult(INCIALRESULTVALUEINTUPLE);

					// sending prepare messages to replicas
					if (!(testStateTransfer && ((msg.getRequest_Number() % messagesBetweenCommits) == 0))) {
						Message prepare = new Message(MessageType.PREPARE,
								state.getView_number(), msg,
								state.getOp_number(), state.getCommit_number());
						server.broadcastToServers(prepare,
								state.getConfiguration(),
								state.getUsingAddress(), false);

						int i = 1;
						int majority = ((state.getConfiguration().size() / 2));
						ArrayList<String> usingIps = new ArrayList<String>();
						while (i < majority) {
							DatagramPacket prepareOk = serverToserver
									.receive(timeout);
							if (prepareOk == null) {
								continue;
							}
							Message newPrepareOk = Network
									.networkToMessage(prepareOk);
							if (newPrepareOk.getType().equals(
									MessageType.PREPARE_OK)
									&& newPrepareOk.getOperation_number() == operationNumberOfTheMsg
									&& state.getConfiguration().contains(
											newPrepareOk.getBackUp_Ip())
									&& !usingIps.contains(newPrepareOk
											.getBackUp_Ip())) {
								System.out
										.println("PREPARE_OK Message received from: "
												+ newPrepareOk.getBackUp_Ip());
								usingIps.add(newPrepareOk.getBackUp_Ip());
								i++;
							}
						}
					}
					state.commit_number_increment();
					Message reply = new Message(MessageType.REPLY,
							state.getView_number(), msg.getRequest_Number(),
							"result" + msg.getRequest_Number());
					reply.setBackUp_Ip(state.getUsingAddress());
					System.out.println("RESPONSE TO CLIENT: " + clientIP + ":"
							+ portDestination);
					try {
						server.send(reply,
								InetAddress.getByName(clientId.split(":")[0]),
								portDestination);
					} catch (UnknownHostException e) {
						e.printStackTrace();
					}
					state.getClientTable().get(clientId)
							.setRequest_number(msg.getRequest_Number());
					state.getClientTable().get(clientId)
							.setResult("Result" + msg.getRequest_Number());
				} else if (msg.getRequest_Number() == (state.getClientTable()
						.get(clientId).getRequest_number())) {
					Message reply = new Message(MessageType.REPLY,
							state.getView_number(), msg.getRequest_Number(),
							state.getClientTable().get(clientId).getResult());
					reply.setBackUp_Ip(state.getUsingAddress());
					server.send(reply, clientIP, portDestination);
				}
				break;
			case ASKREQUESTNUMBER:
				Message update = new Message(MessageType.UPDATERESQUESTNUMBER,
						0);
				if (state.getClientTable().keySet().contains(clientId)) {
					Tuple a = state.getClientTable().get(clientId);
					update.setRequest_Number(a.getRequest_number());
				}
				try {
					server.send(update,
							InetAddress.getByName(clientId.split(":")[0]),
							portDestination);
				} catch (UnknownHostException e1) {
					e1.printStackTrace();
				}
				break;
			default:
				break;
			}
		}
	}
}
