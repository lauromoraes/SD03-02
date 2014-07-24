package reader;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe.SinkChannel;

public class Reader extends Thread {
	
	String fPath;
	FileInputStream fInput;
	SinkChannel[] sink;
	ByteBuffer bBuffer;
	byte[] block;

	long bSize = 64000;
	
	public Reader(String fPath, SinkChannel[] sink) {
		this.fPath = fPath;
		this.sink = sink;
	}
	
	public void setBufferSize(long size) {
		this.bSize = size;
	}

	@Override
	public void run() {	
		int sinkPos = 0;
		int sinkTot = this.sink.length;
		
		block = new byte[(int)bSize];
		
		try {
			this.fInput = new FileInputStream(fPath);
			this.bBuffer = ByteBuffer.allocate((int)this.bSize);
			
			int rc = fInput.read(block);
			while(rc != -1) {
				
				bBuffer.clear();
				bBuffer.put(block, 0 ,rc);
				bBuffer.flip();
				
				while(bBuffer.hasRemaining()) {
					bBuffer.limit(rc);
					int index = (sinkPos++)%sinkTot;
//					System.out.println(Thread.currentThread().getName() + " - put on " + index);
					sink[ index ].write(bBuffer);
				}
				
				rc = fInput.read(block);
			}
			System.out.println("All File Readed");
			for(SinkChannel s : sink) {
				s.close();
			}
			this.fInput.close();
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
}
