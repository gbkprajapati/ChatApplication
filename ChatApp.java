import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

public class ChatApp {

	private int bindingPort;
	private InetAddress bindingHost;
	private Map<Integer, DestinationHost> destinationHostMap = new TreeMap<Integer, DestinationHost>();
	private int clientCounter = 1;
	private Listner serverMessageReciever ;
	
	
	private ChatApp(int bindingPort) {
		super();
		this.bindingPort = bindingPort;
	}

	private String getBindingHost(){
		return bindingHost.getHostAddress();
	}
	
	private int getBindingPort(){
		return bindingPort;
	}
	
	private void initListener(){
		try {
			bindingHost = InetAddress.getLocalHost();
			serverMessageReciever = new Listner();
			new Thread(serverMessageReciever).start();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	private void listDestinations(){
		System.out.println("Id:\tIP Address\tPort");
		if(destinationHostMap.isEmpty()){
			System.out.println("No Destinations available");
		}else{
			for(Integer id : destinationHostMap.keySet()){
				DestinationHost destinationHost = destinationHostMap.get(id);
				System.out.println(id+"\t"+destinationHost.toString());
			}
		}
		System.out.println();
	}
	
	private void help(){
		System.out.println("Following are the command and description  details");
		System.out.println("myip : Display the ip Address of the process");
		System.out.println("myport : Displays the port on which process is listening for incoming connections");
		System.out.println("connect <Destination> <dest-port> : Establish new connection with destination IP and Destination Port");
		System.out.println("list : Display all connection with detsination hosts");
		System.out.println("terminate <connection_id> : close the connection for the destination host wrt to connection id");
		System.out.println("send <connection_id> <message> : Send message with connection id to the destination host and port");
		System.out.println("exit : closes all connections and terminate the process");
		System.out.println("\n");
		
	}
	
	private void sendMessage(String[] commandArg){
		if(commandArg.length > 2){
			try{
				int id = Integer.parseInt(commandArg[1]);
				DestinationHost destinationHost = destinationHostMap.get(id);
					System.out.println("id===="+destinationHostMap.get(id));
				if(destinationHost != null){
					StringBuilder message = new StringBuilder();
					for(int i = 2 ; i < commandArg.length ; i++){
						message.append(commandArg[i]);
						message.append(" ");
					}
					destinationHost.sendMessage(message.toString());
					System.out.println("Mesage send successfully");
				}else
					System.out.println("No Connection available with provided connection id,kindly check list command");
			}catch(NumberFormatException ne){
				System.out.println("Invalid Connection id ,check list command");
			}
		}else{
			System.out.println("Invalid command format , Kindly follow : send <connection id.> <message>");
		}
	}
	private void connect(String[] commandArg){
		if(commandArg != null && commandArg.length == 3){
			try {
				InetAddress remoteAddress = InetAddress.getByName(commandArg[1]);
				int remotePort = Integer.parseInt(commandArg[2]);
				DestinationHost destinationHost = new DestinationHost(remoteAddress,remotePort);
				if(destinationHost.initConnections()){
					destinationHostMap.put(clientCounter, destinationHost);
					clientCounter++;
					System.out.println("Connected successfully");
					
				}else{
				
					System.out.println("Connection not establised, try again");
				}
			}catch(NumberFormatException ne){
				System.out.println("Invalid Remote Host Port, cannot able to connect");
			}catch (UnknownHostException e) {
				System.out.println("Invalid Remote Host Address, cannot able to connect");
			}
		}else{
			System.out.println("Invalid command format , Kindly follow : connect <destination> <port no>");
		}
	}
	private void terminate(String[] commandArg){
		if(commandArg.length == 2){
			try{
				int connectionId = Integer.parseInt(commandArg[1]);
				System.out.println("id=="+connectionId);
				DestinationHost destinationHost = destinationHostMap.get(connectionId);
				if(destinationHost != null){
					destinationHost.closeConnection();
					destinationHostMap.remove(connectionId);
				}else{
					System.out.println("Invalid connection id");
				}
			}catch (NumberFormatException e) {
				System.out.println("Invalid connection id");
			}
		}else
			System.out.println("Invalid command format , Kindly follow : terminate <connection id>");

	}
	private void startSimulation(){
		initListener();
		Scanner scanner = new Scanner(System.in);
		try{
			while(true){
				System.out.print("Enter the command :");
				String command = scanner.nextLine();
				if(command != null && command.trim().length() > 0){
					command = command.trim();
					if(command.equalsIgnoreCase("list")){
						listDestinations();
					}else if(command.equalsIgnoreCase("myip")){
						System.out.println(getBindingHost());
					}else if(command.equalsIgnoreCase("myport")){
						System.out.println(getBindingPort());
					}else if(command.startsWith("connect")){
						String[] commandArg = command.split("\\s+");
						connect(commandArg);
					}
					else if(command.equalsIgnoreCase("help")){
						help();
					}
					else if(command.startsWith("send")){
						String[] commandArg = command.split("\\s+");
						sendMessage(commandArg);
					}
					else if(command.startsWith("terminate")){
						String[] args = command.split("\\s+");
						terminate(args);
					}
					else if(command.equalsIgnoreCase("exit")){
						System.out.println("Good Bye....");
						closeAll();
						System.exit(0);
					}else{
						System.out.println("Invalid command, try again!!!");
						System.out.println();
					}
				}else{
					System.out.println("Invalid command, try again!!!");
					System.out.println();
				}
				
			}
		}finally{
			if(scanner != null)
				scanner.close();
			closeAll();
		}
	}
	private void closeAll(){
		for(Integer id : destinationHostMap.keySet()){
			DestinationHost destinationHost = destinationHostMap.get(id);
			destinationHost.closeConnection();
		}
		destinationHostMap.clear();
		serverMessageReciever.stopSimulation();
	}

	private class Listner implements Runnable{

		BufferedReader in = null;
		Socket socket = null;
		boolean isStopped ;
		List<Clients> clientList = new ArrayList<Listner.Clients>();
		
		private class Clients implements Runnable{

			private BufferedReader in = null;
			private Socket clientSocket = null;
			private boolean isStopped = false;
			private Clients(BufferedReader in,Socket ipAddress) {
				super();
				this.in = in;
				this.clientSocket = ipAddress;
			}

			@Override
			public void run() {
				
				while(!clientSocket.isClosed() && !this.isStopped)
				{
					String st;
					try {
						st = in.readLine();
						System.out.println("Message from " +clientSocket.getInetAddress().getHostAddress()+":"+clientSocket.getPort()+" : "+st);
					} catch (IOException e) {
					}
				}
			}
			
			public void stop(){
				
				if(in != null)
					try {
						in.close();
					} catch (IOException e) {
					}
				
				if(clientSocket != null)
					try {
						clientSocket.close();
					} catch (IOException e) {
					}
				isStopped = true;
				Thread.currentThread().interrupt();
			}
			
		}
		@Override
		public void run() {

			ServerSocket s;
			try {
				s = new ServerSocket(bindingPort);
				System.out.println("Server Waiting For The Client");
				while(!isStopped)
				{
					try {
						socket = s.accept();
						in = new BufferedReader(new
						InputStreamReader(socket.getInputStream()));
						System.out.println(socket.getInetAddress().getHostAddress()+":"+socket.getPort()+" : client successfully connected.");
						Clients clients = new Clients(in, socket);
						new Thread(clients).start();
						clientList.add(clients);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} catch (IOException e1) {
				
			}
			
		}
		
		public void stopSimulation(){
			isStopped = true;
			for(Clients clients : clientList){
				clients.stop();
			}
			Thread.currentThread().interrupt();
		}
		
	}
	
	public static void main(String[] arg){
		
		
		if(arg != null && arg.length > 0){

			try{
				
				int listenPort = Integer.parseInt(arg[0]);
				ChatApp chatApp = new ChatApp(listenPort);
				chatApp.startSimulation();
				
			}catch(NumberFormatException nfe){
				System.out.println("Invalid Argument for the port");
			}

		}else{
			System.out.println("Invalid Args : Java ChatApp <PORT>");
		}
		
	}
}

class DestinationHost{
	
	private InetAddress remoteHost;
	private int remotePort;
	private Socket con ;
	private PrintWriter out;
	private boolean isConnected;
	
	public DestinationHost(InetAddress remoteHost, int remotePort) {
		super();
		this.remoteHost = remoteHost;
		this.remotePort = remotePort;
	}
	
	public boolean initConnections(){
		try {
			this.con = new Socket(remoteHost, remotePort);
			this.out = new PrintWriter(con.getOutputStream(), true);
			isConnected = true;
		} catch (IOException e) {
			
		}
		return isConnected;
	}
	public InetAddress getRemoteHost() {
		return remoteHost;
	}
	public void setRemoteHost(InetAddress remoteHost) {
		this.remoteHost = remoteHost;
	}
	public int getRemotePort() {
		return remotePort;
	}
	public void setRemotePort(int remotePort) {
		this.remotePort = remotePort;
	}
	
	public void sendMessage(String message){
		if(isConnected){
			out.println(message);
		}
	}
	public void closeConnection(){
	
		if(out != null)
			out.close();
		if(con != null){
			try {
				con.close();
			} catch (IOException e) {
			}
		}
		isConnected = false;
	}
	@Override
	public String toString() {
		return  remoteHost + "\t" + remotePort;
	}
	
}
