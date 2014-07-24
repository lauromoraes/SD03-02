package checker;

import host.Host;
import reader.Reader;
import worker.Worker;

public class Checker extends Thread {
	Host host;
	Reader reader;
	Worker[] workers;
	
	public Checker(Host host, Reader reader, Worker[] workers) {
		this.host = host;
		this.reader = reader;
		this.workers = workers;
	}
	
	@Override
	public void run() {
		try {
			reader.join();
			System.out.println("Reader finished.");
			for(Worker w : workers) {
				w.kill();
			}
			int i=0;
			for(Worker w : workers) {
				w.join(1000);
				System.out.println("Worker "+ (i++) +" finished.");
			}
			System.out.println("All threads of sorting has finished.");
			
			this.host.sendValues();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
