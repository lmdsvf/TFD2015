package network;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class Network {
	
	private byte[] sendData;
	private byte[] receiveData;
	private final int BUFFERSIZE = 1024; 
	private int port;
	private InetAddress address;
	private DataGram
	
	public Network(String address, int port){
		sendData = new byte[BUFFERSIZE];
		receiveData = new byte[BUFFERSIZE];
		address = new InetAddress(address,port);
	}
	
	public void send(){
		
	}
	
	public void receive(){
		
	}
}
