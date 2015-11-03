package serverSide;

public class Tuple {
	private int request_number;
	private int executed;
	private String result;

	public Tuple(int op, String r) {
		this.request_number = op;
		this.result = r;
	}

	public int getRequest_number() {
		return request_number;
	}

	public void setRequest_number(int op_number) {
		this.request_number = op_number;
	}

	public int getExecuted() {
		return executed;
	}

	public void setExecuted(int executed) {
		this.executed = executed;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

}
