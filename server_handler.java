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
	private String login_user = null;
	private static int time_out = 32;
	private static int user_not_exist = -2;
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
	public static ConcurrentHashMap<String,ArrayList<String>> user_black_list ;
	public void start ()
	{
		if (t == null)
		{
			t = new Thread (this);
			t.start ();
		}
	}

	/*
	void closeSocket (Socket s) {
		try {
			StackTraceElement[] st	= Thread.currentThread ().getStackTrace ();
			for (int i=0; i<st.length; ++i)
				System.out.println (st[i] + "");

			s.close ();
		} catch (Exception e) {
			System.out.println (e);
		}
	}*/

	@Override
	public void run() {
		// TODO Auto-generated method stub

		try {

			BufferedReader reader = new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
			// channel exclusively used for authentication
			String userPlusPass = reader.readLine().trim();

			String [] combo = new String(userPlusPass).split("\\s+");

			if(combo.length == authen_arg_num && combo[0].equals("Toauth")){
				//System.out.println("it comes here to handle_authentication_login");
				socket_output = new DataOutputStream(this.client_socket.getOutputStream());

				int result = handle_authentication_login(reader, socket_output, combo);
				client_socket.close();

				if(result != success_log_in){
					return;
				}

				System.out.println ("Ready to close");


				broadcast_presence(login_user);
			}
			else if(combo.length == 3 && combo[0].equals("getaddress")){
				socket_output = new DataOutputStream(this.client_socket.getOutputStream());
				
				String response_address = getaddress(combo[2], combo[1]);
				socket_output.writeBytes(response_address);
				socket_output.close();
			}
			else{
				client_socket.close();
				String sender = null;
				String receiver = null;
				String msg = null;
				if(combo.length>= 4 && combo[0].equals("message")){
					sender = combo[combo.length-1];
					receiver = combo[1];
					int receiver_index = userPlusPass.indexOf(receiver, combo[0].length());
					if (user_be_blocked(sender,receiver)){
						handle_block_msg(sender,receiver);
						return;
					}
					int message_index = receiver_index+receiver.length();
					int index = userPlusPass.indexOf(combo[2],message_index);
					int last_index = userPlusPass.lastIndexOf(sender);
					msg = userPlusPass.substring(index,last_index);

					handle_msg(msg,receiver, sender);
				}
				if(combo.length== 2 && combo[0].equals("live")){
					handle_heart_beat(combo[1]);
				}
				if(combo.length == 3 && combo[0].equals("block")){
					block_user(combo[2],combo[1]);
				}
				if(combo.length == 3 && combo[0].equals("unblock")){
					unblock_user(combo[2],combo[1]);
				}
				if(combo.length>= 3 && combo[0].equals("broadcast")){
					sender = combo[combo.length-1];
					int last_index = userPlusPass.lastIndexOf(sender);
					msg = userPlusPass.substring(combo[0].length(), last_index);
					broadcast(sender,msg);
				}
				if(combo.length == 2 &&combo[0].equals("online")){
					print_onine(combo[1]);
				}

			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//Authenticate author = new Authenticate(current_user,)

	}


	private String getaddress(String requestor,String requestee){
		if(user_be_blocked(requestor, requestee)){
			String msg = "you've been blocked by " + requestee;
			return msg;
		}
		IP_port_tuple tuple= user_ip_port.get(requestee);
		String msg=  tuple.get_host_address() +" " + tuple.get_port();
		return msg;

	}
	private void print_onine(String requester){
		Set<String> online_users = current_user.keySet();
		StringBuffer temp_buf = new StringBuffer("users: ");
		for(String receiver :online_users){
			if(!receiver.equals(requester) && client_still_online(receiver)){
				temp_buf.append(receiver);
				temp_buf.append("  ");
			}
		}
		temp_buf.append(" are online\n");
		send_msg_to_user(requester,temp_buf.toString());
	}






	private void broadcast_presence(String login_user){
		String msg = "I'm logged in\n";
		broadcast(login_user,msg);
	}
	private void broadcast(String sender, String msg){
		//client_still_online  ???
		Set<String> online_users = current_user.keySet();
		for(String receiver :online_users){
			if(!receiver.equals(sender) && client_still_online(receiver)){
				handle_msg(msg,receiver,sender);
			}
		}
	}
	private void handle_block_msg(String sender, String receiver){
		String message = "you have been blocked by " +receiver + "\n";
		send_msg_to_user(sender,message);
	}


	private boolean user_be_blocked(String sender, String receiver){
		if(user_black_list.get(receiver)==null || !user_black_list.get(receiver).contains(sender))
			return false;
		return true;
	}

	private void unblock_user(String user_request, String user_to_be_unblock){
		if (user_black_list.get(user_request)==null ||!user_black_list.get(user_request).contains(user_to_be_unblock) )
			return;
		user_black_list.get(user_request).remove(user_to_be_unblock);
	}

	private void block_user(String user_request , String user_to_be_block){
		if(user_black_list.get(user_request) == null)
			user_black_list.put(user_request, new ArrayList<String>());
		user_black_list.get(user_request).add(user_to_be_block);
	}


	public void handle_heart_beat(String username){
		current_user.put(username, System.currentTimeMillis());
		return;
	}

	private boolean client_still_online(String username){
		if(!current_user.containsKey(username))
			return false;
		return ((System.currentTimeMillis()-current_user.get(username))/milli_to_second)>time_out?false:true;
	}

	public void  handle_msg(String msg, String receiver,String sender){
		if (user_be_blocked(sender,receiver)){
			handle_block_msg(sender,receiver);
			return;
		}
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

			String msg_to_pass = sender+": " + msg;
			if(client_still_online(receiver)){
				try {
					IP_port_tuple tuple = user_ip_port.get(receiver);
					String receiver_ip = tuple.get_host_address();
					int receiver_port = tuple.get_port();
					Socket talk_to_client = new Socket(receiver_ip,receiver_port);
					socket_output =
							new DataOutputStream(talk_to_client.getOutputStream());
					socket_output.writeBytes(msg_to_pass+"\n");
					socket_output.close();
					//talk_to_client.close();
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
		login_user = combo[1];
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
			if(!user_passwd.containsKey(user)){
				try {
					socket_output.writeBytes("user name does not exist");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return user_not_exist;
			}
			if(handle_login(user,password, reader, socket_output, client_server_port,client_server_ip)){
				System.out.println(user +"has logged in");
				check_send_offline_msg(user);
				return success_log_in;
			}
			try {
				socket_output.writeBytes("unknown error");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return fail_log_in;
		}
		else{

			if (checkDifferentIP(user)){
				String message = "log in from other IP address, force to log off\n";
				send_msg_to_user(user,message);
				current_user_ip.put(user,  this.client_socket.getInetAddress().getHostAddress());
				return force_log_off;
			}
			try {
				socket_output.writeBytes("unknown error");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return nothing_to_be_done;
		}
	}

	private void send_msg_to_user(String usr,String message){
		IP_port_tuple tuple = user_ip_port.get(usr);
		Socket talk_socket;
		try {
			talk_socket = new Socket(tuple.get_host_address(),tuple.get_port());
			DataOutputStream socket_output =
					new DataOutputStream(talk_socket.getOutputStream());

			socket_output.writeBytes(message);
			talk_socket.close();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	private void check_send_offline_msg(String usr){
		if(off_line_msg.containsKey(usr)){
			ArrayList<String> temp = off_line_msg.get(usr);
			String message ="";
			for (String sentence : temp){
				message += sentence+"\n";
			}

			send_msg_to_user(usr,message);
			off_line_msg.remove(usr);

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

	public server_handler(ConcurrentHashMap<String,ArrayList<String>> user_black_list ,   ConcurrentHashMap<String,ArrayList<String>> off_line_msg,ConcurrentHashMap<String,String> current_user_ip, ConcurrentHashMap<String,IP_port_tuple> user_ip_port,HashMap<String,String> user_passwd,ConcurrentHashMap<String,Long> blocked_by_server,ConcurrentHashMap<String,Long> current_user, Socket client_socket){
		this.user_black_list = user_black_list;
		this.client_socket = client_socket;
		server_handler.off_line_msg = off_line_msg;
		server_handler.current_user = current_user;
		server_handler.blocked_by_server = blocked_by_server;
		server_handler.user_passwd = user_passwd;
		server_handler.user_ip_port = user_ip_port;
		server_handler.current_user_ip = current_user_ip;

	}

}
