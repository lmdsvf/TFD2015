package serverSide;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import message.Message;
import network.Network;

public class ConnecteToServers extends Thread {

	private Network server;
	private Message msg;
	private ServerState state;

	public ConnecteToServers(Message sm, ServerState state) {
		// TODO Auto-generated constructor stub
		this.msg = msg;
		this.state = state;
	}

	@Override
	public void run() {
		Properties p= new Properties();
		try {
			p.load(new FileReader("Configuration.txt"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int i=0;
		for(String ip: state.getConfiguration()){
			if(i!=0){
				Network s= new Network(ip, Integer.parseInt(p.getProperty("PServer")));
				s.send(msg);
			}
			i++;
		}
	}
}
