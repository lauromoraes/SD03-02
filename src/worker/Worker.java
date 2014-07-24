package worker;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe.SourceChannel;
import java.util.ArrayList;

public class Worker extends Thread {
	
	SourceChannel source;
	ArrayList<Integer> buckets;
	Integer mod;
	boolean alive;
	
	public Worker(SourceChannel source, ArrayList<Integer> buckets, Integer mod) {
		this.source = source;
		this.buckets = buckets;
		this.mod = mod;
	}
	
	public void kill() {
		this.alive = false;
	}

	@SuppressWarnings("unused")
	@Override
	public void run() {
		this.alive = true;
		ByteBuffer bb = ByteBuffer.allocate(64);
		int cnt = 0;

		try {
			while(alive) {
				bb.clear();
				while(source.read(bb) > 0) {
					bb.flip();
					while(bb.hasRemaining()) {
						int i = bb.getInt();
						int index = (i % mod) + mod;
//						System.out.println(index);
						synchronized (this.buckets.get(index)) {
							this.buckets.set(index, this.buckets.get(index)+1);
						}
//						System.out.println(alive + ", " + (cnt++) + ", "+Thread.currentThread().getName() + ">>" + i);
					}
					bb.clear();
				}
				if(alive) {
					try {
//						System.out.println("Sleeping.");
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			source.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
