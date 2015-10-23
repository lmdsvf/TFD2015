package serverSide;

import network.Network;

public class servertest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Network listener = new Network(1234);
		while(true){
			String receive = new String(listener.receive());
			System.out.println(receive);
			listener.send(receive.toUpperCase().getBytes());
		}
	}

}
