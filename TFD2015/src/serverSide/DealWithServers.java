package serverSide;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import message.ServerMessage;

public class DealWithServers extends Thread {
	private ObjectOutputStream out;
	private ObjectInputStream in;

	public DealWithServers(ObjectOutputStream out, ObjectInputStream in) {
		this.out = out;
		this.in = in;
		// TODO Auto-generated constructor stub
	}
	public ObjectOutputStream getOut() {
		return out;
	}
	public ObjectInputStream getIn() {
		return in;
	}

	@Override
	public void run() {
		while (true) {
			try {
				ServerMessage msg = (ServerMessage) in.readObject();
				switch (msg.getType()) {
				case PREPARE:
					System.err.println("Recebeu pah!!!!!!!");
					break;

				default:
					break;
				}

			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

}