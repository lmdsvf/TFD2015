package serverSide;

import message.Message;
import network.Network;

public class DealWithServersTest extends Thread {
	private Message msg;
	private String clientName;

	public DealWithServersTest(Message net) {
		this.msg = net;
	}

	public Message getMessage() {
		return msg;
	}

	@Override
	public void run() {
		while (true) {
			switch (msg.getType()) {
			case PREPARE:
				// verificar se o op number
				System.err.println("Recebeu pah!!!!!!!");
				System.out.println("Bham: " + msg.getView_number());
				// Message pOK = new Message(MessageType.PREPARE_OK,);
				break;
			default:
				break;
			}
		}
	}
}
