package clientSide;

import message.Message;
import message.MessageType;
import network.Network;

public class clienttest {

	public static void main(String[] args) {
		
		Network net = new Network("127.0.0.1", 4567);
		for (int i = 0; i < 1; i++) {
			Message a = new Message(MessageType.REQUEST, "Vai trabalhar!", 2, 3);
			net.send(a);
			Message msgReceived = (Message) net.receive();
			System.out.println("Lado Cliente: " + msgReceived.getResult());
		}
	}

}
