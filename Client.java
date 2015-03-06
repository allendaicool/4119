//package socketProgramming;
import java.net.*;
import java.io.*;

public class Client {

	private int port_num;
	private String userName;
	public  int client_port_num;
	public  String client_serverSocket_ip;
	private boolean force_log_off = false;
	private String ip_address;
	private Socket  client_sock= null;
	// another thread to check input
	public ServerSocket server_socket = null;;
	public static final String already_block_by_server = "you have been blocked by the server";
	public static final String block_by_server = "Invalid Password. Your account has been blocked. Please try again after sometime.";
	public static final String welcome_message = "Welcome to simple chat server!";
	public static final String force_log_off_message = "log in from other IP address, force to log off";


	public Client(int port_num, String ip_address){
		this.port_num = port_num;
		this.ip_address = new String(ip_address);
	}
	
	
	public static void main(String[] args) throws Exception{
		if(args.length != 2){
			throw new Exception("invalid number of arguments");
		}
		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
		Client user = new Client(Integer.parseInt(args[1]),args[0]);

		String ip = args[0];
		String port = args[1];
		
		user.set_up_client_server(ip,port, user);
		client_chat_room group_chat = new client_chat_room(ip,Integer.valueOf(port),user.userName);
		client_server_socket client_serv_socket = new client_server_socket(user.server_socket, ip,Integer.parseInt(port));
		client_heart_beat heartbeat = new client_heart_beat(ip,Integer.parseInt(port),user.userName);

		group_chat.start();
		client_serv_socket.start();
		heartbeat.start();
		
		group_chat.t.join();
		client_serv_socket.t.join();
		heartbeat.t.join();
		
	}


	private  void set_up_client_server (String arg0, String arg1,Client user) {
		
		//user = new Client (Integer.parseInt(arg1),arg0);
		
		InetAddress local = null;
		
		try {
			user.server_socket = new ServerSocket(0);
			user.client_port_num = user.server_socket.getLocalPort();
			local = InetAddress.getLocalHost();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		user.client_serverSocket_ip = local.getHostAddress();
		user.authentication(user.client_serverSocket_ip,user.client_port_num);
	}



	private void authentication(String client_ip_address, int client_port_num){
		
		InputStreamReader temp = null;
		BufferedReader socket_input = null;
		DataOutputStream socket_output = null;
		BufferedReader stdin = null;
		try {
			client_sock = new Socket(this.ip_address,this.port_num,null,0);

			temp = new InputStreamReader(client_sock.getInputStream());
			socket_input = new BufferedReader(temp);

			socket_output =
					new DataOutputStream(client_sock.getOutputStream());
			stdin = new BufferedReader(new InputStreamReader(System.in));

			System.out.print("Username: ");
			userName = stdin.readLine().trim();
			System.out.println();

			System.out.print("Password:");
			String password = stdin.readLine().trim();
			
			
			String writeout = "Toauth"+" "+userName+" "+ password+" " + String.valueOf(client_port_num) + " " + client_ip_address;
			socket_output.writeBytes(writeout+"\n");
			System.out.println("Toauth"+" "+userName+" "+ password);
			//socket_output.flush();
			String line = socket_input.readLine();
			System.out.println("come here");
			if (line.equals(already_block_by_server)){
				System.out.println(line);
				System.exit(0);
			}
			while(!line.equals(block_by_server) &&!line.equals(welcome_message) ){
				System.out.println(line);
				System.out.print("Password:");
				password = stdin.readLine().trim();
				socket_output.writeBytes(password+"\n");
				//socket_output.flush();
				line = socket_input.readLine();
			}
			
			if (line.equals(block_by_server) ){
				System.out.println(line);
				System.exit(0); 
			}
			if(line.equals(welcome_message)){
				System.out.println(line);
			}

		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally{
			try {
				client_sock.close();
				temp.close();
				socket_input.close();
				socket_output.close();
				
				//@lfred
				//stdin.close();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}







}
