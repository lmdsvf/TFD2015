package message;

import java.io.Serializable;
import java.util.ArrayList;

public class Message implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private MessageType type;
	private int view_number;
	private Message client_Message;
	private int operation_number;
	private String backUp_Ip;
	private int commit_Number;
	private String result;
	private String operation;
	private String client_Id;
	private int request_Number;
	private ArrayList<Message> log;
	private int lastest_Normal_View_Change;

	// Commit Message
	public Message(MessageType commit, int view_number, int commit_number) {
		this.type = commit;
		this.view_number = view_number;
		this.commit_Number = commit_number;
	}

	// Request Message
	public Message(MessageType request, String op, String c, int s) {
		this.type = request;
		this.operation = op;
		this.client_Id = c;
		this.request_Number = s;
	}

	// Reply Message e prepareOk
	public Message(MessageType reply, int i, int i2, String string) {
		this.type = reply;
		switch (reply) {
		case REPLY:
			this.view_number = i;
			this.request_Number = i2;
			this.result = string;
			break;
		case PREPARE_OK:
			this.view_number = i;
			this.request_Number = i2;
			this.backUp_Ip = string;
			break;
		default:
			break;
		}
	}

	// Prepare Message
	public Message(MessageType prepare, int i, Message msg, int j, int k) {
		this.type = prepare;
		this.view_number = i;
		this.client_Message = msg;
		this.operation_number = j;
		this.commit_Number = k;
	}

	/******************* VIEW CHANGE MESSAGES *******************/

	// Start View Change
	public Message(MessageType startViewChange, int view_number, String backUpIp) {
		this.type = startViewChange;
		this.view_number = view_number;
		this.backUp_Ip = backUpIp;
	}

	// DO VIEW CHANGE
	public Message(MessageType doViewChange, int view_number,
			ArrayList<Message> log, int lastest_normal_view_change,
			int op_number, int commited_number, String backUpIp) {
		this.type = doViewChange;
		this.view_number = view_number;
		this.log = log;
		this.lastest_Normal_View_Change = lastest_normal_view_change;
		this.operation_number = op_number;
		this.commit_Number = commited_number;
		this.backUp_Ip = backUpIp;
	}

	// Start View
	public Message(MessageType start_View, int view_number,
			ArrayList<Message> log, int op_number, int commited_number) {
		this.type = start_View;
		this.view_number = view_number;
		this.log = log;
		this.operation_number = op_number;
		this.commit_Number = commited_number;
	}

	/********** GETTERS **********/

	public int getLastest_Normal_View_Change() {
		return lastest_Normal_View_Change;
	}

	public ArrayList<Message> getLog() {
		return log;
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

	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append(type);
		return str.toString();

	}
}
