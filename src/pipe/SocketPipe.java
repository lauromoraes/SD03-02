package pipe;

import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;


public class SocketPipe {
	
	protected ConcurrentLinkedQueue<Socket> buffer;
	protected boolean finished;
	
	public SocketPipe() {
		finished = false;
		buffer = new ConcurrentLinkedQueue<Socket>();
	}

	public synchronized void write_buffer(Socket data) {
		if(data != null) {			
			buffer.offer(data);
			notify();
		} else {
			System.err.println("Null data: fail to insert on buffer.");
		}
	}

	public synchronized Socket read_buffer() {
		Socket data = null;
		while(!finished) {
			if(!is_empty()) {
				data = buffer.poll();
				return data;
			} else {
				if(!is_finished()) {
					try {
						wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return data;
	}

	public synchronized boolean is_empty() {
		return buffer.isEmpty();
	}

	public synchronized boolean is_finished() {
		return finished;
	}

	public synchronized void set_finished() {
		finished = true;
		notifyAll();
	}

	public synchronized int buffer_size() {
		int t = buffer.size();
		return t;
	}

	public ConcurrentLinkedQueue<Socket> get_all_buffer() {
		synchronized (this) {
			return this.buffer;
		}
	}


}
