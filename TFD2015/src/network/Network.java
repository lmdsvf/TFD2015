package network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Network {
	
	private byte[] receivedData;
	private final int BUFFERSIZE = 1024;
	private int port;
	private InetAddress IPaddress;
	private DatagramSocket socket;
	
	// client constructor
	public Network(String address, int port){
		receivedData = new byte[BUFFERSIZE];
		this.port = port;
		
		try {
			IPaddress = InetAddress.getByName(address);
			socket = new DatagramSocket();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	// server constructor
	public Network(int port){
		receivedData = new byte[BUFFERSIZE];
		this.port = port;
		
		try {
			socket = new DatagramSocket(port);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	public void send(byte[] data){
		try {
			DatagramPacket sendPacket = new DatagramPacket(data, data.length, IPaddress, port);
			socket.send(sendPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public byte[] receive(){
		System.out.println("Receiving data...");
		DatagramPacket receivedPacket = new DatagramPacket(receivedData, receivedData.length);
		try {
			socket.receive(receivedPacket);
		} catch (IOException e) {
			return null;
		}
		
		if(IPaddress == null || port == 0){
			IPaddress = receivedPacket.getAddress();
			port = receivedPacket.getPort();
		}
		
		System.out.println("IP: " + IPaddress.toString() + " data: " + new String(receivedData));
		
		return receivedData;
	}
}
