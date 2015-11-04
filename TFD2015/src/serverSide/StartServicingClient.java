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
		while (true) { // espera q venha clients
			System.out.println("Waiting for clients...");
			DatagramPacket data = server.receive();
			if (data != null) // se nao fez timeout
				new DealWithClient(data).start();
			else {
				/**** Checking ****/
				System.err.println("Current Operation Number: "
						+ state.getOp_number() + "\n Current Commit Number: "
						+ state.getCommit_number()
						+ " \n Current View Number: " + state.getView_number()
						+ " \n Curent Log size: " + state.getLog().size());
				int u = 0;
				for (Message received : state.getLog()) {
					System.err.println("Message " + u + ": "
							+ received.getType() + " from Client:"
							+ received.getClient_Id());
				}
				/********/
				System.out.println("Sending keep_Alive Messages to backups!");
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
					e.printStackTrace();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
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
				System.out.println("Request Message received from client: "
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
					Message prepare = new Message(MessageType.PREPARE,
							state.getView_number(), msg, state.getOp_number(),
							state.getCommit_number());
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
					int i = 1;
					int majority = ((state.getConfiguration().size() / 2) + 1);
					ArrayList<String> usingIps = new ArrayList<String>();
					while (i < majority) {
						DatagramPacket prepareOk = serverToserver.receive();
						if (prepareOk == null) {
							continue;
						}
						System.out.println("BackUp ip: "
								+ prepareOk.getAddress());
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
									.println("Prepare_Ok Message received from: "
											+ newPrepareOk.getBackUp_Ip());
							usingIps.add(newPrepareOk.getBackUp_Ip());
							i++;
						}
					}
					state.commit_number_increment();
					Message reply = new Message(MessageType.REPLY,
							state.getView_number(), msg.getRequest_Number(),
							"result" + msg.getRequest_Number());
					server.send(reply, clientIP, portDestination);
					state.getClientTable().get(clientId)
							.setRequest_number(msg.getRequest_Number());
					state.getClientTable().get(clientId)
							.setResult("Result" + msg.getRequest_Number());
				} else if (msg.getRequest_Number() == (state.getClientTable()
						.get(clientId).getRequest_number())) {
					Message reply = new Message(MessageType.REPLY,
							state.getView_number(), msg.getRequest_Number(),
							state.getClientTable().get(clientId).getResult());
					server.send(reply, clientIP, portDestination);
				}
				break;
			default:
				break;
			}
		}
	}
}
