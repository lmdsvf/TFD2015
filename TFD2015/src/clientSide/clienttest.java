package clientSide;

import network.Network;

public class clienttest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Network net = new Network("192.168.1.3",1234);
		for(int i = 0; i< 100;i++){
			String str = i + " Hello World!";
			byte[] data = str.getBytes();
			net.send(data);
			String receive = new String(net.receive());
			System.out.println(receive);
		}
	}

}
