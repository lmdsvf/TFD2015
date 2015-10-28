package message;

import java.io.Serializable;

public class Message implements Serializable {
	private MessageType type;
	private int view_number;
	private Message client_Message;
	private int operation_number;
	private String backUp_Ip;
	private int commit_Number;
	private String result;
	private String operation;
	private String client_Id;// Tem que ser uma String pois o id terá que ser o
								// IP pois é impossivel fazer ID tipo 1 ou 2 de
								// máquinas que nem sabemos que nos vão
								// contactar
	private int request_Number;

	// Request Message
	public Message(MessageType request, String op, String c, int s) {
		this.type = request;
		this.operation = op;
		this.client_Id = c;
		this.request_Number = s;
	}

	// Reply Message
	public Message(MessageType reply, int i, int i2, String string) {
		this.type = reply;
		this.view_number = i;
		this.request_Number = i2;
		this.result = string;
	}

	// Prepare Message
	public Message(MessageType prepare, int i, Message msg, int j, int k) {
		// TODO Auto-generated constructor stub
		this.view_number=i;
	}

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

	public Message getClient_Message() {
		return client_Message;
	}

	public void setClient_Message(Message client_Message) {
		this.client_Message = client_Message;
	}

	public int getOperation_number() {
		return operation_number;
	}

	public void setOperation_number(int operation_number) {
		this.operation_number = operation_number;
	}

	public String getBackUp_Ip() {
		return backUp_Ip;
	}

	public void setBackUp_Ip(String backUp_Ip) {
		this.backUp_Ip = backUp_Ip;
	}

	public int getCommit_Number() {
		return commit_Number;
	}

	public void setCommit_Number(int commit_Number) {
		this.commit_Number = commit_Number;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

	public String getClient_Id() {
		return client_Id;
	}

	public void setClient_Id(String client_Id) {
		this.client_Id = client_Id;
	}

	public int getRequest_Number() {
		return request_Number;
	}

	public void setRequest_Number(int request_Number) {
		this.request_Number = request_Number;
	}

	public String toString(){
		StringBuilder str = new StringBuilder();
		str.append(type);
		return str.toString();
		
	}
}
