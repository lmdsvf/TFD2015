package serverSide;

import java.util.ArrayList;
import java.util.HashMap;

import message.ClientMessage;
import clientSide.Client;

public class ServerState {
	private ArrayList<String> configuration;
	private int replica_number;
	private int view_number = 0;
	private Status status;
	private int op_number = 0;
	private ArrayList<ClientMessage> log; //ver depois o tipo...
	private int commit_number;
	private HashMap<Client, Tuple> clientTable;
	
	public ServerState(){
		configuration = new ArrayList<String>();
		configuration.add("193.137.143.40");
		configuration.add("193.137.142.16");
		//configuration.add("10.101.149.41");
		replica_number = configuration.indexOf("193.137.143.40");
		status = Status.NORMAL;
		log = new ArrayList<ClientMessage>();
		clientTable = new HashMap<Client, Tuple>(); 
	}
}
