package message;

import java.io.Serializable;

public class ClientMessage implements Serializable{
	private MessageType type;
	private int view_number;
	private Operation op;
	private int c;
	private int s;
	private int v;
	private int x;

	public ClientMessage(MessageType type, Operation op, int c, int s) {
		this.type = type;
		this.op = op;
		this.c = c;
		this.s = s;
	}
	
	public ClientMessage(MessageType type, int v, int s, int x){
		this.type = type;
		this.v = v;
		this.s = s;
		this.x = x;
	}

	public MessageType getType() {
		return type;
	}

	public int getView_number() {
		return view_number;
	}

	public Operation getOp() {
		return op;
	}

	public int getC() {
		return c;
	}

	public int getS() {
		return s;
	}

	public int getV() {
		return v;
	}

	public int getX() {
		return x;
	}
	
	
}
