package clientSide;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import message.Message;
import message.MessageType;
import network.Network;

public class clienttest {

	public static void main(String[] args) {
		Properties properties = new Properties();
		try {
			properties.load(new FileReader("Configuration.txt"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Network net = new Network(properties.getProperty("IP0"),
				Integer.parseInt(properties.getProperty("PClient")));
		try {
			Message m = new Message(MessageType.REQUEST, "Vai trabalhar!",
					InetAddress.getLocalHost().getHostAddress().toString(), 3);
			net.send(m, InetAddress.getByName(properties.getProperty("IP0")),
					Integer.parseInt(properties.getProperty("PClient")));

			DatagramPacket packetReceived = net.receive();
			if (packetReceived == null)
				System.out.println("Couldn't receive data from server!!!");
			else {
				Message msgReceived = Network.networkToMessage(packetReceived);
				System.out.println("Lado Cliente: " + msgReceived.getResult());
			}
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*
		 * for (int i = 0; i < 1; i++) { Message a; try { a = new
		 * Message(MessageType.REQUEST, "Vai trabalhar!",
		 * InetAddress.getLocalHost().getHostAddress().toString(), 3);
		 * net.send(a,InetAddress.getByName(properties.getProperty("IP0"))); }
		 * catch (UnknownHostException e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); } DatagramPacket packetReceived = net.receive();
		 * if(packetReceived == null)
		 * System.out.println("Couldn't receive data from server!!!"); else{
		 * Message msgReceived = Network.networkToMessage(packetReceived);
		 * System.out.println("Lado Cliente: " + msgReceived.getResult()); } }
		 */
	}
}
