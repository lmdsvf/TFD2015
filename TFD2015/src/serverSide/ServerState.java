package serverSide;

import java.io.FileReader;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import network.Network;
import message.Message;

public class ServerState {
	private ArrayList<String> configuration;
	private int replica_number;
	private int view_number = 0;
	private Status status;
	private int op_number = 0;
	private ArrayList<Message> log; // ver depois o tipo...
	private int commit_number = 0;
	private String usingIp;

	private HashMap<String, Tuple> clientTable;

	private Properties properties;
	public static final int NUMBEROFIPS = 3;

	public ServerState() {
		try {
			properties = new Properties();
			properties.load(new FileReader("Configuration.txt"));
			configuration = new ArrayList<String>();
			for (int i = 0; i < NUMBEROFIPS; i++) {
				configuration.add(properties.get("IP" + i).toString());
			}
			System.out.println("Getting my ip!");
			usingIp = null;
			for (String ip : configuration) {
				for (String my_ip : Network.getAllIps()) {
					System.out.println("Testing " + ip + " with " + my_ip);
					if (ip.equals(my_ip)) {
						System.out.println("got it");
						usingIp = ip;
						break;
					}
				}
				if (usingIp != null)
					break;
			}
			System.out.println("My ip: " + usingIp);
			replica_number = configuration.indexOf(usingIp);
			status = Status.NORMAL;
			log = new ArrayList<Message>();
			clientTable = new HashMap<String, Tuple>();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public String getUsingIp() {
		return usingIp;
	}

	public ArrayList<String> getConfiguration() {
		return configuration;
	}

	public Properties getProperties() {
		return properties;
	}

	public void setConfiguration(ArrayList<String> configuration) {
		this.configuration = configuration;
	}

	public int getReplica_number() {
		return replica_number;
	}

	public void setReplica_number(int replica_number) {
		this.replica_number = replica_number;
	}

	public int getView_number() {
		return view_number;
	}

	public void setView_number(int view_number) {
		this.view_number = view_number;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public int getOp_number() {
		return op_number;
	}

	public void setOp_number(int op_number) {
		this.op_number = op_number;
	}

	public ArrayList<Message> getLog() {
		return log;
	}

	public void setLog(ArrayList<Message> log) {
		this.log = log;
	}

	public int getCommit_number() {
		return commit_number;
	}

	public void setCommit_number(int commit_number) {
		this.commit_number = commit_number;
	}

	public HashMap<String, Tuple> getClientTable() {
		return clientTable;
	}

	public void setClientTable(HashMap<String, Tuple> clientTable) {
		this.clientTable = clientTable;
	}

	public void op_number_increment() {
		this.op_number += 1;
	}

	public void commit_number_increment() {
		this.commit_number += 1;
	}

	public void addMessageToLog(Message newMessage) {
		this.log.add(newMessage);
	}
}
