package host;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.Pipe.SinkChannel;
import java.nio.channels.Pipe.SourceChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Scanner;

import checker.Checker;
import op.Op;
import listner.Listener;
import pipe.SocketPipe;
import reader.Reader;
import worker.Worker;

public class Host extends Thread {

	String fPath;
	Integer mod;
	Integer nPipes;
	
	long t1, t2;

	ServerSocket serverSocket;
	SocketPipe socketPipe;
	Listener listener;
	String myAddress;
	String coordAddress;
	boolean alive;
	Op state;
	ArrayList<String> map;
	static final int port = 6969;

	ArrayList<Integer> interLimits;
	Integer myInf;
	Integer mySup;


	Integer nProc = Runtime.getRuntime().availableProcessors();
	ArrayList<Integer> buckets = null;
	Pipe[] pipes = null;
	Worker[] workers = null;
	SinkChannel[] sinks = null;
	SourceChannel[] sources = null;
	Reader reader;
	Checker checker;


	public Host(String fPath, Integer mod, String coordAdress) {
		this.fPath = fPath;
		this.mod = mod;
		this.nPipes = nProc;
		this.coordAddress = coordAdress;
	}

	public void setNPipes(Integer nPipes) {
		this.nPipes = nPipes;
	}

	public void setupHost() {
		this.alive = true;
		this.myAddress = this.getIp();
		this.state = Op.REGISTER;


		this.buckets = new ArrayList<Integer>(Collections.nCopies((mod*2), 0));
		this.pipes = new Pipe[nPipes];
		this.workers = new Worker[nPipes];
		this.sinks = new SinkChannel[nPipes];
		this.sources = new SourceChannel[nPipes];

		try {
			for(int i=0; i<nPipes; i++) {
				pipes[i] = Pipe.open();				
				sinks[i] = pipes[i].sink();
				sources[i] = pipes[i].source();
				workers[i] = new Worker(sources[i], buckets, mod);
				workers[i].setDaemon(true);
			}
			this.reader = new Reader(fPath, sinks);

			this.serverSocket = new ServerSocket(port);
			this.socketPipe = new SocketPipe();
			this.listener = new Listener(serverSocket, socketPipe);

			this.checker = new Checker(this, reader, workers);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void startHost() {
		for(int i=0; i<nPipes; i++) {
			workers[i].start();
		}
		reader.start();
	}

	public void joinHost() {
		try {
			this.listener.setFinished();
			this.alive = false;
			this.socketPipe.set_finished();
			this.listener.join();
			this.checker.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public ArrayList<Integer> getBuckets() {
		return this.buckets;
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

	public void mainLoop() {
		Scanner scanner = new Scanner(System.in);
		while(alive) {
			String line = scanner.nextLine();

			if(line.matches("(end|quit|exit)")) {
				System.out.println("Saindo...");
				this.joinHost();
				listener.setFinished();
				alive = false;
				continue;
			} else if(line.matches("((\\d){1,3}\\.){3}(\\d){1,3}:(\\d){4,5}")) {
				System.out.println("connectando a " + line);
				this.register(line);
				continue;
			} else if(line.matches("state")){
				System.out.println("state: " + state);
			} else {
				System.err.println("Invalid Command.");
			}
		}
		scanner.close();
	}

	public void register(String coordAddress) {
		this.coordAddress = coordAddress;
		String[] fields = coordAddress.split(":");
		String ip = fields[0];
		Integer port = Integer.parseInt(fields[1]);
		System.out.println("Connect to coord: " + coordAddress);

		try {
			Socket socket = new Socket(ip, port);
			ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
			String address = myAddress + ":" + port;
			Object[] o = {Op.REGISTER, address, nProc};
			out.writeObject(o);
			out.close();
			out = null;
			socket.close();
			socket = null;
			System.out.println("Connection Request Sent to: " + coordAddress);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void defineIntervals() {
		System.out.println("DEFINE INTERVALS");
		Integer sizeM = map.size();
		Integer sizeB = buckets.size();
		this.interLimits = new ArrayList<Integer>(sizeM);
		double s = (double)sizeB / (sizeM);
		for(int i=0; i < (sizeM); i++) {
			Integer index = Math.min(sizeB-1, (int)(i*s));
			interLimits.add(index);
			if(map.get(i).matches(myAddress+":"+port)) {
				myInf = index;
				mySup = Math.min(sizeB-1, (int)(((i+1)*s)-1));
			}
		}

		System.out.println("Interlimits");
		for(int i=0; i < (sizeM); i++) {
			System.out.println(interLimits.get(i));
		}
		System.out.println("My: " + myInf + ", " + mySup);
	}

	public void sendValues() {
		Integer size = map.size();
		String address = myAddress + ":" + port;
		Integer inf;
		Integer sup;
		for(int i=0; i<size; i++) {
			if(map.get(i).matches(myAddress+":"+port)) {
				continue;
			}
			inf = interLimits.get(i);
			if((i+1)<size) {
				sup = interLimits.get((i+1));
			} else {
				sup = buckets.size();
			}
			try {
				String[] fields = map.get(i).split(":");
				String toAddress = fields[0];
				Integer p = Integer.parseInt( fields[1]) ;

				Socket s = new Socket(toAddress, p);
				ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
				System.out.println(inf + " - " + sup);
				Object[] o = {op.Op.SORTING, address, (new ArrayList<Integer>(buckets.subList(inf, sup))) };
				out.writeObject(o);

				out.close();
				s.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		this.state = op.Op.COMPLETED;
		this.callBackCoord();
	}
	
	public void mergeBucket(ArrayList<Integer> vet) {
		System.out.println(">>My: " + mySup + ", " + myInf);
		if(vet.size() != ((mySup-myInf)+1) ) {
			System.out.println("Tam incompativel: " + vet.size() + ", " + ((mySup-myInf)+1));
		}
		for(int i=myInf, j=0; i<=mySup; i++, j++) {
			Integer v = buckets.get(i) + vet.get(j);
			buckets.set(i, v);
		}
	}

	public void callBackCoord() {
		String[] fields = coordAddress.split(":");
		String toAddress = fields[0];
		Integer p = Integer.parseInt(fields[1]);
		try {
			Socket s = new Socket(toAddress, p);
			
			ObjectOutputStream out = new ObjectOutputStream( s.getOutputStream() );
			Object[] o = {state, (myAddress+":"+port), null};
			
			out.writeObject(o);
			
			out.close();
			s.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void writeOnFile() {
		try {
			String fName = "output.bin";
			File f = new File(fName);
			f.delete();
			f.createNewFile();
			RandomAccessFile rf = new RandomAccessFile(f, "rw");
			
			System.out.println("Writing on File: INIT");
			System.out.println("-----------");
			for(int i=myInf; i<=mySup; i++) {
				int val = buckets.get(i);
				ByteBuffer bb = ByteBuffer.allocate(val*4);
				bb.limit(val*4);
				bb.clear();
//				System.out.println(">>>!!!" + val + ", " + bb.limit() );
				int v = (i - this.mod);
//				System.out.println(i + " - " + val);
				for(int j=0; j<val; j++) {
//					System.out.println(">>>" + j);
					bb.putInt(v);
				}
				rf.write(bb.array());
				
				bb.flip();
			}
			rf.close();
			System.out.println("Writing on File: DONE");
			System.out.println("-----------");
			
//			RandomAccessFile f2 = new RandomAccessFile(f, "r");
//			byte[] b = new byte[(int)f2.length()];
//			f2.read(b);
//			ByteBuffer bb = ByteBuffer.allocate((int)f2.length());
//			bb.put(b);
//			bb.flip();
//			System.out.println("saida");
//			while(bb.hasRemaining()) {
//				System.out.println(">" + bb.getInt() );
//			}
//			f2.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		System.out.println("HOST RUNNING");
		listener.start();
		if(coordAddress != null) {
			this.register(coordAddress);
		}
		while(alive) {
			Socket s = socketPipe.read_buffer();
			if(s == null) {
				continue;
			}
			try {
				ObjectInputStream in;
				Object[] o;
				switch (state) {
				case REGISTER:

					in = new ObjectInputStream(s.getInputStream());
					o = (Object[])in.readObject();
					in.close();
					
					if((op.State)o[0] == op.State.START_SORT) {
						System.out.println("START_SORT>>>");
						this.t1 = System.currentTimeMillis();
						String toAddress = ((String)o[1]).split(":")[0];
						Integer p = Integer.parseInt( ((String)o[1]).split(":")[1] );
						this.map = (ArrayList<String>)o[2];
						this.defineIntervals();

						Socket s2 = new Socket(toAddress, p);
						ObjectOutputStream out = new ObjectOutputStream(s2.getOutputStream());
						Object[] response = {Op.SORTING, (myAddress+":"+port), null};
						out.writeObject(response);
						s2.close();
						state = Op.SORTING;
						this.startHost();
						this.checker.start();
						System.out.println("Sended response, status changed to: " + state);
					} else {
						System.out.println("Invalid OpCode: " + o[0]);
					}
					
					break;

				case SORTING:
				case COMPLETED:
					in = new ObjectInputStream(s.getInputStream());
					o = (Object[])in.readObject();
					in.close();
					
					if((op.Op)o[0] == op.Op.SORTING) {
						System.out.println("Merging from: " + (String)o[1]);
						ArrayList<Integer> vet = (ArrayList<Integer>)o[2];
						mergeBucket( vet );
					} else if((op.Op)o[0] == op.Op.END) {
						this.t2 = System.currentTimeMillis();
						System.out.println("Sorting Terminate With: " + (double)(t2-t1)/1000 + "s" );
						
						this.t1 = System.currentTimeMillis();
						this.writeOnFile();
						this.t2 = System.currentTimeMillis();
						System.out.println("Writing Terminate With: " + (double)(t2-t1)/1000 + "s" );
						return;
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
