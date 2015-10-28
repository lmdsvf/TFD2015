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
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Properties;

import message.Message;

//Centro de mensagens entre servidores
public class Network {

	private byte[] receivedData;
	private final int BUFFERSIZE = 1024;
	private InetAddress IPaddress;
	private int port;
	private DatagramSocket socket;
	private int timeout;

	// client constructor
	public Network(String address, int port) {
		receivedData = new byte[BUFFERSIZE];
		this.port = port;
		getProperties();
		try {
			IPaddress = InetAddress.getByName(address);
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

	public void send(Message data, InetAddress ip) {
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			ObjectOutputStream os = new ObjectOutputStream(outputStream);
			os.writeObject(data);
			byte[] dataSend = outputStream.toByteArray();
			DatagramPacket sendPacket = new DatagramPacket(dataSend,
					dataSend.length, ip, port);
			System.out.println("Sending to " + ip.getHostAddress() + ":" + port);
			socket.send(sendPacket);
			outputStream.close();
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public DatagramPacket receive() {
		System.out.println("\n\nReceiving data...");
//		Message returnObject = null;
		DatagramPacket receivedPacket = new DatagramPacket(receivedData,
				receivedData.length);
		try {
			socket.setSoTimeout(timeout);
			socket.receive(receivedPacket);
//			System.out.println(socket.getLocalPort() + " - " + socket.getLocalAddress());
//			ByteArrayInputStream in = new ByteArrayInputStream(
//					receivedPacket.getData());
//			ObjectInputStream is = new ObjectInputStream(in);
//			returnObject = (Message) is.readObject();
//			in.close();
//			is.close();
		} catch (SocketTimeoutException e) {
//			//e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
//		} catch (ClassNotFoundException e) {
//			System.err.println("Erro no tipo de mensagem Message!");
//			e.printStackTrace();
		}
//
//		if (IPaddress == null || port == 0) {
//			IPaddress = receivedPacket.getAddress();
//		}
		return receivedPacket;
	}

	public static Message networkToMessage(DatagramPacket data){
		ByteArrayInputStream in = new ByteArrayInputStream(data.getData());
		ObjectInputStream is;
		Message message = null;
		try {
			is = new ObjectInputStream(in);
			message = (Message) is.readObject();
			in.close();
			is.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return message;
	}
	
	public DatagramSocket getSocket() {
		return socket;
	}

	public void setSocket(DatagramSocket socket) {
		this.socket = socket;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public InetAddress getIP() {
		return IPaddress;
	}

	public int getPort() {
		return port;
	}

}
