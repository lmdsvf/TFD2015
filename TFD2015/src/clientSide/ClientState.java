package clientSide;

import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Properties;

public class ClientState {
	public void setRequest_number(int request_number) {
		this.request_number = request_number;
	}

	private String id;
	private String ipAddress;
	private ArrayList<String> configuration;
	private int view_number = 0;
	private int request_number = 0;
	private Properties properties;

	public ClientState() {
		try {
			this.ipAddress = InetAddress.getLocalHost().toString();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		try {
			properties = new Properties();
			properties.load(new FileReader("Configuration.txt"));
			configuration = new ArrayList<String>();
			int NUMBEROFIPS = Integer.parseInt(properties
					.getProperty("NumberOfIps"));
			int clientBasePort = Integer.parseInt(properties.getProperty("PClient"));
			for (int i = 0; i < NUMBEROFIPS; i++) {
				configuration.add(properties.get("IP" + i).toString()+":"+ (clientBasePort +i));
			}
			configuration.sort(null);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public ArrayList<String> getConfiguration() {
		return configuration;
	}

	public void setConfiguration(ArrayList<String> configuration) {
		this.configuration = configuration;
	}

	public int getView_number() {
		return view_number;
	}

	public void setView_number(int view_number) {
		this.view_number = view_number;
	}

	public int getRequest_number() {
		return request_number;
	}

	public Properties getProperties() {
		return properties;
	}

	public void request_number_increment() {
		this.request_number += 1;
	}

	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append("ID:" + id + "\n");
		str.append("IP: " + ipAddress + "\n");
		str.append("View Number: " + view_number + "\n");

		return str.toString();
	}
}
