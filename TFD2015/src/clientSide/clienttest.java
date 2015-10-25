package clientSide;

import java.net.InetAddress;
import java.net.UnknownHostException;

import message.Message;
import message.MessageType;
import network.Network;

public class clienttest {

	public static void main(String[] args) {

		Network net = new Network("192.168.1.3", 4567);
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
		}
	}

}
