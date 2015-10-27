package serverSide;

import network.Network;
import message.Message;
import message.MessageType;


public class DealWithServers extends Thread {
	private Network net;

	public DealWithServers(Network net) {
		this.net = net;
	}
	public Network getNetwork() {
		return net;
	}

	@Override
	public void run() {
		while (true) {
			Message msg = net.receive();
			switch (msg.getType()) {
			case PREPARE:
				// verificar se o op number 
				System.err.println("Recebeu pah!!!!!!!");
				//Message pOK = new Message(MessageType.PREPARE_OK,);
				break;

			default:
				break;
			}

		}
	}

}