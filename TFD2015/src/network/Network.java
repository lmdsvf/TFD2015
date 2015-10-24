package network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Properties;

import message.Message;

//Centro de mensagens entre servidores
public class Network {

	private byte[] receivedData;
	private final int BUFFERSIZE = 1024;
	private int port;
	private InetAddress IPaddress;
	private DatagramSocket socket;
	private int timeout;

	// client constructor
	public Network(String address, int port) {
		receivedData = new byte[BUFFERSIZE];
		this.port = port;
		getProperties();

		try {
			byte [] b = new byte[] {(byte)192,(byte)168,(byte)1,(byte)3};
			IPaddress = InetAddress.getByAddress(b);//InetAddress.getByName(address);
			System.out.println(IPaddress.getHostAddress());
			socket = new DatagramSocket();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	// server constructor
	public Network(int port) {
		receivedData = new byte[BUFFERSIZE];
		this.port = port;
		getProperties();

		try {
			socket = new DatagramSocket(port);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	private void getProperties() {
		Properties p = new Properties();
		try {
			p.load(new FileReader("Configuration.txt"));
			this.timeout = Integer.parseInt(p.getProperty("T"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void send(Message data) {
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			ObjectOutputStream os = new ObjectOutputStream(outputStream);
			os.writeObject(data);
			byte[] dataSend = outputStream.toByteArray();
			DatagramPacket sendPacket = new DatagramPacket(dataSend,
					dataSend.length, IPaddress, port);
			socket.send(sendPacket);
			outputStream.close();
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Message receive() {
		System.out.println("Receiving data...");
		Message returnObject = null;
		DatagramPacket receivedPacket = new DatagramPacket(receivedData,
				receivedData.length);
		try {
			socket.setSoTimeout(timeout);
			socket.receive(receivedPacket);
			ByteArrayInputStream in = new ByteArrayInputStream(
					receivedPacket.getData());
			ObjectInputStream is = new ObjectInputStream(in);

			returnObject = (Message) is.readObject();
			in.close();
			is.close();
		} catch (SocketTimeoutException e) {
			return null;
		} catch (IOException e) {

		} catch (ClassNotFoundException e) {
			System.err.println("Erro no tipo de mensagem Message!");
			e.printStackTrace();
		}

		if (IPaddress == null || port == 0) {
			IPaddress = receivedPacket.getAddress();
			port = receivedPacket.getPort();
		}
		return returnObject;
	}

	public InetAddress getIP() {
		return IPaddress;
	}

	public int getPort() {
		return port;
	}

}
