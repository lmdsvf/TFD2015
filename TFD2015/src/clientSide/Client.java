package clientSide;

import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import javax.swing.JButton;
import javax.swing.JFrame;

import message.Message;
import message.MessageType;
import network.Network;

public class Client {
	private ClientState state;
	private String serverAddress;
	private int port;
	private Network net;
	private static final int SIZE = 300;
	private int timeout;
	private boolean isPrimaryChanged = false;
	private int portWhenPrimaryFalse;
	private DatagramPacket data;
	private int myPort;

	public Client(final String op, int myPort) {
		state = new ClientState();
		this.myPort = myPort;
		System.out.println(state.getConfiguration());
		readConfiguration();
		System.out.println("O que tem: " + state.getIpAddress() + " -  "
				+ state.getId());
		Message ask = new Message(MessageType.ASKREQUESTNUMBER);
		try {
			net.send(ask, InetAddress.getByName(serverAddress), port);
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		data = net.receive(timeout);
		if (data != null) {
			Message update = Network.networkToMessage(data);
			if (update.getType().equals(MessageType.UPDATERESQUESTNUMBER)) {
				System.out.println("Updating the Request Number... Value: "
						+ update.getRequest_Number());
				state.setRequest_number(update.getRequest_Number());
			}
			data = null;
		}
		System.out.println("REQUEST NUMBER: " + state.getRequest_number());
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(SIZE, SIZE);
		Container container = frame.getContentPane();
		container.setSize(SIZE, SIZE);
		container.setLayout(new FlowLayout());
		JButton button = new JButton("New Request");
		container.add(button);
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				 execute(op);
			}
		});
		frame.setVisible(true);
	}

	private void readConfiguration() {
		serverAddress = state.getConfiguration().get(0).split(":")[0];
		port = Integer.parseInt(state.getProperties().getProperty("PClient"));
		net = new Network(serverAddress, port, myPort);
		timeout = Integer.parseInt(state.getProperties().getProperty("T"));
	}

	public void execute(String op) {
		state.setIpAddress(net.getLocalIP() + ":" + net.getLocalPort());
		state.request_number_increment();
		Message msg = new Message(MessageType.REQUEST, op,
				state.getIpAddress(), state.getRequest_number());
		try {
			if (!isPrimaryChanged) {
				net.send(msg, InetAddress.getByName(serverAddress), port);
				System.out.println("Entrou no if");
			} else {
				net.send(msg, InetAddress.getByName(serverAddress),
						portWhenPrimaryFalse);
				// net = new Network(serverAddress, portWhenPrimaryFalse);
				System.out.println("Entrou no else");
			}
			data = net.receive(timeout);
			/***** Broadcast *****/
			if (data == null) {
				isPrimaryChanged = true;
				System.out.println("Didn't receive response from primary...");
				System.out.println("Broadcasting to all servers!");
				net.broadcastToServers(msg, state.getConfiguration(), null,
						true);
				data = net.receive(timeout);
				if (data != null) {
					System.err.println("AHAH: "
							+ Network.networkToMessage(data).getBackUp_Ip());
					System.err.println("Conteudo do msg: "
							+ Network.networkToMessage(data).getBackUp_Ip()
							+ " e agora na posição zero: "
							+ state.getConfiguration().get(1)
							+ " WHATTT? "
							+ state.getConfiguration().indexOf(
									Network.networkToMessage(data)
											.getBackUp_Ip()));
					portWhenPrimaryFalse = (Integer
							.parseInt(Network.networkToMessage(data)
									.getBackUp_Ip().split(":")[1]) - 90);
					System.out.println("Port changed to: "
							+ portWhenPrimaryFalse);
				}
			}

			// data = net.receive(timeout);
			if (data != null) {
				Message reply = Network.networkToMessage(data);
				if (state.getView_number() != reply.getView_number()) {
					state.setView_number(reply.getView_number());
				}
				System.out.println("RESULT: " + reply.getResult());
				data = null;
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws SocketException {
		new Client("Echo Olá Mundo!", Integer.parseInt(args[0]));
		System.out.println("New Client Created!");

	}

}
