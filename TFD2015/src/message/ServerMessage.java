package message;

import java.io.Serializable;
import java.util.ArrayList;

public class ServerMessage implements Serializable {
	public MessageType getType() {
		return type;
	}

	public void setType(MessageType type) {
		this.type = type;
	}

	public int getView_number() {
		return view_number;
	}

	public void setView_number(int view_number) {
		this.view_number = view_number;
	}

	public ClientMessage getClienteMessage() {
		return clienteMessage;
	}

	public void setClienteMessage(ClientMessage clienteMessage) {
		this.clienteMessage = clienteMessage;
	}

	public int getOp_number() {
		return op_number;
	}

	public void setOp_number(int op_number) {
		this.op_number = op_number;
	}

	public int getCommit_Number() {
		return commit_Number;
	}

	public void setCommit_Number(int commit_Number) {
		this.commit_Number = commit_Number;
	}

	public int getReplica_number() {
		return replica_number;
	}

	public void setReplica_number(int replica_number) {
		this.replica_number = replica_number;
	}

	public int getOldView_number() {
		return oldView_number;
	}

	public void setOldView_number(int oldView_number) {
		this.oldView_number = oldView_number;
	}

	public ArrayList<ClientMessage> getLog() {
		return log;
	}

	public void setLog(ArrayList<ClientMessage> log) {
		this.log = log;
	}

	public int getNewView_number() {
		return newView_number;
	}

	public void setNewView_number(int newView_number) {
		this.newView_number = newView_number;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public String getIdPrimaria() {
		return idPrimaria;
	}

	public void setIdPrimaria(String idPrimaria) {
		this.idPrimaria = idPrimaria;
	}

	private MessageType type;
	private int view_number;
	private ClientMessage clienteMessage;
	private int op_number;
	private int commit_Number;
	private int replica_number;
	private int oldView_number;
	private ArrayList<ClientMessage> log;
	private int newView_number;
	private int x;
	private String idPrimaria;

	public ServerMessage(MessageType type, int view_number, int commit_Number) {// commit
																				// e
																				// startview
		super();
		this.type = type;
		this.view_number = view_number;
		this.commit_Number = commit_Number;
	}

	public ServerMessage(MessageType type, int view_number, int op_number,
			int replica_number) {// prepareOk
		super();
		this.type = type;
		this.view_number = view_number;
		this.op_number = op_number;
		this.replica_number = replica_number;
	}

	public ServerMessage(MessageType type, int view_number,
			ClientMessage clienteMessage, int op_number, int commit_Number) {// prepare
		super();
		this.type = type;
		this.view_number = view_number;
		this.clienteMessage = clienteMessage;
		this.op_number = op_number;
		this.commit_Number = commit_Number;
	}

}
