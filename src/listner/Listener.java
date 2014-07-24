package listner;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import pipe.SocketPipe;

public class Listener extends Thread {

	public ServerSocket listener;
	public SocketPipe socket_pipe;
	public boolean isFinished;

	public Listener(ServerSocket listener, SocketPipe socket_pipe) {
		isFinished = false;
		this.listener = listener;
		this.socket_pipe = socket_pipe;
	}
	
	public void setFinished() {
		this.isFinished = true;
	}

	@Override
	public void run() {
		while(!isFinished) {
			try {
				listener.setSoTimeout(2000);
				Socket s = listener.accept();
				socket_pipe.write_buffer(s);
				
			} catch (IOException e) {
//				e.printStackTrace();
			}
		}
		if(listener != null) {
			try {
				listener.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
