package coord;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Scanner;

import listner.Listener;
import pipe.SocketPipe;

public class Coord extends Thread {

	String myAddress;
	HashMap<String, op.Op> map;
	op.State state;
	Listener listener;
	SocketPipe socketPipe;
	ServerSocket serverSocket;
	boolean alive;
	static final int port = 6969;
	long t1, t2;
	
	public Coord() {
		this.alive = true;
		this.map = new HashMap<>();
		this.myAddress = this.getIp();
	}
	
	public void setupCoord() {
		try {
			this.state = op.State.MAPPING;
			this.serverSocket = new ServerSocket(port);
			this.socketPipe = new SocketPipe();
			this.listener = new Listener(serverSocket, socketPipe);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void startCoord() {
		listener.start();
	}
	
	public String getIp() {
		try {
			Enumeration<NetworkInterface> i = NetworkInterface.getNetworkInterfaces();
			while(i.hasMoreElements()) {
				NetworkInterface n = i.nextElement();
				Enumeration<InetAddress> e = n.getInetAddresses();
				while(e.hasMoreElements()) {
					InetAddress a = e.nextElement();
					if(a instanceof Inet4Address && !a.getHostAddress().startsWith("127")) {
						return a.getHostAddress();
					}
				}
			}
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
		return null;
	}
	
	public void startSorting() {
		for(String host_address : map.keySet()) {
			String[] fields = host_address.split(":");
			String ip = fields[0];
			Integer port = Integer.parseInt(fields[1]);
			try {
				System.out.println("enviando requisicao para: " + host_address);
				Socket s = new Socket(ip, port);
				ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
				String address = this.myAddress + ":" + port;
				Object[] o = {op.State.START_SORT, address, ( new ArrayList<String>( map.keySet() ))};
				out.writeObject(o);
				out.close();
				s.close();
				System.out.println("requisicao enviada para: " + host_address);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	public void mainLoop() {
		Scanner scanner = new Scanner(System.in);
		while(alive) {
			String line = scanner.nextLine();

			if(line.matches("(end|quit|exit)")) {
				System.out.println("Saindo...");
				listener.setFinished();
				socketPipe.set_finished();
				alive = false;
				continue;
			} else if(line.matches("((\\d){1,3}\\.){3}(\\d){1,3}:(\\d){4,5}")) {
				System.out.println("connectando a " + line);
				continue;
			} else if(line.matches("list")){
				System.out.println("hosts");
				for(String s : map.keySet()) {
					System.out.println("\t"+s+" >> " + map.get(s));
				}
			} else if(line.matches("state")){
				System.out.println("state: " + state);
			} else if(line.matches("start")){
				this.t1 = System.currentTimeMillis();
				if(state == op.State.MAPPING) {
					state = op.State.START_SORT;
					System.out.println("state mudando para: " + state);
					startSorting();
				} else {
					System.err.println("Command on wrong state: " + state);
				}
			} else {
				System.err.println("Invalid Command.");
			}
		}
		scanner.close();
	}
	
	public boolean terminateHosts() {
		for(op.Op o : map.values()) {
			if(o != op.Op.COMPLETED) {
				return false;
			}
		}
		Object[] o = {op.Op.END, (myAddress+":"+port), null};
		for(String a : map.keySet()) {
			String[] fields = a.split(":");
			String toAddress = fields[0];
			Integer p = Integer.parseInt(fields[1]);
			try {
				Socket s = new Socket(toAddress, p);
				ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
				out.writeObject(o);
				out.close();
				s.close();
				System.out.println("finalizacao enviada para: " + a);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return true;
	}
	
	@Override
	public void run() {
		while(alive) {
			Socket s = this.socketPipe.read_buffer();
			if(s == null) {
				continue;
			}
			try {
				ObjectInputStream in;
				Object[] o;
				switch (state) {
				case MAPPING:
					in = new ObjectInputStream(s.getInputStream());
					o = (Object[]) in.readObject();
					in.close();
					if((op.Op)o[0] == op.Op.REGISTER) {
						String hostAddress = (String)o[1];
						Integer process = (Integer)o[2];
						System.out.println("host register: " + hostAddress + " - " + process);
						map.put(hostAddress, op.Op.REGISTER);
					} else {
						System.out.println("Invalid OpCode: " + o[0]);
					}
					break;
					
				case START_SORT:
					in = new ObjectInputStream(s.getInputStream());
					o = (Object[]) in.readObject();
					in.close();
					if((op.Op)o[0] == op.Op.SORTING) {
						String hostAddress = (String)o[1];
						System.out.println("host sorting: " + hostAddress);
						map.put(hostAddress, op.Op.SORTING);
					} else if((op.Op)o[0] == op.Op.COMPLETED) {
						String hostAddress = (String)o[1];
						System.out.println("host completed: " + hostAddress);
						map.put(hostAddress, op.Op.COMPLETED);
						if( this.terminateHosts() ) {
							this.t2 = System.currentTimeMillis();
							System.out.println("Terminated with: " + (double)(t2-t1)/1000 + "s");
							return;
						}
					} else {
						System.out.println("Invalid OpCode: " + o[0]);
					}
					break;

				default:
					System.err.println("Invalid State Code: " + state);
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
