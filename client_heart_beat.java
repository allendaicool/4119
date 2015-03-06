//package socketProgramming;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class client_heart_beat implements Runnable{
	private Socket client_heart_socket;
	public Thread t;
	private String ip_address;
	private int port_num;
	private String userName;
	private boolean running = true;
	public void start ()
	{
		if (t == null)
		{
			t = new Thread (this);
			t.start ();
		}
	}
	public void kill_thread(){
		this.running = false;
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		DataOutputStream socket_output = null;
		
		while(this.running){
			try {
				
				client_heart_socket = new Socket(ip_address,port_num);
				socket_output =
						new DataOutputStream(client_heart_socket.getOutputStream());
				socket_output.writeBytes("live"+" "+ this.userName+"\n");
				client_heart_socket.close();

				Thread.sleep(30000);

			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}


	}

	public client_heart_beat(String ip_address, int port_num, String username){
		this.ip_address = ip_address;
		this.port_num = port_num;
		this.userName = username;
	}

}
