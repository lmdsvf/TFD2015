package serverSide;

import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import message.ClientMessage;
import clientSide.Client;

public class ServerState {
	private ArrayList<String> configuration;
	private int replica_number;
	private int view_number = 0;
	private Status status;
	private int op_number = 0;
	private ArrayList<ClientMessage> log; // ver depois o tipo...
	private int commit_number;
	private HashMap<Client, Tuple> clientTable;
	private Properties properties;
	private static final int NUMBEROFIPS = 5;

	public ServerState() {
		try {
			properties = new Properties();
			properties.load(new FileReader("Configuration.txt"));
			configuration = new ArrayList<String>();
			for (int i = 0; i < NUMBEROFIPS; i++) {
				configuration.add(properties.get("IP" + i).toString());
			}
			configuration.sort(null);
			replica_number = configuration.indexOf(InetAddress.getLocalHost()
					.getHostAddress().toString());
			status = Status.NORMAL;
			log = new ArrayList<ClientMessage>();
			clientTable = new HashMap<Client, Tuple>();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public ArrayList<String> getConfiguration() {
		return configuration;
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

	public ArrayList<ClientMessage> getLog() {
		return log;
	}

	public void setLog(ArrayList<ClientMessage> log) {
		this.log = log;
	}

	public int getCommit_number() {
		return commit_number;
	}

	public void setCommit_number(int commit_number) {
		this.commit_number = commit_number;
	}

	public HashMap<Client, Tuple> getClientTable() {
		return clientTable;
	}

	public void setClientTable(HashMap<Client, Tuple> clientTable) {
		this.clientTable = clientTable;
	}
}
