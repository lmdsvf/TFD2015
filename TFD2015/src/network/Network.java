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
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;

import message.Message;

//Centro de mensagens entre servidores
public class Network {

	private byte[] receivedData;
	private final int BUFFERSIZE = 10024;
	private InetAddress IPaddress;
	private int port;
	private DatagramSocket socket;
	private int timeout;
	private int timeoutViewChange;

	// client constructor
	public Network(String address, int port, int portMy) {
		receivedData = new byte[BUFFERSIZE];
		this.port = port;
		getProperties();
		try {
			IPaddress = InetAddress.getByName(address);
			socket = new DatagramSocket(portMy);
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
			this.timeoutViewChange = Integer.parseInt(p
					.getProperty("TViewChange"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int getTimeoutViewChange() {
		return timeoutViewChange;
	}

	public void broadcastToServers(Message m, ArrayList<String> addresses,
			String usingAddress, boolean sendToMySelf) {
		try {
			for (String address : addresses) {
				if (sendToMySelf) {
					String[] addr = address.split(":");
					send(m, InetAddress.getByName(addr[0]),
							Integer.parseInt(addr[1]));
				} else {
					if (!usingAddress.equals(address)) {
						String[] addr = address.split(":");
						send(m, InetAddress.getByName(addr[0]),
								Integer.parseInt(addr[1]));
					}

				}
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	public void send(Message data, InetAddress ip, int portDestination) {
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			ObjectOutputStream os = new ObjectOutputStream(outputStream);
			os.writeObject(data);
			byte[] dataSend = outputStream.toByteArray();
			DatagramPacket sendPacket = new DatagramPacket(dataSend,
					dataSend.length, ip, portDestination);
			System.out.println("Sending to " + ip.getHostAddress() + ":"
					+ portDestination);
			socket.send(sendPacket);
			outputStream.close();
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public DatagramPacket receive(int timeout) {
		System.out.println("\n\nReceiving data...");
		DatagramPacket receivedPacket = new DatagramPacket(receivedData,
				receivedData.length);
		try {
			socket.setSoTimeout(timeout);
			socket.receive(receivedPacket);
		} catch (SocketTimeoutException e) {
			return null;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return receivedPacket;
	}

	public static Message networkToMessage(DatagramPacket data) {
		ByteArrayInputStream in = new ByteArrayInputStream(data.getData());
		ObjectInputStream is;
		Message message = null;
		try {
			is = new ObjectInputStream(in);
			message = (Message) is.readObject();
			in.close();
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return message;
	}

	// gets all the ips of the current machine
	public static ArrayList<String> getAllIps() {
		ArrayList<String> allAddresses = new ArrayList<String>();

		Enumeration<NetworkInterface> nets = null;
		try {
			nets = NetworkInterface.getNetworkInterfaces();
		} catch (SocketException e) {
			return null;
		}

		if (nets == null)
			return null;

		for (NetworkInterface netint : Collections.list(nets)) {
			Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
			for (InetAddress inetAddress : Collections.list(inetAddresses)) {
				allAddresses.add(inetAddress.getHostAddress());
			}
		}

		return allAddresses;
	}

	public InetAddress getLocalIP() {
		return socket.getLocalAddress();
	}

	public int getLocalPort() {
		return socket.getLocalPort();
	}

	public void closePort() {
		socket.close();
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
