package clientSide;

import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Properties;

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

	public Client(final String op) {
		readConfiguration();
		state = new ClientState();
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
		Properties properties = new Properties();
		try {
			properties.load(new FileReader("Configuration.txt"));
			serverAddress = properties.getProperty("IP0");
			port = Integer.parseInt(properties.getProperty("PClient"));
			net = new Network(serverAddress, port);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void execute(String op) {
		state.setIpAddress(net.getLocalIP() + ":" + net.getLocalPort());
		state.request_number_increment();
		Message msg = new Message(MessageType.REQUEST, op,
				state.getIpAddress(), state.getRequest_number());
		try {
			net.send(msg, InetAddress.getByName(serverAddress), port);
			DatagramPacket data = net.receive();
			/***** Broadcast *****/
			/*
			 * if (data == null) { for (String ip : state.getConfiguration()) {
			 * Network newNet = new Network(ip, port); newNet.receive(); } }
			 */
			Message reply = Network.networkToMessage(data);
			System.out.println(reply.getResult());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws SocketException {
		new Client("Echo Olá Mundo!");
		System.out.println("New Client Created!");

	}

}
