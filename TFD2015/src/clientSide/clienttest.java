package clientSide;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
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
		Network net = new Network("192.168.1.3", Integer.parseInt(properties
				.getProperty("PClient")));
		for (int i = 0; i < 1; i++) {
			Message a;
			try {
				a = new Message(MessageType.REQUEST, "Vai trabalhar!",
						InetAddress.getLocalHost().getHostAddress().toString(),
						3);
				net.send(a);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Message msgReceived = (Message) net.receive();
			System.out.println("Lado Cliente: " + msgReceived.getResult());
			net.getSocket().close();
		}
	}
}
