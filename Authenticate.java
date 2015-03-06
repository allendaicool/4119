//package socketProgramming;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;


public class Authenticate {
	public HashMap<String,String> user_passwd ;
	public String name;
	public String password;
	public ConcurrentHashMap<String,Long> current_user = new ConcurrentHashMap<String,Long>();
	
	public Authenticate (ConcurrentHashMap<String,Long> current_user, String name, String password, HashMap<String,String> user_passwd ){
		this.name = name;
		this.password = password;
		this.user_passwd = user_passwd;
		this.current_user = current_user;
	}
	public boolean approved(){
		if(user_passwd.containsKey(this.name) && user_passwd.get(this.name).equals(password)){
			this.current_user.put(this.name, System.currentTimeMillis());
			return true;
		}
		return false;
	}
	public boolean update_user_password(String password){
		this.password = password;
		return approved();
	}
}
