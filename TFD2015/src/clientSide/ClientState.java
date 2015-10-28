package clientSide;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class ClientState {
	private int id;
	private String ipAddress; 
	private ArrayList<String> configuration;
	private int view_number = 0;
	
	public ClientState(int id) {
		this.id = id;
		try {
			this.ipAddress = InetAddress.getLocalHost().toString();
			System.out.println();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
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
	
	public String toString(){
		StringBuilder str = new StringBuilder();
		str.append("ID: " + id + "\n");
		str.append("IP: " + ipAddress + "\n");
		str.append("View Number: " + view_number + "\n");
		
		return str.toString();
	}
}
