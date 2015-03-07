//package socketProgramming;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
//import stat.pac.java;

public class Server {
	public static List<Socket> currentSockets = new ArrayList<Socket>();
	public static List<String> currentUsers = new ArrayList<String>();
	public static int port_num;
	public static HashMap<String,String> user_passwd = new HashMap<String,String>();
	public static ServerSocket server_socket;
	public static ConcurrentHashMap<String,Long> current_user = new ConcurrentHashMap<String,Long>();
	public static ConcurrentHashMap<String,Long> blocked_by_server = new ConcurrentHashMap<String,Long>();
	public static ConcurrentHashMap<String,String> current_user_ip = new ConcurrentHashMap<String,String>();
	public static ConcurrentHashMap<String,IP_port_tuple> user_ip_port = new ConcurrentHashMap<String,IP_port_tuple>();
	public static ConcurrentHashMap<String, ArrayList<String>> off_line_msg = new ConcurrentHashMap<String,ArrayList<String>>();
	public static ConcurrentHashMap<String,ArrayList<String>> user_black_list = new ConcurrentHashMap<String,ArrayList<String>>();
	public static void main(String[] args) throws Exception{
		if(args.length != 1){
			System.out.println("invalid number of arguments");
			return;
		}
		port_num = Integer.parseInt(args[0]);
		handle_user_password();
		
		Socket client_socket = null;
		server_socket =  new ServerSocket(port_num);
		//server_socket.getLocalSocketAddress()
		InetAddress addr = server_socket.getInetAddress();
		System.out.println(addr.getHostAddress());
		boolean find = true;
		while (true && find ) {
			
            client_socket = server_socket.accept();
            
            System.out.println("server has accepted connection");
            //System.out.println("local address is " + client_socket.getLocalAddress().getHostAddress());
			//System.out.println("local port num is " + client_socket.getLocalPort());
			//System.out.println("remote ip address is " + client_socket.getInetAddress().getHostAddress());
			//System.out.println("remote port num is " + client_socket.getPort());
			
            
            server_handler handle = new server_handler(user_black_list,off_line_msg,current_user_ip,user_ip_port,user_passwd,blocked_by_server,current_user,client_socket);
           
            handle.start();
            
            //System.out.println(socketName + " has connected!");
        }
		server_socket.close();
		
	}


	@SuppressWarnings("resource")
	public static void handle_user_password() throws Exception{
		String fileName = "credentials.txt";
		BufferedReader reader = null;
		try {
			File file = new File(fileName);

			reader = new BufferedReader(new FileReader(file));
			String line = null;
			while ((line =reader.readLine())!=null){
				String [] user_password = line.split("\\s+");
				if(user_passwd.containsKey(user_password[0]))
					throw new Exception("duplicate user name");
				user_passwd.put(user_password[0], user_password[1]);
			}
			reader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
