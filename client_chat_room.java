//package socketProgramming;
import java.net.*;
import java.util.HashMap;
import java.io.*;
public class client_chat_room implements Runnable{
	public Thread t;
	private Socket client_sock;
	private DataOutputStream socket_output;
	private BufferedReader user_input;
	private BufferedReader server_response;
	private String userName;
	private String ip_address;
	private int port_num;
	private boolean signal =false;
	private HashMap<String,IP_port_tuple> user_ip_port;
	public client_chat_room(String ip_address, int port_num, String userName){
		this.userName = userName;
		this.ip_address = new String(ip_address);
		this.port_num = port_num;
		this.user_ip_port = new HashMap<String,IP_port_tuple>();
	}

	public void handleInput(){		
		//if（user_input == null)
		user_input = new BufferedReader(new InputStreamReader(System.in));


		while (true ){
			try {
				String stdin = null;

				stdin = this.user_input.readLine();

				String stdin_sub = new String(stdin);
				String [] temp_sub = stdin_sub.split("\\s+");





				System.out.print(">");

				while(!sanityCheck(stdin)){
					if(!stdin.trim().isEmpty() && !signal)
						System.out.println("can not understand your command");
					System.out.print("> ");
					stdin = this.user_input.readLine();
				}
				if(stdin.trim().equals("logout")){
					System.exit(0);
				}

				if(temp_sub[0].equals("private")){
					String receiver = temp_sub[1];
					int receiver_index = stdin.indexOf(receiver, temp_sub[0].length());
					int message_index = receiver_index+receiver.length();
					int index = stdin.indexOf(temp_sub[2],message_index);
					String msg = stdin.substring(index);
					if(this.user_ip_port.containsKey(receiver)){
						String ip_address_client = this.user_ip_port.get(receiver).get_host_address();
						int port_num_client = this.user_ip_port.get(receiver).get_port();
						this.client_sock = new Socket(ip_address_client,port_num_client);
						socket_output = 
								new DataOutputStream(client_sock.getOutputStream());
						
						server_response = new BufferedReader(new InputStreamReader(client_sock.getInputStream()));
						socket_output.writeBytes(userName+": " + msg);
						socket_output.close();
					}
					else{
						System.out.println("you have to getaddress first");
						continue;
					}
					//System.out.println("write out message is " +stdin );

				}
				else{
					this.client_sock = new Socket(ip_address,port_num);
					socket_output = 
							new DataOutputStream(client_sock.getOutputStream());
					if(temp_sub[0].equals("getaddress")){
						server_response = new BufferedReader(new InputStreamReader(client_sock.getInputStream()));
						String receiver = temp_sub[1];
						this.socket_output.writeBytes(stdin+" "+ userName+"\n");
						String server_address_response = server_response.readLine();
						if(server_address_response.startsWith("you've been blocked by")){
							System.out.println(server_address_response);
						}
						else{
							String[] split = new String(server_address_response).split("\\s+");
							String ip = split[0];
							int port = Integer.parseInt(split[1]);
							user_ip_port.put(receiver, new IP_port_tuple(ip,port));
							System.out.println(receiver + "'s ip is" + ip +"and its port number is" + port);
						}
						this.socket_output.close();
					}
					else{
						this.socket_output.writeBytes(stdin+" "+ userName+"\n");
						this.client_sock.close();
					}
				}
			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		}
	}


	public boolean sanityCheck(String stdin){

		String input = new String(stdin);
		String [] combo = input.split("\\s+");
		if(combo.length >= 3 && combo[0].equals("message"))
			return true;
		if(combo.length >= 2 && combo[0].equals("broadcast"))
			return true;
		if(combo.length == 1 && combo[0].equals("online"))
			return true;
		if(combo.length == 2 && combo[0].equals("block")){
			if(combo[1].equals(userName)){
				signal = true;
				System.out.println("you can not block yourself");
				return false;
			}
			return true;
		}
		if(combo.length==2 && combo[0].equals("unblock"))
			return true;
		if(combo.length == 1 && combo[0].equals("logout"))
			return true;
		if(combo.length == 2 && combo[0].equals("getaddress"))
			return true;
		if(combo.length == 3 && combo[0].equals("private"))
			return true;
		return false;
	}

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
		handleInput();
	}
}
