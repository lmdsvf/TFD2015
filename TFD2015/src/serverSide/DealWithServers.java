package serverSide;

import java.net.DatagramPacket;

import message.Message;
import network.Network;

public class DealWithServers extends Thread {
	private Network rawData;
	private Message msg;

	public DealWithServers(DatagramPacket data) {
		this.rawData=data;
		this.msg=Network.networkToMessage(data);
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
				// Message pOK = new Message(MessageType.PREPARE_OK,);
				break;

			default:
				break;
			}

		}
	}

}