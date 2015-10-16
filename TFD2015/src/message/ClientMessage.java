package message;

public class ClientMessage {
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
	
}
