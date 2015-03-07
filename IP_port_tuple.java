//package socketProgramming;

public class IP_port_tuple {
    private String hostAddress;
    private int port_num;
    
    public IP_port_tuple(String hostAddress, int port_num){
    	this.hostAddress = new String(hostAddress);
    	this.port_num = port_num;
    }
    public int get_port (){
    	return this.port_num;
    }
    public String get_host_address(){
    	return this.hostAddress;
    }
}
