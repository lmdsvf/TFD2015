package serverSide;

import message.Message;
import message.MessageType;
import network.Network;

public class servertest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Network listener = new Network(1234);
		if(!(listener == null)){
			
			while(true){
				Message msg = (Message)listener.receive();
				switch (msg.getType()) {
				case REQUEST:
					Message reply= new Message(MessageType.REPLY, 20, 20, "Vai tu também!");
					listener.send(reply);
					break;
				default:
					break;
				}
			}
		}
	}

}
