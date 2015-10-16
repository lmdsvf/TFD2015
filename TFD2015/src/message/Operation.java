package message;

import clientSide.OperationType;

public class Operation {
	
	private int a;
	private int b;
	private OperationType type;
	
	public Operation(int a, int b, OperationType type) {
		this.a = a;
		this.b = b;
		this.type = type;
	}
	
	public int getA() {
		return a;
	}
	
	public int getB() {
		return b;
	}
	
	public OperationType getType() {
		return type;
	}
	
	private int execute(){
		
		return 0;
	}
	
}