//package socketProgramming;
import java.net.*;
import java.io.*;
public class client_chat_room implements Runnable{
	public Thread t;
	private Socket client_sock;
	private DataOutputStream socket_output;
	private BufferedReader user_input;
	private String userName;
	private String ip_address;
	private int port_num;
	
	public client_chat_room(String ip_address, int port_num, String userName){
		this.userName = userName;
		this.ip_address = new String(ip_address);
		this.port_num = port_num;
	}

	public void handleInput(){		
			//if（user_input == null)
			user_input = new BufferedReader(new InputStreamReader(System.in));
			

		while (true ){
			try {
				this.client_sock = new Socket(ip_address,port_num);
				socket_output = 
						new DataOutputStream(client_sock.getOutputStream());
				System.out.print(">");
				String stdin = null;
				stdin = this.user_input.readLine();
				while(!sanityCheck(stdin)){
					System.out.println("can not understand your command");
					stdin = this.user_input.readLine();
				}
				if(stdin.equals("logout")){
					System.exit(0);
				}
				System.out.println("write out message is " +stdin );
				
				this.socket_output.writeBytes(stdin+" "+ userName+"\n");
				this.client_sock.close();
			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			finally{
				
				System.out.println("get here-------");
			}
		}
	}
	
	public boolean sanityCheck(String stdin){
		
		String input = new String(stdin);
		String [] combo = input.split("\\s+");
		if(combo.length >= 3 && combo[0].equals("message"))
			return true;
		if(combo.length == 2 && combo[0].equals("broadcast"))
			return true;
		if(combo.length == 1 && combo[0].equals("online"))
			return true;
		if(combo.length ==1 && combo[0].equals("block"))
			return true;
		if(combo.length==2 && combo[0].equals("unblock"))
			return true;
		if(combo.length == 1 && combo[0].equals("logou"))
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
