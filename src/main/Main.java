package main;

import host.Host;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Random;

import coord.Coord;

public class Main {
	
	public Main() {
	}
	
	public void genBinFile(int nIntWords) {
		try {
			Random r = new Random(12344);
			File f = new File("input.bin");
			f.delete();
			f.createNewFile();
			RandomAccessFile rf = new RandomAccessFile(f, "rw");
			
			System.out.println("Generatated");
			System.out.println("-----------");
			for(int i = 0; i < nIntWords; i++) {
				int val = r.nextInt();
				rf.writeInt( val );
				System.out.println(val);
			}
			rf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void genBinFile2() {
		try {
			
			int[] vet = {0, 0, 1, 1, 1, 1, 1, -3, 4, 8};
			File f = new File("input2.bin");
			f.delete();
			f.createNewFile();
			RandomAccessFile rf = new RandomAccessFile(f, "rw");
			
			System.out.println("Generatated");
			System.out.println("-----------");
			for(int i = 0; i < 10; i++) {
				int val = vet[i];
				rf.writeInt( val );
//				System.out.println(val);
			}
			rf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void hostApp(String fPath, Integer mod, String coordAddress) {
		Host host = new Host(fPath, mod, coordAddress);
		System.out.println("Setup Host");
		host.setupHost();
		System.out.println("Starting Host");
		//host.startHost();
		host.start();
		host.mainLoop();
		System.out.println("Joining Host");
		host.joinHost();
		try {
			host.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
//		this.printBucketFreq(host.getBuckets(), mod);
	}
	
	public void coordApp() {
		Coord coord = new Coord();
		coord.setupCoord();
		coord.startCoord();
		coord.start();
		coord.mainLoop();
		try {
			coord.join();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void printBucketFreq(ArrayList<Integer> vet, Integer mod) {
		System.out.println("Print buckets freq");
		for(int i=-mod; i<mod; i++) {
			System.out.println(i +" - " + (vet.get(i+mod)) );
		}
	}

	public static void main(String[] args) {
		long t0 = System.currentTimeMillis();
		System.out.println("Main");
		Main m = new Main();
		String[] fPath = {"teste_1G", "input.bin", "input2.bin"};
		Integer mod = (int)(Math.pow(10.0, 1.0));
		
		String coordAddress = "200.239.133.35:6969";
		
		m.hostApp(fPath[2], mod, coordAddress);
		
		System.out.println("END with: " + ((double)(System.currentTimeMillis() - t0)/1000) + "s");
		

	}

}
