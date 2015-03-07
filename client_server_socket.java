//package socketProgramming;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class client_server_socket implements Runnable{
	public Thread t;
	private ServerSocket server_socket;
	private Socket client_socket;
	private String server_ip;
	private int server_port;
	private boolean running;
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
		while(this.running){
			try {
				this.client_socket = this.server_socket.accept();
				InputStreamReader temp = new InputStreamReader(client_socket.getInputStream());
				BufferedReader socket_input = new BufferedReader(temp);
				String line;
				while ((line = socket_input.readLine())!=null){
					System.out.println(">" + line);
					if(this.client_socket.getInetAddress().getHostAddress().equals(server_ip)&&this.client_socket.getPort() == server_port &&line.equals("log in from other IP address, force to log off")){
						try {
							this.server_socket.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						System.exit(0);
					}
					
				}
				System.out.print("> ");
				temp.close();
				socket_input.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}
		try {
			this.server_socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public client_server_socket ( ServerSocket server_socket, String ip, int port) {
		
		if (server_socket == null)
			System.out.println ("NULL POINTER");
		
		this.server_socket = server_socket;
		this.running = true;
		this.server_ip = new String(ip);
		this.server_port = port;
	}

}
