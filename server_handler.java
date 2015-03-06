//package socketProgramming;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class server_handler implements Runnable{

	public Thread t;
	private static int time_out = 32;
	private static final int authen_arg_num = 5;
	private static final int already_block_by_server = -1;
	private static final int fail_log_in = 3;
	private static final int force_log_off = 1;
	private static final int nothing_to_be_done = 2;
	private static final int success_log_in = 0;
	private  static final int maximum_try = 3;
	private static final int milli_to_second = 1000;
	public static final int block_time = 60;//60 seconds
	public Socket client_socket;
	public static ConcurrentHashMap<String,Long> current_user;
	public static ConcurrentHashMap<String,Long> blocked_by_server;
	public static HashMap<String,String> user_passwd ;
	public static ConcurrentHashMap<String,IP_port_tuple> user_ip_port;
	public static ConcurrentHashMap<String,String> current_user_ip;
	private DataOutputStream socket_output;
	public static ConcurrentHashMap<String,ArrayList<String>> off_line_msg;
	public void start ()
	{
		if (t == null)
		{
			t = new Thread (this);
			t.start ();
		}
	}


	@Override
	public void run() {
		// TODO Auto-generated method stub

		try {

			BufferedReader reader = new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
			socket_output = new DataOutputStream(this.client_socket.getOutputStream());
			String userPlusPass = reader.readLine().trim();

			String [] combo = new String(userPlusPass).split("\\s+");
			
			if(combo.length == authen_arg_num && combo[0].equals("Toauth")){
				//System.out.println("it comes here to handle_authentication_login");
				handle_authentication_login(reader, socket_output, combo);
			}
			if(combo[0].equals("message")){
				String sender = combo[combo.length-1];
				String receiver = combo[1];
				int index = userPlusPass.indexOf(combo[2]);
				int last_index = userPlusPass.lastIndexOf(sender);
				String msg = userPlusPass.substring(index,last_index);
				
				handle_msg(msg,receiver, sender);
			}
			if(combo.length== 2 && combo[0].equals("live")){
				handle_heart_beat(combo[1]);
			}
			

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally{
			try {
				this.client_socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//Authenticate author = new Authenticate(current_user,)

	}

	
	public void handle_heart_beat(String username){
		current_user.put(username, System.currentTimeMillis());
		System.out.println("new timw framw is " + System.currentTimeMillis());
		return;
	}
	
	private boolean client_still_online(String username){
		return ((System.currentTimeMillis()-current_user.get(username))/milli_to_second)>time_out?false:true;
	}
	
	public void  handle_msg(String msg, String receiver,String sender){
		DataOutputStream socket_output = null;
		if (!server_handler.user_passwd.containsKey(receiver)){
			try {
				IP_port_tuple tuple = user_ip_port.get(sender);
				Socket talk_to_client = new Socket(tuple.get_host_address(),tuple.get_port());
				socket_output =
						new DataOutputStream(talk_to_client.getOutputStream());

				socket_output.writeBytes("user does not exist\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			finally{
				try {
					socket_output.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		else{
			IP_port_tuple tuple = user_ip_port.get(receiver);
			String receiver_ip = tuple.get_host_address();
			int receiver_port = tuple.get_port();
			String msg_to_pass = sender+": " + msg;
			if(client_still_online(receiver)){
				try {

					Socket talk_to_client = new Socket(receiver_ip,receiver_port);
					socket_output =
							new DataOutputStream(talk_to_client.getOutputStream());
					socket_output.writeBytes(msg_to_pass+"\n");
					socket_output.close();
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else{
				if (!off_line_msg.containsKey(receiver)){
					off_line_msg.put(receiver, new ArrayList<String>());
				}
				off_line_msg.get(receiver).add(msg_to_pass);
			}
		}
	}

	public int handle_authentication_login(BufferedReader reader,DataOutputStream socket_output,String [] combo){
		String password = combo[2];
		String user = combo[1];
		int client_server_port = Integer.parseInt(combo[3]);
		String client_server_ip = combo[4];
		//boolean valid = false;
		if(blocked_by_server.containsKey(user)){
			if((System.currentTimeMillis()-blocked_by_server.get(user))/milli_to_second > block_time ){
				blocked_by_server.remove(user);
			}
			else{
				try {
					socket_output.writeBytes("you have been blocked by the server\n");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return already_block_by_server;
			}
		}
		
		if(!current_user.containsKey(user) || (System.currentTimeMillis()-(current_user.get(user))/milli_to_second)>time_out ){
			if(handle_login(user,password, reader, socket_output, client_server_port,client_server_ip)){
				check_send_offline_msg(user);
				return success_log_in;
			}
			return fail_log_in;
		}
		else{
			if (checkDifferentIP(user)){
				DataOutputStream output_logoff = null;
				try {
					
					//socket_output.writeBytes("log in from other IP address, force to log off\n");
					IP_port_tuple tuple = user_ip_port.get(user);
					Socket log_off = new Socket(tuple.get_host_address(),tuple.get_port());
					output_logoff =
							new DataOutputStream(log_off.getOutputStream());
					output_logoff.writeBytes("log in from other IP address, force to log off\n");
					
					current_user_ip.put(user,  this.client_socket.getInetAddress().getHostAddress());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//socket_output.flush();
				return force_log_off;
			}

			return nothing_to_be_done;
		}
	}


	public void check_send_offline_msg(String usr){
		if(off_line_msg.containsKey(usr)){
			ArrayList<String> temp = off_line_msg.get(usr);
			IP_port_tuple tuple = user_ip_port.get(usr);
			try {
				Socket talk_socket = new Socket(tuple.get_host_address(),tuple.get_port());
				DataOutputStream socket_output =
						new DataOutputStream(talk_socket.getOutputStream());
				String message ="";
				for (String sentence : temp){
					message += sentence+"\n";
				}
				socket_output.writeBytes(message);
				off_line_msg.remove(usr);
				talk_socket.close();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}


	private boolean handle_login(String user, String password,BufferedReader reader,DataOutputStream socket_output,int client_server_port,String client_server_ip ){
		Authenticate auth;
		int count = 1;
		boolean valid = false;
		
		auth  = new Authenticate(current_user,user,password,server_handler.user_passwd); 
		valid = auth.approved();
		count = check_password(count, reader, auth,  valid, socket_output);
		if(count == maximum_try){
			try {
				socket_output.writeBytes("Invalid Password. Your account has been blocked. Please try again after sometime.\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//socket_output.flush();	
			add_in_blackList (user);
			return false;
		}
		else{
			System.out.println("user successfully added in");
			try {
				add_client_server_port_ip(client_server_ip,client_server_port,user);
				//System.out.println("added in client's ip is " + client_server_ip);
				//System.out.println("added in client's port is " + client_server_port);

				socket_output.writeBytes("Welcome to simple chat server!\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			add_in_user_ip(user);
			return true;
		}
	}
	private void add_client_server_port_ip(String ip_address, int port_num,String username){
		IP_port_tuple temp = new IP_port_tuple(ip_address,port_num);
		server_handler.user_ip_port.put(username, temp);
	}

	private boolean checkDifferentIP (String name){
		return !(this.client_socket.getInetAddress().getHostAddress().equals(current_user_ip.get(name)));
	}


	private void add_in_user_ip(String user ){
		String ip_address = this.client_socket.getInetAddress().getHostAddress();
		current_user_ip.put(user, ip_address);
	}
	
	
	
	private int check_password(int count, BufferedReader reader,Authenticate auth, boolean valid,DataOutputStream socket_output){
		while(count < maximum_try && !valid){
			count++;
			String password = null;
			try {
				socket_output.writeBytes("Invalid Password. Please try again\n");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			//socket_output.flush();
			try {
				password = reader.readLine().trim();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			valid = auth.update_user_password(password);
		}
		return count;
	}

	private void add_in_blackList (String name){
		blocked_by_server.put(name, System.currentTimeMillis());
		return;
	}

	public server_handler(ConcurrentHashMap<String,ArrayList<String>> off_line_msg,ConcurrentHashMap<String,String> current_user_ip, ConcurrentHashMap<String,IP_port_tuple> user_ip_port,HashMap<String,String> user_passwd,ConcurrentHashMap<String,Long> blocked_by_server,ConcurrentHashMap<String,Long> current_user, Socket client_socket){
		this.client_socket = client_socket;
		server_handler.off_line_msg = off_line_msg;
		server_handler.current_user = current_user;
		server_handler.blocked_by_server = blocked_by_server;
		server_handler.user_passwd = user_passwd;
		server_handler.user_ip_port = user_ip_port;
		server_handler.current_user_ip = current_user_ip;

	}

}
